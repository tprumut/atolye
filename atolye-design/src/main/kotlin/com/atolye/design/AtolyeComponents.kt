package com.atolye.design

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import kotlin.math.cos
import kotlin.math.sin

/**
 * Atölye 2.0 — Ortak UI Bileşenleri
 * Referans tasarım (atolye-referans.png) ile uyumlu, tema bilinçli bileşenler.
 * Plugin-spesifik bileşenler toolbox-host modülünde kalır.
 */

// ============================================================================
// KART
// ============================================================================

/**
 * Atölye standart kartı: 24dp köşe, sol dikey şerit, krem/beyaz yüzey, yumuşak gölge.
 * [accent] sol şerit ve vurgu rengi için kullanılır.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AtolyeCard(
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    elevation: androidx.compose.ui.unit.Dp = 2.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val containerColor = MaterialTheme.colorScheme.surface
    val shadowColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    val borderColor = MaterialTheme.colorScheme.outline

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation, AtolyeShapes.CardShape, ambientColor = shadowColor)
            .combinedClickable(
                onClick = onClick ?: {},
                onLongClick = onLongClick ?: {}
            ),
        shape = AtolyeShapes.CardShape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            // Sol dikey accent şeriti (5dp, köşeler yuvarlak)
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(accent, RoundedCornerShape(topStart = 22.dp, bottomStart = 22.dp))
            )
            Column(
                modifier = Modifier.weight(1f).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                content()
            }
        }
    }
}

// ============================================================================
// YUVARLAK İKON BUTONU
// ============================================================================

/**
 * 44dp yuvarlak ikon butonu: surface zemin, outline border, ripple efekti.
 * Header'daki Palet, Yenile, Eklenti Ekle butonları için standart.
 */
@Composable
fun AtolyeIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

// ============================================================================
// ÇIP GRUBU (FilterChip stilli)
// ============================================================================

/**
 * Seçili: koyu lacivert (primary) dolgu + beyaz metin/ikon
 * Seçilmez: krem (surfaceVariant) zemin + ince outline border
 */
@Composable
fun AtolyeFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    leadingIcon: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) accent else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(150)
    )
    val labelColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val borderColor by animateColorAsState(
        targetValue = if (selected) accent else MaterialTheme.colorScheme.outline,
        animationSpec = tween(150)
    )

    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        leadingIcon = leadingIcon,
        label = {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = labelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        shape = RoundedCornerShape(20.dp),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = borderColor,
            selectedBorderColor = accent,
            borderWidth = 1.dp,
            selectedBorderWidth = 1.dp
        ),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = containerColor,
            labelColor = labelColor,
            selectedContainerColor = accent,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

// ============================================================================
// ANAHTAR (Switch)
// ============================================================================

/**
 * Açık: koyu lacivert (primary) zemin + beyaz yuvarlak
 * Kapalı: gri (surfaceVariant) zemin + gri yuvarlak
 * Standart Material3 SwitchDefaults ile uyumlu.
 */
@Composable
fun AtolyeSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = accent,
            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            uncheckedBorderColor = MaterialTheme.colorScheme.outline
        )
    )
}

// ============================================================================
// BÖLÜM BAŞLIĞI + AYIRICI
// ============================================================================

/**
 * "Eklentiler" başlığı + sağda sayaç + ince ayırıcı çizgi.
 * Referans tasarımdaki bölüm başlığı stili.
 */
@Composable
fun AtolyeSectionHeader(
    title: String,
    count: Int? = null,
    countLabel: Int? = null, // string resource id
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (count != null) {
                Text(
                    text = countLabel?.let { stringResource(it, count) } ?: count.toString(),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 22.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        )
        Spacer(Modifier.height(6.dp))
    }
}

// ============================================================================
// ALTIGEN ARKA PLAN DEKORASYONU
// ============================================================================

/**
 * Çok soluk altıgen ızgara deseni (zemin süsü).
 * Opacity 0.08 - içerik okunabilirliğini bozmaz.
 */
@Composable
fun AtolyeHexBackground(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
) {
    Canvas(modifier = modifier) {
        val r = 34.dp.toPx()
        val w = r * 1.732f
        val hStep = r * 1.5f
        var row = 0
        var y = -r
        val width = size.width
        val height = size.height
        while (y < height + r) {
            val xOffset = if (row % 2 == 0) 0f else w / 2f
            var x = -w + xOffset
            while (x < width + w) {
                val path = Path()
                for (i in 0..5) {
                    val a = Math.toRadians((i * 60 - 30).toDouble())
                    val px = x + (r * cos(a)).toFloat()
                    val py = y + (r * sin(a)).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                drawPath(path, color = tint, style = Stroke(width = 1.dp.toPx()))
                x += w
            }
            y += hStep
            row++
        }
    }
}

// ============================================================================
// GÜÇ BUTONU (Power Button)
// ============================================================================

/**
 * Kart sağ üstündeki yuvarlak güç butonu.
 * Açık: yeşil (success) zemin + beyaz ikon
 * Kapalı: surface zemin + outline border + ikincil renk ikon
 */
@Composable
fun AtolyePowerButton(
    running: Boolean,
    accent: Color,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val successColor = MaterialTheme.colorScheme.tertiary.takeIf { running } ?: Color(0xFF159764)
    val bg by animateColorAsState(if (running) successColor else MaterialTheme.colorScheme.surface, tween(180), label = "pbg")
    val borderColor by animateColorAsState(if (running) successColor else MaterialTheme.colorScheme.outline, tween(180), label = "pbd")
    val tint by animateColorAsState(if (running) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, tween(180), label = "pic")

    Box(
        modifier = modifier
            .size(40.dp)
            .semantics {
                this.role = Role.Button
                this.contentDescription = contentDescription
            }
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, borderColor, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.PowerSettingsNew,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
    }
}

// ============================================================================
// DURUM NOKTASI + KATEGORİ ETİKETİ
// ============================================================================

/**
 * Yeşil/nokta + kategori metni (Ağ, Medya, Ekran, Araçlar)
 * Kart başlığının alt kısmında kullanılır.
 */
@Composable
fun AtolyeStatusChip(
    running: Boolean,
    categoryLabel: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (running) Color(0xFF159764) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
        )
        Text(
            text = categoryLabel,
            fontSize = 12.sp,
            color = if (running) Color(0xFF159764) else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (running) FontWeight.Medium else FontWeight.Normal
        )
    }
}