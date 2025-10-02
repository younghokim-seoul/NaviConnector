package com.cm.naviconnector.feature

import com.cm.naviconnector.feature.control.ControlState
import com.cm.naviconnector.feature.control.Feature
import com.cm.naviconnector.feature.control.FeatureState
import com.cm.naviconnector.feature.control.MainFeature
import com.cm.naviconnector.feature.control.PlayerState
import com.cm.naviconnector.feature.upload.UploadState

data class AppUiState(
    val currentFeature: Feature? = null,
    val features: Map<Feature, FeatureState> = MainFeature.allFeatures.associateWith { FeatureState() },
    val isConnected: Boolean = false,
    val isLowBattery: Boolean = false,
    val uploadState: UploadState = UploadState.Idle,
    val selectedFileName: String? = null,
    val playerState: PlayerState = PlayerState()
) {
    val isPowerOn
        get() = isConnected && features.values.any { it.isActive }
}

fun AppUiState.withFeatureLevel(
    feature: Feature,
    level: Int,
): AppUiState {
    val current = features[feature] ?: return this
    return copy(features = features + (feature to current.copy(level = level)))
}

fun AppUiState.withFeatureControlState(
    feature: Feature,
    controlState: ControlState,
): AppUiState {
    val current = features[feature] ?: return this
    return copy(features = features + (feature to current.copy(controlState = controlState)))
}

fun AppUiState.withAllFeaturesEnabled(
    enabled: Boolean,
): AppUiState {
    val updated = features.mapValues { (_, state) ->
        state.copy(enabled = enabled)
    }
    return copy(features = updated)
}