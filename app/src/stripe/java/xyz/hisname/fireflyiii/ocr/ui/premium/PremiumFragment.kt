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
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.google.firebase.auth.FirebaseAuth
import xyz.hisname.fireflyiii.ocr.R
import xyz.hisname.fireflyiii.ocr.databinding.FragmentPremiumBinding
import xyz.hisname.fireflyiii.ocr.ui.ProgressBar
import xyz.hisname.fireflyiii.ocr.ui.paymentmethod.PaymentMethodFragment
import xyz.hisname.fireflyiii.ocr.utils.extension.getViewModel
import xyz.hisname.fireflyiii.ocr.utils.extension.toast
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*

class PremiumFragment: Fragment() {

    private var fragmentPremiumBinding: FragmentPremiumBinding? = null
    private val binding get() = fragmentPremiumBinding!!
    private val premiumViewModel by lazy { getViewModel(PremiumViewModel::class.java) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        fragmentPremiumBinding = FragmentPremiumBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setPremiumFeatures()
        setServicePaymentDate()
        setText()
        binding.subscribeButton.setOnClickListener {
            val paymentMethodFragment = PaymentMethodFragment()
            paymentMethodFragment.arguments  = bundleOf("currencyCode" to premiumViewModel.currencyCode,
                "isRecurring" to binding.subscriptionCheckbox.isChecked)
            paymentMethodFragment.show(parentFragmentManager, "paymentMethodFragment")
        }
        premiumViewModel.isLoading.observe(viewLifecycleOwner){ isLoading ->
            if(isLoading){
                ProgressBar.animateView(binding.progressLayout.progressOverlay, View.VISIBLE, 0.4f)
            } else {
                ProgressBar.animateView(binding.progressLayout.progressOverlay, View.GONE, 0f)
            }
        }
    }

    private fun setServicePaymentDate(){
        val sixMonthsFromNow = LocalDate.now().plusMonths(6).plusDays(2)
        val dayOfMonth = sixMonthsFromNow.dayOfMonth
        val month = sixMonthsFromNow.month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())
        val year = sixMonthsFromNow.year
        val date = "$dayOfMonth $month $year"
        binding.subscriptionCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked){
                binding.serviceDuration.visibility = View.VISIBLE
                binding.serviceDuration.text = getString(R.string.service_duration, date)
            } else {
                binding.serviceDuration.visibility = View.INVISIBLE
            }
        }
    }

    private fun setText(){
        premiumViewModel.getPrice().observe(viewLifecycleOwner){ price ->
            binding.currencyText.text = price + " / 6 months"
        }
        val userTask = FirebaseAuth.getInstance().currentUser?.reload()
        userTask?.addOnSuccessListener {
            premiumViewModel.isStudent.observe(viewLifecycleOwner){ isAvailable ->
                if(isAvailable && FirebaseAuth.getInstance().currentUser?.isEmailVerified == false){
                    binding.studentLayout.visibility = View.VISIBLE
                    binding.verifyEmailButton.setOnClickListener {
                        FirebaseAuth.getInstance().currentUser?.sendEmailVerification()
                        toast("Email sent to: " + FirebaseAuth.getInstance().currentUser?.email)
                        parentFragmentManager.popBackStack()
                    }
                } else {
                    binding.studentLayout.visibility = View.INVISIBLE
                }
            }
        }
    }

    private fun setPremiumFeatures(){
        binding.premiumFeatures.text = "Benefits of getting premium: \n" +
                "• Higher accuracy by using cloud machine learning \n" +
                "• OCR for PDF files \n" +
                "• First 2 days is free!"
    }
}