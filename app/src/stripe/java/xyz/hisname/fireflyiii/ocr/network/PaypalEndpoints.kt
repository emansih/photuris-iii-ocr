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

package xyz.hisname.fireflyiii.ocr.network

import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import xyz.hisname.fireflyiii.ocr.models.Customer

interface PaypalEndpoints {

    @FormUrlEncoded
    @POST("/api/v1/paypal/createOrder")
    suspend fun getOrderId(@Field("currencyCode") currencyCode: String): Response<String>

    @FormUrlEncoded
    @POST("/api/v1/paypal/confirmOrder")
    suspend fun submitOrderId(@Field("orderId") orderId: String): Response<Customer>

}