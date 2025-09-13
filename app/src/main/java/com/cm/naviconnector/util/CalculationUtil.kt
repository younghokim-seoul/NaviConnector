package com.cm.naviconnector.util

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun centeredProgressDiameter(inner: Dp, outer: Dp, stroke: Dp): Dp =
    (((inner.value + outer.value) / 2f) + stroke.value).dp

fun Dp.toPx(density: Density): Float = with(density) { this@toPx.toPx() }