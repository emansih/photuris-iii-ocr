/*
 * Copyright (c)  2021 ASDF Dev Pte. Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package xyz.hisname.fireflyiii.ocr.data.local

import xyz.hisname.fireflyiii.ocr.data.network.UserEndpoint
import xyz.hisname.fireflyiii.ocr.models.CustomerPurchases
import java.lang.Exception

class UserRepository(private val appPref: AppPref,
                     private val userEndpoint: UserEndpoint) {


    suspend fun isUserSubscribed(): Boolean {
        return try {
            val userStatus = userEndpoint.getUserStatus()
            val responseBody = userStatus.body()
            if(userStatus.isSuccessful && responseBody != null && responseBody.endTime != 0L){
                appPref.endTime = responseBody.endTime
                appPref.startTime = responseBody.startTime
                appPref.paymentSource = responseBody.paymentSource
                true
            } else {
                false
            }
        } catch (exception: Exception){
            false
        }
    }

    suspend fun subscriptionList(): List<CustomerPurchases>{
        val purchaseList = mutableListOf<CustomerPurchases>()
        try {
            val userPurchases = userEndpoint.getCustomerHistory()
            val body = userPurchases.body()
            if (userPurchases.isSuccessful && body != null){
                purchaseList.addAll(body)
            }
        } catch(exception: Exception){ }
        return purchaseList
    }

    suspend fun cancelSub(orderId: String): Boolean{
        try {
            val userPurchases = userEndpoint.cancelSub(orderId)
            if (userPurchases.isSuccessful){
                return true
            }
        } catch(exception: Exception){ }
        return false
    }
}