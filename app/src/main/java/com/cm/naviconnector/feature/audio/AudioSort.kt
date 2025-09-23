package com.cm.naviconnector.feature.audio

sealed interface AudioSort {
    data object ByNameAsc : AudioSort
    data object ByNameDesc : AudioSort
    data object ByDurationDesc : AudioSort
}