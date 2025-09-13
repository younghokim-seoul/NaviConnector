package com.cm.naviconnector

import androidx.lifecycle.ViewModel
import com.cm.bluetooth.BluetoothClient
import com.cm.naviconnector.feature.AppEvent
import com.cm.naviconnector.feature.AppUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val bluetoothClient: BluetoothClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
    }

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

            is AppEvent.OnDeviceChosen -> {
                _uiState.update { it.copy(isConnected = true, showDeviceListDialog = false) }
            }

            AppEvent.OnDismissDeviceDialog -> {
                _uiState.update { it.copy(showDeviceListDialog = false) }
            }

            AppEvent.OnPlayClicked -> _uiState.update { it.copy(isPlaying = true) }

            AppEvent.OnPauseClicked -> _uiState.update { it.copy(isPlaying = false) }
        }
    }

    fun startScan() {
        bluetoothClient.startScan()
//            .onEach { device ->
//                _uiState.update { currentState ->
//                    val newDevices = currentState.scannedDevices.toMutableSet()
//                    newDevices.add(device)
//                    currentState.copy(scannedDevices = newDevices)
//                }
//            }
//            .catch { e ->
//                Timber.e(e, "Error during Bluetooth scan")
//            }
//            .launchIn(viewModelScope)
    }

    fun stopScan() {
//        bluetoothClient.stopScan()
    }
}