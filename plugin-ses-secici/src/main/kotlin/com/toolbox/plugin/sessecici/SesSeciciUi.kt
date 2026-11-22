package com.toolbox.plugin.sessecici

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import androidx.compose.runtime.mutableIntStateOf
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
import kotlinx.coroutines.delay

@Composable
fun SesSeciciTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val themeType = remember(context) { AtolyeThemeStore.read(context) }
    AtolyeTheme(themeType = themeType) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            content = content
        )
    }
}

@Composable
private fun OnResume(action: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val current by rememberUpdatedState(action)
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) current() }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }
}

@Composable
private fun RequestNotifPermissionIfNeeded() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SesSeciciScreen(settings: PluginSettingsManager, onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val pluginId = SesSeciciKeys.PLUGIN_ID

    val notifEnabled by settings.observeBoolean(pluginId, SesSeciciKeys.NOTIFICATION_ENABLED, true).collectAsState()
    val autoApply by settings.observeBoolean(pluginId, SesSeciciKeys.AUTO_APPLY_ON_CONNECT, true).collectAsState()
    val experimental by settings.observeBoolean(pluginId, SesSeciciKeys.EXPERIMENTAL_APP_AUDIO, false).collectAsState()

    var devices by remember { mutableStateOf<List<OutputDeviceUi>>(emptyList()) }
    var currentVolume by remember { mutableIntStateOf(SesSeciciAudio.currentMusicVolume(context)) }
    var refreshTick by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }
    val unsupportedMsg = stringResource(R.string.switch_unsupported)

    fun refresh() {
        devices = SesSeciciAudio.listOutputDevices(context, settings)
        currentVolume = SesSeciciAudio.currentMusicVolume(context)
    }

    LaunchedEffect(refreshTick) { refresh() }
    OnResume { refresh(); SesSeciciDeviceService.start(context) }
    LaunchedEffect(Unit) {
        while (true) { delay(2000); refresh() }
    }
    RequestNotifPermissionIfNeeded()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.plugin_sessecici_name), fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = { refreshTick++ }) { Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh)) }
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings)) }
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
                SectionHeader(stringResource(R.string.section_outputs), stringResource(R.string.section_outputs_desc))
            }
            if (devices.isEmpty()) {
                item { InfoCard(stringResource(R.string.no_devices)) }
            } else {
                items(devices, key = { it.key }) { d ->
                    DeviceCard(
                        device = d,
                        currentVolume = currentVolume,
                        onVolumeChange = { v ->
                            currentVolume = v
                            SesSeciciAudio.setMusicVolume(context, v)
                            if (d.remember) SesSeciciAudio.saveDeviceVolume(settings, d.key, v, true)
                        },
                        onRememberChange = { rem ->
                            SesSeciciAudio.saveDeviceVolume(settings, d.key, currentVolume, rem)
                            refresh()
                        },
                        onSelect = {
                            val (ok, _) = SesSeciciAudio.trySwitchOutput(context, d.info)
                            message = if (ok) null else unsupportedMsg
                            refresh()
                        }
                    )
                }
            }

            item { message?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) } }
            item { InfoCard(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) stringResource(R.string.switch_hint_api31) else stringResource(R.string.switch_hint_legacy)) }

            item {
                SectionHeader(stringResource(R.string.section_app_audio), stringResource(R.string.section_app_audio_desc))
            }
            item {
                Card(shape = AtolyeShapes.CardShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.app_audio_experimental), fontWeight = FontWeight.SemiBold)
                        }
                        if (!experimental) {
                            Text(stringResource(R.string.app_audio_unsupported), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(stringResource(R.string.app_audio_limited), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedButton(onClick = { settings.putBoolean(pluginId, SesSeciciKeys.EXPERIMENTAL_APP_AUDIO, true) }) {
                                Text("Denemeyi Göster")
                            }
                        } else {
                            val count = try { SesSeciciAudio.audioManager(context).activePlaybackConfigurations.size } catch (_: Throwable) { 0 }
                            Text(stringResource(R.string.app_audio_limited), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                if (count > 0) "Algılanan aktif oynatma oturumu: $count (uygulama bazlı ayrı ses seviyesi bu cihazda sistemce desteklenmiyor; çökme önlemek için kontrol devre dışı)." else stringResource(R.string.app_audio_no_apps),
                                fontSize = 13.sp
                            )
                            Text(stringResource(R.string.app_audio_unsupported), fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            item {
                SectionHeader(stringResource(R.string.section_settings), stringResource(R.string.section_settings_desc))
            }
            item {
                Card(shape = AtolyeShapes.CardShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingSwitch(stringResource(R.string.auto_apply_on_connect), stringResource(R.string.auto_apply_on_connect_desc), autoApply) {
                            settings.putBoolean(pluginId, SesSeciciKeys.AUTO_APPLY_ON_CONNECT, it)
                            SesSeciciDeviceService.start(context)
                        }
                        HorizontalDivider()
                        SettingSwitch(stringResource(R.string.notification_enabled), stringResource(R.string.notification_enabled_desc), notifEnabled) {
                            settings.putBoolean(pluginId, SesSeciciKeys.NOTIFICATION_ENABLED, it)
                            if (it) SesSeciciDeviceService.start(context) else SesSeciciDeviceService.refresh(context)
                            SesSeciciDeviceService.start(context)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, desc: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        Text(desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DeviceCard(
    device: OutputDeviceUi,
    currentVolume: Int,
    onVolumeChange: (Int) -> Unit,
    onRememberChange: (Boolean) -> Unit,
    onSelect: () -> Unit
) {
    Card(shape = AtolyeShapes.CardShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(device.name, fontWeight = FontWeight.Medium)
                    Text(device.typeLabel + if (device.isCurrent) " • ${stringResource(R.string.active_output)}" else "", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedButton(onClick = onSelect) { Text("Seç") }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Ses", fontSize = 12.sp)
                Spacer(Modifier.width(8.dp))
                Slider(value = currentVolume.toFloat(), onValueChange = { onVolumeChange(it.toInt()) }, valueRange = 0f..device.maxVolume.toFloat(), modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Text("$currentVolume/${device.maxVolume}", fontSize = 12.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.remember_device), fontSize = 14.sp)
                    Text(stringResource(R.string.remember_device_desc), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = device.remember, onCheckedChange = onRememberChange)
            }
        }
    }
}

@Composable
private fun SettingSwitch(title: String, desc: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) { Text(title); Text(desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Spacer(Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun InfoCard(text: String) {
    Card(shape = AtolyeShapes.CardShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Text(text, modifier = Modifier.padding(16.dp), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SesSeciciSettingsScreen(settings: PluginSettingsManager, onBack: () -> Unit) {
    val context = LocalContext.current
    val pluginId = SesSeciciKeys.PLUGIN_ID
    val notifEnabled by settings.observeBoolean(pluginId, SesSeciciKeys.NOTIFICATION_ENABLED, true).collectAsState()
    val autoApply by settings.observeBoolean(pluginId, SesSeciciKeys.AUTO_APPLY_ON_CONNECT, true).collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sessecici_settings_title)) },
                navigationIcon = { androidx.compose.material3.IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(shape = AtolyeShapes.CardShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingSwitch(stringResource(R.string.auto_apply_on_connect), stringResource(R.string.auto_apply_on_connect_desc), autoApply) {
                        settings.putBoolean(pluginId, SesSeciciKeys.AUTO_APPLY_ON_CONNECT, it); SesSeciciDeviceService.start(context)
                    }
                    HorizontalDivider()
                    SettingSwitch(stringResource(R.string.notification_enabled), stringResource(R.string.notification_enabled_desc), notifEnabled) {
                        settings.putBoolean(pluginId, SesSeciciKeys.NOTIFICATION_ENABLED, it); SesSeciciDeviceService.start(context)
                    }
                }
            }
            ReliableWorkCard()
        }
    }
}

@Composable
private fun ReliableWorkCard() {
    val context = LocalContext.current
    var ignoring by remember { mutableStateOf(isIgnoringBatteryOpt(context)) }
    OnResume { ignoring = isIgnoringBatteryOpt(context) }
    Card(shape = AtolyeShapes.CardShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.reliable_section), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            Text(
                "${stringResource(R.string.battery_opt_status)}: " + if (ignoring) stringResource(R.string.battery_opt_granted) else stringResource(R.string.battery_opt_not_granted),
                fontSize = 13.sp,
                color = if (ignoring) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            OutlinedButton(onClick = { openBatteryOptSettings(context) }) { Text(stringResource(R.string.open_battery_settings)) }
            OutlinedButton(onClick = { openAppLaunchOrInfo(context) }) { Text(stringResource(R.string.open_app_launch)) }
        }
    }
}

private fun isIgnoringBatteryOpt(context: Context): Boolean {
    val pm = context.getSystemService(PowerManager::class.java) ?: return true
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

private fun openBatteryOptSettings(context: Context) {
    val intents = listOf(
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).setData(Uri.parse("package:${context.packageName}")),
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    )
    for (i in intents) {
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (runCatching { context.startActivity(i) }.isSuccess) return
    }
}

private fun openAppLaunchOrInfo(context: Context) {
    val pkg = context.packageName
    val intents = listOf(
        Intent().setClassName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
        Intent().setClassName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"),
        Intent().setClassName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"),
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:$pkg"))
    )
    for (i in intents) {
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (runCatching { context.startActivity(i) }.isSuccess) return
    }
}
