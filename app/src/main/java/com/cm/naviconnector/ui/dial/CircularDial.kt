package com.cm.naviconnector.ui.dial

import android.content.Context
import android.graphics.Paint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import me.tankery.lib.circularseekbar.CircularSeekBar

@Composable
fun CircularDial(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    min: Int = 0,
    max: Int = 10,
    outerSize: Dp = 220.dp,       // 가장 큰 원
    innerSize: Dp = 100.dp,       // 숫자 텍스트 원
    ringWidth: Dp = 32.dp,        // 링 두께(노브가 도는 구간)
    ringMargin: Dp = 15.dp,        // 링과 내부 원 사이의 간격
    activeColor: Color = Color.Transparent, // 진행률 색상
    inactiveRingColor: Color = Color.White, // 링 배경
    numberColor: Color = Color(0xFFCC6B6B),         // 중앙 숫자
) {
    val context = LocalContext.current
    val seekSize = innerSize + (ringWidth + ringMargin) * 2  // 시크바 뷰는 두 원 사이에 정확히 들어가도록

    Box(
        modifier = modifier.size(outerSize),
        contentAlignment = Alignment.Center
    ) {
        // 바깥 큰 원(부드러운 그림자 느낌)
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = CircleShape,
            color = Color.White,
            tonalElevation = 2.dp,
            shadowElevation = 10.dp
        ) {}

        // 시크바(원형 링 + 노브) - 두 원 사이에 위치
        AndroidView(
            modifier = Modifier.size(seekSize),
            factory = { ctx ->
                CircularSeekBar(ctx).apply {
                    // (중요) 범위 확실히: 0..10
                    this.max = max.toFloat()

                    // 진행 색/배경
                    circleColor = inactiveRingColor.toArgb()
                    circleProgressColor = activeColor.toArgb()

                    // ★ 노브: 완전 흰색 + 그림자(halo)
                    pointerColor = Color.White.toArgb()
                    pointerHaloColor = Color.Black.copy(alpha = 0.22f).toArgb() // 부드러운 그림자
//                    pointerRadius = (ringWidth * 0.68f).toPx(ctx)               // 노브 크기
//                    pointerHaloWidth = (ringWidth * 0.90f).toPx(ctx)            // 그림자/후광 두께

                    // 링/끝처리
                    circleStrokeWidth = ringWidth.toPx(ctx)
                    circleStyle = Paint.Cap.ROUND

                    // 터치 on (노브 조작용)
//                    isTouchEnabled = true

                    progress = value.toFloat()
                    setOnSeekBarChangeListener(object :
                        CircularSeekBar.OnCircularSeekBarChangeListener {
                        override fun onProgressChanged(
                            circularSeekBar: CircularSeekBar?, progress: Float, fromUser: Boolean
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

                // 동적 색 반영 시
                view.circleColor = inactiveRingColor.toArgb()
                view.circleProgressColor = activeColor.toArgb()

                // 노브/그림자 보정(테마 변경 시)
                view.pointerColor = Color.White.toArgb()
                view.pointerHaloColor = Color.Black.copy(alpha = 0.22f).toArgb()
//                view.pointerRadius = (ringWidth * 0.68f).toPx(view.context)
//                view.pointerHaloWidth = (ringWidth * 0.90f).toPx(view.context)
            }
        )


        // 안쪽 숫자 원
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
