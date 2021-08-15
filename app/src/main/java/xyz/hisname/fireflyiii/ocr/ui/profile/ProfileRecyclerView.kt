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

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import xyz.hisname.fireflyiii.ocr.databinding.ProfileItemsBinding
import xyz.hisname.fireflyiii.ocr.models.ProfileModel

class ProfileRecyclerView(private val profileModel: List<ProfileModel>,
                          private val clickListener:(position: Int) -> Unit): RecyclerView.Adapter<ProfileRecyclerView.ProfileHolder>() {

    private var profileItemsBinding: ProfileItemsBinding? = null
    private val binding get() = profileItemsBinding!!

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileHolder {
        profileItemsBinding = ProfileItemsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProfileHolder(binding)
    }

    override fun onBindViewHolder(holder: ProfileHolder, position: Int) {
        holder.bind(profileModel[position], position)
    }

    override fun getItemCount() = profileModel.size


    inner class ProfileHolder(itemView: ProfileItemsBinding): RecyclerView.ViewHolder(itemView.root){
        fun bind(profileModel: ProfileModel, clickListener: Int){
            binding.profileItem.text = profileModel.items
            binding.root.setOnClickListener{ clickListener(clickListener) }
        }
    }
}