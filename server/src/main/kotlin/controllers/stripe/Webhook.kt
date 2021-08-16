/*
 * Copyright (c)  2021 ASDF Dev Pte. Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package controllers.stripe

import com.google.firebase.auth.FirebaseAuth
import com.stripe.model.*
import com.stripe.model.checkout.Session
import com.stripe.net.Webhook
import com.stripe.param.ChargeListParams
import controllers.BaseHandler
import data.Firestore
import io.javalin.http.Context
import models.Customer
import java.lang.Exception
import java.time.LocalDateTime
import java.time.ZoneOffset

class Webhook: BaseHandler() {

    // $ stripe listen --forward-to localhost:8080/api/v1/stripe/webhook
    // $ stripe trigger charge.succeeded
    // $ stripe trigger subscription_schedule.created

    override fun handle(context: Context) {
        val stripePayload = context.body()
        val userAgent = context.userAgent()
        val stripeHeader = context.header("Stripe-Signature")
        if(!userAgent.isNullOrBlank() &&
            userAgent.contentEquals("Stripe/1.0 (+https://stripe.com/docs/webhooks)") &&
            stripePayload.isNotBlank() && !stripeHeader.isNullOrBlank()){
            try {
                val event = Webhook.constructEvent(stripePayload, stripeHeader, Constants.STRIPE_WEB_HOOK_KEY)
                event(event)
                context.status(200)
            } catch (exception: Exception){
                if(Constants.IS_DEBUG){
                    exception.printStackTrace()
                }
            }

        }
    }

    private fun event(event: Event){
        when {
            event.type?.contentEquals("charge.succeeded") == true -> {
                val chargeSucceed = Charge.GSON.newBuilder()
                    .setLenient()
                    .create()
                    .fromJson(event.dataObjectDeserializer.rawJson, Charge::class.java)
                chargeUser(chargeSucceed)
            }
            event.type?.contentEquals("charge.refunded") == true -> {
                val chargeRefund = Charge.GSON.newBuilder()
                    .setLenient()
                    .create()
                    .fromJson(event.dataObjectDeserializer.rawJson, Charge::class.java)
                refund(chargeRefund)
            }
            event.type?.contentEquals("invoice.paid") == true -> {
                val invoiceModel = Invoice.GSON.newBuilder()
                    .setLenient()
                    .create()
                    .fromJson(event.dataObjectDeserializer.rawJson, Invoice::class.java)
                invoice(invoiceModel)
            }
            event.type?.contentEquals("checkout.session.completed") == true -> {
                val checkoutModel = Session.GSON.newBuilder()
                    .setLenient()
                    .create()
                    .fromJson(event.dataObjectDeserializer.rawJson, Session::class.java)
                checkoutSession(checkoutModel)
            }
        }
    }

    // 2 different checkout session; subscription and non-subscription
    private fun checkoutSession(checkout: Session){
        val subscriptionId = checkout.subscription
        if(subscriptionId != null){
            val firebaseUserId = checkout.clientReferenceId
            val session = Session.retrieve(checkout.id)
            val subscription = Subscription.retrieve(subscriptionId)
            val product = subscription.items.data[0].plan.product
            Firestore().saveUserPurchase(firebaseUserId, session.id,
                Customer(subscription.startDate, subscription.currentPeriodEnd,
                    "stripe", subscriptionId, product))
        } else {
            // Non subscription
            val firebaseUserId = checkout.clientReferenceId
            val charge = Charge.list(ChargeListParams.builder().setCustomer(checkout.customer).build()).data[0]
            val endDate = LocalDateTime.now().plusMonths(6).toEpochSecond(ZoneOffset.UTC)
            Firestore().saveUserPurchase(firebaseUserId, charge.id,
                Customer(charge.created, endDate, "stripe", "", "ONE_TIME"))
        }
    }

    private fun invoice(invoice: Invoice){
        if(invoice.paid && invoice.amountRemaining == 0L){
            val firebaseUser = FirebaseAuth.getInstance().getUserByEmail(invoice.customerEmail)
            val firebaseUserId = firebaseUser.uid
            val product = invoice.lines.data[0].plan.product
            val invoiceId = invoice.id
            val subscription = invoice.lines.data[0].subscription
            val subscriptionAttribute = Subscription.retrieve(subscription)
            Firestore().saveUserPurchase(firebaseUserId, invoiceId,
                Customer(subscriptionAttribute.startDate, subscriptionAttribute.currentPeriodEnd,
                    "stripe", subscription, product))
        }
    }

    private fun chargeUser(charge: Charge){
        val firebaseUser = FirebaseAuth.getInstance().getUserByEmail(charge.receiptEmail)
        val firebaseUserId = firebaseUser.uid
        val endDate = LocalDateTime.now().plusMonths(6).toEpochSecond(ZoneOffset.UTC)
        Firestore().saveUserPurchase(firebaseUserId, charge.id,
            Customer(charge.created, endDate, "stripe", "", "ONE_TIME"))
    }

    private fun refund(charge: Charge){
        val firebaseUser = FirebaseAuth.getInstance().getUserByEmail(charge.receiptEmail)
        val firebaseId = firebaseUser.uid
        Firestore().refundUser(firebaseId, charge.id)
    }
}
