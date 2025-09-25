package com.cm.naviconnector.feature.upload

sealed interface UploadState {
    data object Idle : UploadState
    data class InProgress(val progress: Int) : UploadState
}