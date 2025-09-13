package com.cm.naviconnector.feature

import com.cm.naviconnector.R

data class AppUiState(
    val currentFeature: Feature = Feature.FAN,
    val features: Map<Feature, FeatureState> = Feature.entries.associateWith { FeatureState() },
    val isPlaying: Boolean = false,
    val isConnected: Boolean = false,
    val showDeviceListDialog: Boolean = false
) {
    val powerActive: Boolean = features[Feature.HEAT]?.isActive == true ||
            features[Feature.AUDIO]?.isActive == true ||
            features[Feature.FILM]?.isActive == true
}

enum class Feature(val resourceId: Int) {
    HEAT(R.drawable.heater),
    AUDIO(R.drawable.audio),
    FAN(R.drawable.fan),
    FILM(R.drawable.film)
}

data class FeatureState(
    val level: Int = 0,
    val enabled: Boolean = false
) {
    val isActive: Boolean get() = enabled && level >= 1
}