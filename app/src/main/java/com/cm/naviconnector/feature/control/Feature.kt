package com.cm.naviconnector.feature.control

import androidx.compose.ui.graphics.Color

sealed interface Feature {
    val id: String
    val color: Color
}