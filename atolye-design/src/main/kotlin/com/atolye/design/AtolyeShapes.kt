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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Atölye 2.0 — Şekiller
 * Kart köşeleri 20-24dp, yumuşak ve minimal.
 */
object AtolyeShapes {
    val CornerRadiusSmall = 10.dp
    val CornerRadiusMedium = 16.dp
    val CornerRadiusLarge = 22.dp
    val CornerRadiusXLarge = 24.dp
    val CornerRadiusFull = 1000.dp

    val AtolyeShapes = Shapes(
        extraSmall = RoundedCornerShape(CornerRadiusSmall),
        small = RoundedCornerShape(CornerRadiusMedium),
        medium = RoundedCornerShape(CornerRadiusLarge),
        large = RoundedCornerShape(CornerRadiusXLarge),
        extraLarge = RoundedCornerShape(CornerRadiusFull)
    )

    val CardShape = RoundedCornerShape(CornerRadiusLarge)
    val ButtonShape = RoundedCornerShape(CornerRadiusMedium)
    val ChipShape = RoundedCornerShape(CornerRadiusFull)
    val DialogShape = RoundedCornerShape(CornerRadiusXLarge)
    val BottomSheetShape = RoundedCornerShape(topStart = CornerRadiusXLarge, topEnd = CornerRadiusXLarge)
    val NotificationShape = RoundedCornerShape(CornerRadiusMedium)
    val OverlayShape = RoundedCornerShape(CornerRadiusMedium)
}

@Composable
fun AtolyeCard(
    modifier: Modifier = Modifier,
    colors: AtolyeColorScheme,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.border(1.dp, colors.outlineVariant, shape = AtolyeShapes.CardShape),
        shape = AtolyeShapes.CardShape,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        content = content
    )
}

@Composable
fun AtolyeElevatedCard(
    modifier: Modifier = Modifier,
    colors: AtolyeColorScheme,
    elevation: Dp = 4.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.border(1.dp, colors.outlineVariant, shape = AtolyeShapes.CardShape),
        shape = AtolyeShapes.CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        content = content
    )
}

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
