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

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import xyz.hisname.fireflyiii.ocr.databinding.PaymentItemsBinding
import xyz.hisname.fireflyiii.ocr.models.PaymentModel

class PaymentMethodRecyclerView(private val paymentModel: List<PaymentModel>,
                                private val clickListener:(position: Long) -> Unit):
    RecyclerView.Adapter<PaymentMethodRecyclerView.PaymentHolder>() {

    private var paymentItemsBinding: PaymentItemsBinding? = null
    private val binding get() = paymentItemsBinding!!
    private lateinit var context: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentHolder {
        paymentItemsBinding = PaymentItemsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        context = parent.context
        return PaymentHolder(binding)
    }

    override fun onBindViewHolder(holder: PaymentHolder, position: Int) {
        holder.bind(paymentModel[position])
    }

    override fun getItemCount() = paymentModel.size

    inner class PaymentHolder(itemView: PaymentItemsBinding): RecyclerView.ViewHolder(itemView.root){
        fun bind(paymentModel: PaymentModel){
            Glide.with(context)
                .load(paymentModel.cardImage)
                .into(binding.paymentIcon)
            binding.paymentMethod.text = paymentModel.cardName
            itemView.rootView.setOnClickListener{ clickListener(paymentModel.cardPaymentId) }
        }
    }

}