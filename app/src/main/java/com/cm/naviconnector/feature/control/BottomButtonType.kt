package com.cm.naviconnector.feature.control

import androidx.annotation.DrawableRes
import com.cm.naviconnector.R

enum class BottomButtonType(
    @get:DrawableRes val enabledIcon: Int,
    @get:DrawableRes val disabledIcon: Int
) : Control {
    PLAY(R.drawable.play_enable, R.drawable.play_disable),
    PAUSE(R.drawable.pause_enable, R.drawable.pause_disable);

    override val icon: Int
        get() = enabledIcon
}