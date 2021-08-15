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

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import com.google.firebase.auth.FirebaseAuth
import xyz.hisname.fireflyiii.ocr.data.local.AppPref
import xyz.hisname.fireflyiii.ocr.data.local.UserRepository
import xyz.hisname.fireflyiii.ocr.data.network.RetrofitClient
import xyz.hisname.fireflyiii.ocr.data.network.UserEndpoint

abstract class BaseViewModel(application: Application): AndroidViewModel(application) {


    protected val appPref by lazy { AppPref(PreferenceManager.getDefaultSharedPreferences(getApplication())) }
    protected val isLoggedOut = FirebaseAuth.getInstance().uid.isNullOrBlank()

    protected val networkService by lazy { RetrofitClient.createClient(FirebaseAuth.getInstance().uid ?: "") }

    protected val userEndpoint by lazy { networkService.create(UserEndpoint::class.java) }
    protected val userRepository by lazy { UserRepository(appPref, userEndpoint) }

    val isLoading = MutableLiveData<Boolean>()
    val httpMessage = MutableLiveData<String>()

    override fun onCleared() {
        super.onCleared()
        RetrofitClient.destroy()
    }
}