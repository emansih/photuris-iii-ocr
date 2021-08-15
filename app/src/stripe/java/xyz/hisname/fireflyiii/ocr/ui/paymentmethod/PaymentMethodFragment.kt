/*
 * Copyright (c)  2021 ASDF Dev Pte. Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package xyz.hisname.fireflyiii.ocr.ui.paymentmethod

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.alipay.sdk.app.PayTask
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.stripe.android.*
import com.stripe.android.model.*
import xyz.hisname.fireflyiii.ocr.BuildConfig
import xyz.hisname.fireflyiii.ocr.R
import xyz.hisname.fireflyiii.ocr.databinding.FragmentMethodListBinding
import xyz.hisname.fireflyiii.ocr.models.PaymentModel
import xyz.hisname.fireflyiii.ocr.ui.ProgressBar
import xyz.hisname.fireflyiii.ocr.utils.extension.getViewModel
import xyz.hisname.fireflyiii.ocr.utils.extension.toast


class PaymentMethodFragment: BottomSheetDialogFragment() {

    private var fragmentMethodListBinding: FragmentMethodListBinding? = null
    private val binding get() = fragmentMethodListBinding!!
    private var paymentSession: PaymentSession? = null
    private val currencyCode by lazy { arguments?.getString("currencyCode") ?: "" }
    private val paymentViewModel by lazy { getViewModel(PaymentMethodViewModel::class.java) }
    private val firebaseUser by lazy { FirebaseAuth.getInstance().currentUser }
    private val isRecurring by lazy { arguments?.getBoolean("isRecurring") ?: false }

    private val euroPayment by lazy {
        arrayListOf(PaymentModel("Przelewy24",
            ContextCompat.getDrawable(requireContext(), R.drawable.przelewy24), 3),
            PaymentModel("BanContact",
                ContextCompat.getDrawable(requireContext(), R.drawable.bancontact), 4),
            PaymentModel("GiroPay",
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_giropay), 5),
            PaymentModel("iDEAL",
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_ideal_logo), 6),
            PaymentModel("EPS",
                ContextCompat.getDrawable(requireContext(), R.drawable.eps), 7)
        )
    }

    private val stripe: Stripe by lazy {
        Stripe(requireContext(), PaymentConfiguration.getInstance(requireContext()).publishableKey)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        fragmentMethodListBinding = FragmentMethodListBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        PaymentConfiguration.init(requireContext(),BuildConfig.STRIPE_PUBLISHABLE_KEY)

        binding.cardRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.cardRecyclerView.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        val paymentMethodArray = arrayListOf(
            PaymentModel("Credit Card",
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_credit_card), 0))
        if(!isRecurring){
            if (currencyCode.contentEquals("CNY") || currencyCode.contentEquals("SGD")){
                paymentMethodArray.add(PaymentModel("Alipay",
                    ContextCompat.getDrawable(requireContext(), R.drawable.alipay), 1))
            }
        }

        if(currencyCode.contentEquals("EUR") && !isRecurring){
            paymentMethodArray.addAll(euroPayment)
        }

        if(!isRecurring) {
            if(currencyCode.contentEquals("SGD") || currencyCode.contentEquals("MYR")){
                paymentMethodArray.add(PaymentModel("GrabPay",
                    ContextCompat.getDrawable(requireContext(), R.drawable.grabpay), 2))
            }
        }

        val email = firebaseUser?.email
        val name = firebaseUser?.displayName
        binding.cardRecyclerView.adapter = PaymentMethodRecyclerView(paymentMethodArray){ data ->
            if(email.isNullOrBlank() || name.isNullOrBlank()){
                getUserData(data)
            } else {
                itemClicked(data, email, name)
            }
        }
        paymentViewModel.isLoading.observe(viewLifecycleOwner){ isLoading ->
            if(isLoading){
                ProgressBar.animateView(binding.progressLayout.progressOverlay, View.VISIBLE, 0.4f)
            } else {
                ProgressBar.animateView(binding.progressLayout.progressOverlay, View.GONE, 0f)
            }
        }
        paymentViewModel.httpMessage.observe(viewLifecycleOwner){ message ->
            toast(message)
        }
        paymentMethodArray.add(PaymentModel("Pay via Web Browser",
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_credit_card), 8))
    }

    private fun getUserData(cardId: Long){
        val alertDialog = AlertDialog.Builder(requireContext())
        val inflater = layoutInflater.inflate(R.layout.dialog_user_data_collection, null)
        alertDialog.setView(inflater)
        alertDialog.setTitle("Info Required")
        alertDialog.setMessage("Due to regulatory compliance, we require the following information from you.")
        alertDialog.setPositiveButton("OK") { dialog, which ->
            val emailField = inflater.findViewById<TextInputEditText>(R.id.emailEdittext)
            if(emailField.isVisible && emailField.text.toString().isNotBlank()){
                firebaseUser?.updateEmail(emailField.text.toString())
            }
            val nameField = inflater.findViewById<TextInputEditText>(R.id.nameEdittext)
            if(nameField.isVisible && nameField.text.toString().isNotBlank()){
                firebaseUser?.updateProfile(UserProfileChangeRequest
                    .Builder()
                    .setDisplayName(nameField.text.toString())
                    .build())
            }
            dialog.dismiss()
            itemClicked(cardId, firebaseUser?.email ?: "", firebaseUser?.displayName ?: "")
        }
        alertDialog.setNegativeButton("Cancel"){ dialog, which ->
            dialog.dismiss()
        }
        alertDialog.show()
        if(!firebaseUser?.email.isNullOrBlank()){
            val emailField = inflater.findViewById<TextInputEditText>(R.id.emailEdittext)
            emailField.isGone = true
        }

        if(!firebaseUser?.displayName.isNullOrBlank()){
            val nameField = inflater.findViewById<TextInputEditText>(R.id.nameEdittext)
            nameField.isGone = true
        }
    }

    private fun itemClicked(cardId: Long, email: String, name: String) {
        paymentViewModel.createCustomerSession().observe(viewLifecycleOwner) { isSuccessful ->
            if(isSuccessful){
                when (cardId) {
                    0L -> { creditCard() }
                    1L -> { aliPay() }
                    2L -> { grabPay(email, name) }
                    3L -> { p24(name, email) }
                    4L -> { banConnect(name, email) }
                    5L -> { giroPay(name, email) }
                    6L -> { idealPayment(name, email) }
                    7L -> { epsPayment(name, email) }
                    8L -> { webBrowser() }
                }
            } else {
                toast("There was an issue setting up payment")
                dismiss()
            }
        }
    }

    private fun webBrowser(){
        paymentViewModel.getHostedWebpage(currencyCode, isRecurring).observe(viewLifecycleOwner){ url ->
            if(url.isNotBlank()){
                val browserIntent = Intent(Intent.ACTION_VIEW, url.toUri())
                browserIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                requireContext().startActivity(browserIntent)
            }
        }
    }

    private fun idealPayment(name: String, email: String){
        val inflater = layoutInflater.inflate(R.layout.ideal_dialog, null)
        AlertDialog.Builder(requireContext())
            .setView(inflater)
            .setTitle("Bank Name Required")
            .setMessage("What is the name of your bank?")
            .setPositiveButton("OK") { dialog, which ->
                val bankValue = inflater.findViewById<AutoCompleteTextView>(R.id.bankValue)
                var bankName = "abn_amro"
                bankValue.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
                    when (position) {
                        0 -> {
                            bankName = "abn_amro"
                        }
                        1 -> {
                            bankName = "asn_bank"
                        }
                        2 -> {
                            bankName = "bunq"
                        }
                        3 -> {
                            bankName = "handelsbanken"
                        }
                        4 -> {
                            bankName = "ing"
                        }
                        5 -> {
                            bankName = "knab"
                        }
                        6 -> {
                            bankName = "moneyou"
                        }
                        7 -> {
                            bankName = "rabobank"
                        }
                        8 -> {
                            bankName = "revolut"
                        }
                        9 -> {
                            bankName = "regiobank"
                        }
                        10 -> {
                            bankName = "sns_bank"
                        }
                        11 -> {
                            bankName = "triodos_bank"
                        }
                        12 -> {
                            bankName = "van_lanschot"
                        }
                    }
                }
                paymentViewModel.purchasePlan("", currencyCode, "ideal").observe(viewLifecycleOwner) { clientSecret ->
                    val billingDetails = PaymentMethod.BillingDetails(name = name, email = email)
                    val paymentMethodCreateParams = PaymentMethodCreateParams.create(
                        PaymentMethodCreateParams.Ideal(bankName),
                        billingDetails)
                    val confirmParams = ConfirmPaymentIntentParams
                        .createWithPaymentMethodCreateParams(
                            paymentMethodCreateParams = paymentMethodCreateParams,
                            clientSecret = clientSecret)
                    stripe.confirmPayment(this, confirmParams)
                    paymentSuccess()
                }
            }
            .setNegativeButton("Cancel"){ dialog, which ->
                dialog.dismiss()
            }

    }

    private fun epsPayment(name: String, email: String){
        paymentViewModel.purchasePlan("", currencyCode, "eps").observe(viewLifecycleOwner) { clientSecret ->
            val billingDetails = PaymentMethod.BillingDetails(name = name, email = email)
            val paymentMethodCreateParams = PaymentMethodCreateParams.createEps(billingDetails)
            val confirmParams = ConfirmPaymentIntentParams
                .createWithPaymentMethodCreateParams(
                    paymentMethodCreateParams = paymentMethodCreateParams,
                    clientSecret = clientSecret
                )
            stripe.confirmPayment(this, confirmParams)
            paymentSuccess()
        }
    }

    private fun banConnect(name: String, email: String){
        paymentViewModel.purchasePlan("", currencyCode,"bancontact").observe(viewLifecycleOwner) { clientSecret ->
            val billingDetails = PaymentMethod.BillingDetails(name = name, email = email)
            val paymentMethodCreateParams = PaymentMethodCreateParams.createBancontact(billingDetails)
            val confirmParams = ConfirmPaymentIntentParams
                .createWithPaymentMethodCreateParams(
                    paymentMethodCreateParams = paymentMethodCreateParams,
                    clientSecret = clientSecret
                )
            stripe.confirmPayment(this, confirmParams)
            paymentSuccess()
        }
    }

    private fun p24(name: String, email: String){
        paymentViewModel.purchasePlan("", currencyCode, "p24").observe(viewLifecycleOwner) { clientSecret ->
            val billingDetails = PaymentMethod.BillingDetails(name = name, email = email)
            val paymentMethodCreateParams = PaymentMethodCreateParams.createP24(billingDetails)
            val confirmParams = ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                paymentMethodCreateParams = paymentMethodCreateParams,
                clientSecret = clientSecret,
                savePaymentMethod = true)
            stripe.confirmPayment(this, confirmParams)
            paymentSuccess()
        }

    }

    private fun giroPay(name: String, email: String){
        paymentViewModel.purchasePlan("", currencyCode, "giropay").observe(viewLifecycleOwner) { clientSecret ->
            val billingDetails = PaymentMethod.BillingDetails(name = name, email = email)
            val paymentMethodCreateParams = PaymentMethodCreateParams.createGiropay(billingDetails)
            val confirmParams = ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                paymentMethodCreateParams = paymentMethodCreateParams,
                clientSecret = clientSecret,
                savePaymentMethod = true)
            stripe.confirmPayment(this, confirmParams)
            paymentSuccess()
        }

    }

    private fun paymentSuccess(){
        paymentViewModel.getSubscriptionEnd().observe(viewLifecycleOwner){ isSuccess ->
            if(isSuccess){
                dismiss()
                parentFragmentManager.popBackStack()
                toast("Thank you! Payment Succeeded")
            } else {
                AlertDialog.Builder(requireContext())
                    .setTitle("Issue getting payment")
                    .setMessage("There was an issue getting your subscription. Please wait for a few minutes. Close the app and open again.")
                    .setPositiveButton("OK"){ _, _ ->
                        requireActivity().finish()
                    }
                    .show()
            }
        }
    }

    private fun aliPay(){
        paymentViewModel.purchasePlan("", currencyCode,"alipay").observe(viewLifecycleOwner) { clientSecret ->
            val confirmParams = ConfirmPaymentIntentParams.createAlipay(clientSecret)
            stripe.confirmAlipayPayment(confirmParams,
                authenticator = { data -> PayTask(requireActivity()).payV2(data, true) },
                callback = object : ApiResultCallback<PaymentIntentResult>{
                    override fun onError(e: Exception) {
                        stripe.confirmPayment(requireActivity(), confirmParams)
                    }

                    override fun onSuccess(result: PaymentIntentResult) {
                        val paymentIntent = result.intent
                        when(paymentIntent.status){
                            StripeIntent.Status.Succeeded -> {
                                paymentSuccess()
                            }
                            else -> {
                                toast("Payment Cancelled")
                            }
                        }
                    }

                }
            )
        }
    }

    private fun grabPay(email: String, name: String){
        paymentViewModel.purchasePlan("", currencyCode,"grabpay").observe(viewLifecycleOwner){ clientSecret ->
            val billingDetails = PaymentMethod.BillingDetails(name = name, email = email)
            val paymentMethodCreateParams = PaymentMethodCreateParams.createGrabPay(billingDetails)
            val confirmParams = ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                paymentMethodCreateParams = paymentMethodCreateParams, clientSecret = clientSecret)
            stripe.confirmPayment(requireActivity(), confirmParams)
            paymentSuccess()
        }
    }


    private fun creditCard() {
        paymentSession = PaymentSession(this, PaymentSessionConfig.Builder()
            .setShippingInfoRequired(false)
            .setShippingMethodsRequired(false)
            .setCanDeletePaymentMethods(true)
            .setAddPaymentMethodFooter(R.layout.stripe_footer)
            .setPaymentMethodsFooter(R.layout.stripe_footer)
            .setPaymentMethodTypes(listOf(PaymentMethod.Type.Card))
            .build())
        paymentSession?.init(object : PaymentSession.PaymentSessionListener {
                override fun onCommunicatingStateChanged(isCommunicating: Boolean) {
                    paymentViewModel.isLoading.postValue(isCommunicating)
                }

                override fun onError(errorCode: Int, errorMessage: String) {
                    toast(errorMessage, Toast.LENGTH_LONG)
                    dismiss()
                }

                override fun onPaymentSessionDataChanged(data: PaymentSessionData) {
                    val paymentMethodId = data.paymentMethod?.id
                    val isReadyToCharge = data.isPaymentReadyToCharge
                    if(paymentMethodId != null && isReadyToCharge){
                        paymentViewModel.purchasePlan(paymentMethodId, currencyCode,
                            isRecurring = isRecurring).observe(viewLifecycleOwner){ clientSecret ->
                            if(!isRecurring){
                                stripe.confirmPayment(this@PaymentMethodFragment,
                                    ConfirmPaymentIntentParams.createWithPaymentMethodId(paymentMethodId = paymentMethodId,
                                        clientSecret = clientSecret, savePaymentMethod = true))
                            } else {
                                paymentSuccess()
                            }
                        }
                    }
                }
            })
        paymentSession?.presentPaymentMethodSelection()
    }

    private fun processStripeIntent(stripeIntent: StripeIntent,
                                    paymentMethod: PaymentMethod?){
        if (stripeIntent.requiresAction()) {
            val clientSecret = stripeIntent.clientSecret
            if(clientSecret != null){
                stripe.handleNextActionForPayment(this, clientSecret)
            }
        } else if (stripeIntent.status == StripeIntent.Status.Succeeded) {
            if (stripeIntent is PaymentIntent) {
                paymentSuccess()
            }
        } else if (stripeIntent.status == StripeIntent.Status.RequiresPaymentMethod) {
            if (stripeIntent is PaymentIntent) {
                stripe.confirmPayment(
                    this, ConfirmPaymentIntentParams.createWithPaymentMethodId(
                        paymentMethodId = paymentMethod?.id.orEmpty(),
                        clientSecret = requireNotNull(stripeIntent.clientSecret)))
            } else if (stripeIntent is SetupIntent) {
                stripe.confirmSetupIntent(
                    this,
                    ConfirmSetupIntentParams.create(
                        paymentMethodId = paymentMethod?.id.orEmpty(),
                        clientSecret = requireNotNull(stripeIntent.clientSecret)
                    ))
            }
        } else {
            dismiss()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(data != null && paymentSession != null){
            paymentSession?.handlePaymentData(requestCode, resultCode, data)
            stripe.onPaymentResult(requestCode, data, object : ApiResultCallback<PaymentIntentResult>{
                override fun onError(e: Exception) {
                    toast("There was an issue processing your payment data", Toast.LENGTH_LONG)
                    dismiss()
                }

                override fun onSuccess(result: PaymentIntentResult) {
                    processStripeIntent(result.intent, result.intent.paymentMethod)
                }
            })
        }
    }
}
