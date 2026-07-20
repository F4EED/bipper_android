/*
 * Copyright (c) 2026 Meshtastic LLC
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
package org.meshtastic.core.domain.bipper

/** T1–T4 membership values stored on a Gaulix bipper. */
data class ServiceTagValues(val t1: String = "", val t2: String = "", val t3: String = "", val t4: String = "") {
    operator fun get(tag: Int): String = when (tag) {
        1 -> t1
        2 -> t2
        3 -> t3
        4 -> t4
        else -> ""
    }

    fun withTag(tag: Int, value: String): ServiceTagValues = when (tag) {
        1 -> copy(t1 = value)
        2 -> copy(t2 = value)
        3 -> copy(t3 = value)
        4 -> copy(t4 = value)
        else -> this
    }
}

val EMPTY_SERVICE_TAG_VALUES = ServiceTagValues()

/** Parse `#status` tag field: `Tag: T1=SDIS42,T3=Ricamarie` or `Tag: aucun` */
fun parseServiceTagValues(tagLine: String): ServiceTagValues {
    var values = EMPTY_SERVICE_TAG_VALUES
    val normalized = tagLine.trim()
    if (normalized.isEmpty() || normalized.contains("aucun", ignoreCase = true)) {
        return values
    }

    val body = normalized.replace(Regex("^tag\\s*:\\s*", RegexOption.IGNORE_CASE), "")
    for (part in body.split(",")) {
        val trimmed = part.trim()
        val eq = Regex("^T([1-4])=(.*)$", RegexOption.IGNORE_CASE).matchEntire(trimmed)
        if (eq != null) {
            val tag = eq.groupValues[1].toInt()
            values = values.withTag(tag, eq.groupValues[2].trim())
            continue
        }
        val legacy = Regex("^T([1-4])\\s+(.+)$", RegexOption.IGNORE_CASE).matchEntire(trimmed)
        if (legacy != null) {
            val tag = legacy.groupValues[1].toInt()
            values = values.withTag(tag, legacy.groupValues[2].trim())
        }
    }
    return values
}

fun formatTagValueCommand(tag: Int, value: String): String {
    val trimmed = value.trim()
    if (tag !in 1..4) {
        return ""
    }
    return if (trimmed.isEmpty()) "#tagval $tag" else "#tagval $tag $trimmed"
}

/** Single flash write: `#tagset T1=foo,T2=,T3=bar,T4=` */
fun formatTagSetCommand(values: ServiceTagValues): String {
    val parts = (1..4).map { tag -> "T$tag=${values[tag].trim()}" }
    return "#tagset ${parts.joinToString(",")}"
}

fun formatServiceTagValuesLabel(values: ServiceTagValues): String {
    val parts = (1..4).filter { values[it].isNotEmpty() }.map { "T$it=${values[it]}" }
    return if (parts.isNotEmpty()) parts.joinToString(",") else "aucun"
}

/** Extract tag segment from `Pager Gaulix… | Tag: …` or `Pager OK — Tag: …` */
fun extractTagLineFromPagerReply(text: String): String? {
    val match = Regex("Tag:\\s*(.+)$", RegexOption.IGNORE_CASE).find(text)
    return match?.groupValues?.get(1)?.trim()
}
