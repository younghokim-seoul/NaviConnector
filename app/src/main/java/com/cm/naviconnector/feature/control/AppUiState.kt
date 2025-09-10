package com.cm.naviconnector.feature.control

enum class Feature { HEAT, MUSIC, FAN, WINDOW }

data class FeatureState(
    val level: Int = 0,
    val enabled: Boolean = false
) {
    val isActive: Boolean get() = enabled && level >= 1
}

data class AppUiState(
    val currentFeature: Feature = Feature.FAN,
    val features: Map<Feature, FeatureState> = Feature.values().associateWith { FeatureState() },
    val isPlaying: Boolean = false,
    val isConnected: Boolean = false, // BT 연결 여부
    val showDeviceListDialog: Boolean = false
) {
    val top1PowerActive: Boolean = features[Feature.HEAT]?.isActive == true ||
                                 features[Feature.MUSIC]?.isActive == true ||
                                 features[Feature.WINDOW]?.isActive == true
}

sealed interface AppEvent {
    data class OnFeatureTapped(val feature: Feature) : AppEvent
    data class OnDialChanged(val level: Int) : AppEvent
    data object OnBtLongPress : AppEvent
    data class OnDeviceChosen(val id: String) : AppEvent
    data object OnDismissDeviceDialog : AppEvent
    data object OnPlayClicked : AppEvent
    data object OnPauseClicked : AppEvent
}