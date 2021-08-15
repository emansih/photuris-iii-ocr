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

package xyz.hisname.fireflyiii.ocr.ui.history

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import xyz.hisname.fireflyiii.ocr.models.CustomerPurchases
import xyz.hisname.fireflyiii.ocr.ui.BaseViewModel

class HistoryViewModel(application: Application): BaseViewModel(application) {

    fun getCustomerHistory(): LiveData<List<CustomerPurchases>>{
        isLoading.postValue(true)
        val subscriptionMutableLiveData = MutableLiveData<List<CustomerPurchases>>()
        viewModelScope.launch {
            subscriptionMutableLiveData.postValue(userRepository.subscriptionList())
            isLoading.postValue(false)
        }
        return subscriptionMutableLiveData
    }

    fun cancelSub(orderId: String): LiveData<Boolean>{
        val success = MutableLiveData<Boolean>()
        viewModelScope.launch {
            success.postValue(userRepository.cancelSub(orderId))
        }
        return success
    }

}