package com.cm.naviconnector

import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.Flow
import com.cm.naviconnector.feature.audio.AudioFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cm.bluetooth.BluetoothClient
import com.cm.bluetooth.data.BluetoothDeviceWrapper
import com.cm.naviconnector.feature.AppEvent
import com.cm.naviconnector.feature.AppUiState
import com.cm.naviconnector.feature.audio.AudioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val bluetoothClient: BluetoothClient,
    private val audioRepository: AudioRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private val _scannedDevices = MutableStateFlow(emptyList<BluetoothDeviceWrapper>())
    val scannedDevices: StateFlow<List<BluetoothDeviceWrapper>> = _scannedDevices

    val audioPaging: Flow<PagingData<AudioFile>> =
        audioRepository
            .pagedAudio()
            .cachedIn(viewModelScope)

    fun onEvent(event: AppEvent) {
        when (event) {
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

            AppEvent.OnBtLongPress -> {
                _uiState.update { it.copy(showDeviceListDialog = true) }
            }

            is AppEvent.OnDeviceSelected -> {
                _uiState.update { it.copy(isConnected = true, showDeviceListDialog = false) }
            }

            AppEvent.OnPlayClicked -> _uiState.update { it.copy(isPlaying = true) }

            AppEvent.OnPauseClicked -> _uiState.update { it.copy(isPlaying = false) }

            is AppEvent.SetAudioDialogVisibility -> {
                _uiState.update { it.copy(showAudioListDialog = event.isVisible) }
                if (event.isVisible) {
                    loadAudioFiles()
                }
            }

            AppEvent.OnLoadAudioFiles -> {
                loadAudioFiles()
            }
        }
    }

    private fun loadAudioFiles() = viewModelScope.launch {
//        val audioFiles = audioRepository.getAudioFiles()
    }

    fun startScan() = viewModelScope.launch {
        if (bluetoothClient.startScan() == true) {
            bluetoothClient.collectScanDevices()
                .collect { new ->
                    _scannedDevices.update { current ->
                        (current + new).distinctBy { it.bluetoothDevice.address }
                    }
                }
        } else {

        }
    }

    fun stopScan() {
//        bluetoothClient.stopScan()
    }

    fun onClickDevice(device: BluetoothDeviceWrapper) = viewModelScope.launch {
//        bluetoothClient.getWorker(device.bluetoothDevice.createInsecureL2capChannel())
        bluetoothClient.collectConnectionState()
            .collect {
                if (device.bluetoothDevice.address == it.bluetoothDevice?.address) {

                }
            }
    }
}