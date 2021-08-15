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

package xyz.hisname.fireflyiii.ocr.ui.profile

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import xyz.hisname.fireflyiii.ocr.data.network.RetrofitClient
import xyz.hisname.fireflyiii.ocr.ui.BaseViewModel
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.*

class ProfileViewModel(application: Application): BaseViewModel(application) {

    val userSubscriptionEnd = MutableLiveData<String>()

    init {
        getSubscriptionEnd()
    }

    fun getCustomer(): LiveData<Boolean>{
        val isUserSubscribed = MutableLiveData<Boolean>()
        viewModelScope.launch(Dispatchers.IO + CoroutineExceptionHandler { coroutineContext, throwable ->
            httpMessage.postValue("There was an issue retrieving your subscription")
        }){
            RetrofitClient.destroy()
            val userStatus = userEndpoint.getUserStatus()
            val responseBody = userStatus.body()
            if(userStatus.isSuccessful && responseBody != null){
                appPref.endTime = responseBody.endTime
                appPref.startTime = responseBody.startTime
                appPref.paymentSource = responseBody.paymentSource
                if(responseBody.endTime != 0L || responseBody.endTime >= (Instant.now().epochSecond)){
                    userSubscriptionEnd.postValue(dateTimeFormatter(responseBody.endTime))
                } else {
                    userSubscriptionEnd.postValue("No subscription Found")
                }
                isUserSubscribed.postValue(true)
            }
        }
        return isUserSubscribed
    }

    private fun getSubscriptionEnd(): LiveData<String>{
        if(appPref.endTime != 0L && appPref.endTime >= (Instant.now().epochSecond)){
            userSubscriptionEnd.postValue(dateTimeFormatter(appPref.endTime))
        } else {
            userSubscriptionEnd.postValue("No subscription Found")
        }
        SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            val endTime = sharedPreferences.getLong("endTime", 0)
            if(endTime != 0L || endTime >= (Instant.now().epochSecond)){
                userSubscriptionEnd.postValue(dateTimeFormatter(endTime))
            } else {
                userSubscriptionEnd.postValue("No subscription Found")
            }
        }
        return userSubscriptionEnd
    }


    private fun dateTimeFormatter(timeToFormat: Long): String{
        val date = LocalDateTime.ofInstant(Instant.ofEpochSecond(timeToFormat), ZoneId.systemDefault())
        val dayOfMonth = date.dayOfMonth
        val month = date.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
        val year = date.year
        return "Ending on " + dayOfMonth.toString() + " " + month + " " + year + ", " +
                date.hour + ":" + date.minute
    }
}