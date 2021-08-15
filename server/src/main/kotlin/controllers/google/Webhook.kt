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

package controllers.google

import com.google.api.services.androidpublisher.model.SubscriptionPurchasesAcknowledgeRequest
import com.squareup.moshi.Moshi
import controllers.BaseHandler
import data.Firestore
import io.javalin.http.Context
import models.Customer
import models.google.purchase.PurchaseModel
import models.google.webhook.SubscriptionSuccessModel
import utils.GoogleExtension
import utils.PlaySignatureVerifier
import utils.isUserFromGooglePlay
import utils.isUserSubscribed
import java.util.*

class Webhook: BaseHandler()  {

    override fun handle(context: Context) {
        parseBody(context.body())
    }

    private fun parseBody(body: String){
        val subscription = Moshi.Builder().build()
            .adapter(SubscriptionSuccessModel::class.java)
            .fromJson(body)
        if(subscription != null){
            val playSignatureVerifier = PlaySignatureVerifier()
            val messageToDecode = playSignatureVerifier.decodeData(subscription.message.data)
            val purchase = Moshi.Builder().build()
                .adapter(PurchaseModel::class.java)
                .fromJson(messageToDecode)
            if(purchase != null){
                confirmPurchase(purchase)
            }
        }
    }

    private fun writeToDb(firebaseId: String, purchaseToken: String,
                          subscriptionId: String, orderId: String?,
                          startTime: Long, endTime: Long){
        val customerModel =  Customer(startTime, endTime, "googlePlay",
            purchaseToken, subscriptionId)
        val productOrderId = orderId ?: UUID.randomUUID().toString().dropLast(10)
        Firestore().saveUserPurchase(firebaseId, productOrderId, customerModel)
    }

    private fun confirmPurchase(purchaseModel: PurchaseModel){
        val pub = GoogleExtension.getGoogleCredentials()
        try {
            val packageName = purchaseModel.packageName
            val subscriptionId = purchaseModel.subscriptionNotification.subscriptionId
            val purchaseToken = purchaseModel.subscriptionNotification.purchaseToken

            val purchase = pub.purchases().subscriptions().get(
                packageName,
                subscriptionId,
                purchaseToken
            ).execute()


            val firebaseId = purchase["obfuscatedExternalAccountId"].toString()
            val customer = Firestore().getUserSubscription(firebaseId).customer

            // User is attempting to buy from Google Play even though user is subscribed in stripe
            if(purchase.cancelReason == null && customer.isUserSubscribed()){
                pub.purchases().subscriptions().revoke(
                    packageName,
                    subscriptionId,
                    purchaseToken
                ).execute()
            } else {
                if(purchase.cancelReason == null){
                    pub.purchases().subscriptions().acknowledge(
                        packageName,
                        subscriptionId,
                        purchaseToken, SubscriptionPurchasesAcknowledgeRequest()
                    ).execute()

                    /* Note: Do not use orderId to check for duplicate purchases or as a primary key
                     * in your database, as not all purchases are guaranteed to generate an orderId.
                     * In particular, purchases made with promo codes do not generate an orderId.
                     * https://developer.android.com/google/play/billing/security
                     */
                    writeToDb(firebaseId, purchaseToken,
                        subscriptionId,
                        purchase.orderId,
                        purchase.startTimeMillis.div(1000),
                        purchase.expiryTimeMillis.div(1000))

                }
            }

            if(purchase.cancelReason != null &&
                customer.isUserSubscribed() &&
                customer.isUserFromGooglePlay()){
                    Firestore().cancelSubscription(firebaseId, purchaseToken,
                        purchase.startTimeMillis.div(1000),
                        purchase.expiryTimeMillis.div(1000),
                        purchase.cancelSurveyResult.userInputCancelReason ?: "Unspecified")
            }

        } catch (exception: Exception){
            exception.printStackTrace()
        }
    }
}