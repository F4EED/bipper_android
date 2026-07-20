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
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import org.meshtastic.core.domain.bipper.EMPTY_SERVICE_TAG_VALUES
import org.meshtastic.core.domain.bipper.PagerStatus
import org.meshtastic.core.domain.bipper.ServiceTagValues
import org.meshtastic.core.domain.bipper.extractTagLineFromPagerReply
import org.meshtastic.core.domain.bipper.formatTagSetCommand
import org.meshtastic.core.domain.bipper.isBipperHardwareModelName
import org.meshtastic.core.domain.bipper.isPagerCommandReply
import org.meshtastic.core.domain.bipper.parsePagerStatus
import org.meshtastic.core.domain.bipper.parseServiceTagValues
import org.meshtastic.core.model.ConnectionState
import org.meshtastic.core.model.Message
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.repository.PacketRepository
import org.meshtastic.core.repository.RadioController
import org.meshtastic.core.repository.usecase.SendMessageUseCase
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.bipper_toast_command_err
import org.meshtastic.core.resources.bipper_toast_command_ok
import org.meshtastic.core.resources.bipper_toast_not_connected
import org.meshtastic.core.ui.util.SnackbarManager
import org.meshtastic.core.ui.viewmodel.safeLaunch
import org.meshtastic.core.ui.viewmodel.stateInWhileSubscribed

data class BipperConfigUiState(
    val isConnected: Boolean = false,
    val isBipperHardware: Boolean = false,
    val status: PagerStatus? = null,
    val tagValues: ServiceTagValues = EMPTY_SERVICE_TAG_VALUES,
    val oldCode: String = "GAULIX",
    val newCode: String = "",
    val beepCount: String = "0",
    val busy: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@KoinViewModel
class BipperConfigViewModel(
    private val sendMessageUseCase: SendMessageUseCase,
    private val nodeRepository: NodeRepository,
    private val packetRepository: PacketRepository,
    private val radioController: RadioController,
    private val snackbarManager: SnackbarManager,
) : ViewModel() {
    private val formState = MutableStateFlow(BipperConfigUiState())
    private val handledMessageIds = mutableSetOf<Int>()

    private val isConnected =
        radioController.connectionState
            .map { it is ConnectionState.Connected }
            .stateInWhileSubscribed(initialValue = false)

    private val isBipperHardware =
        nodeRepository.ourNodeInfo
            .map { isBipperHardwareModelName(it?.user?.hw_model?.name) }
            .stateInWhileSubscribed(initialValue = false)

    val uiState: StateFlow<BipperConfigUiState> =
        combine(formState, isConnected, isBipperHardware) { form, connected, bipperHw ->
                form.copy(isConnected = connected, isBipperHardware = bipperHw)
            }
            .stateInWhileSubscribed(initialValue = BipperConfigUiState())

    init {
        viewModelScope.launch {
            nodeRepository.ourNodeInfo
                .flatMapLatest { our ->
                    if (our == null) {
                        flowOf(emptyList())
                    } else {
                        val contactKey = NodeAddress.ByNum(our.num).toContactKey(channel = 0).value
                        packetRepository.getMessagesFrom(
                            contact = contactKey,
                            limit = 40,
                            getNode = { userId -> nodeRepository.getNode(userId ?: NodeAddress.ID_BROADCAST) },
                        )
                    }
                }
                .collect { messages -> consumePagerReplies(messages) }
        }
    }

    fun setTagValue(tag: Int, value: String) {
        formState.update { it.copy(tagValues = it.tagValues.withTag(tag, value)) }
    }

    fun setOldCode(value: String) {
        formState.update { it.copy(oldCode = value) }
    }

    fun setNewCode(value: String) {
        formState.update { it.copy(newCode = value) }
    }

    fun setBeepCount(value: String) {
        formState.update { it.copy(beepCount = value) }
    }

    fun refreshStatus() {
        safeLaunch(tag = "bipperRefreshStatus") { sendLocalCommand("#status") }
    }

    fun applyTagValues() {
        safeLaunch(tag = "bipperApplyTags") {
            val ok = sendLocalCommand(formatTagSetCommand(formState.value.tagValues))
            if (ok) {
                sendLocalCommand("#status", markBusy = false)
            }
        }
    }

    fun applyActivationCode() {
        safeLaunch(tag = "bipperApplyCode") {
            val old = formState.value.oldCode.trim()
            val new = formState.value.newCode.trim()
            if (old.isEmpty() || new.isEmpty()) {
                return@safeLaunch
            }
            val ok = sendLocalCommand("#code $old $new")
            if (ok) {
                sendLocalCommand("#status", markBusy = false)
            }
        }
    }

    fun applyBeepCount() {
        safeLaunch(tag = "bipperApplyBeeps") {
            val count = formState.value.beepCount.toIntOrNull() ?: return@safeLaunch
            if (count !in 0..20) {
                return@safeLaunch
            }
            val ok = sendLocalCommand("#b $count")
            if (ok) {
                sendLocalCommand("#status", markBusy = false)
            }
        }
    }

    private suspend fun sendLocalCommand(text: String, markBusy: Boolean = true): Boolean {
        val our = nodeRepository.ourNodeInfo.value
        if (our == null || isConnected.value.not()) {
            snackbarManager.showSnackbar(message = resolveString(Res.string.bipper_toast_not_connected))
            return false
        }
        if (markBusy) {
            formState.update { it.copy(busy = true) }
        }
        val contactKey = NodeAddress.ByNum(our.num).toContactKey(channel = 0).value
        sendMessageUseCase(text, contactKey)
        return true
    }

    private fun consumePagerReplies(messages: List<Message>) {
        var pendingOk = false
        var gotStatus = false
        for (msg in messages) {
            if (!isPagerCommandReply(msg.text) || handledMessageIds.contains(msg.packetId)) {
                continue
            }
            handledMessageIds.add(msg.packetId)

            val parsed = parsePagerStatus(msg.text)
            if (parsed != null) {
                formState.update {
                    it.copy(
                        status = parsed,
                        tagValues = parsed.tagValues,
                        oldCode = parsed.code.takeIf { code -> code != "--" } ?: it.oldCode,
                        beepCount = if (parsed.beeps == "continu") "0" else parsed.beeps,
                        busy = false,
                    )
                }
                gotStatus = true
                continue
            }

            val tagLine = extractTagLineFromPagerReply(msg.text)
            if (tagLine != null) {
                val tagValues = parseServiceTagValues(tagLine)
                formState.update { prev ->
                    val status =
                        prev.status?.copy(tag = tagLine, tagValues = tagValues)
                            ?: PagerStatus(
                                state = "",
                                alertCount = 0,
                                battery = "--",
                                beeps = "--",
                                tag = tagLine,
                                tagValues = tagValues,
                                code = "--",
                                raw = "",
                            )
                    prev.copy(status = status, tagValues = tagValues)
                }
                continue
            }

            if (msg.text.startsWith("Pager OK")) {
                pendingOk = true
            } else if (msg.text.startsWith("Pager ERR")) {
                formState.update { it.copy(busy = false) }
                val errText = msg.text
                viewModelScope.launch {
                    snackbarManager.showSnackbar(
                        message = "${resolveString(Res.string.bipper_toast_command_err)}: $errText",
                    )
                }
            }
        }
        if (gotStatus || pendingOk) {
            formState.update { it.copy(busy = false) }
        }
        if (pendingOk && !gotStatus) {
            viewModelScope.launch {
                snackbarManager.showSnackbar(message = resolveString(Res.string.bipper_toast_command_ok))
            }
        }
    }

    private suspend fun resolveString(res: org.jetbrains.compose.resources.StringResource): String =
        org.jetbrains.compose.resources.getString(res)
}
