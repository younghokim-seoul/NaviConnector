package com.cm.naviconnector.feature

import android.bluetooth.BluetoothDevice
import com.cm.naviconnector.feature.audio.AudioFile
import com.cm.naviconnector.feature.control.BottomButtonType
import com.cm.naviconnector.feature.control.Feature
import com.cm.naviconnector.feature.control.PlaylistItem
import com.cm.naviconnector.feature.control.TopButtonType

sealed interface AppEvent {
    data class TopButtonClicked(val type: TopButtonType) : AppEvent
    data class FeatureSelected(val feature: Feature) : AppEvent
    data class DialChanged(val level: Int) : AppEvent
    data class DeviceConnectClicked(val device: BluetoothDevice) : AppEvent
    data class BottomButtonClicked(val type: BottomButtonType) : AppEvent
    data class AudioUploadClicked(val file: AudioFile) : AppEvent
    data class PlaylistItemClicked(val item: PlaylistItem) : AppEvent
    data object AudioDialogDismissed : AppEvent
    data object DeviceDialogDismissed : AppEvent
}