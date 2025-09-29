package com.cm.naviconnector.ui.component

import android.graphics.Paint
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.cm.naviconnector.util.centeredProgressDiameter
import com.cm.naviconnector.util.normalizeProgress
import com.cm.naviconnector.util.toPx
import com.cm.naviconnector.util.wrapThresholdForSteps
import me.tankery.lib.circularseekbar.CircularSeekBar

@Composable
fun CircularSeekbar(
    value: Int,
    onValueChangeFinished: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    min: Int = 0,
    max: Int = 10,
    outerSize: Dp = 220.dp,
    innerSize: Dp = 100.dp,
    pointerWidth: Dp = 32.dp,
    startAngle: Float = 90f,
    endEAngle: Float = 90f,
    accentColor: Color = Color.LightGray,
) {
    val density = LocalDensity.current
    val seekSize = centeredProgressDiameter(innerSize, outerSize, pointerWidth)
    var displayedValue by rememberSaveable { mutableIntStateOf(value) }

    val minF = min.toFloat()
    val maxF = max.toFloat()
    var lastUserProgressF by rememberSaveable { mutableFloatStateOf(value.toFloat()) }
    val wrapThreshold = remember(max) { wrapThresholdForSteps(max) }

    LaunchedEffect(value) {
        displayedValue = value
    }

    Box(
        modifier = modifier.size(outerSize),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    elevation = 10.dp,
                    shape = CircleShape,
                    ambientColor = accentColor,
                    spotColor = accentColor
                ),
            shape = CircleShape,
            color = Color.White,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {}

        AndroidView(
            modifier = Modifier.size(seekSize),
            factory = { ctx ->
                CircularSeekBar(ctx).apply {
                    this.max = max.toFloat()
                    progress = value.toFloat()

                    this.startAngle = startAngle
                    this.endAngle = endEAngle

                    circleColor = Color.Transparent.toArgb()
                    circleProgressColor = Color.Transparent.toArgb()
                    circleStyle = Paint.Cap.ROUND

                    pointerColor = Color.White.toArgb()
                    pointerStrokeWidth = pointerWidth.toPx(density)

                    setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                    val blur = 4.dp.toPx(density)
                    val dx = 0f
                    val dy = 4f
                    val dropShadow = Color.LightGray.toArgb()

                    applyCustomPointerPaint { p ->
                        p.isAntiAlias = true
                        p.setShadowLayer(blur, dx, dy, dropShadow)
                    }

                    setOnSeekBarChangeListener(object :
                        CircularSeekBar.OnCircularSeekBarChangeListener {
                        override fun onProgressChanged(
                            circularSeekBar: CircularSeekBar?,
                            progress: Float,
                            fromUser: Boolean
                        ) {
                            if (!fromUser) return

                            val p = progress.coerceIn(minF, maxF)
                            val shown =
                                normalizeProgress(p, min, max, lastUserProgressF, wrapThreshold)

                            displayedValue = shown
                            lastUserProgressF = p
                        }

                        override fun onStartTrackingTouch(seekBar: CircularSeekBar?) {}

                        override fun onStopTrackingTouch(seekBar: CircularSeekBar?) {
                            seekBar ?: return

                            val p = seekBar.progress.coerceIn(minF, maxF)
                            val finalValue =
                                normalizeProgress(p, min, max, lastUserProgressF, wrapThreshold)

                            seekBar.progress = finalValue.toFloat()
                            displayedValue = finalValue
                            lastUserProgressF = p
                            onValueChangeFinished(finalValue)
                        }
                    })
                }
            },
            update = { view ->
                if (!view.isPressed) {
                    if (view.progress.toInt() != value) view.progress = value.toFloat()
                }

                view.isEnabled = enabled
                view.pointerColor = Color.White.toArgb()
                view.pointerStrokeWidth = pointerWidth.toPx(density)
            }
        )

        Surface(
            modifier = Modifier
                .size(innerSize)
                .shadow(
                    elevation = 8.dp,
                    shape = CircleShape,
                    ambientColor = accentColor,
                    spotColor = accentColor
                ),
            shape = CircleShape,
            color = Color.White,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = displayedValue.toString(),
                    fontSize = 32.sp,
                    color = accentColor
                )
            }
        }
    }
}