package com.cm.naviconnector.ui.dial

import android.content.Context
import android.graphics.Paint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import me.tankery.lib.circularseekbar.CircularSeekBar
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CircularDial(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    min: Int = 0,
    max: Int = 10,
    outerSize: Dp = 220.dp,        // 가장 큰 원
    innerSize: Dp = 100.dp,        // 중앙 숫자 원 지름
    ringWidth: Dp = 32.dp,         // 링 두께
    ringMargin: Dp = 15.dp,        // 중앙 원과 링 사이 간격
    activeColor: Color = Color.Transparent, // 진행 링 색
    inactiveRingColor: Color = Color.White, // 링 배경 색
    numberColor: Color = Color(0xFFCC6B6B), // 중앙 숫자 색
    knobScale: Float = 0.82f,      // 노브 지름 = ringWidth * knobScale
    startAngleDeg: Float = 0f,     // ★ tankery 기본과 동일: 0°(오른쪽) 시작
    clockwise: Boolean = true      // tankery 기본: 시계방향
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    // 링 뷰 크기: 중앙 원 + (링두께 + 마진) * 2
    val seekSize = innerSize + (ringWidth + ringMargin) * 2

    Box(
        modifier = modifier.size(outerSize),
        contentAlignment = Alignment.Center
    ) {
        // 바깥 큰 원
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = CircleShape,
            color = Color.White,
            tonalElevation = 2.dp,
            shadowElevation = 10.dp
        ) {}

        // tankery: 링 + 터치 계산만 사용(포인터는 숨김)
        AndroidView(
            modifier = Modifier.size(seekSize),
            factory = { ctx ->
                CircularSeekBar(ctx).apply {
                    this.max = max.toFloat()
                    progress = value.toFloat()

                    circleColor = inactiveRingColor.toArgb()
                    circleProgressColor = activeColor.toArgb()
                    circleStrokeWidth = ringWidth.toPx(ctx)
                    circleStyle = Paint.Cap.ROUND

                    // 포인터는 숨김(겹침/오프셋 문제 방지)
                    pointerColor = Color.Transparent.toArgb()
                    pointerHaloColor = Color.Transparent.toArgb()

//                    isTouchEnabled = true
                    setOnSeekBarChangeListener(object :
                        CircularSeekBar.OnCircularSeekBarChangeListener {
                        override fun onProgressChanged(
                            circularSeekBar: CircularSeekBar?,
                            progress: Float,
                            fromUser: Boolean
                        ) {
                            if (fromUser) onValueChange(progress.toInt().coerceIn(min, max))
                        }
                        override fun onStartTrackingTouch(seekBar: CircularSeekBar?) {}
                        override fun onStopTrackingTouch(seekBar: CircularSeekBar?) {}
                    })
                }
            },
            update = { view ->
                if (view.progress.toInt() != value) view.progress = value.toFloat()
                if (view.max.toInt() != max) view.max = max.toFloat()
                view.circleColor = inactiveRingColor.toArgb()
                view.circleProgressColor = activeColor.toArgb()
                view.circleStrokeWidth = ringWidth.toPx(view.context)
                view.circleStyle = Paint.Cap.ROUND
                view.pointerColor = Color.Transparent.toArgb()
                view.pointerHaloColor = Color.Transparent.toArgb()
            }
        )

        // ===== Compose 오버레이 노브(흰색 + 그림자), 내접 트랙 중앙선 위 배치 =====
        val outerPx = with(density) { outerSize.toPx() }
        val innerPx = with(density) { innerSize.toPx() }
        val ringWidthPx = with(density) { ringWidth.toPx() }
        val ringMarginPx = with(density) { ringMargin.toPx() }

        // 트랙 반지름(노브 중심 궤도) = inner/2 + margin + ringWidth/2
        val trackRadiusPx = innerPx / 2f + ringMarginPx + ringWidthPx / 2f

        // 노브 크기: 링 두께 대비 비율
        val knobDiameter = ringWidth * knobScale
        val knobDiameterPx = with(density) { knobDiameter.toPx() }
        val knobRadiusPx = knobDiameterPx / 2f

        // progress -> angle: tankery 기본과 동일 매핑
        val range = (max - min).coerceAtLeast(1)
        val fraction = ((value - min).toFloat() / range.toFloat()).coerceIn(0f, 1f)
        val signed = if (clockwise) +360f else -360f
        val angleDeg = startAngleDeg + signed * fraction
        val theta = Math.toRadians(angleDeg.toDouble())

        // Box(outerSize) 중심 기준 좌표
        val center = outerPx / 2f
        val knobCx = (center + cos(theta) * trackRadiusPx).toFloat()
        val knobCy = (center + sin(theta) * trackRadiusPx).toFloat()

        // 흰색 노브 + 그림자 (오버레이)
        Surface(
            modifier = Modifier
                .size(knobDiameter)
                .offset {
                    IntOffset(
                        (knobCx - knobRadiusPx).toInt(),
                        (knobCy - knobRadiusPx).toInt()
                    )
                },
            shape = CircleShape,
            color = Color.White,
            tonalElevation = 0.dp,
            shadowElevation = 8.dp
        ) {}

        // 중앙 숫자 원
        Surface(
            modifier = Modifier.size(innerSize),
            shape = CircleShape,
            color = Color(0xFFFDFDFD),
            tonalElevation = 4.dp,
            shadowElevation = 8.dp
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = value.toString(),
                    fontSize = 32.sp,
                    color = numberColor
                )
            }
        }
    }
}

// px 변환
private fun Dp.toPx(context: Context): Float =
    this.value * context.resources.displayMetrics.density
