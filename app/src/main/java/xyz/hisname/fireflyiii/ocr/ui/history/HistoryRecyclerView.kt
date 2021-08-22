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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import xyz.hisname.fireflyiii.ocr.databinding.PurchaseHistoryItemsBinding
import xyz.hisname.fireflyiii.ocr.models.CustomerPurchases
import java.time.*
import java.time.format.TextStyle
import java.util.*

class HistoryRecyclerView(private val historyItems: List<CustomerPurchases>,
                          private val clickListener:(orderId: String) -> Unit): RecyclerView.Adapter<HistoryRecyclerView.HistoryItems>() {

    private var purchaseHistoryItemsBinding: PurchaseHistoryItemsBinding? = null
    private val binding get() = purchaseHistoryItemsBinding!!

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryItems {
        purchaseHistoryItemsBinding = PurchaseHistoryItemsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryItems(binding)
    }

    override fun onBindViewHolder(holder: HistoryItems, position: Int) {
        holder.bind(historyItems[position])
    }

    override fun getItemCount() = historyItems.size

    inner class HistoryItems(itemView: PurchaseHistoryItemsBinding): RecyclerView.ViewHolder(itemView.root){
        fun bind(customerPurchases: CustomerPurchases){
            binding.orderId.text = customerPurchases.orderId
            var paymentSource = customerPurchases.customer.paymentSource
            val source = if(customerPurchases.customer.paymentSource.contentEquals("googlePlay")){
                paymentSource = "googlePlay"
                "Purchased from: Google Play"
            } else if(customerPurchases.customer.paymentSource.contentEquals("stripe")){
                paymentSource = "stripe"
                "Purchased from: Stripe"
            } else if(customerPurchases.customer.paymentSource.contentEquals("paypal")){
                paymentSource = "paypal"
                "Purchased from: Paypal"
            } else {
                paymentSource = "freeTrial"
                "Free Trial"
            }
            binding.purchaseSource.text = source
            val startTime = customerPurchases.customer.startTime
            val endTime = customerPurchases.customer.endTime
            binding.duration.text = getDisplayDate(startTime)  + " - " + getDisplayDate(endTime)
            val currentTime = Instant.now().epochSecond
            if(endTime > currentTime){
                if(paymentSource.contentEquals("freeTrial")){
                    binding.refundButton.visibility = View.GONE
                } else {
                    if(customerPurchases.subscription){
                        binding.refundButton.text = "Cancel Subscription"
                        binding.refundButton.setOnClickListener { clickListener(customerPurchases.orderId) }
                    } else {
                        binding.refundButton.visibility = View.GONE
                    }
                }

            } else {
                binding.refundButton.visibility = View.GONE
            }
        }

        private fun getDisplayDate(epochSecond: Long): String{
            val date = LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSecond), ZoneId.systemDefault())
            val dayOfMonth = date.dayOfMonth
            val month = date.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
            val year = date.year
            return "$dayOfMonth $month $year"
        }
    }
}