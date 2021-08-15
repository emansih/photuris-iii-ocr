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

package xyz.hisname.fireflyiii.ocr.data.network

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*
import xyz.hisname.fireflyiii.ocr.models.Customer
import xyz.hisname.fireflyiii.ocr.models.CustomerPurchases
import xyz.hisname.fireflyiii.ocr.models.ReceiptData

interface UserEndpoint {

    @Multipart
    @POST("/api/v1/ocr")
    suspend fun getReceiptData(@Part file: MultipartBody.Part): Response<ReceiptData>

    @GET("/api/v1/userStatus")
    suspend fun getUserStatus(): Response<Customer>

    @GET("/api/v1/userPurchases")
    suspend fun getCustomerHistory(): Response<List<CustomerPurchases>>

    @POST("/api/v1/cancel")
    @FormUrlEncoded
    suspend fun cancelSub(@Field("orderId") orderId: String): Response<Void>
}