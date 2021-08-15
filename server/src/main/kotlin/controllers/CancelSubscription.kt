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

package controllers

import com.google.firebase.auth.FirebaseAuth
import data.Firestore
import io.javalin.http.Context
import data.PaymentGateway
import utils.GoogleExtension
import utils.isUserFromGooglePlay
import utils.isUserFromStripe
import utils.isUserSubscribed

class CancelSubscription: BaseHandler() {

    override fun handle(context: Context) {
        val firebaseId = context.header("X-FIREBASE-ID", String::class.java).get()
        if(firebaseId.isNotBlank()){
            try {
                FirebaseAuth.getInstance().getUser(firebaseId)
                val userSubscription = Firestore().getUserSubscription(firebaseId)
                val subscribed = userSubscription.customer.isUserSubscribed()
                if(subscribed){
                    if(userSubscription.customer.isUserFromStripe()){
                        if(userSubscription.isSubscription){
                            unsubscribeStripe(userSubscription.customer.purchaseToken)
                        }
                    } else if(userSubscription.customer.isUserFromGooglePlay()){
                        unsubscribeGooglePlay(userSubscription.customer.purchaseToken)
                    }
                    context.status(200)
                } else {
                    context.status(400)
                }
            } catch (exception: Exception){
                context.status(400)
            }
        }
    }

    private fun unsubscribeStripe(token: String){
        val stripe = PaymentGateway()
        stripe.unSubscribe(token)
    }

    private fun unsubscribeGooglePlay(token: String){
        val pub = GoogleExtension.getGoogleCredentials()
        val purchase = pub.purchases().subscriptions().cancel(
            Constants.PACKAGE_NAME, "ocrsubscription", token)
        purchase.execute()
    }
}