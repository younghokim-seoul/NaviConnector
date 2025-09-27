package com.cm.naviconnector.feature.control

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.cm.bluetooth.data.reqeust.ControlTarget
import com.cm.naviconnector.R
import com.cm.naviconnector.ui.theme.Aqua
import com.cm.naviconnector.ui.theme.Goldenrod
import com.cm.naviconnector.ui.theme.Indigo
import com.cm.naviconnector.ui.theme.Rose

sealed class MainFeature(
    override val id: String,
    override val controlTarget: ControlTarget?,
    override val color: Color,
    @DrawableRes
    val icon: Int,
    val subFeatures: List<SubFeature> = emptyList()
) : Feature {

    companion object {
        val mainFeatures = listOf(Heater, Audio, Fan, Film)
        val allFeatures = mainFeatures + mainFeatures.flatMap { it.subFeatures }
    }

    data object Heater : MainFeature(
        id = "heater",
        controlTarget = ControlTarget.HEATER,
        color = Rose,
        icon = R.drawable.heater
    )

    data object Audio : MainFeature(
        id = "audio",
        controlTarget = null,
        color = Aqua,
        icon = R.drawable.audio
    )

    data object Fan : MainFeature(
        id = "fan",
        controlTarget = ControlTarget.FAN,
        color = Indigo,
        icon = R.drawable.fan
    )

    data object Film : MainFeature(
        id = "film",
        controlTarget = ControlTarget.FILM,
        color = Goldenrod,
        icon = R.drawable.film,
        subFeatures = listOf(SubFeature.FilmAndFan)
    )
}