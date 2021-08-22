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

package controllers.paypal

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.cloud.FirestoreClient
import com.paypal.core.PayPalEnvironment
import com.paypal.core.PayPalHttpClient
import com.paypal.orders.OrderRequest
import com.paypal.orders.OrdersCaptureRequest
import data.Firestore
import io.javalin.http.Context
import io.javalin.http.Handler
import models.Customer
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class ConfirmOrder(private val payPalEnvironment: PayPalEnvironment): Handler {

    override fun handle(context: Context) {
        val firebaseId = context.header("X-FIREBASE-ID", String::class.java).get()
        val orderId =  context.formParam("orderId", String::class.java).get()
        if(firebaseId.isNotBlank()){
            val firebaseUser = FirebaseAuth.getInstance().getUser(firebaseId)
            if (!firebaseUser.isDisabled) {
                val client = PayPalHttpClient(payPalEnvironment)
                val request = OrdersCaptureRequest(orderId)
                request.requestBody(OrderRequest())
                client.execute(request)
                val timeNow = Instant.now().epochSecond
                val endDate = LocalDateTime.now().plusMonths(6).toEpochSecond(ZoneOffset.UTC)
                val customer = Customer(
                    timeNow, endDate, "paypal", "", "ONE_TIME"
                )
                Firestore().saveUserPurchase(firebaseId, orderId, customer)
                context.status(200).json(customer)
            }
        }
    }
}