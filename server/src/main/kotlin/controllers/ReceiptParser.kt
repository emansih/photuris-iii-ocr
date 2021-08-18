/*
 * Copyright (c)  2021 ASDF Dev Pte. Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package controllers

import com.azure.ai.formrecognizer.models.FieldValueType
import com.google.cloud.storage.Blob
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.cloud.StorageClient
import data.Firestore
import io.javalin.http.Context
import models.ReceiptData
import data.Microsoft
import utils.FileUtils
import utils.isImage
import utils.isPdf
import utils.isUserSubscribed
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.TimeUnit


// Documentation: https://docs.microsoft.com/en-au/azure/cognitive-services/form-recognizer/concept-receipts

class ReceiptParser: BaseHandler() {

    private var totalPrice: String? = ""
    private var merchantName: String? = ""
    private var transactionDate: String? = ""
    private var transactionTime: String? = ""
    private var blob: Blob? = null

    override fun handle(context: Context) {
        val uploadFile = context.uploadedFile("file")
        val firebaseId = context.header("X-FIREBASE-ID", String::class.java).get()
        if(uploadFile?.content != null && firebaseId.isNotBlank()){
            try {
                val firebaseUser = FirebaseAuth.getInstance().getUser(firebaseId)
                if(firebaseUser.isDisabled){
                    context.status(400)
                }
            } catch (exception: Exception){
                context.status(400)
            }
            val getUserSubscription = Firestore().getUserSubscription(firebaseId).customer
            if(getUserSubscription.isUserSubscribed()){
                if(uploadFile.extension.isPdf()){
                    val receiptFile = FileUtils().convertPdfToImage(uploadFile.content,
                        uploadFile.filename)
                    blob = StorageClient.getInstance().bucket().create("customer/" + firebaseId + "/" + uploadFile.filename, receiptFile)
                } else if(uploadFile.extension.isImage()){
                    blob = StorageClient.getInstance().bucket()
                        .create("customer/" + firebaseId + "/" + uploadFile.filename,
                            uploadFile.content)
                } else {
                    context.status(400)
                }
                val bblob = blob
                if(bblob != null){
                    getImageData(bblob, context)
                }
            } else {
                context.status(400)
            }
        } else {
            if(firebaseId.isBlank()){
                context.status(400)
            } else {
                context.status(400)
            }
        }
    }

    private fun getImageData(blob: Blob, context: Context) {
        try {
            val recognizedFields =
                Microsoft().recogniseReceipts(blob.signUrl(21, TimeUnit.SECONDS).toString())
            if (recognizedFields.isNotEmpty()) {
                recognizedFields.forEach {  form ->
                    val merchantNameField = form.fields["MerchantName"]
                    if (merchantNameField != null && merchantNameField.value.valueType == FieldValueType.STRING) {
                        if(merchantName.isNullOrBlank()){
                            merchantName = merchantNameField.value.asString()
                        }
                    }

                    val transactionDateField = form.fields["TransactionDate"]
                    if (transactionDateField != null && transactionDateField.value.valueType == FieldValueType.DATE) {
                        if(transactionDate.isNullOrBlank()){
                            // Bug here
                            if(transactionDateField.value.asDate() != null){
                                transactionDate = transactionDateField.value.asDate().toString()
                            }
                        }
                    }

                    val transactionTimeField = form.fields["TransactionTime"]
                    if (transactionTimeField != null && transactionTimeField.value.valueType == FieldValueType.TIME) {
                        if(transactionTime.isNullOrBlank()){
                            transactionTime = transactionTimeField.value.asTime().toString()
                        }
                    }

                    val total = form.fields["Total"]
                    if (total != null && FieldValueType.FLOAT == total.value.valueType) {
                        if(totalPrice.isNullOrBlank()){
                            totalPrice = total.value.asFloat().toString()
                        }
                    }
                }
            }
            context.status(200).json(ReceiptData(merchantName, transactionDate, transactionTime, totalPrice)
            )
        } catch (exception: Exception) {
            if(Constants.IS_DEBUG){
                exception.printStackTrace()
            }
        } finally {
            blob.delete()
        }
    }
}