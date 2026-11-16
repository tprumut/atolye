package com.toolbox.plugin.uykucu

import android.Manifest
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.atolye.design.AtolyeShapes
import com.atolye.design.AtolyeTheme
import com.toolbox.plugin.PluginSettingsManager
import kotlinx.coroutines.delay

@Composable
fun UykucuTheme(content: @Composable () -> Unit) {
    AtolyeTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            content = content
        )
    }
}

data class UykucuPerms(
    val accessibility: Boolean,
    val exactAlarm: Boolean,
    val notifications: Boolean
)

@Composable
private fun rememberPerms(): UykucuPerms {
    val context = LocalContext.current
    var perms by remember {
        mutableStateOf(
            UykucuPerms(
                accessibility = UykucuPermissions.isAccessibilityEnabled(context),
                exactAlarm = UykucuPermissions.canScheduleExactAlarms(context),
                notifications = UykucuPermissions.hasNotificationPermission(context)
            )
        )
    }
    OnResume {
        perms = UykucuPerms(
            accessibility = UykucuPermissions.isAccessibilityEnabled(context),
            exactAlarm = UykucuPermissions.canScheduleExactAlarms(context),
            notifications = UykucuPermissions.hasNotificationPermission(context)
        )
    }
    return perms
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

@Composable
fun RequestNotificationPermissionIfNeeded() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {}
    )
    LaunchedEffect(Unit) {
        if (!UykucuPermissions.hasNotificationPermission(context)) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun UykucuScreen(
    settings: PluginSettingsManager,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val pluginId = UykucuKeys.PLUGIN_ID
    val perms = rememberPerms()

    val running by settings.observeBoolean(pluginId, UykucuKeys.TIMER_RUNNING, false).collectAsState()
    val endTime by settings.observeLongFlow(pluginId, UykucuKeys.TIMER_END_TIME, 0L).collectAsState()
    val rememberLast by settings.observeBoolean(pluginId, UykucuKeys.REMEMBER_LAST_DURATION, true).collectAsState()
    val lastDuration by settings.observeInt(pluginId, UykucuKeys.LAST_DURATION_MINUTES, UykucuKeys.DEFAULT_DURATION_MINUTES).collectAsState()

    var selectedMinutes by remember(lastDuration, rememberLast) {
        mutableStateOf(if (rememberLast) lastDuration.coerceAtLeast(1) else UykucuKeys.DEFAULT_DURATION_MINUTES)
    }
    var customText by remember { mutableStateOf("") }
    var showAccessibilityDialog by remember { mutableStateOf(false) }
    var showExactAlarmDialog by remember { mutableStateOf(false) }

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(running, endTime) {
        while (running) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val remaining = (endTime - now).coerceAtLeast(0L)
    val actuallyRunning = running && endTime > now

    RequestNotificationPermissionIfNeeded()

    if (showAccessibilityDialog) {
        InfoDialog(
            title = stringResource(R.string.accessibility_permission_explanation_title),
            text = stringResource(R.string.accessibility_permission_explanation_text),
            confirm = stringResource(R.string.open_settings),
            onConfirm = {
                showAccessibilityDialog = false
                UykucuPermissions.openAccessibilitySettings(context)
            },
            onDismiss = { showAccessibilityDialog = false }
        )
    }
    if (showExactAlarmDialog) {
        InfoDialog(
            title = stringResource(R.string.exact_alarm_permission_explanation_title),
            text = stringResource(R.string.exact_alarm_permission_explanation_text),
            confirm = stringResource(R.string.open_settings),
            onConfirm = {
                showExactAlarmDialog = false
                UykucuPermissions.openExactAlarmSettings(context)
            },
            onDismiss = { showExactAlarmDialog = false }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.plugin_uykucu_name), fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.uykucu_settings_title))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (actuallyRunning) {
                item {
                    RunningCard(
                        remainingMs = remaining,
                        onCancel = { UykucuTimerService.cancel(context) },
                        onExtend = { UykucuTimerService.extend(context) }
                    )
                }
            } else {
                item {
                    DurationPickerCard(
                        selectedMinutes = selectedMinutes,
                        customText = customText,
                        onSelectPreset = { m ->
                            selectedMinutes = m
                            customText = ""
                        },
                        onCustomChange = { v ->
                            customText = v.filter { it.isDigit() }.take(3)
                            customText.toIntOrNull()?.let { if (it > 0) selectedMinutes = it }
                        },
                        onStart = {
                            val minutes = customText.toIntOrNull()?.takeIf { it > 0 } ?: selectedMinutes
                            if (!perms.accessibility) showAccessibilityDialog = true
                            if (!perms.exactAlarm) showExactAlarmDialog = true
                            UykucuTimerService.start(context, minutes)
                        }
                    )
                }
            }

            item {
                PermissionStatusCard(
                    perms = perms,
                    onOpenAccessibility = { showAccessibilityDialog = true },
                    onOpenExactAlarm = { showExactAlarmDialog = true }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DurationPickerCard(
    selectedMinutes: Int,
    customText: String,
    onSelectPreset: (Int) -> Unit,
    onCustomChange: (String) -> Unit,
    onStart: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AtolyeShapes.CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.uykucu_select_duration),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                UykucuKeys.PRESET_MINUTES.forEach { m ->
                    FilterChip(
                        selected = selectedMinutes == m && customText.isBlank(),
                        onClick = { onSelectPreset(m) },
                        label = { Text(stringResource(R.string.uykucu_minutes_short, m)) }
                    )
                }
            }
            OutlinedTextField(
                value = customText,
                onValueChange = onCustomChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.uykucu_custom_duration)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = AtolyeShapes.ButtonShape
            ) {
                Text(stringResource(R.string.uykucu_start), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun RunningCard(
    remainingMs: Long,
    onCancel: () -> Unit,
    onExtend: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AtolyeShapes.CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.uykucu_remaining),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = UykucuFormat.remaining(remainingMs),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onExtend,
                    modifier = Modifier.weight(1f),
                    shape = AtolyeShapes.ButtonShape
                ) { Text(stringResource(R.string.uykucu_extend_5)) }
                Button(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    shape = AtolyeShapes.ButtonShape,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.uykucu_cancel)) }
            }
        }
    }
}

@Composable
private fun PermissionStatusCard(
    perms: UykucuPerms,
    onOpenAccessibility: () -> Unit,
    onOpenExactAlarm: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AtolyeShapes.CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.uykucu_permissions_title),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            HorizontalDivider()
            PermissionRow(
                title = stringResource(R.string.permission_accessibility_title),
                ok = perms.accessibility,
                onClick = onOpenAccessibility
            )
            PermissionRow(
                title = stringResource(R.string.permission_exact_alarm_title),
                ok = perms.exactAlarm,
                onClick = onOpenExactAlarm
            )
            PermissionRow(
                title = stringResource(R.string.permission_post_notifications_title),
                ok = perms.notifications,
                onClick = {}
            )
            if (!perms.accessibility) {
                Text(
                    text = stringResource(R.string.uykucu_accessibility_missing),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (!perms.exactAlarm) {
                Text(
                    text = stringResource(R.string.uykucu_exact_alarm_missing),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun PermissionRow(title: String, ok: Boolean, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (ok) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, modifier = Modifier.weight(1f), fontSize = 14.sp)
        if (!ok) {
            TextButton(onClick = onClick) { Text(stringResource(R.string.open_settings)) }
        }
    }
}

@Composable
private fun InfoDialog(
    title: String,
    text: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.uykucu_cancel)) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UykucuSettingsScreen(
    settings: PluginSettingsManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val pluginId = UykucuKeys.PLUGIN_ID
    val perms = rememberPerms()

    val closeApps by settings.observeBoolean(pluginId, UykucuKeys.CLOSE_APPS_ENABLED, true).collectAsState()
    val stopMedia by settings.observeBoolean(pluginId, UykucuKeys.STOP_MEDIA_ENABLED, true).collectAsState()
    val rememberLast by settings.observeBoolean(pluginId, UykucuKeys.REMEMBER_LAST_DURATION, true).collectAsState()

    var showAccessibilityDialog by remember { mutableStateOf(false) }
    var showExactAlarmDialog by remember { mutableStateOf(false) }

    if (showAccessibilityDialog) {
        InfoDialog(
            title = stringResource(R.string.accessibility_permission_explanation_title),
            text = stringResource(R.string.accessibility_permission_explanation_text),
            confirm = stringResource(R.string.open_settings),
            onConfirm = { showAccessibilityDialog = false; UykucuPermissions.openAccessibilitySettings(context) },
            onDismiss = { showAccessibilityDialog = false }
        )
    }
    if (showExactAlarmDialog) {
        InfoDialog(
            title = stringResource(R.string.exact_alarm_permission_explanation_title),
            text = stringResource(R.string.exact_alarm_permission_explanation_text),
            confirm = stringResource(R.string.open_settings),
            onConfirm = { showExactAlarmDialog = false; UykucuPermissions.openExactAlarmSettings(context) },
            onDismiss = { showExactAlarmDialog = false }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.uykucu_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back)) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(shape = AtolyeShapes.CardShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingSwitch(
                            title = stringResource(R.string.uykucu_stop_media),
                            desc = stringResource(R.string.uykucu_stop_media_desc),
                            checked = stopMedia,
                            onChecked = { settings.putBoolean(pluginId, UykucuKeys.STOP_MEDIA_ENABLED, it) }
                        )
                        HorizontalDivider()
                        SettingSwitch(
                            title = stringResource(R.string.uykucu_close_apps),
                            desc = stringResource(R.string.uykucu_close_apps_desc),
                            checked = closeApps,
                            onChecked = { settings.putBoolean(pluginId, UykucuKeys.CLOSE_APPS_ENABLED, it) }
                        )
                        HorizontalDivider()
                        SettingSwitch(
                            title = stringResource(R.string.uykucu_remember_last),
                            desc = stringResource(R.string.uykucu_remember_last_desc),
                            checked = rememberLast,
                            onChecked = { settings.putBoolean(pluginId, UykucuKeys.REMEMBER_LAST_DURATION, it) }
                        )
                    }
                }
            }
            item {
                Card(shape = AtolyeShapes.CardShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.uykucu_permissions_title), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        HorizontalDivider()
                        PermissionRow(stringResource(R.string.permission_accessibility_title), perms.accessibility) { showAccessibilityDialog = true }
                        PermissionRow(stringResource(R.string.permission_exact_alarm_title), perms.exactAlarm) { showExactAlarmDialog = true }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingSwitch(title: String, desc: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp)
            Text(desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

// Extension to observe Long as StateFlow-like convenience using polling-free approach not available;
// PluginSettingsManager has no observeLong, so implement simple one-shot+OnResume refresh via remember.
@Composable
private fun PluginSettingsManager.observeLongFlow(pluginId: String, key: String, default: Long): kotlinx.coroutines.flow.StateFlow<Long> {
    // Fallback: create a MutableStateFlow updated on resume ticks by reading directly is complex here.
    // Instead, use a simple holder refreshed in UI; but easier: wrap with remember+LaunchedEffect.
    // However to keep API simple we emulate: not used as true flow elsewhere except collectAsState.
    // We'll provide a tiny wrapper.
    return remember(pluginId, key) {
        kotlinx.coroutines.flow.MutableStateFlow(getLong(pluginId, key, default))
    }.also { flow ->
        // Periodically sync from prefs while screen active
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner, pluginId, key) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    flow.value = getLong(pluginId, key, default)
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
        LaunchedEffect(pluginId, key) {
            while (true) {
                flow.value = getLong(pluginId, key, default)
                delay(1000)
            }
        }
    }
}
