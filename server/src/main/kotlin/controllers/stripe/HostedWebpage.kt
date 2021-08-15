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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserRecord
import data.Firestore
import data.PaymentGateway
import io.javalin.http.Context
import io.javalin.http.Handler
import models.HttpResponse
import utils.GeoLite
import utils.findSchoolNames
import utils.isAcademic
import utils.isGoodSchool

class HostedWebpage: Handler {

    override fun handle(context: Context) {
        val ipAddress = context.ip()
        val firebaseId = context.header("X-FIREBASE-ID", String::class.java).get()
        val currencyCode = context.formParam("currencyCode", String::class.java).get()
        val isRecurring = context.formParam("isRecurring", Boolean::class.java).get()
        if(firebaseId.isNotBlank()){
            val firebaseUser = FirebaseAuth.getInstance().getUser(firebaseId)
            if(!firebaseUser.isDisabled){
                var customerId: String? = null
                val firebaseEmail = firebaseUser.email
                val firebaseDisplayName = firebaseUser.displayName
                val paymentGateway = PaymentGateway()
                try {
                    val customerDetails = paymentGateway.getCustomerByEmail(firebaseUser.email)
                    customerId = customerDetails.id
                } catch (exception: CustomerNotFoundException){
                    if(firebaseEmail.isEmpty() || firebaseDisplayName.isEmpty()){
                        context.status(400).json(HttpResponse("Additional user information required"))
                    } else {
                        val customerDetails = paymentGateway.createCustomer(firebaseUser.email, firebaseUser.displayName)
                        customerId = customerDetails.id
                    }
                }
                if(customerId != null){
                    val url = if(isRecurring){
                        paymentGateway.createSubscriptionCheckoutPage(customerId, currencyCode,
                            getPrice(ipAddress, currencyCode, firebaseUser), firebaseId)
                    } else {
                        paymentGateway.createCheckoutPage(customerId, currencyCode,
                            getPrice(ipAddress, currencyCode, firebaseUser), firebaseId)
                    }
                    context.status(200).json(HttpResponse(url))
                } else {
                    context.status(400).json(HttpResponse("There was an issue creating an account"))
                }
            } else {
                context.status(400).json(HttpResponse("Your account has been disabled!"))
            }
        }
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

    private fun isVerifiedStudentAccount(userRecord: UserRecord): Boolean{
        val isVerifiedStudent = userRecord.isEmailVerified
        val isStudent = isAcademic(userRecord.email)
        return isStudent && isVerifiedStudent
    }
}