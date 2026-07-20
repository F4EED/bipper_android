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

data class PagerStatus(
    val state: String,
    val alertCount: Int,
    val battery: String,
    val beeps: String,
    val tag: String,
    val tagValues: ServiceTagValues,
    val code: String,
    val raw: String,
)

/** Parse a `#status` reply from GaulixPagerModule::sendStatusReply. */
fun parsePagerStatus(text: String): PagerStatus? {
    val raw = text.trim()
    if (!raw.startsWith("Pager Gaulix")) {
        return null
    }

    val parts = raw.split("|").map { it.trim() }
    if (parts.size < 2) {
        return null
    }

    val state = parts[0].replace(Regex("^Pager Gaulix\\s*-\\s*", RegexOption.IGNORE_CASE), "").trim()
    var alertCount = 0
    var battery = "--"
    var beeps = "--"
    var tag = "aucun"
    var code = "--"

    for (part in parts.drop(1)) {
        val alertsMatch = Regex("^Alertes:\\s*(\\d+)", RegexOption.IGNORE_CASE).find(part)
        if (alertsMatch != null) {
            alertCount = alertsMatch.groupValues[1].toInt()
            continue
        }
        if (part.startsWith("Batterie")) {
            battery = part.replace(Regex("^Batterie\\s*:\\s*", RegexOption.IGNORE_CASE), "").trim()
            continue
        }
        if (part.startsWith("Bips")) {
            beeps = part.replace(Regex("^Bips\\s*:\\s*", RegexOption.IGNORE_CASE), "").trim()
            continue
        }
        if (part.startsWith("Tag")) {
            tag = part.replace(Regex("^Tag\\s*:\\s*", RegexOption.IGNORE_CASE), "").trim()
            continue
        }
        if (part.startsWith("Code")) {
            code = part.replace(Regex("^Code\\s*:\\s*", RegexOption.IGNORE_CASE), "").trim()
        }
    }

    return PagerStatus(
        state = state,
        alertCount = alertCount,
        battery = battery,
        beeps = beeps,
        tag = tag,
        tagValues = parseServiceTagValues(tag),
        code = code,
        raw = raw,
    )
}

fun isPagerCommandReply(text: String): Boolean {
    val t = text.trim()
    return t.startsWith("Pager Gaulix") || t.startsWith("Pager OK") || t.startsWith("Pager ERR")
}

fun isBipperHardwareModelName(hwModelName: String?): Boolean {
    if (hwModelName.isNullOrBlank()) {
        return false
    }
    return hwModelName == "SEEED_WIO_TRACKER_L1" || hwModelName == "SEEED_WIO_TRACKER_L1_EINK"
}
