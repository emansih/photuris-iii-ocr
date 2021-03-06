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

package xyz.hisname.fireflyiii.ocr.ui.paymentmethod

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.stripe.android.CustomerSession
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import xyz.hisname.fireflyiii.ocr.network.StripeEndpoints
import xyz.hisname.fireflyiii.ocr.ui.BaseViewModel
import java.util.concurrent.TimeUnit

class PaymentMethodViewModel(application: Application): BaseViewModel(application) {

    private val stripeEndpoint by lazy { networkService.create(StripeEndpoints::class.java) }
    private var clientKey = ""

    fun getSubscriptionEnd(): LiveData<Boolean>{
        val isUserSubscribed = MutableLiveData<Boolean>()
        isLoading.postValue(true)
        viewModelScope.launch(Dispatchers.IO + CoroutineExceptionHandler { coroutineContext, throwable ->
            isLoading.postValue(false)
            httpMessage.postValue("There was an issue retrieving your subscription")
        }){
            // Hack: Wait for 5 seconds while we process the data on the backend.
            // Not cool but it works
            TimeUnit.SECONDS.sleep(5)
            val userStatus = userEndpoint.getUserStatus()
            val responseBody = userStatus.body()
            if(userStatus.isSuccessful && responseBody != null && responseBody.endTime != 0L){
                appPref.endTime = responseBody.endTime
                appPref.startTime = responseBody.startTime
                appPref.paymentSource = responseBody.paymentSource
                isUserSubscribed.postValue(true)
            } else {
                // We try getting the subscription 1 more time
                TimeUnit.SECONDS.sleep(5)
                val secondUserStatus = userEndpoint.getUserStatus()
                val secondResponseBody = secondUserStatus.body()
                if(secondUserStatus.isSuccessful && secondResponseBody != null && secondResponseBody.endTime != 0L){
                    appPref.endTime = secondResponseBody.endTime
                    appPref.startTime = secondResponseBody.startTime
                    appPref.paymentSource = secondResponseBody.paymentSource
                    isUserSubscribed.postValue(true)
                } else {
                    isUserSubscribed.postValue(false)
                }
            }
            isLoading.postValue(false)
        }
        return isUserSubscribed
    }

    fun purchasePlan(paymentMethodId: String, currencyCode: String,
                     paymentMethodType: String = "", isRecurring: Boolean = false): LiveData<String> {
        val clientSecret = MutableLiveData<String>()
        viewModelScope.launch(Dispatchers.IO + CoroutineExceptionHandler { coroutineContext, throwable ->
            isLoading.postValue(false)
        }){
            isLoading.postValue(true)
            if(isRecurring){
                val serverResponse = stripeEndpoint.purchasePlan(currencyCode, paymentMethodId,
                    paymentMethodType, true)
                if(!serverResponse.isSuccessful){
                    httpMessage.postValue("Purchase Failed!")
                } else {
                    clientSecret.postValue("")
                }
            } else {
                val serverResponse = stripeEndpoint.purchaseProduct(currencyCode, paymentMethodId,
                    paymentMethodType, false)
                val responseBody = serverResponse.body()
                if(serverResponse.isSuccessful && responseBody != null){
                    clientSecret.postValue(responseBody.response)
                } else {
                    httpMessage.postValue("Purchase Failed!")
                }
            }
            isLoading.postValue(false)
        }
        return clientSecret
    }

    fun createCustomerSession(): LiveData<Boolean> {
        val isSuccessful = MutableLiveData<Boolean>()
        // Request key once. Reduce load on my server
        if(clientKey.isBlank()){
            CustomerSession.initCustomerSession(getApplication(), { apiVersion, keyUpdateListener ->
                viewModelScope.launch(Dispatchers.IO + CoroutineExceptionHandler { coroutineContext, throwable ->
                    isLoading.postValue(false)
                    isSuccessful.postValue(false)
                }){
                    isLoading.postValue(true)
                    val keyResponse = stripeEndpoint.getKey(apiVersion)
                    val body = keyResponse.body()
                    if(keyResponse.isSuccessful && body != null){
                        keyUpdateListener.onKeyUpdate(body.toString())
                        isSuccessful.postValue(true)
                    } else {
                        httpMessage.postValue("Failed to process your payment")
                        isSuccessful.postValue(false)
                    }
                    isLoading.postValue(false)
                }
            })
        } else {
            isSuccessful.postValue(true)
        }
        return isSuccessful
    }
}