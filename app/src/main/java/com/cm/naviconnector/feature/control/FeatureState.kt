package com.cm.naviconnector.feature.control

data class FeatureState(
    val enabled: Boolean = false,
    val level: Int = 0,
    val controlState: ControlState = ControlState.Idle
) {
    val isActive
        get() = enabled && level > 0

    val isLoading
        get() = controlState == ControlState.Loading
}