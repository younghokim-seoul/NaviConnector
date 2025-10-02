package com.cm.naviconnector.feature.control

sealed class ControlState {
    object Idle : ControlState()
    object Loading : ControlState()
}