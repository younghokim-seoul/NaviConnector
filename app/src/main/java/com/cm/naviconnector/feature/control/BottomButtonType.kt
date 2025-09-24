package com.cm.naviconnector.feature.control

import androidx.annotation.DrawableRes
import com.cm.naviconnector.R

enum class BottomButtonType(@DrawableRes val icon: Int) {
    PLAY(R.drawable.play),
    PAUSE(R.drawable.pause),
}