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

package xyz.hisname.fireflyiii.ocr.ui.camera

import android.app.Application
import android.content.ContentResolver
import androidx.core.net.toUri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.MultipartBody
import xyz.hisname.fireflyiii.ocr.models.ReceiptData
import xyz.hisname.fireflyiii.ocr.ui.BaseViewModel
import xyz.hisname.fireflyiii.ocr.utils.InputStreamRequestBody
import xyz.hisname.fireflyiii.ocr.utils.extension.findFloat
import java.io.File
import java.time.Instant
import java.util.*

class CameraViewModel(application: Application): BaseViewModel(application) {

    private val receiptData = ReceiptData()

    fun analyseImage(photoFile: File, contentResolver: ContentResolver): MutableLiveData<ReceiptData?>{
        val receiptMutableLiveData = MutableLiveData<ReceiptData?>()
        viewModelScope.launch(Dispatchers.IO + CoroutineExceptionHandler { coroutineContext, throwable ->
            isLoading.postValue(false)
            receiptMutableLiveData.postValue(ReceiptData())
            throwable.printStackTrace()
        }){
            isLoading.postValue(true)
            if(appPref.endTime >= (Instant.now().epochSecond)){
                val upload = MultipartBody.Part.createFormData("file", photoFile.name,
                    InputStreamRequestBody(MediaType.parse("multipart/form-data"),
                        contentResolver, photoFile.toUri())
                )
                val uploadService = userEndpoint.getReceiptData(upload)
                val uploadBody = uploadService.body()
                if(uploadService.isSuccessful && uploadBody != null){
                    receiptMutableLiveData.postValue(uploadBody)
                }
            } else {
                receiptMutableLiveData.postValue(offlineAnalysis(photoFile))
            }
            isLoading.postValue(false)
            photoFile.delete()
        }
        return receiptMutableLiveData
    }

    private fun offlineAnalysis(photoFile: File): ReceiptData{
        val textDeviceDetector = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        textDeviceDetector.process(InputImage.fromFilePath(getApplication(), photoFile.toUri())).addOnSuccessListener { text ->
            getReceipts(text.text)
        }
        return receiptData
    }


    private fun getReceipts(text: String): ReceiptData {
        val originalResult = text.findFloat()
        return if (originalResult.isEmpty()){
            receiptData
        } else {
            val receipts = receiptData
            val totalF = Collections.max(originalResult)
            receipts.totalPrice = totalF.toString()
            receipts
        }
    }

}