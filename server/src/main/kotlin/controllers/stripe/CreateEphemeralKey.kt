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

import CustomerNotFoundException
import network.StripeUtils
import com.google.firebase.auth.FirebaseAuth
import controllers.BaseHandler
import data.Firestore
import io.javalin.http.Context
import utils.isUserSubscribed

class CreateEphemeralKey: BaseHandler() {

    override fun handle(context: Context) {
        val stripeApiVersion = context.formParam("stripeApiVersion", String::class.java).get()
        val firebaseId = context.header("X-FIREBASE-ID", String::class.java).get()
        if(firebaseId.isNotBlank()){
            try {
                val firebaseUser = FirebaseAuth.getInstance().getUser(firebaseId)
                val firebaseEmail = firebaseUser.email
                val firebaseDisplayName = firebaseUser.displayName
                var customerId: String? = null
                try {
                    val customerDetails = StripeUtils().getCustomerByEmail(firebaseUser.email)
                    customerId = customerDetails.id
                } catch (exception: CustomerNotFoundException){
                    if(firebaseDisplayName.isEmpty() || firebaseEmail.isEmpty()){
                        context.status(400).json("More information required")
                    } else {
                        val customerDetails = StripeUtils().createCustomer(firebaseUser.email, firebaseUser.displayName)
                        customerId = customerDetails.id
                    }
                }
                if (!firebaseUser.isDisabled) {
                    if(customerId != null){
                        val getUserSubscription = Firestore().getUserSubscription(firebaseId).customer
                        if(getUserSubscription.isUserSubscribed()){
                            context.status(400).json("You are subscribed already")
                        } else {
                            val key = StripeUtils().createEphemeralKey(customerId, stripeApiVersion)
                            context.status(200).json(key.rawJson)
                        }
                    } else {
                        context.status(400).json("More information required")
                    }
                } else {
                    context.status(400).json("This account has been disabled")
                }
            } catch (exception: Exception){
                context.status(400).json("This account is not found")
            }
        } else {
            context.status(400).json("This account is not found")
        }
    }
}