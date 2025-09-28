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
    override val color: Color,
    val controlTarget: ControlTarget?,
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
        color = Rose,
        controlTarget = ControlTarget.HEATER,
        icon = R.drawable.heater
    )

    data object Audio : MainFeature(
        id = "audio",
        color = Aqua,
        controlTarget = null,
        icon = R.drawable.audio
    )

    data object Fan : MainFeature(
        id = "fan",
        color = Indigo,
        controlTarget = ControlTarget.FAN,
        icon = R.drawable.fan
    )

    data object Film : MainFeature(
        id = "film",
        color = Goldenrod,
        controlTarget = ControlTarget.FILM,
        icon = R.drawable.film,
        subFeatures = listOf(SubFeature.Random)
    )
}