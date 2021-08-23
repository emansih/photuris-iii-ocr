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

import com.paypal.core.PayPalEnvironment
import com.stripe.Stripe
import controllers.*
import controllers.paypal.ConfirmOrder
import controllers.stripe.*
import io.github.bucket4j.Bucket
import controllers.paypal.PurchaseGoods as PaypalPurchaseGoods
import controllers.stripe.Webhook as StripeWebhook
import controllers.google.Webhook as GoogleWebhook
import io.javalin.Javalin
import io.javalin.core.JavalinConfig
import io.javalin.http.staticfiles.Location
import utils.GoogleExtension
import java.util.concurrent.ConcurrentHashMap

class App

private val cache: ConcurrentHashMap<String, Bucket> = ConcurrentHashMap()

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
    val app = Javalin.create{ obj: JavalinConfig ->
        obj.enableDevLogging()
        if(Constants.IS_DEBUG){
            obj.enableDevLogging()
        }
        obj.addStaticFiles("/css", Location.CLASSPATH)
        obj.showJavalinBanner = false
    }.start(8181)
    configureRoutes(app)
    try {
        GoogleExtension.getFirebaseCredentials()
        System.setProperty("pdfbox.fontcache", "/tmp")
    } catch (exception: Exception){
        if(Constants.IS_DEBUG) {
            exception.printStackTrace()
        }
    }

}

private fun configureRoutes(app: Javalin) {
    app.get("/privacy", PrivacyController())
    app.get("/tos", TosController())
    app.get("/healthCheck", HealthCheck())
    app.get("/cancel", PurchaseCancelController())

    app.post("/api/v1/cancel", CancelSubscription())

    app.post("/api/v1/price", GetPriceController())
    app.post("/api/v1/stripe/createKey", CreateEphemeralKey())
    app.post("/api/v1/stripe/webhook", StripeWebhook())
    app.post("/api/v1/stripe/purchase", PurchaseGoods(cache))
    app.post("/api/v1/stripe/hosted_page", HostedWebpage())
    app.get("/stripe/thankyou", PurchaseSuccessController())

    val payPalEnvironment = if(Constants.IS_DEBUG){
        PayPalEnvironment.Sandbox(Constants.PAYPAL_CLIENT_ID, Constants.PAYPAL_CLIENT_SECRET)
    } else {
        PayPalEnvironment.Live(Constants.PAYPAL_CLIENT_ID, Constants.PAYPAL_CLIENT_SECRET)
    }

    app.post("/api/v1/paypal/createOrder", PaypalPurchaseGoods(payPalEnvironment))
    app.post("/api/v1/paypal/confirmOrder", ConfirmOrder(payPalEnvironment))

    app.post("/api/v1/google/webhook", GoogleWebhook())

    app.post("/api/v1/ocr", ReceiptParser())
    app.get("/api/v1/userStatus", GetUserStatus())
    app.get("/api/v1/userPurchases", PurchaseHistory())
}