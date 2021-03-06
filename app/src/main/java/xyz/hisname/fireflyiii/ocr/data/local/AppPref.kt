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

package xyz.hisname.fireflyiii.ocr.data.local

import android.content.SharedPreferences
import androidx.core.content.edit

class AppPref(private val sharedPreferences: SharedPreferences) {

    var endTime
        get() = sharedPreferences.getLong("endTime", 0)
        set(value) = sharedPreferences.edit{ putLong("endTime", value) }

    var startTime
        get() = sharedPreferences.getLong("startTime", 0)
        set(value) = sharedPreferences.edit{ putLong("startTime", value) }

    var paymentSource
        get() = sharedPreferences.getString("paymentSource", "") ?: ""
        set(value) = sharedPreferences.edit { putString("paymentSource", value) }
}