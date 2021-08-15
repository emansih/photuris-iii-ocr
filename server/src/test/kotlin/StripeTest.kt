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
import com.stripe.model.*
import data.PaymentGateway
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.*

class StripeTest {

    private lateinit var customer: Customer
    private val stripeUtils = PaymentGateway()
    private val couponCode = UUID.randomUUID().toString().substring(0, 6)

    companion object {

        @BeforeAll
        fun setup(){
            Stripe.apiKey = Constants.STRIPE_SECRET_KEY
        }

        @AfterAll
        fun tearDown(){
            PaymentGateway().deleteCustomerByEmail(Constants.TEST_EMAIL)
        }
    }

    @BeforeEach
    fun createCustomer(){
        customer = stripeUtils.createCustomer(Constants.TEST_EMAIL, "Daniel")
    }

    @Test
    fun testCustomerData(){
        assertEquals(customer.email, Constants.TEST_EMAIL)
    }

    @Test
    fun deleteCustomerData(){
        stripeUtils.deleteCustomerByEmail(Constants.TEST_EMAIL)
        val deletedCustomer = stripeUtils.getCustomerById(customer.id)
        assertNull(deletedCustomer.email)
    }

    @Test
    fun testCreateEphemeralKey(){
        val ephemeralKey = stripeUtils.createEphemeralKey(customer.id, "2020-08-27")
        assertNotNull(ephemeralKey)
    }

    @Test
    fun testCreatePromoCode(){
        val promotionCode = stripeUtils.createPromotionByCustomer(customer.id, "First Purchase", couponCode)
        assertNotNull(promotionCode.created)
        assertEquals(customer.id, promotionCode.customer)
        assertEquals("First Purchase", promotionCode.coupon.name)
        assertTrue(promotionCode.restrictions.firstTimeTransaction)

    }

    @ParameterizedTest
    @ValueSource(strings = ["pm_card_visa", "pm_card_visa_debit", "pm_card_mastercard_prepaid", "pm_card_unionpay", "pm_card_amex", "pm_card_bg", "pm_card_at", "pm_card_au", "pm_card_sg", "pm_card_jcb"])
    @Test
    fun testChargeUser(input: String){
        val chargeUser = stripeUtils.getClientSecret(100.0, customer.id,
            "JUnit Test", Constants.TEST_EMAIL, "sgd", input)
        assertNotNull(chargeUser)
    }

}