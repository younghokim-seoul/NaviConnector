package com.cm.naviconnector.ui.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CircularDial(
    level: Int,
    onLevelChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color,
    inactiveColor: Color
) {
    var angle by remember { mutableStateOf(0f) }
    var lastAngle by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .size(250.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { lastAngle = 0f },
                    onDrag = { change, _ ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val dragAngle = atan2(center.y - change.position.y, change.position.x - center.x) * (180f / PI.toFloat())
                        val normalizedAngle = (dragAngle + 450f) % 360f

                        if (lastAngle != 0f) {
                            angle += normalizedAngle - lastAngle
                        }
                        lastAngle = normalizedAngle

                        val newLevel = (angle / 27f).coerceIn(0f, 10f).toInt()
                        onLevelChange(newLevel)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) { 
            val dialRadius = size.minDimension / 2.5f
            val innerCircleRadius = dialRadius / 2f
            val strokeWidth = 20.dp.toPx()

            // Inactive track
            drawCircle(color = inactiveColor, style = Stroke(width = strokeWidth), radius = dialRadius)

            // Active track
            drawArc(
                color = activeColor,
                startAngle = -90f,
                sweepAngle = (level / 10f) * 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Inner circle
            drawCircle(color = Color.White, style = Stroke(width = 2.dp.toPx()), radius = innerCircleRadius)

            // Knob
            val knobAngle = (level / 10f) * 2 * PI - PI / 2
            val knobX = center.x + cos(knobAngle).toFloat() * dialRadius
            val knobY = center.y + sin(knobAngle).toFloat() * dialRadius
            drawCircle(color = Color.White, radius = 25.dp.toPx(), center = Offset(knobX, knobY))
            drawCircle(color = activeColor, radius = 20.dp.toPx(), center = Offset(knobX, knobY))
        }

        Text(text = level.toString(), fontSize = 60.sp, fontWeight = FontWeight.Bold, color = activeColor)
    }
}