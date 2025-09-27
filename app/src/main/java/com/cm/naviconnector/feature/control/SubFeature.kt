package com.cm.naviconnector.feature.control

import androidx.compose.ui.graphics.Color
import com.cm.bluetooth.data.reqeust.ControlTarget
import com.cm.naviconnector.ui.theme.Blue

sealed class SubFeature(
    override val id: String,
    override val controlTarget: ControlTarget?,
    override val color: Color
) : Feature {

    data object FilmAndFan : SubFeature(
        id = "Random",
        controlTarget = ControlTarget.FILM_AND_FAN,
        color = Blue
    )
}