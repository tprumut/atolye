@file:OptIn(ExperimentalLayoutApi::class)

package com.toolbox.plugin.autoscroll

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Accessible
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.atolye.design.AtolyeShapes
import com.atolye.design.AtolyeTheme
import com.atolye.design.AtolyeThemeStore
import com.toolbox.plugin.PluginSettingsManager

@Composable
fun AutoScrollTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val themeType = remember(context) { AtolyeThemeStore.read(context) }
    AtolyeTheme(themeType = themeType) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            content = content,
        )
    }
}

@Composable
private fun OnResume(action: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val current by rememberUpdatedState(action)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) current()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

@Composable
private fun RequestNotificationPermissionIfNeeded(onResult: (Boolean) -> Unit) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
        onResult,
    )
    LaunchedEffect(Unit) {
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoScrollScreen(
    settings: PluginSettingsManager,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val pluginId = AutoScrollKeys.PLUGIN_ID

    val running by settings.observeBoolean(pluginId, AutoScrollKeys.RUNNING, false).collectAsState()
    val overlayVisible by settings.observeBoolean(
        pluginId,
        AutoScrollKeys.OVERLAY_VISIBLE,
        AutoScrollKeys.DEFAULT_OVERLAY_VISIBLE,
    ).collectAsState()
    val mode by settings.observeString(
        pluginId, AutoScrollKeys.MODE, AutoScrollKeys.DEFAULT_MODE
    ).collectAsState()
    val direction by settings.observeString(
        pluginId, AutoScrollKeys.DIRECTION, AutoScrollKeys.DEFAULT_DIRECTION
    ).collectAsState()
    val target by settings.observeString(
        pluginId, AutoScrollKeys.TARGET, AutoScrollKeys.DEFAULT_TARGET
    ).collectAsState()
    val interval by settings.observeInt(
        pluginId, AutoScrollKeys.INTERVAL_SECONDS, AutoScrollKeys.DEFAULT_INTERVAL_SECONDS
    ).collectAsState()
    val continuousInterval by settings.observeInt(
        pluginId,
        AutoScrollKeys.CONTINUOUS_INTERVAL_SECONDS,
        AutoScrollKeys.DEFAULT_CONTINUOUS_INTERVAL_SECONDS,
    ).collectAsState()
    val lastApp by settings.observeString(pluginId, AutoScrollKeys.LAST_APP, "").collectAsState()
    val lastResult by settings.observeString(pluginId, AutoScrollKeys.LAST_RESULT, "").collectAsState()

    var accessibilityOn by remember {
        mutableStateOf(AutoScrollPermissions.isAccessibilityEnabled(context))
    }
    var notificationGranted by remember {
        mutableStateOf(AutoScrollPermissions.hasNotificationPermission(context))
    }
    var overlayPermission by remember {
        mutableStateOf(AutoScrollPermissions.canDrawOverlays(context))
    }

    fun refreshStatus() {
        accessibilityOn = AutoScrollPermissions.isAccessibilityEnabled(context)
        notificationGranted = AutoScrollPermissions.hasNotificationPermission(context)
        overlayPermission = AutoScrollPermissions.canDrawOverlays(context)
        AutoScrollService.publishState(context)
    }

    OnResume { refreshStatus() }
    RequestNotificationPermissionIfNeeded { granted -> notificationGranted = granted }

    fun applySetting(change: () -> Unit) {
        change()
        AutoScrollService.publishState(context)
        if (running) AutoScrollService.refresh(context)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.plugin_autoscroll_name),
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                actions = {
                    // Ekranın üstündeki hızlı aç/kapat anahtarı.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(
                                if (running) R.string.auto_toggle_on else R.string.auto_toggle_off
                            ),
                            fontSize = 12.sp,
                        )
                        Switch(
                            checked = running,
                            onCheckedChange = { wanted ->
                                when {
                                    !accessibilityOn ->
                                        AutoScrollPermissions.openAccessibilitySettings(context)
                                    wanted -> AutoScrollService.start(context)
                                    else -> AutoScrollService.stop(context)
                                }
                                refreshStatus()
                            },
                        )
                    }
                    IconButton(onClick = { refreshStatus() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.auto_refresh),
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.auto_settings),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                StatusCard(
                    running = running,
                    accessibilityOn = accessibilityOn,
                    notificationGranted = notificationGranted,
                    lastApp = lastApp,
                    lastResult = lastResult,
                )
            }

            item {
                Button(
                    onClick = {
                        when {
                            !accessibilityOn -> AutoScrollPermissions.openAccessibilitySettings(context)
                            running -> AutoScrollService.stop(context)
                            else -> AutoScrollService.start(context)
                        }
                        refreshStatus()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        when {
                            !accessibilityOn -> Icons.AutoMirrored.Filled.Accessible
                            running -> Icons.Default.Stop
                            else -> Icons.Default.PlayArrow
                        },
                        contentDescription = null,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when {
                            !accessibilityOn -> stringResource(R.string.auto_open_accessibility)
                            running -> stringResource(R.string.auto_stop)
                            else -> stringResource(R.string.auto_start)
                        }
                    )
                }
            }

            item {
                SwitchCard(
                    title = stringResource(R.string.auto_overlay_title),
                    description = stringResource(
                        if (overlayVisible && !overlayPermission)
                            R.string.auto_overlay_permission_needed
                        else R.string.auto_overlay_desc
                    ),
                    checked = overlayVisible,
                    onCheckedChange = { wanted ->
                        if (wanted) AutoScrollOverlayService.enable(context)
                        else AutoScrollOverlayService.disable(context)
                        refreshStatus()
                    },
                )
            }

            item {
                ChoiceCard(
                    title = stringResource(R.string.auto_mode_title),
                    description = stringResource(R.string.auto_mode_desc),
                ) {
                    ChoiceChips(
                        options = listOf(
                            AutoScrollKeys.MODE_TIMED to stringResource(R.string.auto_mode_timed),
                            AutoScrollKeys.MODE_CONTINUOUS to stringResource(R.string.auto_mode_continuous),
                        ),
                        selected = mode,
                    ) { selected ->
                        applySetting {
                            settings.putString(pluginId, AutoScrollKeys.MODE, selected)
                        }
                    }
                }
            }

            item {
                if (mode == AutoScrollKeys.MODE_CONTINUOUS) {
                    ChoiceCard(
                        title = stringResource(R.string.auto_continuous_interval_title),
                        description = stringResource(R.string.auto_continuous_interval_desc),
                    ) {
                        ChoiceChips(
                            options = AutoScrollKeys.CONTINUOUS_INTERVAL_OPTIONS.map { seconds ->
                                seconds.toString() to stringResource(R.string.auto_seconds, seconds)
                            },
                            selected = continuousInterval.toString(),
                        ) { selected ->
                            applySetting {
                                settings.putInt(
                                    pluginId,
                                    AutoScrollKeys.CONTINUOUS_INTERVAL_SECONDS,
                                    selected.toIntOrNull()
                                        ?: AutoScrollKeys.DEFAULT_CONTINUOUS_INTERVAL_SECONDS,
                                )
                            }
                        }
                    }
                } else {
                    ChoiceCard(
                        title = stringResource(R.string.auto_interval_title),
                        description = stringResource(R.string.auto_interval_desc),
                    ) {
                        ChoiceChips(
                            options = AutoScrollKeys.INTERVAL_OPTIONS.map { seconds ->
                                seconds.toString() to stringResource(R.string.auto_seconds, seconds)
                            },
                            selected = interval.toString(),
                        ) { selected ->
                            applySetting {
                                settings.putInt(
                                    pluginId,
                                    AutoScrollKeys.INTERVAL_SECONDS,
                                    selected.toIntOrNull()
                                        ?: AutoScrollKeys.DEFAULT_INTERVAL_SECONDS,
                                )
                            }
                        }
                    }
                }
            }

            item {
                ChoiceCard(
                    title = stringResource(R.string.auto_direction_title),
                    description = stringResource(R.string.auto_direction_desc),
                ) {
                    ChoiceChips(
                        options = listOf(
                            AutoScrollKeys.DIRECTION_UP to stringResource(R.string.auto_direction_up),
                            AutoScrollKeys.DIRECTION_DOWN to stringResource(R.string.auto_direction_down),
                        ),
                        selected = direction,
                    ) { selected ->
                        applySetting {
                            settings.putString(pluginId, AutoScrollKeys.DIRECTION, selected)
                        }
                    }
                }
            }

            item {
                ChoiceCard(
                    title = stringResource(R.string.auto_target_title),
                    description = stringResource(R.string.auto_target_desc),
                ) {
                    ChoiceChips(
                        options = listOf(
                            AutoScrollKeys.TARGET_ACTIVE to stringResource(R.string.auto_target_active),
                            AutoScrollKeys.TARGET_PRIMARY to stringResource(R.string.auto_target_primary),
                            AutoScrollKeys.TARGET_SECONDARY to stringResource(R.string.auto_target_secondary),
                        ),
                        selected = target,
                    ) { selected ->
                        applySetting {
                            settings.putString(pluginId, AutoScrollKeys.TARGET, selected)
                        }
                    }
                }
            }

            item {
                InfoCard(
                    title = stringResource(R.string.auto_info_title),
                    text = stringResource(R.string.auto_info_text),
                )
            }
        }
    }
}

@Composable
private fun SwitchCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(
        shape = AtolyeShapes.CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(Modifier.height(3.dp))
                Text(
                    description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(10.dp))
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun StatusCard(
    running: Boolean,
    accessibilityOn: Boolean,
    notificationGranted: Boolean,
    lastApp: String,
    lastResult: String,
) {
    Card(
        shape = AtolyeShapes.CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (running) Icons.Default.CheckCircle else Icons.Default.Stop,
                    contentDescription = null,
                    tint = if (running) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(
                        if (running) R.string.auto_status_running else R.string.auto_status_ready
                    ),
                    fontWeight = FontWeight.SemiBold,
                )
            }

            StatusLine(
                label = stringResource(
                    if (accessibilityOn) R.string.auto_accessibility_enabled
                    else R.string.auto_accessibility_disabled
                ),
                ok = accessibilityOn,
            )
            StatusLine(
                label = stringResource(
                    R.string.auto_notification_permission,
                    stringResource(
                        if (notificationGranted) R.string.auto_granted
                        else R.string.auto_not_granted
                    ),
                ),
                ok = notificationGranted,
            )
            Text(
                text = stringResource(
                    R.string.auto_current_app,
                    lastApp.ifBlank { stringResource(R.string.auto_current_app_unknown) },
                ),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(
                    R.string.auto_last_result,
                    lastResult.ifBlank { stringResource(R.string.auto_status_ready) },
                ),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatusLine(label: String, ok: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (ok) Icons.Default.CheckCircle else Icons.Default.Block,
            contentDescription = null,
            tint = if (ok) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 12.sp)
    }
}

@Composable
private fun ChoiceCard(
    title: String,
    description: String,
    content: @Composable () -> Unit,
) {
    Card(
        shape = AtolyeShapes.CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(
                description,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            content()
        }
    }
}

@Composable
private fun ChoiceChips(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = value == selected,
                onClick = { onSelect(value) },
                label = { Text(label, fontSize = 12.sp) },
            )
        }
    }
}

@Composable
private fun InfoCard(title: String, text: String) {
    Card(
        shape = AtolyeShapes.CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
            Text(
                text,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoScrollSettingsScreen(
    settings: PluginSettingsManager,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val pluginId = AutoScrollKeys.PLUGIN_ID

    val mode by settings.observeString(
        pluginId, AutoScrollKeys.MODE, AutoScrollKeys.DEFAULT_MODE
    ).collectAsState()
    val direction by settings.observeString(
        pluginId, AutoScrollKeys.DIRECTION, AutoScrollKeys.DEFAULT_DIRECTION
    ).collectAsState()
    val target by settings.observeString(
        pluginId, AutoScrollKeys.TARGET, AutoScrollKeys.DEFAULT_TARGET
    ).collectAsState()
    val interval by settings.observeInt(
        pluginId, AutoScrollKeys.INTERVAL_SECONDS, AutoScrollKeys.DEFAULT_INTERVAL_SECONDS
    ).collectAsState()
    val continuousInterval by settings.observeInt(
        pluginId,
        AutoScrollKeys.CONTINUOUS_INTERVAL_SECONDS,
        AutoScrollKeys.DEFAULT_CONTINUOUS_INTERVAL_SECONDS,
    ).collectAsState()
    val running by settings.observeBoolean(pluginId, AutoScrollKeys.RUNNING, false).collectAsState()

    fun applySetting(change: () -> Unit) {
        change()
        AutoScrollService.publishState(context)
        if (running) AutoScrollService.refresh(context)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.autoscroll_settings_title),
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.auto_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ChoiceCard(
                    title = stringResource(R.string.auto_mode_title),
                    description = stringResource(R.string.auto_mode_desc),
                ) {
                    ChoiceChips(
                        options = listOf(
                            AutoScrollKeys.MODE_TIMED to stringResource(R.string.auto_mode_timed),
                            AutoScrollKeys.MODE_CONTINUOUS to stringResource(R.string.auto_mode_continuous),
                        ),
                        selected = mode,
                    ) { selected ->
                        applySetting {
                            settings.putString(pluginId, AutoScrollKeys.MODE, selected)
                        }
                    }
                }
            }

            item {
                ChoiceCard(
                    title = stringResource(R.string.auto_interval_title),
                    description = stringResource(R.string.auto_interval_desc),
                ) {
                    ChoiceChips(
                        options = AutoScrollKeys.INTERVAL_OPTIONS.map { seconds ->
                            seconds.toString() to stringResource(R.string.auto_seconds, seconds)
                        },
                        selected = interval.toString(),
                    ) { selected ->
                        applySetting {
                            settings.putInt(
                                pluginId,
                                AutoScrollKeys.INTERVAL_SECONDS,
                                selected.toIntOrNull()
                                    ?: AutoScrollKeys.DEFAULT_INTERVAL_SECONDS,
                            )
                        }
                    }
                }
            }

            item {
                ChoiceCard(
                    title = stringResource(R.string.auto_continuous_interval_title),
                    description = stringResource(R.string.auto_continuous_interval_desc),
                ) {
                    ChoiceChips(
                        options = AutoScrollKeys.CONTINUOUS_INTERVAL_OPTIONS.map { seconds ->
                            seconds.toString() to stringResource(R.string.auto_seconds, seconds)
                        },
                        selected = continuousInterval.toString(),
                    ) { selected ->
                        applySetting {
                            settings.putInt(
                                pluginId,
                                AutoScrollKeys.CONTINUOUS_INTERVAL_SECONDS,
                                selected.toIntOrNull()
                                    ?: AutoScrollKeys.DEFAULT_CONTINUOUS_INTERVAL_SECONDS,
                            )
                        }
                    }
                }
            }

            item {
                ChoiceCard(
                    title = stringResource(R.string.auto_direction_title),
                    description = stringResource(R.string.auto_direction_desc),
                ) {
                    ChoiceChips(
                        options = listOf(
                            AutoScrollKeys.DIRECTION_UP to stringResource(R.string.auto_direction_up),
                            AutoScrollKeys.DIRECTION_DOWN to stringResource(R.string.auto_direction_down),
                        ),
                        selected = direction,
                    ) { selected ->
                        applySetting {
                            settings.putString(pluginId, AutoScrollKeys.DIRECTION, selected)
                        }
                    }
                }
            }

            item {
                ChoiceCard(
                    title = stringResource(R.string.auto_target_title),
                    description = stringResource(R.string.auto_target_desc),
                ) {
                    ChoiceChips(
                        options = listOf(
                            AutoScrollKeys.TARGET_ACTIVE to stringResource(R.string.auto_target_active),
                            AutoScrollKeys.TARGET_PRIMARY to stringResource(R.string.auto_target_primary),
                            AutoScrollKeys.TARGET_SECONDARY to stringResource(R.string.auto_target_secondary),
                        ),
                        selected = target,
                    ) { selected ->
                        applySetting {
                            settings.putString(pluginId, AutoScrollKeys.TARGET, selected)
                        }
                    }
                }
            }

            item {
                InfoCard(
                    title = stringResource(R.string.auto_info_title),
                    text = stringResource(R.string.auto_info_text),
                )
            }
        }
    }
}
