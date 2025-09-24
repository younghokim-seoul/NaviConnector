package com.cm.naviconnector

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.cm.bluetooth.BluetoothClient
import com.cm.bluetooth.data.reqeust.ControlPacket
import com.cm.bluetooth.data.reqeust.ControlTarget
import com.cm.bluetooth.data.reqeust.TrainingMode
import com.cm.bluetooth.data.reqeust.TrainingModeRequest
import com.cm.naviconnector.feature.AppEffect
import com.cm.naviconnector.feature.AppEvent
import com.cm.naviconnector.feature.AppUiState
import com.cm.naviconnector.feature.audio.AudioFile
import com.cm.naviconnector.feature.audio.AudioRepository
import com.cm.naviconnector.feature.control.Feature
import com.cm.naviconnector.feature.control.FeatureState
import com.cm.naviconnector.feature.control.TopButtonType
import com.cm.naviconnector.feature.control.toControlTargetOrNull
import com.cm.naviconnector.util.sendAll
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val bluetoothClient: BluetoothClient,
    audioRepository: AudioRepository
) : ViewModel() {

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
            is AppEvent.OnTopButtonTapped -> {
                when (event.type) {
                    TopButtonType.POWER -> onPowerButtonClick()
                    TopButtonType.BLUETOOTH -> onBluetoothButtonClick()
                    TopButtonType.WIFI -> onWifiButtonClick()
                    TopButtonType.UPLOAD -> onUploadButtonClick()
                }
            }

            is AppEvent.OnFeatureTapped -> {
                if (!_uiState.value.isConnected) return
                _uiState.update { it.copy(currentFeature = event.feature) }
            }

            is AppEvent.OnDialChanged -> {
                if (!_uiState.value.isConnected) return

                val currentFeature = _uiState.value.currentFeature ?: return
                val newLevel = event.level

                val controlTarget = currentFeature.toControlTargetOrNull()
                if (controlTarget != null) {

                    val packet = ControlPacket(target = controlTarget, value = newLevel)
                    val isSuccess = bluetoothConnection?.sendPacket(packet.toByteArray())

                    if (isSuccess == true) {
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

            is AppEvent.OnDeviceSelected -> {
                _uiState.update { it.copy(isConnected = true) }
            }

            AppEvent.OnPlayClicked -> _uiState.update { it.copy(isPlaying = true) }

            AppEvent.OnPauseClicked -> _uiState.update { it.copy(isPlaying = false) }
        }
    }

    fun startScan() = viewModelScope.launch {
        if (bluetoothClient.startScan() == true) {
            _scannedDevices.update { bluetoothClient.bondedDevices()?.toList().orEmpty() }
        } else {
            _effects.send(AppEffect.ShowToast("블루투스 스캔에 실패했습니다"))
        }
    }

    fun onClickDevice(device: BluetoothDevice) = viewModelScope.launch {
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

            bluetoothClient.collectConnectionState().collect {
                when (it.state) {
                    BluetoothAdapter.STATE_CONNECTED -> {
                    }

                    BluetoothAdapter.STATE_DISCONNECTED -> { // TODO: feature deactivate
                        _uiState.update { currentState ->
                            currentState.copy(
                                isConnected = false,
                                isPlaying = false
                            )
                        }
                        _effects.send(AppEffect.ShowToast("장치와 연결이 해제되었습니다"))
                    }

                    else -> {}
                }
            }
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
    }

    fun onBluetoothButtonClick() {
        _effects.trySend(AppEffect.SetDeviceDialogVisible(true))
    }

    fun onWifiButtonClick() {

    }

    fun onUploadButtonClick() {
        _effects.trySend(AppEffect.SetAudioDialogVisible(true))
    }

    fun onAudioFileClick(file: AudioFile) {

    }

    fun onPlayButtonClick() {

    }

    fun onPauseButtonClick() {

    }
}