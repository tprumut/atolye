package com.atolye.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.layout.ContentScale

/**
 * Atölye Tasarım Sistemi - İkon Bileşenleri
 *
 * Altıgen yuvalı ikon (uyarlanabilir ikon standardı)
 * Orta kısmı delikli altıgen şeklinde
 */

@Composable
fun AtolyeAdaptiveIcon(
    modifier: Modifier = Modifier,
    iconPainter: androidx.compose.ui.graphics.painter.Painter,
    backgroundColor: Color,
    foregroundTint: Color = Color.White,
    size: Dp = 108.dp
) {
    val hexagonPath = rememberHexagonPath(size)
    val innerHexagonPath = rememberInnerHexagonPath(size)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Arka plan (altıgen)
        Canvas(modifier = Modifier.size(size)) {
            drawPath(
                path = hexagonPath,
                color = backgroundColor
            )
        }

        // İkon (orta yuvada)
        Box(
            modifier = Modifier
                .size((size.value * 0.65f).dp)
                .graphicsLayer { alpha = 1f },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = iconPainter,
                contentDescription = null,
                colorFilter = ColorFilter.tint(foregroundTint),
                contentScale = ContentScale.Fit
            )
        }

        // İç yuva çerçevesi (opsiyonel - derinlik hissi için)
        Canvas(modifier = Modifier.size(size)) {
            drawPath(
                path = innerHexagonPath,
                color = backgroundColor.copy(alpha = 0.15f),
                style = Stroke(width = 1.5f)
            )
        }
    }
}

/**
 * Basit hexagon path (vector drawable yerine Canvas ile çizim)
 */
@Composable
private fun rememberHexagonPath(size: Dp): Path {
    val density = LocalDensity.current
    val pxSize = density.run { size.toPx() }
    return remember(pxSize) {
        val radius = pxSize / 2f
        val center = Offset(radius, radius)
        Path().apply {
            fillType = PathFillType.EvenOdd
            for (i in 0..5) {
                val angle = (i * 60 - 30) * Math.PI / 180
                val x = center.x + radius * Math.cos(angle).toFloat()
                val y = center.y + radius * Math.sin(angle).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
    }
}

@Composable
private fun rememberInnerHexagonPath(size: Dp): Path {
    val density = LocalDensity.current
    val pxSize = density.run { size.toPx() }
    return remember(pxSize) {
        val radius = pxSize / 2f * 0.65f // İç yuva yarıçapı
        val center = Offset(pxSize / 2f, pxSize / 2f)
        Path().apply {
            for (i in 0..5) {
                val angle = (i * 60 - 30) * Math.PI / 180
                val x = center.x + radius * Math.cos(angle).toFloat()
                val y = center.y + radius * Math.sin(angle).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
    }
}

/**
 * Eklenti ikonları için özel bileşenler
 * Not: drawable resource'lar host app modülünde olduğundan,
 * bu fonksiyonlar host app'te kullanılmalı veya resource'lar design modülüne taşınmalı.
 * Şimdilik basit placeholder implementasyon.
 */

@Composable
fun SpeedMeterIcon(
    modifier: Modifier = Modifier,
    colors: AtolyeColorScheme,
    size: Dp = 48.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .background(colors.pluginSpeedMeter, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = null,
            tint = Color.White
        )
    }
}

@Composable
fun KeepAwakeIcon(
    modifier: Modifier = Modifier,
    colors: AtolyeColorScheme,
    size: Dp = 48.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .background(colors.pluginKeepAwake, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = null,
            tint = Color.White
        )
    }
}

/**
 * Genel araç ikonu (host app için)
 */
@Composable
fun ToolboxIcon(
    modifier: Modifier = Modifier,
    colors: AtolyeColorScheme,
    size: Dp = 48.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .background(colors.primary, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Build,
            contentDescription = null,
            tint = Color.White
        )
    }
}

/**
 * Hexagon outline çizer (kart köşelerinde, butonlarda kullanım için)
 */
@Composable
fun HexagonOutline(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    strokeWidth: Dp = 1.dp,
    size: Dp = 24.dp
) {
    val path = rememberHexagonPath(size)
    val density = LocalDensity.current
    val strokeWidthPx = density.run { strokeWidth.toPx() }
    val resolvedColor = if (color == Color.Unspecified) MaterialTheme.colorScheme.outline else color
    Canvas(
        modifier = modifier.size(size),
        onDraw = {
            drawPath(
                path = path,
                color = resolvedColor,
                style = Stroke(width = strokeWidthPx)
            )
        }
    )
}

/**
 * Hexagon filled shape
 */
@Composable
fun HexagonFilled(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 24.dp
) {
    val path = rememberHexagonPath(size)
    Canvas(
        modifier = modifier.size(size),
        onDraw = {
            drawPath(
                path = path,
                color = color
            )
        }
    )
}