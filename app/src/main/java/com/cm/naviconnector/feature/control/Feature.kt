package com.cm.naviconnector.feature.control

import androidx.annotation.DrawableRes
import com.cm.naviconnector.R

enum class Feature(@DrawableRes val icon: Int) {
    HEATER(R.drawable.heater),
    AUDIO(R.drawable.audio),
    FAN(R.drawable.fan),
    FILM(R.drawable.film)
}