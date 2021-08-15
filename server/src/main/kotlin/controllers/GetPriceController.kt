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
import com.google.firebase.auth.UserRecord
import data.Firestore
import io.javalin.http.Context
import data.PriceCalculator
import models.PriceOfGoods
import models.ProductDetails
import utils.GeoLite
import utils.findSchoolNames
import utils.isAcademic
import utils.isGoodSchool

class GetPriceController: BaseHandler() {

    override fun handle(context: Context) {
        val ipAddress = context.ip()
        val firebaseId = context.header("X-FIREBASE-ID", String::class.java).get()
        val market = context.formParam("market") ?: "googlePlay"
        try {
            val firebaseUser = FirebaseAuth.getInstance().getUser(firebaseId)
            if(firebaseUser.isDisabled){
                context.status(400)
            } else {
                if(firebaseId.isNotBlank()){
                    val isVerified = isVerifiedStudentAccount(firebaseUser)
                    val isGoodSchool = isGoodSchool(findSchoolNames(firebaseUser.email))
                    var price = PriceOfGoods(0.0, "")
                    var discount = 0.0
                    if(market.contentEquals("stripe")){
                        price = PriceCalculator().getPrice(ipAddress)
                        val countryName = GeoLite().getCountryName(ipAddress)

                        if(isVerified && isGoodSchool){
                            discount = 75.0
                            price = PriceOfGoods(Firestore().getPriceOfGoods(price.currencyCode,
                                countryName).times(75).div(100), price.currencyCode)
                        }
                        if(isVerified){
                            discount = 50.0
                            price = PriceOfGoods(Firestore().getPriceOfGoods(price.currencyCode,
                                countryName).times(50).div(100), price.currencyCode)
                        }
                    }
                    val productDetails = ProductDetails(price, "",
                        "", 0, 0, discount, isEducationAccount(firebaseUser.email))
                    context.status(200).json(productDetails)
                } else {
                    context.status(400)
                }
            }
        } catch (exception: Exception){
            context.status(400)
        }
    }

    private fun isEducationAccount(email: String): Boolean{
        return isAcademic(email)
    }

    private fun isVerifiedStudentAccount(userRecord: UserRecord): Boolean{
        val isVerifiedStudent = userRecord.isEmailVerified
        val isStudent = isAcademic(userRecord.email)
        return isStudent && isVerifiedStudent
    }

}