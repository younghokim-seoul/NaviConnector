package com.cm.naviconnector

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.cm.bluetooth.BluetoothClient
import com.cm.naviconnector.feature.AppEffect
import com.cm.naviconnector.feature.AppEvent
import com.cm.naviconnector.feature.AppUiState
import com.cm.naviconnector.feature.audio.AudioFile
import com.cm.naviconnector.feature.audio.AudioRepository
import com.cm.naviconnector.feature.control.TopButtonType
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

    fun onEvent(event: AppEvent) {
        when (event) {
            is AppEvent.OnTopButtonTapped -> {
                when (event.type) {
                    TopButtonType.POWER -> {
                    }

                    TopButtonType.BLUETOOTH -> {
                        _effects.trySend(AppEffect.SetDeviceDialogVisible(true))
                    }

                    TopButtonType.WIFI -> {
                    }

                    TopButtonType.UPLOAD -> {
                    }
                }
            }

            is AppEvent.OnFeatureTapped -> {
                if (!_uiState.value.isConnected) return
                _uiState.update { currentState ->
                    val newFeatures = currentState.features.toMutableMap()
                    val featureState = newFeatures[event.feature]
                    if (featureState != null) {
                        newFeatures[event.feature] =
                            featureState.copy(enabled = !featureState.enabled)
                    }
                    currentState.copy(currentFeature = event.feature, features = newFeatures)
                }
            }

            is AppEvent.OnDialChanged -> {
                if (!_uiState.value.isConnected) return
                _uiState.update { currentState ->
                    val newFeatures = currentState.features.toMutableMap()
                    val currentFeatureState = newFeatures[currentState.currentFeature]
                    if (currentFeatureState != null && currentFeatureState.enabled) {
                        newFeatures[currentState.currentFeature] =
                            currentFeatureState.copy(level = event.level)
                    }
                    currentState.copy(features = newFeatures)
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
            // TODO: scan failed toast message
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
            _uiState.update { it.copy(isConnected = true) }
            _effects.send(AppEffect.SetDeviceDialogVisible(false))
            // TODO: observe connection
        } else {
            // TODO: connection failed toast message
        }
    }

    fun onAudioFileClick(file: AudioFile) {

    }
}