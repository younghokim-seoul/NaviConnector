package com.cm.naviconnector.feature.control

import androidx.annotation.DrawableRes
import com.cm.naviconnector.R

enum class BottomButtonType(@get:DrawableRes override val icon: Int) : ControlButton {
    PLAY(R.drawable.play),
    PAUSE(R.drawable.pause)
}