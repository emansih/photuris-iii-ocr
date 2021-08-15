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

import okhttp3.Headers
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import xyz.hisname.fireflyiii.ocr.BuildConfig
import xyz.hisname.fireflyiii.ocr.Constants
import java.util.concurrent.TimeUnit

class RetrofitClient {

    companion object {

        @Volatile private var INSTANCE: Retrofit? = null

        fun createClient(header: String): Retrofit{
            return INSTANCE ?: Retrofit.Builder()
                .baseUrl(Constants.SERVER_URL)
                .client(header(header))
                .addConverterFactory(MoshiConverterFactory
                    .create()
                    .withNullSerialization()
                    .asLenient())
                .build().also { INSTANCE = it }
        }

        private fun header(header: String): OkHttpClient{
            return OkHttpClient().newBuilder()
                // This is required because Microsoft Azure Image recognition is really slow...
                .callTimeout(90, TimeUnit.SECONDS)
                .connectTimeout(90, TimeUnit.SECONDS)
                .writeTimeout(90, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    val request = chain.request()
                    val authenticatedRequest = request.newBuilder()
                        .headers(Headers.of(mutableMapOf("X-FIREBASE-ID" to header,
                            "APP-VERSION" to BuildConfig.VERSION_NAME)))
                        .build()
                    chain.proceed(authenticatedRequest)
                }.build()
        }

        fun destroy(){
            INSTANCE = null
        }
    }
}