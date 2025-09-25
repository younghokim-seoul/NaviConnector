package com.cm.naviconnector

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.cm.bluetooth.BluetoothClient
import com.cm.bluetooth.data.reqeust.ControlPacket
import com.cm.bluetooth.data.reqeust.TrainingMode
import com.cm.bluetooth.data.reqeust.TrainingModeRequest
import com.cm.naviconnector.feature.AppEffect
import com.cm.naviconnector.feature.AppEvent
import com.cm.naviconnector.feature.AppUiState
import com.cm.naviconnector.feature.audio.AudioFile
import com.cm.naviconnector.feature.audio.AudioRepository
import com.cm.naviconnector.feature.control.BottomButtonType
import com.cm.naviconnector.feature.control.Feature
import com.cm.naviconnector.feature.control.FeatureState
import com.cm.naviconnector.feature.control.TopButtonType
import com.cm.naviconnector.feature.control.toControlTargetOrNull
import com.cm.naviconnector.util.sendAll
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@HiltViewModel
class MainViewModel @Inject constructor(
    private val bluetoothClient: BluetoothClient,
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
                    TopButtonType.BLUETOOTH -> onBluetoothButtonClick()
                    TopButtonType.WIFI -> onWifiButtonClick()
                    TopButtonType.UPLOAD -> onUploadButtonClick()
                }
            }

            is AppEvent.FeatureSelected -> {
                if (!_uiState.value.isPowerOn) return
                _uiState.update { it.copy(currentFeature = event.feature) }
            }

            is AppEvent.DialChanged -> {
                if (!_uiState.value.isPowerOn) return

                val currentFeature = _uiState.value.currentFeature ?: return
                val newLevel = event.level

                _uiState.update { currentState ->
                    val newFeatures = currentState.features.toMutableMap()
                    val currentFeatureState = newFeatures[currentFeature]
                    if (currentFeatureState != null && currentFeatureState.enabled) {
                        newFeatures[currentFeature] =
                            currentFeatureState.copy(level = newLevel)
                    }
                    currentState.copy(features = newFeatures)
                }

                viewModelScope.launch {
                    val isSuccess = sendControlPacket(currentFeature, newLevel)
                    if (isSuccess != true) {
                        _effects.trySend(AppEffect.ShowToast("명령 전송에 실패했습니다"))
                    }
                }
            }

            is AppEvent.DeviceConnectClicked -> {
                connectDevice(event.device)
            }

            is AppEvent.BottomButtonClicked -> {
                if (!_uiState.value.isConnected) return
                when (event.type) {
                    BottomButtonType.PLAY -> setPlaying(true)
                    BottomButtonType.PAUSE -> setPlaying(false)
                }
            }

            is AppEvent.AudioUploadClicked -> {
                sendAudioFile(event.file)
            }
        }
    }

    private fun connectDevice(device: BluetoothDevice) = viewModelScope.launch {
        val connected = withContext(Dispatchers.IO) {
            runCatching {
                withTimeout(CONNECT_TIMEOUT_MS) {
                    bluetoothClient.connect(device, uuid)
                }
            }.isSuccess
        }

        if (connected) {
            // TODO: collectConnectionState로 처리 가능한지 확인 필요
            _uiState.update { it.copy(isConnected = true) }
            _effects.sendAll(
                AppEffect.SetDeviceDialogVisible(false),
                AppEffect.ShowToast("장치에 연결되었습니다")
            )
            bluetoothConnection?.sendPacket(TrainingModeRequest(TrainingMode.RANDOM).toByteArray())
        } else {
            _effects.send(AppEffect.ShowToast("장치 연결에 실패했습니다"))
        }
    }

    fun onPowerButtonClick() {
        if (!_uiState.value.isConnected) return
        val isPowerOn = _uiState.value.isPowerOn
        setAllFeaturesEnabled(!isPowerOn)
    }

    private fun setAllFeaturesEnabled(enabled: Boolean) {
        val level = if (enabled) 1 else 0
        _uiState.update {
            it.copy(features = Feature.entries.associateWith { FeatureState(enabled, level) })
        }

        Feature.entries.forEach { feature ->
            viewModelScope.launch {
                sendControlPacket(feature, level)
            }
        }
    }

    fun onBluetoothButtonClick() {
        val bondedDevices = bluetoothClient.bondedDevices()
        _scannedDevices.update { bondedDevices?.toList() ?: emptyList() }
        _effects.trySend(AppEffect.SetDeviceDialogVisible(true))
    }

    fun onWifiButtonClick() {

    }

    fun onUploadButtonClick() {
        _effects.trySend(AppEffect.SetAudioDialogVisible(true))
    }

    private fun sendAudioFile(file: AudioFile) {

    }

    fun setPlaying(isPlaying: Boolean) {
        _uiState.update { it.copy(isPlaying = isPlaying) }
        viewModelScope.launch {
//            val packet = PlayAudioRequest()
//            if (isSuccess != true) {
//                _effects.trySend(AppEffect.ShowToast("명령 전송에 실패했습니다"))
//            }
        }
    }

    private suspend fun sendControlPacket(feature: Feature, level: Int): Boolean =
        withContext(Dispatchers.IO) {
            val controlTarget = feature.toControlTargetOrNull()
            return@withContext if (controlTarget != null) {
                val packet = ControlPacket(target = controlTarget, value = level)
                bluetoothConnection?.sendPacket(packet.toByteArray()) == true
            } else {
                false
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
                            _uiState.update { it.copy(isConnected = false, isPlaying = false) }
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
                }
        }
    }
}