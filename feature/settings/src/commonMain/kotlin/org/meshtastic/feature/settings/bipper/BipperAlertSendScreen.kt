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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.domain.bipper.PagerAlertKind
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.bipper_send_affiliation_hint
import org.meshtastic.core.resources.bipper_send_affiliation_label
import org.meshtastic.core.resources.bipper_send_affiliation_placeholder
import org.meshtastic.core.resources.bipper_send_dest_alerte
import org.meshtastic.core.resources.bipper_send_dest_alerte_missing
import org.meshtastic.core.resources.bipper_send_dest_direct
import org.meshtastic.core.resources.bipper_send_dest_label
import org.meshtastic.core.resources.bipper_send_kind_alerte
import org.meshtastic.core.resources.bipper_send_kind_fin
import org.meshtastic.core.resources.bipper_send_kind_info
import org.meshtastic.core.resources.bipper_send_kind_label
import org.meshtastic.core.resources.bipper_send_kind_secours
import org.meshtastic.core.resources.bipper_send_kind_vigilance
import org.meshtastic.core.resources.bipper_send_page_description
import org.meshtastic.core.resources.bipper_send_page_title
import org.meshtastic.core.resources.bipper_send_pick_node
import org.meshtastic.core.resources.bipper_send_preview
import org.meshtastic.core.resources.bipper_send_submit
import org.meshtastic.core.resources.bipper_send_text_label
import org.meshtastic.core.resources.bipper_send_text_placeholder
import org.meshtastic.core.ui.component.MainAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BipperAlertSendScreen(viewModel: BipperAlertSendViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var kindExpanded by remember { mutableStateOf(false) }
    var nodeExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            MainAppBar(
                title = stringResource(Res.string.bipper_send_page_title),
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
            Text(text = stringResource(Res.string.bipper_send_page_description), style = MaterialTheme.typography.bodyMedium)

            ExposedDropdownMenuBox(expanded = kindExpanded, onExpandedChange = { kindExpanded = it }) {
                OutlinedTextField(
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    readOnly = true,
                    value = kindLabel(state.kind),
                    onValueChange = {},
                    label = { Text(stringResource(Res.string.bipper_send_kind_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = kindExpanded) },
                )
                ExposedDropdownMenu(expanded = kindExpanded, onDismissRequest = { kindExpanded = false }) {
                    PagerAlertKind.entries.forEach { kind ->
                        DropdownMenuItem(
                            text = { Text(kindLabel(kind)) },
                            onClick = {
                                viewModel.setKind(kind)
                                kindExpanded = false
                            },
                        )
                    }
                }
            }

            if (state.kind != PagerAlertKind.FIN) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = state.text,
                    onValueChange = viewModel::setText,
                    label = { Text(stringResource(Res.string.bipper_send_text_label)) },
                    placeholder = { Text(stringResource(Res.string.bipper_send_text_placeholder)) },
                    minLines = 3,
                )
            }

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.affiliation,
                onValueChange = viewModel::setAffiliation,
                label = { Text(stringResource(Res.string.bipper_send_affiliation_label)) },
                placeholder = { Text(stringResource(Res.string.bipper_send_affiliation_placeholder)) },
                supportingText = { Text(stringResource(Res.string.bipper_send_affiliation_hint)) },
            )

            Text(text = stringResource(Res.string.bipper_send_dest_label), style = MaterialTheme.typography.titleSmall)

            val alerteLabel =
                if (state.alerteChannelIndex != null) {
                    stringResource(Res.string.bipper_send_dest_alerte, "Alerte (#${state.alerteChannelIndex})")
                } else {
                    stringResource(Res.string.bipper_send_dest_alerte, stringResource(Res.string.bipper_send_dest_alerte_missing))
                }
            DestRadioRow(
                selected = state.destMode == BipperDestMode.ALERTE_CHANNEL,
                label = alerteLabel,
                onClick = { viewModel.setDestMode(BipperDestMode.ALERTE_CHANNEL) },
            )
            DestRadioRow(
                selected = state.destMode == BipperDestMode.DIRECT,
                label = stringResource(Res.string.bipper_send_dest_direct),
                onClick = { viewModel.setDestMode(BipperDestMode.DIRECT) },
            )

            if (state.destMode == BipperDestMode.DIRECT) {
                ExposedDropdownMenuBox(expanded = nodeExpanded, onExpandedChange = { nodeExpanded = it }) {
                    val selected =
                        state.peerNodes.firstOrNull { it.num == state.directNodeNum }?.let {
                            it.user.long_name.ifBlank { it.user.short_name }
                        } ?: stringResource(Res.string.bipper_send_pick_node)
                    OutlinedTextField(
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        readOnly = true,
                        value = selected,
                        onValueChange = {},
                        label = { Text(stringResource(Res.string.bipper_send_pick_node)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = nodeExpanded) },
                    )
                    ExposedDropdownMenu(expanded = nodeExpanded, onDismissRequest = { nodeExpanded = false }) {
                        state.peerNodes.forEach { node ->
                            DropdownMenuItem(
                                text = {
                                    Text(node.user.long_name.ifBlank { node.user.short_name }.ifBlank { "!${node.num.toString(16)}" })
                                },
                                onClick = {
                                    viewModel.setDirectNodeNum(node.num)
                                    nodeExpanded = false
                                },
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = stringResource(Res.string.bipper_send_preview), style = MaterialTheme.typography.titleSmall)
                Text(text = state.preview, style = MaterialTheme.typography.bodyLarge)
            }

            Button(
                onClick = viewModel::send,
                enabled = !state.busy && state.isConnected,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.bipper_send_submit))
            }
        }
    }
}

@Composable
private fun DestRadioRow(selected: Boolean, label: String, onClick: () -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth().selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label)
    }
}

@Composable
private fun kindLabel(kind: PagerAlertKind): String = when (kind) {
    PagerAlertKind.ALERTE -> stringResource(Res.string.bipper_send_kind_alerte)
    PagerAlertKind.SECOURS -> stringResource(Res.string.bipper_send_kind_secours)
    PagerAlertKind.VIGILANCE -> stringResource(Res.string.bipper_send_kind_vigilance)
    PagerAlertKind.INFO -> stringResource(Res.string.bipper_send_kind_info)
    PagerAlertKind.FIN -> stringResource(Res.string.bipper_send_kind_fin)
}
