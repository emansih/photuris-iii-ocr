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

package utils

import com.maxmind.geoip2.DatabaseReader
import java.net.InetAddress

class GeoLite {

    fun getCountryIsoCode(ipAddress: String): String{
        val databaseFile = GeoLite::class.java.getResourceAsStream("/GeoLite2-Country.mmdb")
        val dbReader = DatabaseReader.Builder(databaseFile).build()
        val inetAddress = InetAddress.getByName(ipAddress)
        return dbReader.country(inetAddress).country.isoCode
    }

    fun getCountryName(ipAddress: String): String{
        if(InetAddress.getByName(ipAddress).isAnyLocalAddress){
            val databaseFile = GeoLite::class.java.getResourceAsStream("/GeoLite2-Country.mmdb")
            val dbReader = DatabaseReader.Builder(databaseFile).build()
            val inetAddress = InetAddress.getByName(ipAddress)
            return dbReader.country(inetAddress).country.name
        } else {
           return "US"
        }
    }

}