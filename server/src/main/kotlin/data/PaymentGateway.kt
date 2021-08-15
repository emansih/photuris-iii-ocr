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

import CustomerNotFoundException
import com.stripe.model.*
import com.stripe.model.checkout.Session
import com.stripe.model.billingportal.Session as BillPortalSession
import com.stripe.net.RequestOptions
import com.stripe.param.*
import com.stripe.param.billingportal.SessionCreateParams
import com.stripe.param.checkout.SessionCreateParams as CheckoutSession
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.jvm.Throws

class PaymentGateway {

    fun createCustomer(email: String, name: String): Customer{
        return Customer.create(
            CustomerCreateParams.builder()
                    .setName(name)
                    .setEmail(email)
                    .build())
    }

    fun createEphemeralKey(customerId: String, stripeVersion: String): EphemeralKey{
        val requestOptions = RequestOptions.RequestOptionsBuilder()
            .setStripeVersionOverride(stripeVersion)
            .build()
        val options = mutableMapOf<String, Any>()
        options["customer"] = customerId
        return EphemeralKey.create(options, requestOptions)
    }

    fun deleteCustomerByEmail(email: String){
        Customer.list(
            CustomerListParams.builder()
            .setEmail(email)
            .build())
            .data[0].delete()
    }

    fun getCustomerById(id: String) = Customer.retrieve(id)

    @Throws(CustomerNotFoundException::class)
    fun getCustomerByEmail(email: String): Customer{
        val customerList = CustomerListParams.builder()
                .setEmail(email)
                .build()
        val customerListData = Customer.list(customerList).data
        if(customerListData.isEmpty()){
            throw CustomerNotFoundException("Customer details not found!")
        } else {
            customerListData.forEach { customer ->
                if(customer.email?.contentEquals(email) == true){
                    return customer
                }
            }
        }
        throw CustomerNotFoundException("Customer details not found!")
    }

    fun createCustomerPortal(customerId: String): String{
        val params = SessionCreateParams.builder()
            .setCustomer(customerId)
            .setReturnUrl("https://www.google.com")
            .build()
        val session = BillPortalSession.create(params)
        return session.url
    }


    fun createSubscriptionCheckoutPage(customerId: String, currency: String,
                                       amount: Double, firebaseId: String): String{
        val paymentMethodTypeList = arrayListOf(CheckoutSession.PaymentMethodType.CARD)
        val params = CheckoutSession.builder()
            .addAllPaymentMethodType(paymentMethodTypeList)
            .setMode(CheckoutSession.Mode.SUBSCRIPTION)
            .setCustomer(customerId)
            .setClientReferenceId(firebaseId)
            .setSuccessUrl(Constants.APP_URL + "/stripe/thankyou?customer=$customerId")
            .setCancelUrl(Constants.APP_URL + "/cancel")
            .setSubscriptionData(CheckoutSession
                .SubscriptionData
                .builder()
                .setTrialPeriodDays(2)
                .build())
            .addLineItem(CheckoutSession.LineItem.Builder()
                .setQuantity(1L)
                .setPrice(createProduct(customerId + "_product", currency, amount, 6))
                .build())
            .build()
        val session = Session.create(params)
        return session.url
    }

    fun createCheckoutPage(customerId: String, currency: String,
                           amount: Double, firebaseId: String): String{
        val paymentMethodTypeList = arrayListOf(CheckoutSession.PaymentMethodType.CARD)
        if(currency.contentEquals("EUR")){
            paymentMethodTypeList.add(CheckoutSession.PaymentMethodType.P24)
            paymentMethodTypeList.add(CheckoutSession.PaymentMethodType.BANCONTACT)
            paymentMethodTypeList.add(CheckoutSession.PaymentMethodType.GIROPAY)
            paymentMethodTypeList.add(CheckoutSession.PaymentMethodType.IDEAL)
            paymentMethodTypeList.add(CheckoutSession.PaymentMethodType.EPS)
        }
        if(currency.contentEquals("SGD") || currency.contentEquals("MYR")){
            paymentMethodTypeList.add(CheckoutSession.PaymentMethodType.GRABPAY)
        }
        if (currency.contentEquals("CNY") || currency.contentEquals("SGD")){
            paymentMethodTypeList.add(CheckoutSession.PaymentMethodType.ALIPAY)
        }

        val params = CheckoutSession.builder()
            .addAllPaymentMethodType(paymentMethodTypeList)
            .setMode(CheckoutSession.Mode.PAYMENT)
            .setCustomer(customerId)
            .setClientReferenceId(firebaseId)
            .setSuccessUrl(Constants.APP_URL + "/stripe/thankyou?customer=$customerId")
            .setCancelUrl(Constants.APP_URL + "/cancel")
            .addLineItem(
                CheckoutSession.LineItem.builder()
                    .setQuantity(1L)
                    .setPriceData(
                        CheckoutSession.LineItem.PriceData.builder()
                            .setCurrency(currency)
                            .setUnitAmount(getAmount(amount, currency))
                            .setProductData(
                                CheckoutSession.LineItem.PriceData.ProductData.builder()
                                    .setName("Photuris III OCR")
                                    .build())
                            .build())
                    .build())
            .build()
        val session = Session.create(params)
        return session.url
    }

    private fun createProduct(name: String, currencyCode: String,
                              amount: Double, durationInMonth: Long): String{
        val product = PriceCreateParams.builder()
            .setRecurring(PriceCreateParams.Recurring.builder()
                .setInterval(PriceCreateParams.Recurring.Interval.MONTH)
                .setIntervalCount(durationInMonth)
                .build())
            .setProductData(PriceCreateParams
                .builder()
                .setNickname(name)
                .setProductData(PriceCreateParams.ProductData
                    .builder()
                    .setName("Photuris III OCR")
                    .setStatementDescriptor("OCR Subscription")
                    .build())
                .setProduct(name)
                .setUnitAmount(getAmount(amount, currencyCode))
                .setCurrency(currencyCode)
                .build().productData
            )
            .setUnitAmount(getAmount(amount, currencyCode))
            .setCurrency(currencyCode)
            .build()
        return Price.create(product).id
    }

    fun unSubscribe(subscriptionId: String){
        val subscription = Subscription.retrieve(subscriptionId)
        subscription.cancel()
    }

    fun refundPurchase(chargeId: String){
        Refund.create(RefundCreateParams
            .builder()
            .setCharge(chargeId)
            .build())
    }

    fun subscribe(currencyCode: String,
                  amount: Double, customerId: String,
                  durationInMonth: Long, paymentMethodId: String){

        val price = createProduct(customerId + "_product", currencyCode, amount, durationInMonth)

        val subCreateParams = SubscriptionCreateParams
            .builder()
            .setCustomer(customerId)
            .addItem(
                SubscriptionCreateParams
                    .Item
                    .builder()
                    .setPrice(price)
                    .build()
            )
            .setTrialPeriodDays(2)
            .setDefaultPaymentMethod(paymentMethodId)
            .build()

        Subscription.create(subCreateParams)
    }

    fun createPromotionByCustomer(customerId: String, couponName: String,
                                  couponCode: String): PromotionCode{
        val endOfDay = LocalDateTime.now(ZoneId.of("Etc/UTC"))
            .plusMonths(1)
            .plusDays(1)
            .toLocalDate()
            .atStartOfDay(ZoneId.of("Etc/UTC"))
            .toEpochSecond()
        val coupon = Coupon.create(
            CouponCreateParams.builder()
                .setRedeemBy(endOfDay)
                .setName(couponName)
                .setDuration(CouponCreateParams.Duration.ONCE)
                .setPercentOff(BigDecimal.valueOf(100))
                .build())
        val promoCodeParams = PromotionCodeCreateParams.builder()
            .setCustomer(customerId)
            .setMaxRedemptions(1)
            .setCode(couponCode)
            .setCoupon(coupon.id)
            .setRestrictions(
                PromotionCodeCreateParams.Restrictions
                .builder()
                .setFirstTimeTransaction(true)
                .build())
            .build()
        return PromotionCode.create(promoCodeParams)
    }


    fun paymentCreateIntent(amount: Double, customerId: String, description: String,
                            customerEmail: String, currency: String,
                            paymentMethodType: String): String{
        if(checkPaymentType(paymentMethodType, currency)){
            val params =
                PaymentIntentCreateParams.builder()
                    .setCustomer(customerId)
                    .setDescription(description)
                    .setAmount(getAmount(amount, currency))
                    .setCurrency(currency)
                    .setReceiptEmail(customerEmail)
                    .addPaymentMethodType(paymentMethodType)
                    .build()
            return PaymentIntent.create(params).clientSecret
        } else {
            return ""
        }
    }

    /*
    * Valid Payment Method Type: grabpay , alipay, bancontact , p24 , giropay, iDEAL, EPS
    */
    private fun checkPaymentType(paymentMethodType: String, currency: String): Boolean{
        val validPaymentType = arrayListOf("grabpay", "alipay", "bancontact", "p24", "giropay", "ideal", "eps")
        val euroOnly = arrayListOf("bancontact", "p24", "giropay", "ideal", "eps")
        if(validPaymentType.contains(paymentMethodType)){
            if(currency.contentEquals("EUR")){
                return euroOnly.contains(paymentMethodType)
            } else {
                if(paymentMethodType.contentEquals("grabpay")){
                    return currency.contentEquals("SGD") || currency.contentEquals("MYR")
                }
                if(paymentMethodType.contentEquals("alipay")){
                    return currency.contentEquals("CNY") || currency.contentEquals("SGD")
                }
            }
        }
        return false
    }

    fun getClientSecret(amount: Double, customerId: String,
                        description: String, customerEmail: String, currency: String,
                        paymentMethodId: String): String{
        val createParams = PaymentIntentCreateParams.Builder()
            .setCustomer(customerId)
            .setDescription(description)
            .setCurrency(currency)
            .setAmount(getAmount(amount, currency))
            .setPaymentMethod(paymentMethodId)
            .setReceiptEmail(customerEmail)
            .build()
        return PaymentIntent.create(createParams).clientSecret
    }

    // https://stripe.com/docs/currencies#zero-decimal
    private fun getAmount(amount: Double, currency: String): Long{
        val specialCurrency = arrayListOf("BIF", "CLP", "DJF", "GNF", "JPY", "KMF", "KRW",
            "MGA", "PYG", "RWF", "UGX", "VND", "VUV", "XAF", "XOF", "XPF")
        return if(specialCurrency.contains(currency)){
            amount.toLong()
        } else {
            val bigDecimal = BigDecimal(amount)
            bigDecimal.times(BigDecimal(100)).toLong()
        }
    }
}