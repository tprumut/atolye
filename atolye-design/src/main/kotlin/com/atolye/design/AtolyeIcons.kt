package com.atolye.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Atölye 2.0 — Özgün ikon ailesi (Canvas)
 * Geometrik, lacivert ana gövde, turuncu/bakır vurgu, tutarlı ağırlık.
 */

@Composable
fun AtolyeAppLogo(modifier: Modifier = Modifier, size: Dp = 52.dp) {
    Canvas(modifier = modifier.size(size)) {
        val s = min(this.size.width, this.size.height)
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val navy = Color(0xFF203B56)
        val white = Color(0xFFFDFCF9)
        val orange = Color(0xFFF39220)

        // outer hex nut
        val outerR = s * 0.46f
        drawPath(hexPath(cx, cy, outerR), color = navy)
        // inner mechanical center (circle + hex hole feel)
        drawCircle(color = white, radius = s * 0.185f, center = Offset(cx, cy))
        drawPath(hexPath(cx, cy, s * 0.11f), color = navy)
        // small top-right orange geometric accent
        val ax = cx + outerR * 0.58f
        val ay = cy - outerR * 0.58f
        drawCircle(color = orange, radius = s * 0.085f, center = Offset(ax, ay))
    }
}

@Composable
fun SpeedMeterPluginIcon(modifier: Modifier = Modifier, size: Dp = 44.dp, accent: Color = Color(0xFF00A8B5)) {
    Canvas(modifier = modifier.size(size)) {
        val s = min(sizePxW(), sizePxH())
        val cx = sizePxW() / 2f
        val cy = sizePxH() / 2f
        val navy = Color(0xFF203B56)
        val teal = accent
        val orange = Color(0xFFF39220)
        // body
        drawCircle(color = navy, radius = s * 0.46f, center = Offset(cx, cy))
        // gauge arc
        val arcR = s * 0.30f
        val rect = Rect(cx - arcR, cy - arcR + s * 0.04f, cx + arcR, cy + arcR + s * 0.04f)
        drawArc(
            color = teal,
            startAngle = 150f,
            sweepAngle = 240f,
            useCenter = false,
            topLeft = rect.topLeft,
            size = rect.size,
            style = Stroke(width = s * 0.075f, cap = StrokeCap.Round)
        )
        // ticks
        for (i in 0..4) {
            val a = Math.toRadians((150.0 + i * 60.0))
            val r1 = s * 0.22f; val r2 = s * 0.30f
            val x1 = cx + (r1 * cos(a)).toFloat(); val y1 = cy + s * 0.04f + (r1 * sin(a)).toFloat()
            val x2 = cx + (r2 * cos(a)).toFloat(); val y2 = cy + s * 0.04f + (r2 * sin(a)).toFloat()
            drawLine(Color(0xCCFFFFFF), Offset(x1, y1), Offset(x2, y2), strokeWidth = s * 0.03f, cap = StrokeCap.Round)
        }
        // needle (orange)
        val na = Math.toRadians(40.0)
        val nx = cx + (s * 0.24f * cos(na)).toFloat()
        val ny = cy + s * 0.04f + (s * 0.24f * sin(na)).toFloat()
        drawLine(orange, Offset(cx, cy + s * 0.06f), Offset(nx, ny), strokeWidth = s * 0.055f, cap = StrokeCap.Round)
        drawCircle(orange, radius = s * 0.055f, center = Offset(cx, cy + s * 0.06f))
    }
}

@Composable
fun KeepAwakePluginIcon(modifier: Modifier = Modifier, size: Dp = 44.dp, accent: Color = Color(0xFFF39220)) {
    Canvas(modifier = modifier.size(size)) {
        val s = min(sizePxW(), sizePxH())
        val cx = sizePxW() / 2f
        val cy = sizePxH() / 2f
        val sun = accent
        val navy = Color(0xFF203B56)
        // base badge
        drawCircle(color = navy, radius = s * 0.46f, center = Offset(cx, cy))
        // sun core
        drawCircle(color = sun, radius = s * 0.17f, center = Offset(cx, cy))
        // rays
        for (i in 0 until 8) {
            val a = Math.toRadians(i * 45.0)
            val r1 = s * 0.23f; val r2 = s * 0.33f
            val x1 = cx + (r1 * cos(a)).toFloat(); val y1 = cy + (r1 * sin(a)).toFloat()
            val x2 = cx + (r2 * cos(a)).toFloat(); val y2 = cy + (r2 * sin(a)).toFloat()
            drawLine(sun, Offset(x1, y1), Offset(x2, y2), strokeWidth = s * 0.055f, cap = StrokeCap.Round)
        }
    }
}

@Composable
fun SleepTimerPluginIcon(modifier: Modifier = Modifier, size: Dp = 44.dp, accent: Color = Color(0xFF294D70)) {
    Canvas(modifier = modifier.size(size)) {
        val s = min(sizePxW(), sizePxH())
        val cx = sizePxW() / 2f
        val cy = sizePxH() / 2f
        val navy = Color(0xFF203B56)
        val moon = Color(0xFFEAF1FF)
        drawCircle(color = accent.copy(alpha = 1f).let { if (it == Color.Transparent) navy else accent }, radius = s * 0.46f, center = Offset(cx, cy))
        // crescent (two circles)
        drawCircle(color = moon, radius = s * 0.20f, center = Offset(cx - s * 0.02f, cy))
        drawCircle(color = accent, radius = s * 0.17f, center = Offset(cx + s * 0.08f, cy - s * 0.05f))
        // Z
        val zc = Color(0xFFF39220)
        val zx = cx + s * 0.06f; val zy = cy - s * 0.22f; val zs = s * 0.11f
        drawLine(zc, Offset(zx, zy), Offset(zx + zs, zy), zs * 0.18f, StrokeCap.Round)
        drawLine(zc, Offset(zx + zs, zy), Offset(zx, zy + zs), zs * 0.18f, StrokeCap.Round)
        drawLine(zc, Offset(zx, zy + zs), Offset(zx + zs, zy + zs), zs * 0.18f, StrokeCap.Round)
    }
}

@Composable
fun AudioSelectorPluginIcon(modifier: Modifier = Modifier, size: Dp = 44.dp, accent: Color = Color(0xFF5B5FAE)) {
    Canvas(modifier = modifier.size(size)) {
        val s = min(sizePxW(), sizePxH())
        val cx = sizePxW() / 2f
        val cy = sizePxH() / 2f
        val navy = Color(0xFF203B56)
        val purple = accent
        val orange = Color(0xFFF39220)
        drawCircle(color = purple, radius = s * 0.46f, center = Offset(cx, cy))
        // speaker body
        val bx = cx - s * 0.18f; val by = cy - s * 0.10f
        drawRect(Color(0xFFF6F3FF), topLeft = Offset(bx, by), size = Size(s * 0.14f, s * 0.20f))
        val cone = Path().apply {
            moveTo(bx + s * 0.14f, by)
            lineTo(bx + s * 0.28f, by - s * 0.08f)
            lineTo(bx + s * 0.28f, by + s * 0.28f)
            lineTo(bx + s * 0.14f, by + s * 0.20f)
            close()
        }
        drawPath(cone, Color(0xFFF6F3FF))
        // waves
        val wx = bx + s * 0.33f
        for (i in 1..2) {
            val r = s * (0.08f + i * 0.075f)
            val rect = Rect(wx - r, cy - r, wx + r, cy + r)
            drawArc(Color(0xFFF6F3FF), -40f, 80f, false, rect.topLeft, rect.size, style = Stroke(s * 0.045f, cap = StrokeCap.Round))
        }
        drawCircle(orange, radius = s * 0.045f, center = Offset(cx + s * 0.28f, cy - s * 0.24f))
    }
}

@Composable
fun AtolyePluginIcon(
    pluginId: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    speedColor: Color = Color(0xFF00A8B5),
    keepAwakeColor: Color = Color(0xFFF39220),
    audioColor: Color = Color(0xFF5B5FAE),
    sleepColor: Color = Color(0xFF294D70),
    fallbackColor: Color = Color(0xFF203B56)
) {
    when (pluginId) {
        "com.toolbox.plugin.speedmeter" -> SpeedMeterPluginIcon(modifier, size, speedColor)
        "com.toolbox.plugin.keepawake" -> KeepAwakePluginIcon(modifier, size, keepAwakeColor)
        "com.toolbox.plugin.sessecici" -> AudioSelectorPluginIcon(modifier, size, audioColor)
        "com.toolbox.plugin.uykucu" -> SleepTimerPluginIcon(modifier, size, sleepColor)
        else -> Canvas(modifier = modifier.size(size)) {
            val s = min(sizePxW(), sizePxH())
            drawCircle(fallbackColor, radius = s * 0.46f, center = Offset(sizePxW() / 2f, sizePxH() / 2f))
            drawCircle(Color.White.copy(alpha = 0.9f), radius = s * 0.14f, center = Offset(sizePxW() / 2f, sizePxH() / 2f))
        }
    }
}

private fun hexPath(cx: Float, cy: Float, r: Float): Path = Path().apply {
    for (i in 0..5) {
        val angle = Math.toRadians((i * 60 - 30).toDouble())
        val x = cx + (r * cos(angle)).toFloat()
        val y = cy + (r * sin(angle)).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

private fun DrawScope.sizePxW() = size.width
private fun DrawScope.sizePxH() = size.height
