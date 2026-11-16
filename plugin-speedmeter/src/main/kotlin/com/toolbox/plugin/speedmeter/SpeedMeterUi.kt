package com.toolbox.plugin.speedmeter

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.platform.LocalContext
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

/** Speed Meter ayar anahtarları. */
object SpeedMeterKeys {
    const val PLUGIN_ID = "com.toolbox.plugin.speedmeter"

    const val NOTIFICATION_ENABLED = "notification_enabled"
    const val SHOW_UPLOAD = "show_upload"
    const val NETWORK_MODE = "network_mode"
    const val UNIT = "unit"
    const val AUTOSTART = "autostart"
    const val SERVICE_RUNNING = "service_running"
}

/**
 * Eklentinin ana ekranı. Servisten gelen anlık hız yayınlarını dinler,
 * veri kullanımını ve tüm ayarları tek ekranda sunar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedMeterScreen(
    settings: PluginSettingsManager,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val isRunning by settings
        .observeBoolean(SpeedMeterKeys.PLUGIN_ID, SpeedMeterKeys.SERVICE_RUNNING, false)
        .collectAsState()
    val showUpload by settings
        .observeBoolean(SpeedMeterKeys.PLUGIN_ID, SpeedMeterKeys.SHOW_UPLOAD, true)
        .collectAsState()
    val unit by settings
        .observeInt(SpeedMeterKeys.PLUGIN_ID, SpeedMeterKeys.UNIT, 0)
        .collectAsState()

    var downloadSpeed by remember { mutableStateOf(0L) }
    var uploadSpeed by remember { mutableStateOf(0L) }
    var usage by remember { mutableStateOf(SpeedMeterStats.read(context)) }

    RequestNotificationPermission()

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action != SpeedMeterService.ACTION_SPEED_UPDATE) return
                downloadSpeed = intent.getLongExtra(SpeedMeterService.EXTRA_DOWNLOAD_SPEED, 0L)
                uploadSpeed = intent.getLongExtra(SpeedMeterService.EXTRA_UPLOAD_SPEED, 0L)
                val daily = intent.getLongExtra(SpeedMeterService.EXTRA_DAILY_USAGE, 0L)
                val monthly = intent.getLongExtra(SpeedMeterService.EXTRA_MONTHLY_USAGE, 0L)
                usage = SpeedMeterUsage(
                    dailyRx = daily,
                    dailyTx = 0L,
                    monthlyRx = monthly,
                    monthlyTx = 0L
                )
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(SpeedMeterService.ACTION_SPEED_UPDATE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    LaunchedEffect(isRunning) {
        if (!isRunning) {
            downloadSpeed = 0L
            uploadSpeed = 0L
            usage = SpeedMeterStats.read(context)
        }
    }

    val resetStats = {
        SpeedMeterStats.reset(context)
        SpeedMeterService.resetStats(context)
        usage = SpeedMeterUsage()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.plugin_speedmeter_name),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.plugin_speedmeter_short),
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
                            contentDescription = stringResource(R.string.speedmeter_settings_title)
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
                SpeedControlCard(
                    isRunning = isRunning,
                    downloadSpeed = downloadSpeed,
                    uploadSpeed = uploadSpeed,
                    showUpload = showUpload,
                    unitMode = unit,
                    onToggle = {
                        if (isRunning) {
                            SpeedMeterService.stop(context)
                        } else {
                            SpeedMeterService.start(context)
                        }
                    }
                )
            }

            item {
                UsageStatsCard(
                    usage = usage,
                    unitMode = unit,
                    onReset = resetStats
                )
            }

            item {
                SpeedMeterSettingsSections(settings = settings, onReset = resetStats)
            }
        }
    }
}

/** Ayarlar ekranı: aynı bölümleri başlık çubuğuyla gösterir. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedMeterSettingsScreen(
    settings: PluginSettingsManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val resetStats = {
        SpeedMeterStats.reset(context)
        SpeedMeterService.resetStats(context)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.speedmeter_settings_title)) },
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
                SpeedMeterSettingsSections(settings = settings, onReset = resetStats)
            }
        }
    }
}

/** Ana ekran ve ayarlar ekranında ortak kullanılan ayar kartları. */
@Composable
fun SpeedMeterSettingsSections(
    settings: PluginSettingsManager,
    onReset: () -> Unit
) {
    val context = LocalContext.current
    val pluginId = SpeedMeterKeys.PLUGIN_ID

    val notificationEnabled by settings
        .observeBoolean(pluginId, SpeedMeterKeys.NOTIFICATION_ENABLED, true)
        .collectAsState()
    val showUpload by settings
        .observeBoolean(pluginId, SpeedMeterKeys.SHOW_UPLOAD, true)
        .collectAsState()
    val networkMode by settings
        .observeInt(pluginId, SpeedMeterKeys.NETWORK_MODE, 0)
        .collectAsState()
    val unit by settings
        .observeInt(pluginId, SpeedMeterKeys.UNIT, 0)
        .collectAsState()
    val autostart by settings
        .observeBoolean(pluginId, SpeedMeterKeys.AUTOSTART, true)
        .collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SettingsSection(title = stringResource(R.string.notification_settings)) {
            SettingRow(
                title = stringResource(R.string.speedmeter_show_notification),
                description = stringResource(R.string.speedmeter_show_notification_desc),
                trailing = {
                    Switch(
                        checked = notificationEnabled,
                        onCheckedChange = {
                            settings.putBoolean(pluginId, SpeedMeterKeys.NOTIFICATION_ENABLED, it)
                        }
                    )
                }
            )

            HorizontalDivider()

            SettingRow(
                title = stringResource(R.string.speedmeter_show_upload),
                description = stringResource(R.string.speedmeter_show_upload_desc),
                trailing = {
                    Switch(
                        checked = showUpload,
                        onCheckedChange = {
                            settings.putBoolean(pluginId, SpeedMeterKeys.SHOW_UPLOAD, it)
                        }
                    )
                },
                enabled = notificationEnabled
            )
        }

        SettingsSection(title = stringResource(R.string.network_settings)) {
            OptionRow(
                title = stringResource(R.string.speedmeter_network_mode_total),
                selected = networkMode == 0,
                onClick = { settings.putInt(pluginId, SpeedMeterKeys.NETWORK_MODE, 0) }
            )
            OptionRow(
                title = stringResource(R.string.speedmeter_network_mode_wifi),
                selected = networkMode == 1,
                onClick = { settings.putInt(pluginId, SpeedMeterKeys.NETWORK_MODE, 1) }
            )
            OptionRow(
                title = stringResource(R.string.speedmeter_network_mode_mobile),
                selected = networkMode == 2,
                onClick = { settings.putInt(pluginId, SpeedMeterKeys.NETWORK_MODE, 2) }
            )
        }

        SettingsSection(title = stringResource(R.string.display_settings)) {
            OptionRow(
                title = stringResource(R.string.speedmeter_unit_auto),
                selected = unit == 0,
                onClick = { settings.putInt(pluginId, SpeedMeterKeys.UNIT, 0) }
            )
            OptionRow(
                title = stringResource(R.string.speedmeter_unit_kbps),
                selected = unit == 1,
                onClick = { settings.putInt(pluginId, SpeedMeterKeys.UNIT, 1) }
            )
            OptionRow(
                title = stringResource(R.string.speedmeter_unit_mbps),
                selected = unit == 2,
                onClick = { settings.putInt(pluginId, SpeedMeterKeys.UNIT, 2) }
            )
        }

        SettingsSection(title = stringResource(R.string.general_settings)) {
            SettingRow(
                title = stringResource(R.string.speedmeter_autostart),
                description = stringResource(R.string.speedmeter_autostart_desc),
                trailing = {
                    Switch(
                        checked = autostart,
                        onCheckedChange = {
                            settings.putBoolean(pluginId, SpeedMeterKeys.AUTOSTART, it)
                            SpeedMeterBootReceiver.setAutostart(context, it)
                        }
                    )
                }
            )

            HorizontalDivider()

            SettingRow(
                title = stringResource(R.string.reset_stats),
                description = stringResource(R.string.reset_stats_desc),
                trailing = {
                    TextButton(onClick = onReset) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.reset),
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
            )
        }

        ReliableWorkCard()
        InfoCard()
    }
}

@Composable
fun SpeedControlCard(
    isRunning: Boolean,
    downloadSpeed: Long,
    uploadSpeed: Long,
    showUpload: Boolean,
    unitMode: Int,
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SpeedValue(
                    label = stringResource(R.string.download_speed),
                    value = SpeedMeterFormat.speed(downloadSpeed, unitMode),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                )
                if (showUpload) {
                    SpeedValue(
                        label = stringResource(R.string.upload_speed),
                        value = SpeedMeterFormat.speed(uploadSpeed, unitMode),
                        icon = {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (isRunning) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isRunning) {
                        stringResource(R.string.service_running)
                    } else {
                        stringResource(R.string.service_stopped)
                    },
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

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
                        stringResource(R.string.stop_service)
                    } else {
                        stringResource(R.string.service_start)
                    },
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun SpeedValue(
    label: String,
    value: String,
    icon: @Composable () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon()
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun UsageStatsCard(
    usage: SpeedMeterUsage,
    unitMode: Int,
    onReset: () -> Unit
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.data_usage),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = onReset) {
                    Text(stringResource(R.string.reset))
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                UsageValue(
                    label = stringResource(R.string.daily_usage),
                    value = SpeedMeterFormat.data(usage.dailyTotal, unitMode)
                )
                UsageValue(
                    label = stringResource(R.string.monthly_usage),
                    value = SpeedMeterFormat.data(usage.monthlyTotal, unitMode)
                )
            }
        }
    }
}

@Composable
private fun UsageValue(label: String, value: String) {
    Column {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ReliableWorkCard() {
    val context = LocalContext.current
    var ignoring by remember { mutableStateOf(isIgnoringBatteryOpt(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val current by rememberUpdatedState(newValue = { ignoring = isIgnoringBatteryOpt(context) })
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) current() }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AtolyeShapes.CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.reliable_section), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            Text(
                "${stringResource(R.string.battery_opt_status)}: " +
                    if (ignoring) stringResource(R.string.battery_opt_granted) else stringResource(R.string.battery_opt_not_granted),
                fontSize = 13.sp,
                color = if (ignoring) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            OutlinedButton(onClick = { openBatteryOptSettings(context) }, shape = AtolyeShapes.ButtonShape) {
                Text(stringResource(R.string.open_battery_settings))
            }
            OutlinedButton(onClick = { openAppLaunchOrInfo(context) }, shape = AtolyeShapes.ButtonShape) {
                Text(stringResource(R.string.open_app_launch))
            }
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

@Composable
private fun InfoCard() {
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
                    text = stringResource(R.string.speedmeter_info_title),
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = stringResource(R.string.speedmeter_info_text),
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
private fun OptionRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = title, fontSize = 14.sp)
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
fun SpeedMeterTheme(content: @Composable () -> Unit) {
    AtolyeTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            content = content
        )
    }
}

/** Ana Activity'den ayarları açmak için. */
fun openSpeedMeterSettings(context: Context) {
    context.startActivity(Intent(context, SpeedMeterSettingsActivity::class.java))
}
