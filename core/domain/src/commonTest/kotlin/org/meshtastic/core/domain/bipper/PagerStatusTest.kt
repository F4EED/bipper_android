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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PagerStatusTest {
    @Test
    fun parseStatusReply() {
        val raw =
            "Pager Gaulix - En écoute | Alertes: 2 | Batterie: 87% | Bips: continu | Tag: T1=SDIS42,T3=test | Code: GAULIX"
        val status = parsePagerStatus(raw)!!
        assertEquals("En écoute", status.state)
        assertEquals(2, status.alertCount)
        assertEquals("87%", status.battery)
        assertEquals("continu", status.beeps)
        assertEquals("T1=SDIS42,T3=test", status.tag)
        assertEquals("SDIS42", status.tagValues.t1)
        assertEquals("test", status.tagValues.t3)
        assertEquals("GAULIX", status.code)
    }

    @Test
    fun formatTagSet() {
        assertEquals(
            "#tagset T1=SDIS42,T2=,T3=test,T4=",
            formatTagSetCommand(ServiceTagValues(t1 = "SDIS42", t3 = "test")),
        )
    }

    @Test
    fun pagerReplyDetection() {
        assertTrue(isPagerCommandReply("Pager OK — Tag: T1=a"))
        assertTrue(isPagerCommandReply("Pager ERR code"))
        assertFalse(isPagerCommandReply("hello"))
    }
}
