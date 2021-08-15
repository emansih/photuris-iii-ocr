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

package utils

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.AndroidPublisherScopes
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import java.util.*

class GoogleExtension {

    companion object {
        fun getGoogleCredentials(): AndroidPublisher{
            val serviceAccount = GoogleExtension::class.java
                .getResourceAsStream("/credentials/play_publisher_credentials.json")
            val scopes = Collections.singleton(AndroidPublisherScopes.ANDROIDPUBLISHER)
            val credentials = GoogleCredentials
                .fromStream(serviceAccount)
                .createScoped(scopes)
            credentials.refreshIfExpired()

            val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
            val jsonFactory = GsonFactory.getDefaultInstance()

            return AndroidPublisher.Builder(httpTransport, jsonFactory,
                HttpCredentialsAdapter(credentials)
            ).setApplicationName("Photuris III OCR").build()
        }

        fun getFirebaseCredentials(){
            val serviceAccount = GoogleExtension::class.java.getResourceAsStream("/credentials/credentials.json")
            val options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .setStorageBucket(Constants.STORAGE_BUCKET)
                .build()
            FirebaseApp.initializeApp(options)
        }

    }

}