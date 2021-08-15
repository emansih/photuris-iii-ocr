/*
 *  The MIT License (MIT)
 *  Copyright (c) 2013 Lee Reilly
 *  Copyright (c) 2021 ASDF Dev Pte Ltd
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  https://github.com/JetBrains/swot
 */

package utils

import java.util.*

fun isAcademic(email: String): Boolean {
    val parts = domainParts(email)
    return !isStoplisted(parts) && (isUnderTLD(parts) || findSchoolNames(parts).isNotEmpty())
}

fun findSchoolNames(emailOrDomain: String): List<String> {
    return findSchoolNames(domainParts(emailOrDomain))
}

fun isUnderTLD(parts: List<String>): Boolean {
    return checkSet(Resources.tlds, parts)
}

fun isStoplisted(parts: List<String>): Boolean {
    return checkSet(Resources.stoplist, parts)
}

/*
 * Heng Swee Kiat once said that `every school is a good school`.
 * http://web.archive.org/web/20201112015236/https://www.straitstimes.com/singapore/education/heng-swee-keat-as-education-minister-a-study-in-bold-moves
 * https://www.straitstimes.com/singapore/education/heng-swee-keat-as-education-minister-a-study-in-bold-moves
 *
 * Sometimes I say dumb shit in my code.
 */

fun isGoodSchool(schoolName: List<String>): Boolean {
    if(schoolName.contains("Royal Melbourne Institute of Technology")){
        return true
    }
    if(schoolName.contains("Singapore Polytechnic")){
        return true
    }
    return false
}


private object Resources {
    val tlds = readList("/swot/tlds.txt") ?: error("Cannot find /swot/tlds.txt")
    val stoplist = readList("/swot/stoplist.txt") ?: error("Cannot find /swot/stoplist.txt")

    fun readList(resource: String) : Set<String>? {
        return javaClass.getResourceAsStream(resource)?.reader()?.buffered()?.lineSequence()?.toHashSet()
    }
}

private fun findSchoolNames(parts: List<String>): List<String> {
    val resourcePath = StringBuilder()
    for (part in parts) {
        resourcePath.append('/').append(part)
        val school = Resources.readList("/swot${resourcePath}.txt")
        if (school != null) {
            return school.toList()
        }
    }

    return arrayListOf()
}

private fun domainParts(emailOrDomain: String): List<String> {
    return emailOrDomain.trim().lowercase(Locale.getDefault()).substringAfter('@').substringAfter("://").substringBefore(':').split('.').reversed()
}

internal fun checkSet(set: Set<String>, parts: List<String>): Boolean {
    val subj = StringBuilder()
    for (part in parts) {
        subj.insert(0, part)
        if (set.contains(subj.toString())) return true
        subj.insert(0 ,'.')
    }
    return false
}
