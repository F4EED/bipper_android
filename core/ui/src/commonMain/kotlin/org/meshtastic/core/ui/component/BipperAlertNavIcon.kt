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
package org.meshtastic.core.ui.component

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.painterResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.ic_bipper_alert_siren

/** Gyrophare bleu sur fond rouge — entrée « envoi alerte » (nav, barre d’app, etc.). */
@Composable
fun BipperAlertNavIcon(contentDescription: String, modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(Res.drawable.ic_bipper_alert_siren),
        contentDescription = contentDescription,
        modifier = modifier,
        tint = Color.Unspecified,
    )
}
