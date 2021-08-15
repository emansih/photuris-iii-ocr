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
import com.google.firebase.cloud.FirestoreClient
import data.Firestore
import io.javalin.http.Context
import io.javalin.http.Handler
import models.Customer
import java.time.Instant
import java.util.*

class GetUserStatus: Handler {

    override fun handle(context: Context) {
        val firebaseId = context.header("X-FIREBASE-ID", String::class.java).get()
        if(firebaseId.isEmpty()){
            context.status(400)
        } else {
            val firebaseUser = FirebaseAuth.getInstance().getUser(firebaseId)
            if(firebaseUser.isDisabled){
                context.status(400)
            } else {
                val localTime = Instant.now().toEpochMilli()
                val userTimeStamp = firebaseUser.userMetadata.creationTimestamp
                val timeDifference = localTime.minus(userTimeStamp)
                /* User created within last 11 seconds
                 * Ideally, we should be using Firebase Functions but it requires me to
                 * use a credit card :(
                 */
                val customer = if(timeDifference <= 11000L){
                    // New user
                    freeTrial(firebaseId)
                } else {
                    isUserSubscribed(firebaseId)
                }
                context.status(200).json(customer)
            }
        }
    }

    private fun freeTrial(firebaseUserId: String): Customer{
        val randomString = UUID.randomUUID().toString().dropLast(10)
        val timeNow = Instant.now().epochSecond
        // 1 week = 604800 seconds
        val trialEnds = timeNow.plus(604800)
        val customer = Customer(timeNow, trialEnds, "Free Trial", "", "FREE TRIAL")
        Firestore().saveUserPurchase(firebaseUserId, randomString,customer)
        return customer
    }


    private fun isUserSubscribed(firebaseId: String): Customer{
        val db = FirestoreClient.getFirestore()
        val collection = db.collection("orders/purchased/$firebaseId")
            .whereGreaterThanOrEqualTo("endTime",
                System.currentTimeMillis().div(1000))
            .get().get()
        if(!collection.isEmpty) {
            val data = collection.documents[0]
            val orderAttribute = data.toObject(Customer::class.java)
            return Customer(orderAttribute.startTime, orderAttribute.endTime, orderAttribute.paymentSource)
        }
        return Customer(0, 0, "")
    }
}