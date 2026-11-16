package com.atolye.host.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atolye.design.AtolyeColorScheme
import com.atolye.design.AtolyeShapes
import com.atolye.design.KagitColorScheme
import com.atolye.host.R
import com.toolbox.contract.InstalledPlugin
import com.toolbox.contract.PluginCategory
import com.toolbox.contract.PluginMetadata

/**
 * Eklenti kartı: ikon, ad, kısa açıklama ve etkinleştirme anahtarı.
 */
@Composable
fun PluginCard(
    plugin: InstalledPlugin,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    colors: AtolyeColorScheme = KagitColorScheme()
) {
    val metadata = plugin.metadata
    val accent = pluginColor(metadata, colors)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = AtolyeShapes.CardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (plugin.isEnabled) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
            contentColor = if (plugin.isEnabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (plugin.isEnabled) {
                accent.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PluginIcon(metadata = metadata, colors = colors, size = 44.dp)

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = metadata.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = categoryLabel(metadata.category),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = metadata.description,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                minLines = 2,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (plugin.isEnabled) {
                        stringResource(R.string.plugin_enabled)
                    } else {
                        stringResource(R.string.plugin_disabled)
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (plugin.isEnabled) accent else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Switch(
                    checked = plugin.isEnabled,
                    onCheckedChange = onToggle
                )
            }
        }
    }
}

/**
 * "Eklenti ekle" kartı.
 */
@Composable
fun AddPluginCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = AtolyeShapes.CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = stringResource(R.string.add_plugin),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(R.string.add_plugin_desc_short),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Eklenti ikonu: eklenti kimliğine/kategorisine göre bir simge ve renk.
 */
@Composable
fun PluginIcon(
    metadata: PluginMetadata,
    colors: AtolyeColorScheme,
    size: Dp = 56.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .background(pluginColor(metadata, colors), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = pluginIcon(metadata),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(size * 0.55f)
        )
    }
}

fun pluginColor(metadata: PluginMetadata, colors: AtolyeColorScheme): Color {
    metadata.accentColor
        ?.let { value -> parseColor(value) }
        ?.let { return it }

    return when (metadata.pluginId) {
        "com.toolbox.plugin.speedmeter" -> colors.pluginSpeedMeter
        "com.toolbox.plugin.keepawake" -> colors.pluginKeepAwake
        else -> when (metadata.category) {
            PluginCategory.NETWORK -> colors.pluginSpeedMeter
            PluginCategory.DISPLAY -> colors.pluginKeepAwake
            PluginCategory.SYSTEM -> colors.secondary
            PluginCategory.DEVELOPER -> colors.tertiary
            PluginCategory.MEDIA -> colors.error
            PluginCategory.PRODUCTIVITY -> colors.primary
            PluginCategory.TOOLS -> colors.primary
        }
    }
}

private fun parseColor(value: String): Color? {
    val hex = value.removePrefix("#")
    if (hex.length != 6) return null
    return runCatching { Color(0xFF000000L or hex.toLong(16)) }.getOrNull()
}

fun pluginIcon(metadata: PluginMetadata): ImageVector {
    return when (metadata.pluginId) {
        "com.toolbox.plugin.speedmeter" -> Icons.Default.Speed
        "com.toolbox.plugin.keepawake" -> Icons.Default.LightMode
        else -> when (metadata.category) {
            PluginCategory.NETWORK -> Icons.Default.Wifi
            PluginCategory.DISPLAY -> Icons.Default.Visibility
            PluginCategory.SYSTEM -> Icons.Default.Settings
            PluginCategory.DEVELOPER -> Icons.Default.Code
            PluginCategory.MEDIA -> Icons.Default.PlayArrow
            PluginCategory.PRODUCTIVITY -> Icons.Default.CheckCircle
            PluginCategory.TOOLS -> Icons.Default.Build
        }
    }
}

fun categoryLabel(category: PluginCategory): String = when (category) {
    PluginCategory.TOOLS -> "Araçlar"
    PluginCategory.NETWORK -> "Ağ"
    PluginCategory.DISPLAY -> "Ekran"
    PluginCategory.SYSTEM -> "Sistem"
    PluginCategory.DEVELOPER -> "Geliştirici"
    PluginCategory.MEDIA -> "Medya"
    PluginCategory.PRODUCTIVITY -> "Verimlilik"
}
