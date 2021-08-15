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
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import xyz.hisname.fireflyiii.ocr.data.network.UserEndpoint
import xyz.hisname.fireflyiii.ocr.network.StripeEndpoints
import xyz.hisname.fireflyiii.ocr.ui.BaseViewModel

class PremiumViewModel(application: Application): BaseViewModel(application) {

    var currencyCode = ""

    private val stripeEndpoint by lazy { networkService.create(StripeEndpoints::class.java) }

    val isStudent = MutableLiveData<Boolean>()

    fun getPrice(): LiveData<String> {
        val priceOfGoods = MutableLiveData<String>()
        viewModelScope.launch(Dispatchers.IO + CoroutineExceptionHandler { coroutineContext, throwable ->
            throwable.printStackTrace()
        }){
            isLoading.postValue(true)
            val service = stripeEndpoint.getPrice()
            val serviceBody = service.body()
            if(service.isSuccessful && serviceBody != null){
                currencyCode = serviceBody.priceOfGoods.currencyCode
                priceOfGoods.postValue(currencyCode + " " + serviceBody.priceOfGoods.price)
                isStudent.postValue(serviceBody.discountAvailable)
            }
            isLoading.postValue(false)
        }
        return priceOfGoods
    }

}