package com.cm.naviconnector.feature.control

data class FeatureState(
    val level: Int = 0
) {
    val enabled
        get() = level > 0
}