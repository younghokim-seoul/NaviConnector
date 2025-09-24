package com.cm.naviconnector.feature.control

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.cm.bluetooth.data.reqeust.ControlTarget
import com.cm.naviconnector.R
import com.cm.naviconnector.ui.theme.Aqua
import com.cm.naviconnector.ui.theme.Goldenrod
import com.cm.naviconnector.ui.theme.Indigo
import com.cm.naviconnector.ui.theme.Rose

enum class Feature(
    @get:DrawableRes override val icon: Int,
    @get:ColorInt val color: Color
) : ControlButton {
    HEATER(R.drawable.heater, Rose),
    AUDIO(R.drawable.audio, Aqua),
    FAN(R.drawable.fan, Indigo),
    FILM(R.drawable.film, Goldenrod)
}

fun Feature.toControlTargetOrNull(): ControlTarget? =
    when (this) {
        Feature.HEATER -> ControlTarget.HEATER
        Feature.FAN -> ControlTarget.FAN
        Feature.FILM -> ControlTarget.FILM
        Feature.AUDIO -> null
    }
