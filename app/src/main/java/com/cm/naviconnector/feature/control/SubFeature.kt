package com.cm.naviconnector.feature.control

import androidx.compose.ui.graphics.Color
import com.cm.naviconnector.ui.theme.Blue

sealed class SubFeature(
    override val id: String,
    override val color: Color
) : Feature {

    data object Random : SubFeature(
        id = "Random",
        color = Blue
    )
}