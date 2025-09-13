package com.cm.naviconnector.feature.control

data class FeatureState(
    val level: Int = 0,
    val enabled: Boolean = false
) {
    val isActive: Boolean get() = enabled && level >= 1
}