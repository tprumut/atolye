package com.toolbox.plugin.keepawake

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.atolye.design.AtolyeShapes
import com.atolye.design.AtolyeTheme
import com.toolbox.plugin.PluginSettingsManager

/** Keep Awake ayar anahtarları ve mod sabitleri. */
object KeepAwakeKeys {
    const val PLUGIN_ID = "com.toolbox.plugin.keepawake"

    const val MODE = "mode"
    const val TIMER_DURATION = "timer_duration"
    const val SHOW_NOTIFICATION = "show_notification"
    const val QUICK_SETTINGS = "quick_settings"
    const val SELECTED_APPS = "selected_apps"
    const val SERVICE_RUNNING = "service_running"

    const val MODE_MANUAL = 0
    const val MODE_TIMER = 1
    const val MODE_INFINITE = 2
    const val MODE_APP_BASED = 3
}

object KeepAwakeFormat {
    fun duration(millis: Long): String {
        val totalSeconds = (millis / 1000).coerceAtLeast(0)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
    }
}

data class KeepAwakePermissionState(
    val hasOverlay: Boolean,
    val hasUsageStats: Boolean
)

/** Ana ekran: tek dokunuşla aç/kapa, mod seçimi ve uygulama listesi özeti. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeepAwakeScreen(
    settings: PluginSettingsManager,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val pluginId = KeepAwakeKeys.PLUGIN_ID

    val isRunning by settings
        .observeBoolean(pluginId, KeepAwakeKeys.SERVICE_RUNNING, false)
        .collectAsState()
    val mode by settings.observeInt(pluginId, KeepAwakeKeys.MODE, KeepAwakeKeys.MODE_MANUAL)
        .collectAsState()
    val timerDuration by settings
        .observeInt(pluginId, KeepAwakeKeys.TIMER_DURATION, 15)
        .collectAsState()
    val showNotification by settings
        .observeBoolean(pluginId, KeepAwakeKeys.SHOW_NOTIFICATION, true)
        .collectAsState()
    val quickSettings by settings
        .observeBoolean(pluginId, KeepAwakeKeys.QUICK_SETTINGS, true)
        .collectAsState()
    val selectedApps by settings
        .observeStringSet(pluginId, KeepAwakeKeys.SELECTED_APPS, emptySet())
        .collectAsState()

    val permissions = rememberKeepAwakePermissions()

    RequestNotificationPermission()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.plugin_keepawake_name),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.plugin_keepawake_short),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.keepawake_settings_title)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                KeepAwakeControlCard(
                    isRunning = isRunning,
                    mode = mode,
                    hasOverlayPermission = permissions.hasOverlay,
                    onToggle = {
                        when {
                            !permissions.hasOverlay ->
                                KeepAwakePermissions.openOverlaySettings(context)

                            !isRunning && mode == KeepAwakeKeys.MODE_APP_BASED &&
                                !permissions.hasUsageStats ->
                                KeepAwakePermissions.openUsageStatsSettings(context)

                            !isRunning && mode == KeepAwakeKeys.MODE_APP_BASED &&
                                selectedApps.isEmpty() -> onOpenSettings()

                            isRunning -> KeepAwakeOverlayService.stop(context)

                            else -> KeepAwakeOverlayService.start(
                                context,
                                mode,
                                timerDuration
                            )
                        }
                    }
                )
            }

            item {
                KeepAwakeModeSection(
                    mode = mode,
                    timerDuration = timerDuration,
                    hasUsageStats = permissions.hasUsageStats,
                    onModeChange = {
                        if (it == KeepAwakeKeys.MODE_APP_BASED && !permissions.hasUsageStats) {
                            KeepAwakePermissions.openUsageStatsSettings(context)
                        } else {
                            settings.putInt(pluginId, KeepAwakeKeys.MODE, it)
                        }
                    },
                    onTimerChange = {
                        settings.putInt(pluginId, KeepAwakeKeys.TIMER_DURATION, it)
                    }
                )
            }

            if (mode == KeepAwakeKeys.MODE_APP_BASED) {
                item {
                    AppSelectionSummaryCard(
                        selectedCount = selectedApps.size,
                        hasUsageStats = permissions.hasUsageStats,
                        onOpenSettings = onOpenSettings,
                        onRequestUsage = {
                            KeepAwakePermissions.openUsageStatsSettings(context)
                        }
                    )
                }
            }

            item {
                KeepAwakeSettingsCard(
                    showNotification = showNotification,
                    onShowNotificationChange = {
                        settings.putBoolean(
                            pluginId,
                            KeepAwakeKeys.SHOW_NOTIFICATION,
                            it
                        )
                    },
                    quickSettings = quickSettings,
                    onQuickSettingsChange = {
                        settings.putBoolean(pluginId, KeepAwakeKeys.QUICK_SETTINGS, it)
                    }
                )
            }

            item {
                KeepAwakePermissionsCard(
                    permissions = permissions,
                    onRequestOverlay = {
                        KeepAwakePermissions.openOverlaySettings(context)
                    },
                    onRequestUsage = {
                        KeepAwakePermissions.openUsageStatsSettings(context)
                    }
                )
            }

            item { KeepAwakeInfoCard() }
        }
    }
}

/** Ayarlar ekranı: mod, uygulama listesi, bildirim ve izinler. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeepAwakeSettingsScreen(
    settings: PluginSettingsManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val pluginId = KeepAwakeKeys.PLUGIN_ID

    val mode by settings.observeInt(pluginId, KeepAwakeKeys.MODE, KeepAwakeKeys.MODE_MANUAL)
        .collectAsState()
    val timerDuration by settings
        .observeInt(pluginId, KeepAwakeKeys.TIMER_DURATION, 15)
        .collectAsState()
    val showNotification by settings
        .observeBoolean(pluginId, KeepAwakeKeys.SHOW_NOTIFICATION, true)
        .collectAsState()
    val quickSettings by settings
        .observeBoolean(pluginId, KeepAwakeKeys.QUICK_SETTINGS, true)
        .collectAsState()
    val selectedApps by settings
        .observeStringSet(pluginId, KeepAwakeKeys.SELECTED_APPS, emptySet())
        .collectAsState()

    val permissions = rememberKeepAwakePermissions()
    val apps = remember {
        runCatching { loadLaunchableApps(context) }.getOrDefault(emptyList())
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.keepawake_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                KeepAwakeModeSection(
                    mode = mode,
                    timerDuration = timerDuration,
                    hasUsageStats = permissions.hasUsageStats,
                    onModeChange = {
                        if (it == KeepAwakeKeys.MODE_APP_BASED && !permissions.hasUsageStats) {
                            KeepAwakePermissions.openUsageStatsSettings(context)
                        } else {
                            settings.putInt(pluginId, KeepAwakeKeys.MODE, it)
                        }
                    },
                    onTimerChange = {
                        settings.putInt(pluginId, KeepAwakeKeys.TIMER_DURATION, it)
                    }
                )
            }

            item {
                KeepAwakeSettingsCard(
                    showNotification = showNotification,
                    onShowNotificationChange = {
                        settings.putBoolean(
                            pluginId,
                            KeepAwakeKeys.SHOW_NOTIFICATION,
                            it
                        )
                    },
                    quickSettings = quickSettings,
                    onQuickSettingsChange = {
                        settings.putBoolean(pluginId, KeepAwakeKeys.QUICK_SETTINGS, it)
                    }
                )
            }

            item {
                KeepAwakePermissionsCard(
                    permissions = permissions,
                    onRequestOverlay = {
                        KeepAwakePermissions.openOverlaySettings(context)
                    },
                    onRequestUsage = {
                        KeepAwakePermissions.openUsageStatsSettings(context)
                    }
                )
            }

            if (mode == KeepAwakeKeys.MODE_APP_BASED) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = AtolyeShapes.CardShape,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.keepawake_app_list),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.keepawake_app_list_desc),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            HorizontalDivider()

                            if (!permissions.hasUsageStats) {
                                Text(
                                    text = stringResource(R.string.usage_stats_permission_needed),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 420.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                items(apps, key = { it.packageName }) { app ->
                                    AppSelectionRow(
                                        app = app,
                                        isSelected = selectedApps.contains(app.packageName),
                                        onClick = {
                                            val updated = selectedApps.toMutableSet()
                                            if (!updated.add(app.packageName)) {
                                                updated.remove(app.packageName)
                                            }
                                            settings.putStringSet(
                                                pluginId,
                                                KeepAwakeKeys.SELECTED_APPS,
                                                updated
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { KeepAwakeInfoCard() }
        }
    }
}

@Composable
fun KeepAwakeControlCard(
    isRunning: Boolean,
    mode: Int,
    hasOverlayPermission: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AtolyeShapes.CardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (isRunning) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(72.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(
                        id = if (isRunning) R.drawable.ic_screen_on else R.drawable.ic_screen_off
                    ),
                    contentDescription = null,
                    tint = if (isRunning) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(56.dp)
                )
            }

            Text(
                text = if (isRunning) {
                    stringResource(R.string.screen_kept_awake)
                } else {
                    stringResource(R.string.screen_normal)
                },
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = if (isRunning) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )

            Text(
                text = modeDescription(mode),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onToggle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = AtolyeShapes.ButtonShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            ) {
                Text(
                    text = if (isRunning) {
                        stringResource(R.string.stop_keeping_awake)
                    } else {
                        stringResource(R.string.keep_awake_now)
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            if (!hasOverlayPermission) {
                Text(
                    text = stringResource(R.string.overlay_permission_required),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun KeepAwakeModeSection(
    mode: Int,
    timerDuration: Int,
    hasUsageStats: Boolean,
    onModeChange: (Int) -> Unit,
    onTimerChange: (Int) -> Unit
) {
    SettingsSection(title = stringResource(R.string.keepawake_mode)) {
        ModeOptionRow(
            title = stringResource(R.string.keepawake_mode_manual),
            description = stringResource(R.string.keepawake_mode_manual_desc),
            icon = Icons.Default.PowerSettingsNew,
            selected = mode == KeepAwakeKeys.MODE_MANUAL,
            onClick = { onModeChange(KeepAwakeKeys.MODE_MANUAL) }
        )
        HorizontalDivider()
        ModeOptionRow(
            title = stringResource(R.string.keepawake_mode_timer),
            description = stringResource(R.string.keepawake_mode_timer_desc),
            icon = Icons.Default.Timer,
            selected = mode == KeepAwakeKeys.MODE_TIMER,
            onClick = { onModeChange(KeepAwakeKeys.MODE_TIMER) }
        )
        if (mode == KeepAwakeKeys.MODE_TIMER) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(15, 30, 60, 120).forEach { minutes ->
                    FilterChip(
                        selected = timerDuration == minutes,
                        onClick = { onTimerChange(minutes) },
                        label = {
                            Text(
                                text = stringResource(
                                    R.string.keepawake_minutes_format,
                                    minutes
                                ),
                                fontSize = 11.sp
                            )
                        }
                    )
                }
            }
        }
        HorizontalDivider()
        ModeOptionRow(
            title = stringResource(R.string.keepawake_mode_infinite),
            description = stringResource(R.string.keepawake_mode_infinite_desc),
            icon = Icons.Default.TimerOff,
            selected = mode == KeepAwakeKeys.MODE_INFINITE,
            onClick = { onModeChange(KeepAwakeKeys.MODE_INFINITE) }
        )
        HorizontalDivider()
        ModeOptionRow(
            title = stringResource(R.string.keepawake_mode_app_based),
            description = stringResource(R.string.keepawake_mode_app_based_desc),
            icon = Icons.Default.Apps,
            selected = mode == KeepAwakeKeys.MODE_APP_BASED,
            onClick = { onModeChange(KeepAwakeKeys.MODE_APP_BASED) },
            warning = if (!hasUsageStats) {
                stringResource(R.string.usage_stats_permission_needed)
            } else {
                null
            }
        )
    }
}

@Composable
private fun ModeOptionRow(
    title: String,
    description: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    warning: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 15.sp)
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        warning?.let {
            Text(
                text = it,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 32.dp)
            )
        }
    }
}

@Composable
private fun AppSelectionSummaryCard(
    selectedCount: Int,
    hasUsageStats: Boolean,
    onOpenSettings: () -> Unit,
    onRequestUsage: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AtolyeShapes.CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.keepawake_app_list),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.keepawake_selected_apps_count, selectedCount),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!hasUsageStats) {
                Text(
                    text = stringResource(R.string.usage_stats_permission_needed),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
            TextButton(
                onClick = if (hasUsageStats) onOpenSettings else onRequestUsage
            ) {
                Text(
                    text = if (hasUsageStats) {
                        stringResource(R.string.keepawake_select_apps)
                    } else {
                        stringResource(R.string.open_usage_stats_settings)
                    }
                )
            }
        }
    }
}

@Composable
private fun KeepAwakeSettingsCard(
    showNotification: Boolean,
    onShowNotificationChange: (Boolean) -> Unit,
    quickSettings: Boolean,
    onQuickSettingsChange: (Boolean) -> Unit
) {
    SettingsSection(title = stringResource(R.string.settings)) {
        SettingRow(
            title = stringResource(R.string.keepawake_show_notification),
            description = stringResource(R.string.keepawake_show_notification_desc),
            trailing = {
                Switch(
                    checked = showNotification,
                    onCheckedChange = onShowNotificationChange
                )
            }
        )
        HorizontalDivider()
        SettingRow(
            title = stringResource(R.string.keepawake_quick_settings),
            description = stringResource(R.string.keepawake_quick_settings_desc),
            trailing = {
                Switch(
                    checked = quickSettings,
                    onCheckedChange = onQuickSettingsChange
                )
            }
        )
    }
}

@Composable
private fun KeepAwakePermissionsCard(
    permissions: KeepAwakePermissionState,
    onRequestOverlay: () -> Unit,
    onRequestUsage: () -> Unit
) {
    SettingsSection(title = stringResource(R.string.keepawake_permissions_title)) {
        PermissionRow(
            title = stringResource(R.string.overlay_permission_required),
            description = stringResource(R.string.permission_system_alert_window_desc),
            granted = permissions.hasOverlay,
            onRequest = onRequestOverlay
        )
        HorizontalDivider()
        PermissionRow(
            title = stringResource(R.string.permission_usage_stats_title),
            description = stringResource(R.string.permission_usage_stats_desc),
            granted = permissions.hasUsageStats,
            onRequest = onRequestUsage
        )
    }
}

@Composable
private fun PermissionRow(
    title: String,
    description: String,
    granted: Boolean,
    onRequest: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (granted) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = if (granted) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            },
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 14.sp)
            Text(
                text = description,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (!granted) {
            TextButton(onClick = onRequest) {
                Text(stringResource(R.string.open_overlay_settings))
            }
        }
    }
}

@Composable
private fun KeepAwakeInfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AtolyeShapes.CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.about_this_plugin),
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = stringResource(R.string.keepawake_info_text),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            HorizontalDivider()
            content()
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    description: String,
    trailing: @Composable () -> Unit = {},
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 15.sp)
            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        trailing()
    }
}

@Composable
private fun AppSelectionRow(
    app: LaunchableApp,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Apps,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = app.label, fontSize = 14.sp)
            Text(
                text = app.packageName,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onClick() }
        )
    }
}

@Composable
private fun modeDescription(mode: Int): String = when (mode) {
    KeepAwakeKeys.MODE_TIMER -> stringResource(R.string.keepawake_mode_timer_desc)
    KeepAwakeKeys.MODE_INFINITE -> stringResource(R.string.keepawake_mode_infinite_desc)
    KeepAwakeKeys.MODE_APP_BASED -> stringResource(R.string.keepawake_mode_app_based_desc)
    else -> stringResource(R.string.keepawake_mode_manual_desc)
}

@Composable
private fun rememberKeepAwakePermissions(): KeepAwakePermissionState {
    val context = LocalContext.current
    var state by remember {
        mutableStateOf(
            KeepAwakePermissionState(
                hasOverlay = KeepAwakePermissions.canDrawOverlays(context),
                hasUsageStats = KeepAwakePermissions.hasUsageStats(context)
            )
        )
    }

    OnResume {
        state = KeepAwakePermissionState(
            hasOverlay = KeepAwakePermissions.canDrawOverlays(context),
            hasUsageStats = KeepAwakePermissions.hasUsageStats(context)
        )
    }

    return state
}

@Composable
private fun OnResume(action: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentAction by rememberUpdatedState(action)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) currentAction()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

/** Android 13+ için bildirim izni ister. */
@Composable
fun RequestNotificationPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {}
    )

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

/** Eklenti içeriğini Atölye teması ile sarar. */
@Composable
fun KeepAwakeTheme(content: @Composable () -> Unit) {
    AtolyeTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            content = content
        )
    }
}

fun openKeepAwakeSettings(context: Context) {
    context.startActivity(Intent(context, KeepAwakeSettingsActivity::class.java))
}
