package com.cm.naviconnector.feature

import com.cm.naviconnector.feature.control.Feature
import com.cm.naviconnector.feature.control.FeatureState

data class AppUiState(
    val currentFeature: Feature? = null,
    val features: Map<Feature, FeatureState> = Feature.entries.associateWith { FeatureState() },
    val isPlaying: Boolean = false,
    val isConnected: Boolean = false,
) {
    val isPowerOn
        get() = isConnected && features.values.any { it.enabled }
}