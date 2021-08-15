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

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.commit
import xyz.hisname.fireflyiii.ocr.BuildConfig
import xyz.hisname.fireflyiii.ocr.R
import xyz.hisname.fireflyiii.ocr.databinding.ActivityMainBinding
import xyz.hisname.fireflyiii.ocr.ui.profile.ProfileFragment

class MainActivity : AppCompatActivity() {


    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        supportFragmentManager.commit {
            replace(R.id.frameLayout, HomeFragment())
        }
        setBottomNav()
    }

    private fun setBottomNav(){
        binding.bottomNavigation.setOnNavigationItemSelectedListener{ item ->
            when(item.itemId) {
                R.id.action_home -> {
                    supportFragmentManager.commit {
                        replace(R.id.frameLayout, HomeFragment())
                    }
                }
                R.id.action_account -> {
                    supportFragmentManager.commit {
                        replace(R.id.frameLayout, ProfileFragment())
                        addToBackStack(null)
                    }
                }
            }
            true
        }
    }
}