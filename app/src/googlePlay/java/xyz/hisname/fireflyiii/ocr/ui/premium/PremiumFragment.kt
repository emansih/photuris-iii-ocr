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

package xyz.hisname.fireflyiii.ocr.ui.premium

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.android.billingclient.api.*
import com.google.firebase.auth.FirebaseAuth
import xyz.hisname.fireflyiii.ocr.databinding.FragmentPremiumBinding
import xyz.hisname.fireflyiii.ocr.ui.ProgressBar
import xyz.hisname.fireflyiii.ocr.utils.extension.getViewModel
import xyz.hisname.fireflyiii.ocr.utils.extension.toast


class PremiumFragment: Fragment(), BillingClientStateListener {

    private var fragmentPremiumBinding: FragmentPremiumBinding? = null
    private val binding get() = fragmentPremiumBinding!!
    private val premiumViewModel by lazy { getViewModel(PremiumViewModel::class.java) }
    private lateinit var billingClient: BillingClient

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        fragmentPremiumBinding = FragmentPremiumBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setPremiumFeatures()
        billingClient = BillingClient.newBuilder(requireContext())
            .enablePendingPurchases()
            .setListener { billingResult: BillingResult, purchaseList: MutableList<Purchase>? ->
                onPurchasesUpdated(billingResult)
            }
            .build()
        billingClient.startConnection(this)
        // Text has to be set here. No idea why. Magic
        premiumViewModel.getPlayPrice.observe(viewLifecycleOwner){ price ->
            binding.currencyText.text = price
        }

    }

    private fun setPremiumFeatures(){
        binding.premiumFeatures.text = "Benefits of getting premium: \n" +
                "• Higher accuracy by using on cloud machine learning \n" +
                "• OCR for PDF files"
    }

    private fun queryOneTimeProducts() {
        val skuListToQuery = arrayListOf("ocrsubscription")
        val params = SkuDetailsParams.newBuilder()
            .setSkusList(skuListToQuery)
            .setType(BillingClient.SkuType.SUBS)
            .build()

        billingClient.querySkuDetailsAsync(params){ billingResult, mutableList ->
            if(!mutableList.isNullOrEmpty()){
                premiumViewModel.setPrice(mutableList[0].price)
                binding.subscribeButton.setOnClickListener {
                    val firebaseId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                    val flowParams = BillingFlowParams.newBuilder()
                        .setObfuscatedAccountId(firebaseId)
                        .setSkuDetails(mutableList[0])
                        .build()
                    billingClient.launchBillingFlow(requireActivity(), flowParams)

                }
            }
        }
    }

    private fun onPurchasesUpdated(billingResult: BillingResult) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            ProgressBar.animateView(binding.progressLayout.progressOverlay, View.VISIBLE, 0.4f)
            premiumViewModel.getSubscriptionEnd().observe(viewLifecycleOwner){
                ProgressBar.animateView(binding.progressLayout.progressOverlay, View.GONE, 0f)
                parentFragmentManager.popBackStack()
            }
        }
    }

    override fun onBillingServiceDisconnected() {
        toast("Disconnected from Google Play")
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            queryOneTimeProducts()
        }
    }
}