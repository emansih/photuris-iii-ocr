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

package xyz.hisname.fireflyiii.ocr.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import xyz.hisname.fireflyiii.ocr.databinding.FragmentHistoryBinding
import xyz.hisname.fireflyiii.ocr.utils.extension.getViewModel
import xyz.hisname.fireflyiii.ocr.utils.extension.toast

class HistoryFragment: Fragment() {

    private var fragmentHistory: FragmentHistoryBinding? = null
    private val binding get() = fragmentHistory!!
    private val historyViewModel by lazy { getViewModel(HistoryViewModel::class.java) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View{
        fragmentHistory = FragmentHistoryBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.historyList.layoutManager = LinearLayoutManager(requireContext())
        getList()
        setSwipe()
    }

    private fun getList(){
        historyViewModel.getCustomerHistory().observe(viewLifecycleOwner){ purchases ->
            binding.historyList.adapter = HistoryRecyclerView(purchases){ data ->
                itemClicked(data)
            }
        }
    }


    private fun setSwipe(){
        historyViewModel.isLoading.observe(viewLifecycleOwner){ refreshing ->
            binding.swipeLayout.isRefreshing = refreshing
        }
        binding.swipeLayout.setOnRefreshListener {
            getList()
        }
    }

    private fun itemClicked(orderId: String){
        AlertDialog.Builder(requireContext())
            .setTitle("Sorry to see you go!")
            .setMessage("I want to sincerely apologize for the negative experience that you had with " +
                    "this service. Please do not hesitate to contact me directly to improve on the product. ")
            .setPositiveButton("Dismiss"){ dialog, _ ->
                dialog.dismiss()
            }
            .setNegativeButton("Cancel Subscription"){ _, _ ->
                historyViewModel.cancelSub(orderId).observe(viewLifecycleOwner){ isSuccess ->
                    if(isSuccess){
                        toast("Success!")
                    } else {
                        AlertDialog.Builder(requireContext())
                            .setTitle("There was an issue handling your transaction")
                            .setMessage("Please contact me ASAP to sort this out. ")
                            .setPositiveButton(android.R.string.ok){ dialog, _ ->
                                dialog.dismiss()
                            }
                            .show()
                    }
                }
            }
            .show()
    }
}