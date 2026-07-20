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

/** Wire kinds for Gaulix pager text commands (`#alerte`, `#secours`, …). */
enum class PagerAlertKind(val wire: String) {
    ALERTE("alerte"),
    SECOURS("secours"),
    VIGILANCE("vigilance"),
    INFO("info"),
    FIN("fin"),
}

data class PagerAlertPayload(val kind: PagerAlertKind, val text: String = "", val affiliation: String = "")

/** Build wire text: `#alerte <texte> #appartenance` */
fun formatPagerAlertCommand(payload: PagerAlertPayload): String {
    val text = payload.text.trim().replace(Regex("\\s+"), " ")
    val affiliation = payload.affiliation.trim().removePrefix("#").replace(Regex("\\s+"), "")

    if (payload.kind == PagerAlertKind.FIN) {
        return if (affiliation.isNotEmpty()) "#fin #$affiliation" else "#fin"
    }

    val parts = mutableListOf("#${payload.kind.wire}")
    if (text.isNotEmpty()) {
        parts.add(text)
    }
    if (affiliation.isNotEmpty()) {
        parts.add("#$affiliation")
    }
    return parts.joinToString(" ")
}

fun parsePagerAlertCommand(raw: String): PagerAlertPayload? {
    val trimmed = raw.trim().replace(Regex("\\s+"), " ")
    val match = Regex("^#(alerte|secours|vigilance|info|fin)(?:\\s+(.*))?$", RegexOption.IGNORE_CASE).matchEntire(trimmed)
        ?: return null
    val kindWire = match.groupValues[1].lowercase()
    val kind = PagerAlertKind.entries.first { it.wire == kindWire }
    val rest = match.groupValues.getOrElse(2) { "" }.trim()
    if (rest.isEmpty()) {
        return PagerAlertPayload(kind = kind)
    }

    val tokens = rest.split(" ")
    val last = tokens.lastOrNull().orEmpty()
    if (last.startsWith("#") && last.length > 1) {
        return PagerAlertPayload(
            kind = kind,
            text = tokens.dropLast(1).joinToString(" "),
            affiliation = last.removePrefix("#"),
        )
    }
    return PagerAlertPayload(kind = kind, text = rest)
}
