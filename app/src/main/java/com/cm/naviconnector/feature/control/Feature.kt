package com.cm.naviconnector.feature.control

import androidx.compose.ui.graphics.Color
import com.cm.bluetooth.data.reqeust.ControlTarget

interface Feature {
    val id: String
    val controlTarget: ControlTarget?
    val color: Color
}