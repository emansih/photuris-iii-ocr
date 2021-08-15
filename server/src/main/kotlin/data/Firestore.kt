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

import com.google.cloud.firestore.SetOptions
import com.google.firebase.cloud.FirestoreClient
import models.Customer
import models.CustomerPurchases
import models.google.Cancel
import utils.isUserFromGooglePlay

class Firestore {

    private val db by lazy { FirestoreClient.getFirestore() }

    fun getUserSubscription(firebaseId: String): CustomerPurchases {
        val collection = db.collection("orders/purchased/$firebaseId")
            .whereGreaterThanOrEqualTo("endTime",
                System.currentTimeMillis().div(1000))
            .get().get()
        if(!collection.isEmpty) {
            val data = collection.documents[0]
            val documentId = collection.documents[0].id
            val orderAttribute = data.toObject(Customer::class.java)
            if(orderAttribute.endTime != 0L) {
                val isGooglePlay = orderAttribute.isUserFromGooglePlay()
                return if (isGooglePlay) {
                    CustomerPurchases(true, documentId, orderAttribute)
                } else {
                    val isSubscription = documentId.startsWith("in_")
                    CustomerPurchases(isSubscription, documentId, orderAttribute)
                }
            }
        }
        return CustomerPurchases(false, "",
            Customer(0, 0, "", "", ""))
    }

    fun getUserPurchaseHistory(firebaseId: String): List<CustomerPurchases>{
        val userHistoryCollection = mutableListOf<CustomerPurchases>()
        val collection = db.collection("orders/purchased/$firebaseId")
        collection.get().get().documents.forEach { query ->
            val paymentSource = query.data["paymentSource"].toString()
            val queryId = query.id
            val customer = Customer(
                query.data["startTime"].toString().toLong(),
                query.data["endTime"].toString().toLong(),
                paymentSource)
            if (paymentSource.contentEquals("googlePlay")){
                userHistoryCollection.add(CustomerPurchases(true, queryId, customer))
            } else {
                // Very dirty hack that works
                val isSubscription = queryId.startsWith("in_")
                userHistoryCollection.add(CustomerPurchases(isSubscription, queryId, customer))
            }
        }
        return userHistoryCollection
    }

    fun saveUserPurchase(firebaseId: String, productOrderId: String, customer: Customer){
        db.document("orders/purchased/$firebaseId/$productOrderId").set(customer, SetOptions.merge())
    }

    fun cancelSubscription(firebaseId: String, purchaseToken: String, startTime: Long,
                           endTime: Long, reason: String){
        val collection = db.collection("orders/purchased/$firebaseId")
            .whereEqualTo("purchaseToken", purchaseToken)
            .whereEqualTo("paymentSource", "googlePlay").get().get()
        collection.documents[0].toObject(Customer::class.java)
        val collectionId = collection.documents[0].id
        val cancelModel = Cancel(startTime, endTime, "googlePlay",
            purchaseToken, reason)
        db.document("orders/cancel/$firebaseId/$collectionId").set(cancelModel)
        db.document("orders/purchased/$firebaseId/$collectionId").delete()
    }

    fun refundUser(firebaseId: String, chargeId: String){
        val purchasedAttributes = db.collection("orders/purchased/$firebaseId/$chargeId")
            .get().get().toObjects(Customer::class.java)
        db.document("orders/refunded/$firebaseId/$chargeId").set(purchasedAttributes)
        db.document("orders/purchased/$firebaseId/$chargeId").delete()
    }


    fun getPriceOfGoods(currencyCode: String, countryName: String): Double{
        if(currencyCode.contentEquals("EUR")){
            val priceOfGoods = db.collection("products").document("OCRSixMonth").get().get()
            val priceHashMap = priceOfGoods.get(currencyCode) as HashMap<String, Double>
            val priceOfGoodsInCountry = priceHashMap[countryName]
            return if(priceOfGoodsInCountry != null){
                priceHashMap[countryName] ?: 3.69
            } else {
                3.69
            }
        } else {
            return db.collection("products")
                .document("OCRSixMonth").get().get().get(currencyCode) as Double
        }
    }
}