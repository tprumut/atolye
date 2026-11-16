package com.atolye.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp

/**
 * Atölye Tasarım Sistemi - Şekiller
 * Köşe yuvarlaklığı: 16dp (standart)
 * Kartlarda gölge yerine ince çerçeve kullanılır
 */
object AtolyeShapes {

    // Temel köşe yarıçapları
    val CornerRadiusSmall = 8.dp
    val CornerRadiusMedium = 12.dp
    val CornerRadiusLarge = 16.dp  // Standart kart köşesi
    val CornerRadiusXLarge = 24.dp
    val CornerRadiusFull = 1000.dp // Large enough to be fully rounded

    // Material3 Shapes mapping
    val AtolyeShapes = Shapes(
        extraSmall = RoundedCornerShape(CornerRadiusSmall),
        small = RoundedCornerShape(CornerRadiusMedium),
        medium = RoundedCornerShape(CornerRadiusLarge),
        large = RoundedCornerShape(CornerRadiusXLarge),
        extraLarge = RoundedCornerShape(CornerRadiusFull)
    )

    // Özel şekiller
    val CardShape = RoundedCornerShape(CornerRadiusLarge)
    val ButtonShape = RoundedCornerShape(CornerRadiusMedium)
    val ChipShape = RoundedCornerShape(CornerRadiusFull)
    val DialogShape = RoundedCornerShape(CornerRadiusXLarge)
    val BottomSheetShape = RoundedCornerShape(topStart = CornerRadiusXLarge, topEnd = CornerRadiusXLarge)
    val NotificationShape = RoundedCornerShape(CornerRadiusMedium)
    val OverlayShape = RoundedCornerShape(CornerRadiusSmall)

    // Altıgen ikon şekli (uyarlanabilir ikon için) - PathOutline için
    val HexagonPath = Path().apply {
        fillType = PathFillType.EvenOdd
        val radius = 48f
        val centerX = 48f
        val centerY = 48f
        val innerRadius = 18f // Orta yuva yarıçapı

        // Dış altıgen
        for (i in 0..5) {
            val angle = (i * 60 - 30) * Math.PI / 180
            val x = centerX + radius * Math.cos(angle).toFloat()
            val y = centerY + radius * Math.sin(angle).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()

        // İç altıgen (yuva) - ters yönde çizdirerek "delik" efekti
        for (i in 5 downTo 0) {
            val angle = (i * 60 - 30) * Math.PI / 180
            val x = centerX + innerRadius * Math.cos(angle).toFloat()
            val y = centerY + innerRadius * Math.sin(angle).toFloat()
            lineTo(x, y)
        }
        close()
    }
}

/**
 * Kart çerçeve çizimi için helper
 * Gölge yerine ince çerçeve (1dp) kullanılır
 */
@Composable
fun AtolyeCard(
    modifier: Modifier = Modifier,
    colors: AtolyeColorScheme,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .border(1.dp, colors.outlineVariant, shape = AtolyeShapes.CardShape),
        shape = AtolyeShapes.CardShape,
        colors = CardDefaults.cardColors(
            containerColor = colors.surface
        ),
        content = content
    )
}

/**
 * Yükseltme gölgeli kart (gerektiğinde)
 */
@Composable
fun AtolyeElevatedCard(
    modifier: Modifier = Modifier,
    colors: AtolyeColorScheme,
    elevation: Dp = 4.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .border(1.dp, colors.outlineVariant, shape = AtolyeShapes.CardShape),
        shape = AtolyeShapes.CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        colors = CardDefaults.cardColors(
            containerColor = colors.surface
        ),
        content = content
    )
}

/**
 * İçerik kartı - alt bölümler için
 */
@Composable
fun AtolyeContentCard(
    modifier: Modifier = Modifier,
    colors: AtolyeColorScheme,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surfaceContainer, shape = AtolyeShapes.CardShape)
            .border(1.dp, colors.outlineVariant, shape = AtolyeShapes.CardShape),
        contentAlignment = Alignment.Center,
        content = content
    )
}