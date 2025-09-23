package com.cm.naviconnector.feature

import com.cm.naviconnector.feature.control.Feature
import com.cm.naviconnector.feature.control.FeatureState

data class AppUiState(
    val currentFeature: Feature = Feature.FAN,
    val features: Map<Feature, FeatureState> = Feature.entries.associateWith { FeatureState() },
    val isPlaying: Boolean = false,
    val isConnected: Boolean = false,
) {
    val isPowerOn
        get() = features.values.any { it.enabled }
}