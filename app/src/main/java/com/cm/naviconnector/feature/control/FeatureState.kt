package com.cm.naviconnector.feature.control

import com.cm.naviconnector.feature.RequestState

data class FeatureState(
    val enabled: Boolean = false,
    val level: Int = 0,
    val requestState: RequestState = RequestState.Idle
) {
    val isActive
        get() = enabled && level > 0

    val isLoading
        get() = requestState == RequestState.Loading
}