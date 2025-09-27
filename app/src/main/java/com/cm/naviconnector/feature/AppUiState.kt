package com.cm.naviconnector.feature

import com.cm.naviconnector.feature.control.Feature
import com.cm.naviconnector.feature.control.FeatureState
import com.cm.naviconnector.feature.control.MainFeature
import com.cm.naviconnector.feature.control.PlayerState
import com.cm.naviconnector.feature.upload.UploadState

data class AppUiState(
    val currentFeature: Feature? = null,
    val features: Map<Feature, FeatureState> = MainFeature.allFeatures.associateWith { FeatureState() },
    val isConnected: Boolean = false,
    val uploadState: UploadState = UploadState.Idle,
    val selectedFileName: String? = null,
    val playerState: PlayerState = PlayerState()
) {
    val isPowerOn
        get() = isConnected && features.values.any { it.enabled }
}