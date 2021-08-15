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

import com.stripe.Stripe
import controllers.*
import controllers.stripe.*
import controllers.stripe.Webhook as StripeWebhook
import controllers.google.Webhook as GoogleWebhook
import io.javalin.Javalin
import utils.GoogleExtension

class App

fun main(){
    if(Constants.STRIPE_SECRET_KEY.isNullOrBlank()){
        throw Exception("Stripe Key not found!")
    } else {
        Stripe.apiKey = Constants.STRIPE_SECRET_KEY
    }
    if(Constants.STRIPE_WEB_HOOK_KEY.isNullOrBlank()){
        throw Exception("Stripe Webhook Key not found!")
    }
	if(Constants.AZUREKEY.isNullOrBlank()){
		throw Exception("Azure Key not found!")
	}
	if(Constants.AZUREURL.isNullOrBlank()){
		throw Exception("Azure URL not found!")
	}
	if(Constants.STORAGE_BUCKET.isNullOrBlank()){
		throw Exception("Storage Bucket URL not found!")
	}
    val app = Javalin.create().start(8181)
    configureRoutes(app)
    try {
        GoogleExtension.getFirebaseCredentials()
        System.setProperty("pdfbox.fontcache", "/tmp")
    } catch (exception: Exception){
        exception.printStackTrace()
    }

}


private fun configureRoutes(app: Javalin) {
    app.get("/privacy", PrivacyController())
    app.get("/tos", TosController())
    app.get("/healthCheck", HealthCheck())

    app.post("/api/v1/cancel", CancelSubscription())


    app.post("/api/v1/price", GetPriceController())
    app.post("/api/v1/stripe/createKey", CreateEphemeralKey())
    app.post("/api/v1/stripe/webhook", StripeWebhook())
    app.post("/api/v1/stripe/purchase", PurchaseGoods())

    app.post("/api/v1/google/webhook", GoogleWebhook())

    app.post("/api/v1/ocr", ReceiptParser())
    app.get("/api/v1/userStatus", GetUserStatus())
    app.get("/api/v1/userPurchases", PurchaseHistory())
}