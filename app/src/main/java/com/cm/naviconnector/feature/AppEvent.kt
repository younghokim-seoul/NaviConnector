package com.cm.naviconnector.feature

import com.cm.naviconnector.feature.control.Feature
import com.cm.naviconnector.feature.control.TopButtonType

sealed interface AppEvent {
    data class OnTopButtonTapped(val type: TopButtonType) : AppEvent
    data class OnFeatureTapped(val feature: Feature) : AppEvent
    data class OnDialChanged(val level: Int) : AppEvent
    data class OnDeviceSelected(val id: String) : AppEvent
    data object OnPlayClicked : AppEvent
    data object OnPauseClicked : AppEvent
}