package com.cm.naviconnector

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.ContentResolver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.cm.bluetooth.BluetoothClient
import com.cm.bluetooth.data.reqeust.ControlPacket
import com.cm.bluetooth.data.reqeust.GetAudioListRequest
import com.cm.bluetooth.data.reqeust.PlayAudioRequest
import com.cm.bluetooth.data.reqeust.RequestPacket
import com.cm.bluetooth.data.reqeust.SetVolumeRequest
import com.cm.bluetooth.data.reqeust.StatusInfoRequest
import com.cm.bluetooth.data.reqeust.StopAudioRequest
import com.cm.bluetooth.data.reqeust.TrainingMode
import com.cm.bluetooth.data.reqeust.TrainingModeRequest
import com.cm.bluetooth.data.reqeust.UploadDoingRequest
import com.cm.bluetooth.data.reqeust.UploadEndRequest
import com.cm.bluetooth.data.reqeust.UploadStartRequest
import com.cm.bluetooth.data.response.InvalidPacket
import com.cm.bluetooth.data.response.ParsedPacket
import com.cm.naviconnector.feature.AppEffect
import com.cm.naviconnector.feature.AppEvent
import com.cm.naviconnector.feature.AppUiState
import com.cm.naviconnector.feature.audio.AudioFile
import com.cm.naviconnector.feature.audio.AudioRepository
import com.cm.naviconnector.feature.control.BottomButtonType
import com.cm.naviconnector.feature.control.Feature
import com.cm.naviconnector.feature.control.FeatureState
import com.cm.naviconnector.feature.control.PlaylistItem
import com.cm.naviconnector.feature.control.TopButtonType
import com.cm.naviconnector.feature.control.toControlTargetOrNull
import com.cm.naviconnector.feature.upload.UploadState
import com.cm.naviconnector.util.sendAll
import com.cm.naviconnector.util.trySendAll
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class MainViewModel @Inject constructor(
    private val bluetoothClient: BluetoothClient,
    private val contentResolver: ContentResolver,
    audioRepository: AudioRepository
) : ViewModel() {

    init {
        observeConnectionState()
        observeBluetoothPackets()
    }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 5000L
    }

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private val _effects = Channel<AppEffect>(Channel.BUFFERED)
    val effects: Flow<AppEffect> = _effects.receiveAsFlow()

    private val _scannedDevices = MutableStateFlow(emptyList<BluetoothDevice>())
    val scannedDevices: StateFlow<List<BluetoothDevice>> = _scannedDevices

    private val _playlist = MutableStateFlow<List<PlaylistItem>>(emptyList())
    val playlist: StateFlow<List<PlaylistItem>> = _playlist

    val audioPaging: Flow<PagingData<AudioFile>> =
        audioRepository
            .pagedAudio()
            .cachedIn(viewModelScope)

    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")

    private val bluetoothConnection
        get() = bluetoothClient.getBluetoothConnection()

    fun onEvent(event: AppEvent) {
        when (event) {
            is AppEvent.TopButtonClicked -> {
                when (event.type) {
                    TopButtonType.POWER -> onPowerButtonClick()
                    TopButtonType.BLUETOOTH -> showDeviceDialog()
                    TopButtonType.WIFI -> onWifiButtonClick()
                    TopButtonType.UPLOAD -> showAudioDialog()
                }
            }

            is AppEvent.FeatureSelected -> {
                _uiState.update { it.copy(currentFeature = event.feature) }
            }

            is AppEvent.DialChanged -> {
                val currentFeature = _uiState.value.currentFeature ?: return
                val newLevel = event.level

                viewModelScope.launch {
                    val isSuccess = sendControlPacket(currentFeature, newLevel)
                    if (isSuccess) {
                        _uiState.update { currentState ->
                            val newFeatures = currentState.features.toMutableMap()
                            val currentFeatureState = newFeatures[currentFeature]
                            if (currentFeatureState != null && currentFeatureState.enabled) {
                                newFeatures[currentFeature] =
                                    currentFeatureState.copy(level = newLevel)
                            }
                            currentState.copy(features = newFeatures)
                        }
                    } else {
                        _effects.trySend(AppEffect.ShowToast("명령 전송에 실패했습니다"))
                    }
                }
            }

            is AppEvent.DeviceConnectClicked -> {
                connectDevice(event.device)
            }

            is AppEvent.BottomButtonClicked -> {
                when (event.type) {
                    BottomButtonType.PLAY -> setPlaying(true)
                    BottomButtonType.PAUSE -> setPlaying(false)
                }
            }

            is AppEvent.AudioUploadClicked -> {
                onUploadClick(event.file)
            }

            is AppEvent.PlaylistItemClicked -> {
                _uiState.update {
                    it.copy(
                        playerState = it.playerState.copy(
                            selectedFileName = event.item.fileName,
                            isPlaying = false
                        )
                    )
                }
            }
        }
    }

    private fun connectDevice(device: BluetoothDevice) {
        viewModelScope.launch(Dispatchers.IO) {
            val connected = runCatching {
                withTimeout(CONNECT_TIMEOUT_MS) {
                    bluetoothClient.connect(device, uuid).await()
                    true
                }
            }.getOrElse { e ->
                when (e) {
                    is TimeoutCancellationException -> false
                    is CancellationException -> throw e
                    else -> false
                }
            }

            if (connected) {
                // TODO: collectConnectionState로 처리 가능한지 확인 필요
                _uiState.update { it.copy(isConnected = true) }
                _effects.sendAll(
                    AppEffect.SetDeviceDialogVisible(false),
                    AppEffect.ShowToast("장치에 연결되었습니다")
                )

                val trainingModePacket = TrainingModeRequest(TrainingMode.RANDOM)
                if (bluetoothConnection?.sendPacket(trainingModePacket.toByteArray()) != true) {
                    Timber.e("send TrainingModeRequest failed")
                }
            } else {
                _effects.send(AppEffect.ShowToast("장치 연결에 실패했습니다"))
            }
        }
    }

    private fun onPowerButtonClick() {
        val isPowerOn = _uiState.value.isPowerOn
        setAllFeaturesEnabled(!isPowerOn)
    }

    private fun setAllFeaturesEnabled(enabled: Boolean) {
        val level = if (enabled) 1 else 0

        viewModelScope.launch {
            val partialUpdates = buildMap<Feature, FeatureState> {
                for (f in Feature.entries) {
                    if (sendControlPacket(f, level)) {
                        put(f, FeatureState(enabled = enabled, level = level))
                    }
                }
            }

            if (partialUpdates.isNotEmpty()) {
                _uiState.update { state ->
                    state.copy(
                        features = state.features + partialUpdates
                    )
                }
            }
        }
    }

    private fun showDeviceDialog() {
        val bondedDevices = bluetoothClient.bondedDevices()
        _scannedDevices.update { bondedDevices?.toList() ?: emptyList() }
        _effects.trySend(AppEffect.SetDeviceDialogVisible(true))
    }

    private fun onWifiButtonClick() {

    }

    private fun showAudioDialog() {
        _effects.trySend(AppEffect.SetAudioDialogVisible(true))
    }

    private fun onUploadClick(file: AudioFile) {
        viewModelScope.launch {
            _effects.send(AppEffect.SetUploadDialogVisible(true)) // TODO: 업로드 이벤트와 상태가 따로 관리되는 문제
            _uiState.update { it.copy(uploadState = UploadState.InProgress(0)) }

            val isSuccess = runCatching {
                sendAudioFile(file)
            }.fold(
                onSuccess = { it },
                onFailure = { e ->
                    if (e is CancellationException) throw e
                    Timber.e(e, "upload failed")
                    false
                }
            )

            if (isSuccess) {
                getDeviceAudioList()
            }

            withContext(NonCancellable) {
                _uiState.update { it.copy(uploadState = UploadState.Idle) }

                val message = if (isSuccess) "오디오 업로드에 성공했습니다" else "오디오 업로드에 실패했습니다"
                _effects.trySendAll(
                    AppEffect.SetUploadDialogVisible(false),
                    AppEffect.ShowToast(message)
                )
            }
        }
    }

    private suspend fun sendAudioFile(file: AudioFile): Boolean = withContext(Dispatchers.IO) {
        val audioBytes = contentResolver.openInputStream(file.uri)?.use { it.readBytes() }
            ?: return@withContext false

        val chunkSize = 2048
        val chunks = audioBytes.asList().chunked(chunkSize)
        val totalFrames = chunks.size

        if (!sendPacket(UploadStartRequest(file.name, totalFrames))) return@withContext false

        chunks.forEachIndexed { index, chunk ->
            if (!sendPacket(
                    UploadDoingRequest(
                        frameNumber = index,
                        frameData = chunk.toByteArray()
                    )
                )
            ) return@withContext false

            val progress = ((index + 1) * 100) / totalFrames
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(uploadState = UploadState.InProgress(progress)) }
            }
        }

        return@withContext sendPacket(UploadEndRequest(file.name))
    }

    private fun setPlaying(isPlaying: Boolean) {
        val selectedFileName = _uiState.value.playerState.selectedFileName
        if (selectedFileName == null) {
            _effects.trySend(AppEffect.ShowToast("재생할 곡을 선택해주세요"))
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val audioPacket =
                if (isPlaying) PlayAudioRequest(selectedFileName) else StopAudioRequest()
            if (!sendPacket(audioPacket)) {
                _effects.trySend(AppEffect.ShowToast("명령 전송에 실패했습니다"))
            }
        }
    }

    private suspend fun sendControlPacket(feature: Feature, level: Int): Boolean =
        withContext(Dispatchers.IO) {
            if (feature == Feature.AUDIO) {
                return@withContext sendPacket(SetVolumeRequest(level))
            }

            val controlTarget = feature.toControlTargetOrNull()
            return@withContext if (controlTarget != null) {
                val packet = ControlPacket(target = controlTarget, value = level)
                bluetoothConnection?.sendPacket(packet.toByteArray()) == true
            } else {
                false
            }
        }

    private suspend fun sendPacket(packet: RequestPacket): Boolean = withContext(Dispatchers.IO) {
        val isSuccess = bluetoothConnection?.sendPacket(packet.toByteArray()) == true
        if (!isSuccess) {
            Timber.e("sendPacket failed: $packet")
        }
        return@withContext isSuccess
    }

    private fun getDeviceStatusInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            val isSuccess =
                bluetoothConnection?.sendPacket(StatusInfoRequest().toByteArray()) == true
            if (!isSuccess) {
                Timber.e("getDeviceStatusInfo: sendPacket failed")
            }
        }
    }

    private fun getDeviceAudioList() {
        viewModelScope.launch(Dispatchers.IO) {
            val isSuccess =
                bluetoothConnection?.sendPacket(GetAudioListRequest(10).toByteArray()) == true // TODO: 페이징
            if (!isSuccess) {
                Timber.e("getDeviceAudioList: sendPacket failed")
            }
        }
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            bluetoothClient
                .collectConnectionState()
                .map { it.state } // TODO: device 정보 고려
                .distinctUntilChanged()
                .catch { e -> Timber.e(e, "collectConnectionState failed") }
                .collect { state ->
                    when (state) {
                        BluetoothAdapter.STATE_DISCONNECTED -> {
                            resetUiState()
                            _effects.trySend(AppEffect.ShowToast("장치와 연결이 해제되었습니다"))
                        }

                        else -> Unit
                    }
                }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeBluetoothPackets() {
        viewModelScope.launch {
            uiState
                .map { it.isConnected }
                .distinctUntilChanged()
                .flatMapLatest { connected ->
                    if (connected) {
                        bluetoothConnection?.receivePacket() ?: emptyFlow()
                    } else {
                        emptyFlow()
                    }
                }
                .catch { e -> Timber.e(e, "received failed") }
                .collect { packet ->
                    Timber.d("received packet: $packet")
                    when (packet) {
                        is ParsedPacket.AudioList -> {
                            val items =
                                packet.fileNames.map { name -> PlaylistItem(fileName = name) }
                            _playlist.value = items
                        }

                        is ParsedPacket.PlayAudioResult -> {
                            val isSuccess =
                                packet.resultCode == ParsedPacket.PlayAudioResult.ResultCode.SUCCESS
                            _uiState.update {
                                it.copy(playerState = it.playerState.copy(isPlaying = isSuccess))
                            }

                            val message = if (isSuccess) {
                                "오디오 재생을 시작했습니다"
                            } else when (packet.resultCode) {
                                ParsedPacket.PlayAudioResult.ResultCode.SD_CARD_NOT_FOUND -> "SD 카드가 없습니다"
                                ParsedPacket.PlayAudioResult.ResultCode.PLAYBACK_FAILED -> "재생에 실패했습니다"
                                else -> "오디오 재생에 실패했습니다"
                            }
                            _effects.trySend(AppEffect.ShowToast(message))
                        }

                        is ParsedPacket.LowBattery -> {
                            _effects.trySend(AppEffect.ShowToast("배터리 잔량이 ${packet.battery}% 남았습니다"))
                            getDeviceStatusInfo()
                        }

                        is ParsedPacket.StatusInfo -> {
                            updateFeaturesFromStatusInfo(packet)
                        }

                        is ParsedPacket.Ack -> {
                            Timber.d("ACK for cmd: ${packet.command}")
                        }

                        is InvalidPacket -> {
                            Timber.e("Invalid packet received: $packet")
                        }

                        else -> Timber.w("Unknown packet: $packet")
                    }
                }
        }
    }

    private fun resetUiState() {
        _uiState.value = AppUiState()
    }

    private fun updateFeaturesFromStatusInfo(status: ParsedPacket.StatusInfo) {
        _uiState.update {
            val updated = it.features.toMutableMap()
            updated[Feature.FILM] =
                FeatureState(enabled = status.filmStatus > 0, level = status.filmStatus)
            updated[Feature.FAN] =
                FeatureState(enabled = status.fanStatus > 0, level = status.fanStatus)
            updated[Feature.HEATER] =
                FeatureState(enabled = status.heaterStatus > 0, level = status.heaterStatus)
            updated[Feature.AUDIO] =
                FeatureState(enabled = status.isAudioPlaying, level = status.volume)

            it.copy(
                features = updated,
                playerState = it.playerState.copy(isPlaying = status.isAudioPlaying)
            )
        }
    }

    private fun updateFeatureState() { // TODO: 구현 필요
    }
}