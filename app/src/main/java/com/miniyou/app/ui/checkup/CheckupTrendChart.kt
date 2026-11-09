package com.miniyou.app.ui.checkup

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miniyou.app.data.FetalReference
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

internal data class TrendPoint(val x: Double, val value: Double)
internal data class TrendSeries(val label: String, val color: Color, val points: List<TrendPoint>)
internal data class TrendReference(val x: Double, val reference: FetalReference)

@Composable
internal fun CheckupTrendChart(
    series: List<TrendSeries>,
    unit: String,
    ticks: List<Pair<Double, String>>,
    modifier: Modifier = Modifier,
    reference: List<TrendReference> = emptyList(),
    selectedX: Double? = null,
    onSelect: ((Double) -> Unit)? = null,
) {
    val colors = LocalPrenatalColors.current
    val points = series.flatMap { it.points }
    if (points.isEmpty()) return
    val xValues = points.map { it.x } + reference.map { it.x }
    val firstX = xValues.min()
    val lastX = xValues.max()
    val minX = if (firstX == lastX) firstX - 1 else firstX
    val maxX = if (firstX == lastX) lastX + 1 else lastX
    val yValues = points.map { it.value } + reference.flatMap {
        listOf(it.reference.median - 2 * it.reference.deviation, it.reference.median + 2 * it.reference.deviation)
    }
    val minimum = yValues.min()
    val maximum = yValues.max()
    val padding = ((maximum - minimum) * 0.12).coerceAtLeast(if (unit == "cm") 0.1 else 0.5)
    val lower = if (reference.isEmpty()) (minimum - padding).coerceAtLeast(0.0) else 0.0
    val upper = maximum + padding
    val roughStep = (upper - lower) / 5
    val magnitude = 10.0.pow(floor(log10(roughStep)))
    val step = listOf(1.0, 2.0, 2.5, 5.0, 10.0).first { it * magnitude >= roughStep } * magnitude
    val minY = floor(lower / step) * step
    val maxY = ceil(upper / step) * step
    val xMargin = 37.dp
    val endMargin = 21.dp
    val touch = if (onSelect == null) Modifier else Modifier.pointerInput(points, onSelect) {
        detectTapGestures { position ->
            val span = (size.width - xMargin.toPx() - endMargin.toPx()).coerceAtLeast(1f)
            val fraction = ((position.x - xMargin.toPx()) / span).coerceIn(0f, 1f)
            val target = minX + (maxX - minX) * fraction
            onSelect(points.minBy { abs(it.x - target) }.x)
        }
    }
    val description = series.filter { it.points.isNotEmpty() }.joinToString("，") {
        "${it.label} ${it.points.size} 个记录值，最近 ${trendNumber(it.points.last().value)} $unit"
    }

    Canvas(
        modifier
            .fillMaxWidth()
            .height(145.dp)
            .semantics { contentDescription = description }
            .then(touch),
    ) {
        val left = xMargin.toPx()
        val right = (size.width - endMargin.toPx()).coerceAtLeast(left + 1)
        val top = 20.dp.toPx()
        val bottom = size.height - 20.dp.toPx()
        fun x(value: Double) = left + ((value - minX) / (maxX - minX)).toFloat() * (right - left)
        fun y(value: Double) = bottom - ((value - minY) / (maxY - minY)).toFloat() * (bottom - top)
        fun path(values: List<TrendPoint>) = Path().apply {
            moveTo(x(values.first().x), y(values.first().value))
            values.drop(1).forEach { lineTo(x(it.x), y(it.value)) }
        }
        val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 9.sp.toPx()
            color = colors.muted.toArgb()
        }
        val dash = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()))

        if (reference.isNotEmpty()) {
            val high = reference.map { TrendPoint(it.x, it.reference.median + 2 * it.reference.deviation) }
            val low = reference.map { TrendPoint(it.x, it.reference.median - 2 * it.reference.deviation) }
            val band = path(high).apply {
                low.asReversed().forEach { lineTo(x(it.x), y(it.value)) }
                close()
            }
            drawPath(band, colors.accent.copy(alpha = 0.13f))
            listOf(high, low).forEach { drawPath(path(it), colors.line, style = Stroke(1.dp.toPx())) }
            val mid = reference.map { TrendPoint(it.x, it.reference.median) }
            drawPath(path(mid), colors.muted, style = Stroke(1.dp.toPx()))
        }

        val yTickCount = ((maxY - minY) / step).roundToInt()
        for (i in 0..yTickCount) {
            val value = minY + step * i
            val position = y(value)
            drawLine(colors.outline, Offset(left, position), Offset(right, position), 1.dp.toPx(), pathEffect = dash)
            drawIntoCanvas {
                axisPaint.textAlign = Paint.Align.RIGHT
                it.nativeCanvas.drawText(trendNumber(value), left - 5.dp.toPx(), position + 3.dp.toPx(), axisPaint)
            }
        }
        drawLine(colors.muted, Offset(left, top), Offset(left, bottom), 0.7.dp.toPx())
        drawLine(colors.muted, Offset(left, bottom), Offset(right, bottom), 0.7.dp.toPx())
        drawIntoCanvas {
            axisPaint.textAlign = Paint.Align.LEFT
            it.nativeCanvas.drawText(unit, left, top - 9.dp.toPx(), axisPaint)
            axisPaint.textAlign = Paint.Align.CENTER
            ticks.forEach { (value, label) ->
                it.nativeCanvas.drawText(label, x(value), bottom + 13.dp.toPx(), axisPaint)
            }
        }
        selectedX?.let {
            drawLine(colors.muted, Offset(x(it), top), Offset(x(it), bottom), 1.dp.toPx(), pathEffect = dash)
        }
        val valuePaint = Paint(axisPaint).apply {
            textSize = 10.sp.toPx()
            textAlign = Paint.Align.CENTER
        }
        series.filter { it.points.isNotEmpty() }.forEach { line ->
            val values = line.points
            val plot = path(values)
            if (series.size == 1 && reference.isEmpty() && values.size > 1) {
                val area = Path().apply {
                    addPath(plot)
                    lineTo(x(values.last().x), bottom)
                    lineTo(x(values.first().x), bottom)
                    close()
                }
                drawPath(area, Brush.verticalGradient(listOf(line.color.copy(alpha = 0.28f), Color.Transparent), top, bottom))
            }
            drawPath(plot, line.color, style = Stroke(2.dp.toPx()))
            val showValues = reference.isEmpty() && values.size <= 4 && values.zipWithNext().all { (left, right) ->
                abs(x(left.x) - x(right.x)) >= 32.dp.toPx()
            }
            val activeIndex = values.indexOfLast { it.x == selectedX }
            values.forEachIndexed { index, point ->
                val center = Offset(x(point.x), y(point.value))
                val active = index == activeIndex
                drawCircle(line.color, if (active) 5.dp.toPx() else 3.5.dp.toPx(), center)
                drawCircle(colors.card, if (active) 2.5.dp.toPx() else 1.5.dp.toPx(), center)
                if (active || showValues) {
                    drawIntoCanvas {
                        valuePaint.color = line.color.toArgb()
                        val label = trendNumber(point.value)
                        val inset = (valuePaint.measureText(label) / 2 + 2.dp.toPx()).coerceAtMost((right - left) / 2)
                        val labelX = center.x.coerceIn(left + inset, right - inset)
                        it.nativeCanvas.drawText(label, labelX, center.y - 7.dp.toPx(), valuePaint)
                    }
                }
            }
        }
    }
}

internal fun trendNumber(value: Double): String =
    String.format(Locale.ROOT, "%.2f", value).trimEnd('0').trimEnd('.')
