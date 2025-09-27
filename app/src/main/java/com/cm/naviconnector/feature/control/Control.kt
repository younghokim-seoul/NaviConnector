package com.cm.naviconnector.feature.control

import androidx.annotation.DrawableRes

interface Control {
    @get:DrawableRes
    val icon: Int
}