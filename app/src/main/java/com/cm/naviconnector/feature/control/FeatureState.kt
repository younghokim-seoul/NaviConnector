package com.cm.naviconnector.feature.control

data class FeatureState(
    val enabled: Boolean = false,
    val level: Int = 0
) {
    val isActive: Boolean get() = enabled && level >= 1
}