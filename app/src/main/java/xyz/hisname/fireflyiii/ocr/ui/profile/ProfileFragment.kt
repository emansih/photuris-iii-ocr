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

package xyz.hisname.fireflyiii.ocr.ui.profile

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import xyz.hisname.fireflyiii.ocr.Constants
import xyz.hisname.fireflyiii.ocr.R
import xyz.hisname.fireflyiii.ocr.databinding.FragmentProfileBinding
import xyz.hisname.fireflyiii.ocr.models.ProfileModel
import xyz.hisname.fireflyiii.ocr.ui.history.HistoryFragment
import xyz.hisname.fireflyiii.ocr.utils.extension.getViewModel
import xyz.hisname.fireflyiii.ocr.utils.extension.toast

class ProfileFragment: Fragment() {

    private var fragmentProfileBinding: FragmentProfileBinding? = null
    private val binding get() = fragmentProfileBinding!!
    private val profileViewModel by lazy { getViewModel(ProfileViewModel::class.java) }
    private lateinit var signInIntent: ActivityResultLauncher<Intent>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        fragmentProfileBinding = FragmentProfileBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        signIn()
    }

    private fun signIn() {
        profileViewModel.userSubscriptionEnd.observe(viewLifecycleOwner){ endDate ->
            binding.subscriptionValidity.text = endDate
        }
        FirebaseAuth.getInstance().addAuthStateListener {  firebaseAuth ->
            val user = firebaseAuth.currentUser
            if(user != null){
                // signed in
                setStatus(user)
                binding.signInView.isGone = true
                binding.userProfileLayout.isVisible = true
            } else {
                binding.signInView.isVisible = true
                binding.userProfileLayout.isGone = true
            }
        }

        binding.signInButton.setOnClickListener {
            val providers = arrayListOf(
                    AuthUI.IdpConfig.EmailBuilder().build(),
                    AuthUI.IdpConfig.GoogleBuilder().build(),
                    AuthUI.IdpConfig.GitHubBuilder().build(),
                    AuthUI.IdpConfig.MicrosoftBuilder().build())
            signInIntent.launch(AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAlwaysShowSignInMethodScreen(true)
                .setIsSmartLockEnabled(false)
                .setTosAndPrivacyPolicyUrls(Constants.PRIVACY_URL, Constants.TOS_URL)
                .setAvailableProviders(providers)
                .build())
        }
    }

    private fun itemClicked(position: Int){
        if(position == 0){
            parentFragmentManager.commit {
                replace(R.id.frameLayout, HistoryFragment())
                addToBackStack(null)
            }
        } else if(position == 1){
            val browserIntent = Intent(Intent.ACTION_VIEW, Constants.PRIVACY_URL.toUri())
            browserIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            if (browserIntent.resolveActivity(requireContext().packageManager) != null) {
                requireContext().startActivity(browserIntent)
            }  else {
                toast("No browser installed")
            }
        }
    }

    private fun setStatus(user: FirebaseUser?){
        binding.name.text = user?.displayName
        binding.email.text = user?.email
        binding.userProfile.layoutManager = LinearLayoutManager(requireContext())
        binding.userProfile.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        val profileModelArray = arrayListOf(
            ProfileModel("Subscription History"),
            ProfileModel("Privacy Policy")
        )

        binding.userProfile.adapter = ProfileRecyclerView(profileModelArray){ data ->
            itemClicked(data)
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        signInIntent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
            if (activityResult.resultCode != Activity.RESULT_OK) {
                Toast.makeText(requireActivity(), "Sign In Cancelled", Toast.LENGTH_SHORT).show()
            } else if(activityResult.resultCode == Activity.RESULT_OK){
                profileViewModel.getCustomer()
            }
        }
    }


}