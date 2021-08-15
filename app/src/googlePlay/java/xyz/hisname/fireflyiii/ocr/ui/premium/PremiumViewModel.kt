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

package xyz.hisname.fireflyiii.ocr.ui.premium

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import xyz.hisname.fireflyiii.ocr.ui.BaseViewModel
import java.util.concurrent.TimeUnit

class PremiumViewModel(application: Application): BaseViewModel(application) {

    val getPlayPrice = MutableLiveData<String>()

    fun getSubscriptionEnd(): LiveData<Boolean> {
        val isUserSubscribed = MutableLiveData<Boolean>()
        isLoading.postValue(true)
        viewModelScope.launch(Dispatchers.IO){
            // Hack: Wait for 4 seconds while we process the data on the backend.
            // Not cool but it works
            TimeUnit.SECONDS.sleep(4)
            isUserSubscribed.postValue(userRepository.isUserSubscribed())
            isLoading.postValue(false)
        }
        return isUserSubscribed
    }

    fun getProducts(){
        viewModelScope.launch(Dispatchers.IO){

        }
    }

    fun setPrice(price: String) {
        getPlayPrice.postValue(price)
    }

}