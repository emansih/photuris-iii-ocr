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

import com.google.firebase.cloud.FirestoreClient
import models.PriceOfGoods
import utils.GeoLite
import java.net.InetAddress
import java.util.*
import kotlin.collections.HashMap

class PriceCalculator {

    fun getPrice(ipAddress: String): PriceOfGoods {
        val isLocalIpAddress = InetAddress.getByName(ipAddress).isAnyLocalAddress
        val countryIsoCode = if(isLocalIpAddress){
            "SG"
        } else {
            try {
                GeoLite().getCountryIsoCode(ipAddress)
            } catch (exception: Exception){
                "US"
            }
        }
        val currencyCode = Currency.getInstance(Locale("", countryIsoCode)).currencyCode
        val db = FirestoreClient.getFirestore()
        val priceOfGoods = db.collection("products").document("OCRSixMonth").get().get()
        if(currencyCode?.contentEquals("EUR") == true){
            val price = priceOfGoods.get(currencyCode) as HashMap<String, Double>
            val countryName = GeoLite().getCountryName(ipAddress)
            val priceOfGoodsInCountry = price[countryName]
            if(priceOfGoodsInCountry != null){
                return PriceOfGoods(price[countryName] ?: 3.69, "EUR")
            } else {
                return PriceOfGoods(price["Unknown"] ?: 3.69, "EUR")
            }
        } else {
            if(priceOfGoods.get(currencyCode) != null){
                return PriceOfGoods(priceOfGoods.get(currencyCode).toString().toDouble(), currencyCode)
            } else {
                // Default currency
                val defaultPriceOfGoods = priceOfGoods.get("USD")
                return PriceOfGoods(defaultPriceOfGoods.toString().toLong().toDouble(), "USD")
            }
        }
    }
}