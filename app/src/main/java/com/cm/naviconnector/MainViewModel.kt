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
import com.cm.bluetooth.data.response.NabiPacket
import com.cm.bluetooth.data.response.ParsedPacket
import com.cm.bluetooth.data.toHex
import com.cm.naviconnector.data.DataStoreRepository
import com.cm.naviconnector.feature.AppEffect
import com.cm.naviconnector.feature.AppEvent
import com.cm.naviconnector.feature.AppUiState
import com.cm.naviconnector.feature.audio.AudioFile
import com.cm.naviconnector.feature.audio.AudioRepository
import com.cm.naviconnector.feature.control.BottomButtonType
import com.cm.naviconnector.feature.control.ControlState
import com.cm.naviconnector.feature.control.Feature
import com.cm.naviconnector.feature.control.FeatureState
import com.cm.naviconnector.feature.control.MainFeature
import com.cm.naviconnector.feature.control.PlaylistItem
import com.cm.naviconnector.feature.control.SubFeature
import com.cm.naviconnector.feature.control.TopButtonType
import com.cm.naviconnector.feature.upload.UploadState
import com.cm.naviconnector.feature.withAllFeaturesEnabled
import com.cm.naviconnector.feature.withFeatureControlState
import com.cm.naviconnector.feature.withFeatureLevel
import com.cm.naviconnector.util.commandByte
import com.cm.naviconnector.util.scaleFrom
import com.cm.naviconnector.util.scaleTo
import com.cm.naviconnector.util.sendAll
import com.cm.naviconnector.util.trySendAll
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CompletableDeferred
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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class MainViewModel @Inject constructor(
    private val bluetoothClient: BluetoothClient,
    private val contentResolver: ContentResolver,
    private val dataStoreRepository: DataStoreRepository,
    audioRepository: AudioRepository
) : ViewModel() {

    companion object {
        private const val CONNECT_TIMEOUT_MS = 10_000L
        private const val MAX_FRAME_DATA = 2048
        private const val ACK_TIMEOUT_MS = 10_000L
    }

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private val _effects = Channel<AppEffect>(Channel.BUFFERED)
    val effects: Flow<AppEffect> = _effects.receiveAsFlow()

    private val _scannedDevices = MutableStateFlow(emptyList<BluetoothDevice>())
    val scannedDevices: StateFlow<List<BluetoothDevice>> = _scannedDevices

    private val _playlist = MutableStateFlow<List<PlaylistItem>>(emptyList())
    val playlist: StateFlow<List<PlaylistItem>> = _playlist

    private val ackWaiters = ConcurrentHashMap<Byte, CompletableDeferred<ParsedPacket.Ack>>()
    private val sendMutex = Mutex()

    val audioPaging: Flow<PagingData<AudioFile>> =
        audioRepository
            .pagedAudio()
            .cachedIn(viewModelScope)

    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")

    private val bluetoothConnection
        get() = bluetoothClient.getBluetoothConnection()

    init {
        observeConnectionState()
        observeBluetoothPackets()
    }

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
                updateCurrentFeature(event.feature)
            }

            is AppEvent.DialChanged -> {
                val currentFeature = _uiState.value.currentFeature ?: return
                val newLevel = event.level
                setFeatureLevel(currentFeature, newLevel)
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

            is AppEvent.AudioDialogDismissed -> {
                _effects.trySend(AppEffect.SetAudioDialogVisible(false))
            }

            is AppEvent.DeviceDialogDismissed -> {
                _effects.trySend(AppEffect.SetDeviceDialogVisible(false))

            }
        }
    }

    private fun updateCurrentFeature(feature: Feature) {
        _uiState.update { it.copy(currentFeature = feature) }
    }

    private fun connectDevice(device: BluetoothDevice) {
        viewModelScope.launch(Dispatchers.IO) {
            val isConnected = runCatching {
                withTimeout(CONNECT_TIMEOUT_MS) {
                    bluetoothClient.connect(device, uuid).await()
                    bluetoothConnection != null
                }
            }.getOrElse { e ->
                when (e) {
                    is TimeoutCancellationException -> false
                    is CancellationException -> throw e
                    else -> false
                }
            }

            Timber.d("isConnected: $isConnected")
            _uiState.update {
                it.copy(isConnected = isConnected).withAllFeaturesEnabled(isConnected)
            }

            if (isConnected) {
                _effects.sendAll(
                    AppEffect.SetDeviceDialogVisible(false),
                    AppEffect.ShowToast("장치에 연결되었습니다")
                )
                getDeviceAudioList()
                getFeatureLevel()
            } else {
                _effects.trySend(AppEffect.ShowToast("장치 연결에 실패했습니다"))
            }
        }
    }

    private fun onPowerButtonClick() {
        val isPowerOn = _uiState.value.isPowerOn
        setPowerMode(!isPowerOn)
    }

    private fun setPowerMode(isPowerOn: Boolean) {
        val level = if (isPowerOn) 1 else 0
        if (isPowerOn) {
            MainFeature.mainFeatures.forEach { feature ->
                setFeatureLevel(feature, level)
            }
        } else {
            MainFeature.allFeatures.forEach { feature ->
                setFeatureLevel(feature, level)
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
                    Timber.e(e, "Upload failed")
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

        val chunks = audioBytes.asList().chunked(MAX_FRAME_DATA)
        val totalFrames = chunks.size

        if (!sendRequestAndWaitForAck(UploadStartRequest(file.name, totalFrames))) {
            Timber.e("UploadStartRequest failed to get ACK")
            return@withContext false
        }

        chunks.forEachIndexed { index, chunk ->
            if (!sendRequestAndWaitForAck(
                    UploadDoingRequest(
                        frameNumber = index,
                        frameData = chunk.toByteArray()
                    )
                )
            ) {
                Timber.e("UploadDoingRequest for frame $index failed to get ACK")
                return@withContext false
            }

            val progress = ((index + 1) * 100) / totalFrames
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(uploadState = UploadState.InProgress(progress)) }
            }
        }

        return@withContext sendRequestAndWaitForAck(UploadEndRequest(file.name))
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
            if (!sendRequestAndWaitForAck(audioPacket)) {
                val command = if (isPlaying) "재생" else "정지"
                showCommandFailedToast("오디오 $command")
            }
        }
    }

    private suspend fun sendControlPacket(feature: Feature, level: Int): Boolean =
        withContext(Dispatchers.IO) {
            val packet = when (feature) {
                is MainFeature -> when (feature) {
                    MainFeature.Audio -> SetVolumeRequest(level)
                    else -> feature.controlTarget?.let { ControlPacket(it, scaleTo(level)) }
                }

                is SubFeature -> when (feature) {
                    is SubFeature.Random -> TrainingModeRequest(
                        TrainingMode.RANDOM,
                        scaleTo(level = level, outMax = 100)
                    )
                }
            }

            Timber.d("sendControlPacket: feature: $feature, level: $level, packet: $packet")
            return@withContext packet?.let { sendRequestAndWaitForAck(it) } ?: false
        }

    private suspend fun sendRequestAndWaitForAck( // TODO: 뷰모델과 분리
        packet: RequestPacket,
        timeout: Long = ACK_TIMEOUT_MS
    ): Boolean = withContext(Dispatchers.IO) {
        sendMutex.withLock {
            Timber.d("sendRequestAndWaitForAck: packet: $packet")
            val byteArray = packet.toByteArray()
            Timber.d("sendRequestAndWaitForAck: packet to hex: ${byteArray.toHex()}")

            val command = packet.commandByte() ?: run {
                Timber.e("sendRequestAndWaitForAck: Unknown packet type: $packet")
                return@withLock false
            }
            val deferred = CompletableDeferred<ParsedPacket.Ack>()

            if (ackWaiters.putIfAbsent(command, deferred) != null) {
                Timber.w("sendRequestAndWaitForAck: Concurrent request for command $command. Failing.")
                return@withLock false
            }

            try {
                val sent = bluetoothConnection?.sendPacket(byteArray) == true
                if (!sent) {
                    Timber.e("sendRequestAndWaitForAck: sendPacket failed for command $command")
                    return@withLock false
                }

                withTimeout(timeout) {
                    val ack = deferred.await()
                    ack.command == command
                }
            } catch (e: TimeoutCancellationException) {
                Timber.e(
                    e,
                    "sendRequestAndWaitForAck: timed out waiting for ACK for command $command"
                )
                false
            } finally {
                ackWaiters.remove(command, deferred)
            }
        }
    }

    private fun getDeviceStatusInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            if (!sendRequestAndWaitForAck(StatusInfoRequest())) {
                Timber.e("getDeviceStatusInfo: sendPacket failed")
            }
        }
    }

    private fun getDeviceAudioList() {
        viewModelScope.launch(Dispatchers.IO) {
            if (!sendRequestAndWaitForAck(GetAudioListRequest(10))) { // TODO: 페이징
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
                .catch { e -> Timber.e(e, "Received failed") }
                .collect { packet ->
                    Timber.tag("packet").d("Received packet: $packet")
                    handlePacket(packet)
                }
        }
    }

    private fun handlePacket(packet: NabiPacket) {
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
                val battery = packet.battery
                if (battery !in 0..100) {
                    Timber.w("Invalid battery level: $battery")
                    return
                }

                val isLow = battery <= 10
                _uiState.update { it.copy(isLowBattery = isLow) }

                if (isLow) {
                    _effects.trySend(AppEffect.ShowToast("배터리 잔량이 ${battery}% 남았습니다"))
                    getDeviceStatusInfo()
                }
            }

            is ParsedPacket.StatusInfo -> {
                updateFeaturesFromStatusInfo(packet)
            }

            is ParsedPacket.Ack -> {
                Timber.d("ACK for cmd: ${packet.command}")
                ackWaiters[packet.command]?.complete(packet)
            }

            is InvalidPacket -> {
                Timber.e("Invalid packet received: $packet")
            }

            else -> Timber.w("Unknown packet: $packet")
        }
    }

    private fun updateFeaturesFromStatusInfo(status: ParsedPacket.StatusInfo) {
        _uiState.update {
            val updated = it.features.toMutableMap()
            updated[MainFeature.Heater] =
                FeatureState(enabled = true, level = scaleFrom(status.heaterStatus))
            updated[MainFeature.Audio] = FeatureState(enabled = true, level = status.volume)
            updated[MainFeature.Fan] =
                FeatureState(enabled = true, level = scaleFrom(status.fanStatus))
            updated[MainFeature.Film] =
                FeatureState(enabled = true, level = scaleFrom(status.filmStatus))

            it.copy(
                features = updated,
                playerState = it.playerState.copy(isPlaying = status.isAudioPlaying)
            )
        }
    }

    private fun resetUiState() {
        _uiState.value = AppUiState()
    }

    private fun setFeatureLevel(feature: Feature, level: Int) {
        viewModelScope.launch {
            if (_uiState.value.features[feature]?.isLoading == true) {
                return@launch
            }

            try {
                _uiState.update { it.withFeatureControlState(feature, ControlState.Loading) }
                val isSuccess = sendControlPacket(feature, level)
                if (isSuccess) {
                    _uiState.update { it.withFeatureLevel(feature, level) }
                    dataStoreRepository.saveFeatureLevel(feature, level)
                } else {
                    showCommandFailedToast(feature.toString())
                }
            } finally {
                _uiState.update { it.withFeatureControlState(feature, ControlState.Idle) }
            }
        }
    }

    private fun showCommandFailedToast(command: String) {
        val message = "명령 전송에 실패했습니다: $command"
        _effects.trySend(AppEffect.ShowToast(message))
    }

    private suspend fun getFeatureLevel() {
        MainFeature.allFeatures.forEach { feature ->
            dataStoreRepository.getFeatureLevel(feature).firstOrNull()?.let { level ->
                setFeatureLevel(feature, level)
            }
        }
    }

    fun disconnect() {
        bluetoothClient.closeConnections()
    }
}