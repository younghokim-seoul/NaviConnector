package com.cm.naviconnector.feature

import com.cm.naviconnector.feature.control.Feature

sealed interface AppEvent {
    data class OnFeatureTapped(val feature: Feature) : AppEvent
    data class OnDialChanged(val level: Int) : AppEvent
    data object OnBtLongPress : AppEvent
    data class OnDeviceSelected(val id: String) : AppEvent
    data object OnPlayClicked : AppEvent
    data object OnPauseClicked : AppEvent
    data class SetAudioDialogVisible(val visible: Boolean) : AppEvent
}