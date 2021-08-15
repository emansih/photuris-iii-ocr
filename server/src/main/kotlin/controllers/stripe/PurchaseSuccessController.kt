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

package controllers.stripe

import com.stripe.model.Charge
import com.stripe.model.Invoice
import com.stripe.param.ChargeListParams
import data.PaymentGateway
import io.javalin.http.Context
import io.javalin.http.Handler
import java.math.BigDecimal
import java.util.*

class PurchaseSuccessController: Handler {

    override fun handle(context: Context) {
        val customerId = context.queryParam("customer") ?: ""
        if(customerId.isBlank()){
            context.render("views/404.html")
        } else {
            val customerName = PaymentGateway().getCustomerById(customerId).name
            val charge = Charge.list(ChargeListParams.builder().setCustomer(customerId).build()).data[0]
            val currency = charge.currency
            val currencySymbol = Currency.getInstance(charge.currency.uppercase(Locale.ENGLISH)).symbol
            val amount = getAmount(charge.amount, currency)
            val invoice = Invoice.retrieve(charge.invoice)
            val orderNumber = invoice.number
            val model = mapOf("invoiceId" to orderNumber, "customerName" to customerName,
                "receiptUrl" to invoice.invoicePdf, "amount" to amount, "currency" to currencySymbol)
            context.render("views/thankyou.html", model)
        }
    }

    // https://stripe.com/docs/currencies#zero-decimal
    private fun getAmount(amount: Long, currency: String): Long{
        val specialCurrency = arrayListOf("BIF", "CLP", "DJF", "GNF", "JPY", "KMF", "KRW",
            "MGA", "PYG", "RWF", "UGX", "VND", "VUV", "XAF", "XOF", "XPF")
        return if(specialCurrency.contains(currency)){
            amount
        } else {
            val bigDecimal = BigDecimal(amount)
            bigDecimal.div(BigDecimal(100)).toLong()
        }
    }
}