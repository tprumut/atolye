@file:OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)

package com.atolye.host.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atolye.design.AtolyeAppLogo
import com.atolye.design.AtolyeCard
import com.atolye.design.AtolyeFilterChip
import com.atolye.design.AtolyePluginIcon
import com.atolye.design.AtolyePowerButton
import com.atolye.design.AtolyeShapes
import com.atolye.design.AtolyeStatusChip
import com.atolye.design.AtolyeSwitch
import com.atolye.host.R
import com.atolye.host.ui.PluginUiItem
import com.toolbox.contract.PluginCategory
import com.toolbox.contract.PluginMetadata
import com.toolbox.contract.PluginQuickSetting
import com.toolbox.contract.QuickSettingType

@Composable
fun PluginCard(
    item: PluginUiItem,
    onCardClick: () -> Unit,
    onPowerClick: () -> Unit,
    onHideToggle: () -> Unit,
    onEditCard: () -> Unit,
    onQuickSettingChange: (key: String, value: String) -> Unit,
    onDragStart: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val plugin = item.plugin
    val metadata = plugin.metadata
    val accent = pluginAccent(metadata.pluginId, metadata.category)
    val running = item.runtime.running
    var menuExpanded by remember { mutableStateOf(false) }

    val powerDesc = if (running) stringResource(R.string.power_on_state, metadata.name)
    else stringResource(R.string.power_off_state, metadata.name)

    val dragState = remember { mutableStateOf(false) }
    val dragStart = onDragStart

    AtolyeCard(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onCardClick,
                onLongClick = { menuExpanded = true }
            )
            .pointerInput(Unit) {
                // Manual drag detection for reorder
                var dragStarted = false
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val changes = event.changes
                        for (change in changes) {
                            when (change.type) {
                                androidx.compose.ui.input.pointer.PointerType.Mouse -> {}
                                else -> {
                                    if (change.pressed && !dragStarted) {
                                        dragStarted = true
                                        dragState.value = true
                                        dragStart?.invoke()
                                    } else if (!change.pressed && dragStarted) {
                                        dragStarted = false
                                        dragState.value = false
                                    }
                                }
                            }
                        }
                        if (event.changes.any { it.type == androidx.compose.ui.input.pointer.PointerType.Mouse }) {}
                    }
                }
            },
        accent = accent,
        onClick = onCardClick,
        onLongClick = { menuExpanded = true }
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
                AtolyeStatusChip(
                    running = running,
                    categoryLabel = categoryLabel(metadata.category)
                )
            }
            Box(
                modifier = Modifier
                    .width(44.dp) // Sabit genişlik: fillMaxWidth() burada metin kolonunu sıfırlıyor
            ) {
                AtolyePowerButton(
                    running = running,
                    accent = accent,
                    contentDescription = powerDesc,
                    onClick = onPowerClick
                )
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
                    AtolyeQuickSettingRow(
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AtolyeQuickSettingRow(
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
            AtolyeSwitch(
                checked = checked,
                onCheckedChange = { onChange(if (it) "true" else "false") },
                accent = accent
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                setting.options.forEach { opt ->
                    val selected = opt.value == currentValue
                    AtolyeFilterChip(
                        selected = selected,
                        onClick = { onChange(opt.value) },
                        label = opt.label.ifBlank { opt.value },
                        accent = accent
                    )
                }
            }
        }
    }
}

// AddPluginCard REMOVED (A1 - moved to header as icon button)

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