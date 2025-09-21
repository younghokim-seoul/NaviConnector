package com.cm.naviconnector.feature.audio

import android.net.Uri

data class AudioFile(
    val uri: Uri,
    val name: String,
    val duration: Long,
    val size: Long
)
