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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AlertCommandsTest {
    @Test
    fun formatAlerteWithAffiliation() {
        val wire =
            formatPagerAlertCommand(
                PagerAlertPayload(kind = PagerAlertKind.ALERTE, text = "mise en alerte", affiliation = "test"),
            )
        assertEquals("#alerte mise en alerte #test", wire)
    }

    @Test
    fun formatFinWithoutAffiliation() {
        assertEquals("#fin", formatPagerAlertCommand(PagerAlertPayload(kind = PagerAlertKind.FIN)))
    }

    @Test
    fun formatFinWithAffiliation() {
        assertEquals(
            "#fin #SDIS42",
            formatPagerAlertCommand(PagerAlertPayload(kind = PagerAlertKind.FIN, affiliation = "SDIS42")),
        )
    }

    @Test
    fun parseRoundTrip() {
        val raw = "#vigilance surveillance #test"
        val parsed = parsePagerAlertCommand(raw)!!
        assertEquals(PagerAlertKind.VIGILANCE, parsed.kind)
        assertEquals("surveillance", parsed.text)
        assertEquals("test", parsed.affiliation)
        assertEquals(raw, formatPagerAlertCommand(parsed))
    }

    @Test
    fun parseInvalid() {
        assertNull(parsePagerAlertCommand("hello"))
    }
}
