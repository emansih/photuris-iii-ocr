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
import data.PaymentGateway
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserRecord
import controllers.BaseHandler
import data.Firestore
import io.javalin.http.Context
import models.HttpResponse
import utils.GeoLite
import utils.findSchoolNames
import utils.isAcademic
import utils.isGoodSchool

class PurchaseGoods: BaseHandler() {

    private val stripeUtils by lazy { PaymentGateway() }

    override fun handle(context: Context) {
        val ipAddress = context.ip()
        val firebaseId = context.header("X-FIREBASE-ID", String::class.java).get()
        val currencyCode = context.formParam("currencyCode", String::class.java).get()
        val paymentMethod = context.formParam("paymentMethod") ?: ""
        val paymentMethodType = context.formParam("paymentMethodType") ?: ""
        val isRecurring = context.formParam("isRecurring", Boolean::class.java).get()
        if(firebaseId.isNotBlank() || currencyCode.isNotBlank()){
            try {
                val firebaseUser = FirebaseAuth.getInstance().getUser(firebaseId)
                val firebaseEmail = firebaseUser.email
                val firebaseDisplayName = firebaseUser.displayName
                var customerId: String? = null
                if (!firebaseUser.isDisabled){
                    try {
                        val customerDetails = stripeUtils.getCustomerByEmail(firebaseUser.email)
                        customerId = customerDetails.id
                    } catch (exception: CustomerNotFoundException){
                        if(firebaseEmail.isEmpty() || firebaseDisplayName.isEmpty()){
                            context.status(400).json(HttpResponse("Additional user information required"))
                        } else {
                            val customerDetails = stripeUtils.createCustomer(firebaseUser.email, firebaseUser.displayName)
                            customerId = customerDetails.id
                        }
                    }
                    if(customerId != null){
                        if(paymentMethodType.isBlank()){
                            // Credit Card
                            if(isRecurring){
                                recurringSubscription(ipAddress, firebaseUser, currencyCode,
                                    customerId, paymentMethod, context)
                            } else {
                                purchase(firebaseUser, currencyCode, customerId, paymentMethod, ipAddress, context)
                            }
                        } else {
                            // Alipay, GrabPay, Giropay, Ban Contact, iDEAL, Przelewy24, EPS
                            purchaseWithPaymentMethodType(firebaseUser, currencyCode,
                                customerId, paymentMethodType, ipAddress, context)
                        }
                    }
                }
            } catch (exception: Exception){
                context.status(400).json(HttpResponse("User not found"))
            }
        }
    }

    private fun recurringSubscription(ipAddress: String, userRecord: UserRecord, currencyCode: String,
                                      customerId: String, paymentMethod: String, context: Context){
        try {
            stripeUtils.subscribe(currencyCode, getPrice(ipAddress, currencyCode, userRecord),
                customerId, 6, paymentMethod)
            context.status(200)
        } catch (exception: Exception){
            context.status(400).json(HttpResponse("Invalid currency or price plan"))
        }
    }

    private fun purchaseWithPaymentMethodType(userRecord: UserRecord,
                                              currencyCode: String, customerId: String,
                                              paymentMethodType: String, ipAddress: String, context: Context){
        try {
            val secret = stripeUtils.paymentCreateIntent(getPrice(currencyCode, ipAddress, userRecord), customerId,
                "OCR For Photuris III ", userRecord.email, currencyCode, paymentMethodType)
            if(secret.isBlank()){
                context.status(400).json(HttpResponse("Unable to process your payment"))
            } else {
                context.status(200).json(HttpResponse(secret))
            }

        } catch (exception: Exception){
            context.status(400).json(HttpResponse("Invalid currency or price plan"))
        }
    }

    private fun purchase(userRecord: UserRecord, currencyCode: String, customerId: String,
                         paymentMethod: String, ipAddress: String, context: Context){
        try {
            val secret = stripeUtils.getClientSecret(getPrice(currencyCode, ipAddress, userRecord),
                customerId, "Photuris III OCR", userRecord.email, currencyCode, paymentMethod)
            context.status(200).json(HttpResponse(secret))
        } catch (exception: Exception){
            context.status(400).json(HttpResponse("Invalid currency or price plan"))
        }
    }

    private fun isVerifiedStudentAccount(userRecord: UserRecord): Boolean{
        val isVerifiedStudent = userRecord.isEmailVerified
        val isStudent = isAcademic(userRecord.email)
        return isStudent && isVerifiedStudent
    }

    private fun getPrice(ipAddress: String, currencyCode: String, userRecord: UserRecord): Double{
        val isVerified = isVerifiedStudentAccount(userRecord)
        val isGoodSchool = isGoodSchool(findSchoolNames(userRecord.email))
        val countryName = GeoLite().getCountryName(ipAddress)
        val price = Firestore().getPriceOfGoods(currencyCode, countryName)
        if(isVerified && isGoodSchool){
            return price.times(75).div(100)
        }
        if(isVerified){
            return price.times(50).div(100)
        }
        return price
    }
}