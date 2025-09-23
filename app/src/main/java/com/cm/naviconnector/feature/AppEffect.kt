package com.cm.naviconnector.feature

sealed interface AppEffect {
    data class SetDeviceDialogVisible(val visible: Boolean) : AppEffect
    data class SetAudioDialogVisible(val visible: Boolean) : AppEffect
}