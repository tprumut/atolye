package com.atolye.host.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atolye.design.AtolyePluginIcon
import com.atolye.design.AtolyeShapes
import com.atolye.host.R
import com.atolye.host.ui.PluginUiItem
import com.toolbox.contract.PluginCategory
import com.toolbox.contract.PluginMetadata
import com.toolbox.contract.PluginQuickSetting
import com.toolbox.contract.QuickSettingType

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun PluginCard(
    item: PluginUiItem,
    onCardClick: () -> Unit,
    onPowerClick: () -> Unit,
    onHideToggle: () -> Unit,
    onEditCard: () -> Unit,
    onQuickSettingChange: (key: String, value: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val plugin = item.plugin
    val metadata = plugin.metadata
    val accent = pluginAccent(metadata.pluginId, metadata.category)
    val running = item.runtime.running
    var menuExpanded by remember { mutableStateOf(false) }

    val statusLabel = if (running) "Aktif" else "Devre dışı"
    val powerDesc = if (running) stringResource(R.string.power_on_state, metadata.name)
    else stringResource(R.string.power_off_state, metadata.name)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, AtolyeShapes.CardShape, ambientColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            .combinedClickable(onClick = onCardClick, onLongClick = { menuExpanded = true }),
        shape = AtolyeShapes.CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(accent, RoundedCornerShape(topStart = 22.dp, bottomStart = 22.dp))
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AtolyePluginIcon(
                        pluginId = metadata.pluginId,
                        size = 44.dp,
                        speedColor = pluginAccent("com.toolbox.plugin.speedmeter", PluginCategory.NETWORK),
                        keepAwakeColor = pluginAccent("com.toolbox.plugin.keepawake", PluginCategory.DISPLAY),
                        audioColor = pluginAccent("com.toolbox.plugin.sessecici", PluginCategory.MEDIA),
                        sleepColor = pluginAccent("com.toolbox.plugin.uykucu", PluginCategory.TOOLS),
                        fallbackColor = accent
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = metadata.name,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${categoryLabel(metadata.category)} • $statusLabel",
                            fontSize = 12.sp,
                            color = if (running) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (running) FontWeight.Medium else FontWeight.Normal
                        )
                    }
                    Box {
                        PowerButton(running = running, accent = accent, contentDescription = powerDesc, onClick = onPowerClick)
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text(if (item.hidden) stringResource(R.string.card_show) else stringResource(R.string.card_hide)) },
                                onClick = { menuExpanded = false; onHideToggle() }
                            )
                            if (metadata.quickSettings.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.card_edit)) },
                                    onClick = { menuExpanded = false; onEditCard() }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.plugin_settings)) },
                                onClick = { menuExpanded = false; onCardClick() }
                            )
                        }
                    }
                }

                val runtimeLine = runtimeSummary(item)
                Text(
                    text = runtimeLine ?: metadata.description,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = if (runtimeLine != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (runtimeLine != null) 2 else 3,
                    overflow = TextOverflow.Ellipsis
                )

                val qs = visibleQuickSettings(item)
                if (qs.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        qs.forEach { setting ->
                            QuickSettingRow(
                                setting = setting,
                                currentValue = resolveQuickValue(item, setting),
                                accent = accent,
                                onChange = { v -> onQuickSettingChange(setting.key, v) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PowerButton(running: Boolean, accent: Color, contentDescription: String, onClick: () -> Unit) {
    val success = MaterialTheme.colorScheme.tertiary.takeIf { running } ?: Color(0xFF159764)
    val bg by animateColorAsState(if (running) Color(0xFF159764) else MaterialTheme.colorScheme.surface, tween(180), label = "pbg")
    val border by animateColorAsState(if (running) Color(0xFF159764) else MaterialTheme.colorScheme.outline, tween(180), label = "pbd")
    val tint by animateColorAsState(if (running) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, tween(180), label = "pic")
    Box(
        modifier = Modifier
            .size(40.dp)
            .semantics {
                role = Role.Button
                this.contentDescription = contentDescription
                stateDescription = contentDescription
            }
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, border, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.PowerSettingsNew, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickSettingRow(
    setting: PluginQuickSetting,
    currentValue: String,
    accent: Color,
    onChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = setting.title,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        if (setting.type == QuickSettingType.TOGGLE) {
            val checked = currentValue.equals("true", ignoreCase = true) || currentValue == "1"
            Switch(
                checked = checked,
                onCheckedChange = { onChange(if (it) "true" else "false") },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = accent,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    uncheckedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                setting.options.forEach { opt ->
                    val selected = opt.value == currentValue
                    FilterChip(
                        selected = selected,
                        onClick = { onChange(opt.value) },
                        label = {
                            Text(
                                opt.label.ifBlank { opt.value },
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                maxLines = 1
                            )
                        },
                        shape = RoundedCornerShape(20.dp),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = if (selected) accent else MaterialTheme.colorScheme.outline,
                            selectedBorderColor = accent,
                            borderWidth = 1.dp,
                            selectedBorderWidth = 1.dp
                        ),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.onSurface,
                            selectedContainerColor = accent.copy(alpha = 0.16f),
                            selectedLabelColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun AddPluginCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val stroke = MaterialTheme.colorScheme.outline
    val bg = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp)
            .clip(AtolyeShapes.CardShape)
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawRoundRect(
                color = stroke,
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                ),
                cornerRadius = CornerRadius(22.dp.toPx(), 22.dp.toPx())
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("+", fontSize = 22.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
            }
            Column {
                Text(stringResource(R.string.add_plugin), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(stringResource(R.string.add_plugin_desc_short), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

fun visibleQuickSettings(item: PluginUiItem): List<PluginQuickSetting> {
    val all = item.plugin.metadata.quickSettings
    if (all.isEmpty()) return emptyList()
    val keys = item.visibleQuickSettingKeys
    val selected = if (keys.isEmpty()) all.take(3) else keys.mapNotNull { k -> all.find { it.key == k } }.take(3)
    return if (selected.isEmpty()) all.take(3) else selected
}

fun resolveQuickValue(item: PluginUiItem, setting: PluginQuickSetting): String {
    return item.runtime.quickSettingValues[setting.key] ?: setting.defaultValue.ifBlank {
        if (setting.type == QuickSettingType.TOGGLE) "false" else setting.options.firstOrNull()?.value.orEmpty()
    }
}

fun runtimeSummary(item: PluginUiItem): String? {
    val r = item.runtime
    if (!r.running && r.statusText.isNullOrBlank()) return null
    val parts = mutableListOf<String>()
    if (r.running && r.remainingMs > 0) {
        val totalSec = (r.remainingMs / 1000).coerceAtLeast(0)
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        val t = if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
        parts.add("Kalan $t")
    }
    if (!r.statusText.isNullOrBlank()) parts.add(r.statusText!!)
    if (parts.isEmpty() && r.running) parts.add("Çalışıyor")
    return parts.joinToString(" • ").ifBlank { null }
}

fun pluginAccent(pluginId: String, category: PluginCategory): Color {
    return when (pluginId) {
        "com.toolbox.plugin.speedmeter" -> Color(0xFF00A8B5)
        "com.toolbox.plugin.keepawake" -> Color(0xFFF39220)
        "com.toolbox.plugin.sessecici" -> Color(0xFF5B5FAE)
        "com.toolbox.plugin.uykucu" -> Color(0xFF294D70)
        else -> when (category) {
            PluginCategory.NETWORK -> Color(0xFF00A8B5)
            PluginCategory.DISPLAY -> Color(0xFFF39220)
            PluginCategory.MEDIA -> Color(0xFF5B5FAE)
            PluginCategory.TOOLS, PluginCategory.PRODUCTIVITY -> Color(0xFF203B56)
            PluginCategory.SYSTEM -> Color(0xFF294D70)
            PluginCategory.DEVELOPER -> Color(0xFF159764)
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
