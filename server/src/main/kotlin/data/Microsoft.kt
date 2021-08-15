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

package data

import Constants
import com.azure.ai.formrecognizer.FormRecognizerClientBuilder
import com.azure.ai.formrecognizer.models.RecognizedForm
import com.azure.core.credential.AzureKeyCredential

class Microsoft {

    fun recogniseReceipts(url: String): MutableList<RecognizedForm>{
        if(Constants.AZUREKEY.isNullOrBlank() && Constants.AZUREURL.isNullOrBlank()){
            throw Exception("Azure Key or URL not set!")
        }
        val formRecognizerClient = FormRecognizerClientBuilder()
            .credential(AzureKeyCredential(Constants.AZUREKEY))
            .endpoint(Constants.AZUREURL)
            .buildClient()
        val syncPoller = formRecognizerClient.beginRecognizeReceiptsFromUrl(url)
        return syncPoller.finalResult
    }
}