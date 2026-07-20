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
package org.meshtastic.feature.settings.bipper

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.bipper_config_not_bipper_warning
import org.meshtastic.core.resources.bipper_config_page_description
import org.meshtastic.core.resources.bipper_config_page_title
import org.meshtastic.core.resources.bipper_gaulix_beep_apply
import org.meshtastic.core.resources.bipper_gaulix_beep_description
import org.meshtastic.core.resources.bipper_gaulix_beep_label
import org.meshtastic.core.resources.bipper_gaulix_beep_title
import org.meshtastic.core.resources.bipper_gaulix_code_apply
import org.meshtastic.core.resources.bipper_gaulix_code_description
import org.meshtastic.core.resources.bipper_gaulix_code_new
import org.meshtastic.core.resources.bipper_gaulix_code_old
import org.meshtastic.core.resources.bipper_gaulix_code_title
import org.meshtastic.core.resources.bipper_gaulix_description
import org.meshtastic.core.resources.bipper_gaulix_not_connected
import org.meshtastic.core.resources.bipper_gaulix_status_alerts
import org.meshtastic.core.resources.bipper_gaulix_status_battery
import org.meshtastic.core.resources.bipper_gaulix_status_beeps
import org.meshtastic.core.resources.bipper_gaulix_status_code
import org.meshtastic.core.resources.bipper_gaulix_status_empty
import org.meshtastic.core.resources.bipper_gaulix_status_refresh
import org.meshtastic.core.resources.bipper_gaulix_status_state
import org.meshtastic.core.resources.bipper_gaulix_status_tag
import org.meshtastic.core.resources.bipper_gaulix_status_title
import org.meshtastic.core.resources.bipper_gaulix_tag_apply
import org.meshtastic.core.resources.bipper_gaulix_tag_description
import org.meshtastic.core.resources.bipper_gaulix_tag_hint
import org.meshtastic.core.resources.bipper_gaulix_tag_placeholder
import org.meshtastic.core.resources.bipper_gaulix_tag_title
import org.meshtastic.core.resources.bipper_gaulix_title
import org.meshtastic.core.resources.bipper_gaulix_usb_hint
import org.meshtastic.core.ui.component.MainAppBar

@Composable
fun BipperConfigScreen(viewModel: BipperConfigViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.isConnected) {
        if (state.isConnected) {
            viewModel.refreshStatus()
        }
    }

    Scaffold(
        topBar = {
            MainAppBar(
                title = stringResource(Res.string.bipper_config_page_title),
                canNavigateUp = true,
                onNavigateUp = onBack,
                ourNode = null,
                showNodeChip = false,
                actions = {},
                onClickChip = {},
            )
        },
    ) { padding ->
        Column(
            modifier =
            Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(Res.string.bipper_config_page_description), style = MaterialTheme.typography.bodyMedium)

            if (!state.isConnected) {
                Text(stringResource(Res.string.bipper_gaulix_not_connected), color = MaterialTheme.colorScheme.error)
                return@Column
            }

            if (!state.isBipperHardware) {
                Text(
                    stringResource(Res.string.bipper_config_not_bipper_warning),
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }

            Text(stringResource(Res.string.bipper_gaulix_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(Res.string.bipper_gaulix_description), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(Res.string.bipper_gaulix_usb_hint), style = MaterialTheme.typography.bodySmall)

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            stringResource(Res.string.bipper_gaulix_status_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        OutlinedButton(onClick = viewModel::refreshStatus, enabled = !state.busy) {
                            Text(stringResource(Res.string.bipper_gaulix_status_refresh))
                        }
                    }
                    val status = state.status
                    if (status == null) {
                        Text(stringResource(Res.string.bipper_gaulix_status_empty))
                    } else {
                        StatusLine(stringResource(Res.string.bipper_gaulix_status_state), status.state)
                        StatusLine(stringResource(Res.string.bipper_gaulix_status_alerts), status.alertCount.toString())
                        StatusLine(stringResource(Res.string.bipper_gaulix_status_battery), status.battery)
                        StatusLine(stringResource(Res.string.bipper_gaulix_status_beeps), status.beeps)
                        StatusLine(stringResource(Res.string.bipper_gaulix_status_tag), status.tag)
                        StatusLine(stringResource(Res.string.bipper_gaulix_status_code), status.code)
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(Res.string.bipper_gaulix_tag_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(Res.string.bipper_gaulix_tag_description), style = MaterialTheme.typography.bodySmall)
                    (1..4).forEach { tag ->
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = state.tagValues[tag],
                            onValueChange = { viewModel.setTagValue(tag, it) },
                            label = { Text("T$tag") },
                            placeholder = { Text(stringResource(Res.string.bipper_gaulix_tag_placeholder)) },
                            enabled = !state.busy,
                        )
                    }
                    Button(onClick = viewModel::applyTagValues, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(Res.string.bipper_gaulix_tag_apply))
                    }
                    Text(stringResource(Res.string.bipper_gaulix_tag_hint), style = MaterialTheme.typography.bodySmall)
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(Res.string.bipper_gaulix_code_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(Res.string.bipper_gaulix_code_description), style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.oldCode,
                        onValueChange = viewModel::setOldCode,
                        label = { Text(stringResource(Res.string.bipper_gaulix_code_old)) },
                        enabled = !state.busy,
                    )
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.newCode,
                        onValueChange = viewModel::setNewCode,
                        label = { Text(stringResource(Res.string.bipper_gaulix_code_new)) },
                        enabled = !state.busy,
                    )
                    Button(
                        onClick = viewModel::applyActivationCode,
                        enabled = !state.busy && state.newCode.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(Res.string.bipper_gaulix_code_apply))
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(Res.string.bipper_gaulix_beep_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(Res.string.bipper_gaulix_beep_description), style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.beepCount,
                        onValueChange = viewModel::setBeepCount,
                        label = { Text(stringResource(Res.string.bipper_gaulix_beep_label)) },
                        enabled = !state.busy,
                    )
                    Button(onClick = viewModel::applyBeepCount, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(Res.string.bipper_gaulix_beep_apply))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusLine(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
