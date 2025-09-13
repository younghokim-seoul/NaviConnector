package com.cm.naviconnector.feature

sealed interface AppEvent {
    data class OnFeatureTapped(val feature: Feature) : AppEvent
    data class OnDialChanged(val level: Int) : AppEvent
    data object OnBtLongPress : AppEvent
    data class OnDeviceChosen(val id: String) : AppEvent
    data object OnDismissDeviceDialog : AppEvent
    data object OnPlayClicked : AppEvent
    data object OnPauseClicked : AppEvent
}