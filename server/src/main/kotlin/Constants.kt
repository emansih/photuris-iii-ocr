/*
 * Copyright (c)  2021 ASDF Dev Pte. Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */
class Constants private constructor() {

    companion object {
        val AZUREKEY: String? = System.getenv("AZUREKEY")
        val AZUREURL: String? = System.getenv("AZUREURL")
        val STRIPE_SECRET_KEY: String? = System.getenv("STRIPE_SECRET_KEY")
        val STORAGE_BUCKET: String? = System.getenv("STORAGE_BUCKET")
		val STRIPE_WEB_HOOK_KEY: String? = System.getenv("STRIPE_WEB_HOOK_KEY")
        val APP_URL: String? = System.getenv("APP_URL")
        val IS_DEBUG: Boolean = System.getenv("").toBoolean()
        const val PACKAGE_NAME = "xyz.hisname.fireflyiii.ocr"
        const val TEST_EMAIL = "RandomEmail@gmail.com"
        const val TEST_FIREBASE_ID = "some_random_id"
    }
}