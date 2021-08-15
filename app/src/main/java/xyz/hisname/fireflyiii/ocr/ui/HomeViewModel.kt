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

package xyz.hisname.fireflyiii.ocr.ui

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.MultipartBody
import xyz.hisname.fireflyiii.ocr.data.network.RetrofitClient
import xyz.hisname.fireflyiii.ocr.data.network.UserEndpoint
import xyz.hisname.fireflyiii.ocr.models.ReceiptData
import xyz.hisname.fireflyiii.ocr.utils.InputStreamRequestBody
import java.time.Instant

class HomeViewModel(application: Application): BaseViewModel(application) {

    fun subscriptionStatus(){
        if(!isLoggedOut){
            viewModelScope.launch(Dispatchers.IO + CoroutineExceptionHandler { coroutineContext, throwable -> }){
                val userStatus = userEndpoint.getUserStatus()
                val responseBody = userStatus.body()
                if(userStatus.isSuccessful && responseBody != null){
                    appPref.endTime = responseBody.endTime
                    appPref.startTime = responseBody.startTime
                    appPref.paymentSource = responseBody.paymentSource
                }
            }
        }
    }

    fun uploadDocument(contentResolver: ContentResolver, uri: Uri, documentName: String): LiveData<ReceiptData?>{
        val receiptMutableLiveData = MutableLiveData<ReceiptData?>()
        if(appPref.endTime >= (Instant.now().epochSecond)){
            viewModelScope.launch(Dispatchers.IO + CoroutineExceptionHandler { coroutineContext, throwable ->
                throwable.printStackTrace()
            }){
                isLoading.postValue(true)
                val upload = MultipartBody.Part.createFormData("file", documentName,
                    InputStreamRequestBody(MediaType.parse("multipart/form-data"), contentResolver, uri))
                val uploadService = userEndpoint.getReceiptData(upload)
                val uploadBody = uploadService.body()
                if(uploadService.isSuccessful && uploadBody != null){
                    receiptMutableLiveData.postValue(uploadBody)
                } else {
                    receiptMutableLiveData.postValue(ReceiptData())
                }
                isLoading.postValue(false)
            }
        } else {
            httpMessage.postValue("Unfortunately this feature is not available")
        }
        return receiptMutableLiveData
    }
}