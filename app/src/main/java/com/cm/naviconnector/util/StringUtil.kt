package com.cm.naviconnector.util

import java.util.concurrent.TimeUnit

fun formatDuration(durationMillis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis)
    val seconds =
        TimeUnit.MILLISECONDS.toSeconds(durationMillis) - TimeUnit.MINUTES.toSeconds(minutes)
    return String.format("%02d:%02d", minutes, seconds)
}