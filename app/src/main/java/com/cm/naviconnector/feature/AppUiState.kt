package com.cm.naviconnector.feature

import com.cm.naviconnector.feature.control.Feature
import com.cm.naviconnector.feature.control.FeatureState

data class AppUiState(
    val currentFeature: Feature = Feature.FAN,
    val features: Map<Feature, FeatureState> = Feature.entries.associateWith { FeatureState() },
    val isPlaying: Boolean = false,
    val isConnected: Boolean = false,
    val showDeviceListDialog: Boolean = false,
    val showAudioListDialog: Boolean = false,
) {
    val powerActive: Boolean = features[Feature.HEAT]?.isActive == true ||
            features[Feature.AUDIO]?.isActive == true ||
            features[Feature.FILM]?.isActive == true
}