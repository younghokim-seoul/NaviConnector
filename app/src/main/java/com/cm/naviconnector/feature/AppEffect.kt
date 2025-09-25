package com.cm.naviconnector.feature

sealed interface AppEffect {
    data class SetDeviceDialogVisible(val visible: Boolean) : AppEffect
    data class SetAudioDialogVisible(val visible: Boolean) : AppEffect
    data class SetUploadDialogVisible(val visible: Boolean) : AppEffect
    data class ShowToast(val message: String) : AppEffect
}