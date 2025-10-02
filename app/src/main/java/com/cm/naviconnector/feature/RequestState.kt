package com.cm.naviconnector.feature

sealed interface RequestState {
    data object Idle : RequestState
    data object Loading : RequestState
    data class InProgress(val progress: Int) : RequestState
    data object Success : RequestState
    data class Error(val reason: String? = null) : RequestState
}