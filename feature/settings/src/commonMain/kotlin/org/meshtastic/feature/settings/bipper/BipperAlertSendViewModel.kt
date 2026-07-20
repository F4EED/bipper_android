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

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.KoinViewModel
import org.meshtastic.core.domain.bipper.PagerAlertKind
import org.meshtastic.core.domain.bipper.PagerAlertPayload
import org.meshtastic.core.domain.bipper.formatPagerAlertCommand
import org.meshtastic.core.model.ConnectionState
import org.meshtastic.core.model.ContactKey
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.core.model.util.getChannel
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.repository.RadioConfigRepository
import org.meshtastic.core.repository.RadioController
import org.meshtastic.core.repository.usecase.SendMessageUseCase
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.bipper_send_need_node
import org.meshtastic.core.resources.bipper_send_need_text
import org.meshtastic.core.resources.bipper_send_no_alerte_channel
import org.meshtastic.core.resources.bipper_send_sent
import org.meshtastic.core.resources.bipper_toast_not_connected
import org.meshtastic.core.ui.util.SnackbarManager
import org.meshtastic.core.ui.viewmodel.safeLaunch
import org.meshtastic.core.ui.viewmodel.stateInWhileSubscribed
import org.meshtastic.proto.ChannelSet

enum class BipperDestMode {
    ALERTE_CHANNEL,
    DIRECT,
}

data class BipperAlertSendUiState(
    val kind: PagerAlertKind = PagerAlertKind.ALERTE,
    val text: String = "",
    val affiliation: String = "",
    val destMode: BipperDestMode = BipperDestMode.ALERTE_CHANNEL,
    val directNodeNum: Int? = null,
    val alerteChannelIndex: Int? = null,
    val peerNodes: List<Node> = emptyList(),
    val isConnected: Boolean = false,
    val busy: Boolean = false,
) {
    val preview: String
        get() =
            formatPagerAlertCommand(
                PagerAlertPayload(
                    kind = kind,
                    text = if (kind == PagerAlertKind.FIN) "" else text,
                    affiliation = affiliation,
                ),
            )
}

@KoinViewModel
class BipperAlertSendViewModel(
    private val sendMessageUseCase: SendMessageUseCase,
    private val nodeRepository: NodeRepository,
    private val radioConfigRepository: RadioConfigRepository,
    private val radioController: RadioController,
    private val snackbarManager: SnackbarManager,
) : ViewModel() {
    private val formState = MutableStateFlow(BipperAlertSendUiState())

    private val alerteChannelIndex =
        radioConfigRepository.channelSetFlow
            .map { findAlerteChannelIndex(it) }
            .stateInWhileSubscribed(initialValue = null)

    private val peerNodes =
        combine(nodeRepository.nodeDBbyNum, nodeRepository.ourNodeInfo) { db, our ->
                db.values
                    .filter { it.num != our?.num }
                    .sortedBy { it.user.long_name.ifBlank { it.user.short_name } }
            }
            .stateInWhileSubscribed(initialValue = emptyList())

    private val isConnected =
        radioController.connectionState
            .map { it is ConnectionState.Connected }
            .stateInWhileSubscribed(initialValue = false)

    val uiState: StateFlow<BipperAlertSendUiState> =
        combine(formState, alerteChannelIndex, peerNodes, isConnected) { form, alerteIdx, peers, connected ->
                form.copy(
                    alerteChannelIndex = alerteIdx,
                    peerNodes = peers,
                    isConnected = connected,
                )
            }
            .stateInWhileSubscribed(initialValue = BipperAlertSendUiState())

    fun setKind(value: PagerAlertKind) {
        formState.update { it.copy(kind = value) }
    }

    fun setText(value: String) {
        formState.update { it.copy(text = value) }
    }

    fun setAffiliation(value: String) {
        formState.update { it.copy(affiliation = value) }
    }

    fun setDestMode(value: BipperDestMode) {
        formState.update { it.copy(destMode = value) }
    }

    fun setDirectNodeNum(value: Int?) {
        formState.update { it.copy(directNodeNum = value) }
    }

    fun send() {
        safeLaunch(tag = "bipperSendAlert") {
            val state = uiState.value
            if (!state.isConnected) {
                snackbarManager.showSnackbar(message = resolveString(Res.string.bipper_toast_not_connected))
                return@safeLaunch
            }
            if (state.kind != PagerAlertKind.FIN && state.text.isBlank()) {
                snackbarManager.showSnackbar(message = resolveString(Res.string.bipper_send_need_text))
                return@safeLaunch
            }

            val contactKey =
                when (state.destMode) {
                    BipperDestMode.ALERTE_CHANNEL -> {
                        val index = state.alerteChannelIndex
                        if (index == null) {
                            snackbarManager.showSnackbar(
                                message = resolveString(Res.string.bipper_send_no_alerte_channel),
                            )
                            return@safeLaunch
                        }
                        ContactKey.broadcast(index).value
                    }
                    BipperDestMode.DIRECT -> {
                        val num = state.directNodeNum
                        if (num == null) {
                            snackbarManager.showSnackbar(message = resolveString(Res.string.bipper_send_need_node))
                            return@safeLaunch
                        }
                        NodeAddress.ByNum(num).toContactKey(channel = 0).value
                    }
                }

            formState.update { it.copy(busy = true) }
            try {
                sendMessageUseCase(state.preview, contactKey)
                snackbarManager.showSnackbar(
                    message = "${resolveString(Res.string.bipper_send_sent)}: ${state.preview}",
                )
            } finally {
                formState.update { it.copy(busy = false) }
            }
        }
    }

    private suspend fun resolveString(res: org.jetbrains.compose.resources.StringResource): String =
        org.jetbrains.compose.resources.getString(res)

    companion object {
        fun findAlerteChannelIndex(channelSet: ChannelSet): Int? {
            for (index in channelSet.settings.indices) {
                val name = channelSet.getChannel(index)?.name ?: channelSet.settings[index].name
                if (name.equals("Alerte", ignoreCase = true)) {
                    return index
                }
            }
            return null
        }
    }
}
