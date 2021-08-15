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

package xyz.hisname.fireflyiii.ocr.ui

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.preference.PreferenceManager
import com.google.firebase.auth.FirebaseAuth
import xyz.hisname.fireflyiii.ocr.R
import xyz.hisname.fireflyiii.ocr.data.local.AppPref
import xyz.hisname.fireflyiii.ocr.databinding.FragmentHomeBinding
import xyz.hisname.fireflyiii.ocr.ui.camera.CameraFragment
import xyz.hisname.fireflyiii.ocr.ui.premium.PremiumFragment
import xyz.hisname.fireflyiii.ocr.utils.extension.getViewModel
import xyz.hisname.fireflyiii.ocr.utils.extension.toast
import java.io.File
import java.time.Instant

class HomeFragment: Fragment() {

    private var fragmentHomeBinding: FragmentHomeBinding? = null
    private val binding get() = fragmentHomeBinding!!
    private lateinit var chooseDocument: ActivityResultLauncher<Array<String>>
    private val homeViewModel by lazy { getViewModel(HomeViewModel::class.java) }
    private val fireflyIntent = Intent()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        fragmentHomeBinding = FragmentHomeBinding.inflate(inflater, container, false)
        fireflyIntent.action = "firefly.hisname.PREFILL_TRANSACTION"
        val view = binding.root
        return view
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chooseDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { fileChoosen ->
            if (fileChoosen != null && fileChoosen.path != null) {
                val fileName = File(fileChoosen.path).name
                homeViewModel.uploadDocument(requireContext().contentResolver, fileChoosen, fileName).observe(viewLifecycleOwner) { receiptData ->
                    AlertDialog.Builder(requireContext())
                        .setTitle("Scanned Data")
                        .setMessage("Description: " + receiptData?.merchantName + "\n" +
                                    "Amount: " + receiptData?.totalPrice + "\n" +
                                    "Date: "  + receiptData?.date + "\n" +
                                    "Time: " + receiptData?.time)
                        .setPositiveButton("OK"){ _,_ ->
                            if(isInstalled(fireflyIntent)){
                                homeViewModel.uploadDocument(requireContext().contentResolver,
                                    fileChoosen, fileName).observe(viewLifecycleOwner) { receiptData ->
                                    if(isInstalled(fireflyIntent)){
                                        fireflyIntent.apply {
                                            putExtra("description", receiptData?.merchantName)
                                            putExtra("amount", receiptData?.totalPrice)
                                            putExtra("date", receiptData?.date)
                                            putExtra("time", receiptData?.time)
                                            putExtra("transactionType", "withdrawal")
                                        }
                                        startActivity(fireflyIntent)
                                    }
                                }
                            } else {
                                toast("Please install the main app")
                            }
                        }
                        .show()
                }
            }
        }
    }

    private fun isInstalled(intent: Intent): Boolean {
       val list = requireActivity().packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return list.size > 0
    }

    private fun setInstalledStatus(){
        if(isInstalled(fireflyIntent)){
            homeViewModel.subscriptionStatus()
            switchUi()
            binding.subscribeButton.setOnClickListener {
                parentFragmentManager.commit {
                    replace(R.id.frameLayout, PremiumFragment())
                    addToBackStack(null)
                }
            }
            binding.cameraCircle.setOnClickListener {
                parentFragmentManager.commit {
                    replace(R.id.frameLayout, CameraFragment())
                    addToBackStack(null)
                }
            }
            binding.documentCircle.setOnClickListener {
                chooseDocument.launch(arrayOf("application/pdf", "image/jpeg", "image/png"))
            }
            homeViewModel.isLoading.observe(viewLifecycleOwner){ loader ->
                if(loader){
                    ProgressBar.animateView(binding.progressLayout.progressOverlay, View.VISIBLE, 0.4f)
                } else {
                    ProgressBar.animateView(binding.progressLayout.progressOverlay, View.GONE, 0f)
                }
            }
            homeViewModel.httpMessage.observe(viewLifecycleOwner){ message ->
                toast(message, Toast.LENGTH_LONG)
            }
        } else {
            binding.ocrInstructions.text = "Please install the main app"
            binding.documentCircle.isVisible = false
            binding.documentText.isVisible = false
            binding.cameraCircle.isVisible = false
            binding.cameraText.isVisible = false
            binding.subscribeText.isVisible = false
            binding.subscribeButton.isVisible = true
            binding.subscribeButton.text = "Click here to install"
            binding.subscribeButton.setOnClickListener {
                val browserIntent = Intent(Intent.ACTION_VIEW, ("https://github.com/emansih/FireflyMobile").toUri())
                browserIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                requireContext().startActivity(browserIntent)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setInstalledStatus()
    }

    override fun onResume() {
        super.onResume()
        setInstalledStatus()
    }

    private fun switchUi(){
        shouldShowDocument()
        FirebaseAuth.getInstance().addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if(user == null){
                binding.subscribeButton.isVisible = false
                binding.subscribeLayout.isVisible = false
                binding.documentText.isVisible = false
                binding.documentCircle.isVisible = false
            }
        }
    }

    private fun shouldShowDocument(){
        val appPref = AppPref(PreferenceManager.getDefaultSharedPreferences(requireContext()))
        if(appPref.endTime != 0L && appPref.endTime >= (Instant.now().epochSecond)){
            binding.documentText.isVisible = true
            binding.documentCircle.isVisible = true
            binding.subscribeLayout.isVisible = false
        } else {
            binding.subscribeLayout.isVisible = true
            binding.documentText.isVisible = false
            binding.documentCircle.isVisible = false
        }
        SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if(key?.contentEquals("endTime") == true){
                val endTime = sharedPreferences.getLong("endTime", 0L)
                if(endTime != 0L && endTime >= (Instant.now().epochSecond)){
                    binding.documentText.isVisible = false
                    binding.documentCircle.isVisible = false
                } else {
                    binding.documentText.isVisible = true
                    binding.documentCircle.isVisible = true
                }
            }
        }
    }
}