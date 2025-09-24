package com.cm.naviconnector.util

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * CircularSeekBar 등의 float progress를 정수 스텝으로 스냅하면서,
 * 360° 경계(0/최대)에서 최대→0으로 급락하는 랩(회전) 현상을 보정
 *
 * - 반올림(roundToInt)으로 정수 스텝을 정확히 잡고(예: 9.6 → 10),
 * - 직전 값이 MAX 근처이고 현재 값이 MIN 근처면 랩으로 간주하여 MAX로 보정
 *
 * @param raw           현재 progress(float, coerceIn 전/후 모두 허용)
 * @param min           최소 정수 값 (예: 0)
 * @param max           최대 정수 값 (예: 10)
 * @param last          직전 progress(float) 값
 * @param wrapThreshold 랩 감지 완충값(예: max * 0.15f, 최소 1f 권장)
 * @return              정수 스텝으로 스냅된 값(랩 보정 적용)
 */
fun normalizeProgress(
    raw: Float,
    min: Int,
    max: Int,
    last: Float,
    wrapThreshold: Float = wrapThresholdForSteps(max)
): Int {
    val minF = min.toFloat()
    val maxF = max.toFloat()
    val p = raw.coerceIn(minF, maxF)
    val wrapped = last >= (maxF - wrapThreshold) && p <= (minF + wrapThreshold)
    return if (wrapped) max else p.roundToInt().coerceIn(min, max)
}

/**
 * 스텝 수에 비례한 랩 감지 완충값을 산출
 *
 * @param max       최대 정수 값 (예: 10)
 * @param fraction  완충 비율(0~1), 기본 0.15 = 전체의 15%
 * @return          랩 감지 완충값(float)
 */
fun wrapThresholdForSteps(max: Int, fraction: Float = 0.15f): Float =
    (max * fraction).coerceAtLeast(1f)

fun centeredProgressDiameter(inner: Dp, outer: Dp, stroke: Dp): Dp =
    (((inner.value + outer.value) / 2f) + stroke.value).dp

fun Dp.toPx(density: Density): Float = with(density) { this@toPx.toPx() }