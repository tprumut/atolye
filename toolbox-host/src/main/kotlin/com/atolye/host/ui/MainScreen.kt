package com.atolye.host.ui

import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.atolye.design.AtolyeAppLogo
import com.atolye.design.AtolyePluginIcon
import com.atolye.design.AtolyeShapes
import com.atolye.design.AtolyeThemeType
import com.atolye.host.AtolyeApplication
import com.atolye.host.R
import com.atolye.host.ui.components.EmptyPluginsState
import com.atolye.host.ui.components.LoadingGrid
import com.atolye.host.ui.components.PluginGrid
import com.atolye.host.ui.components.categoryLabel
import com.toolbox.contract.PluginPermission
import kotlin.math.cos
import kotlin.math.sin

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    themeType: AtolyeThemeType,
    onThemeChange: (AtolyeThemeType) -> Unit,
    onPluginClick: (PluginUiItem) -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val items by viewModel.items.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showAddDialog by remember { mutableStateOf(false) }
    var showHidden by remember { mutableStateOf(false) }
    var editItem by remember { mutableStateOf<PluginUiItem?>(null) }
    var pendingStartItem by remember { mutableStateOf<PluginUiItem?>(null) }

    // AtolyeApplication instance for cycleTheme
    val app = context.applicationContext as AtolyeApplication

    OnResume { viewModel.requestStates() }

    LaunchedEffect(error) {
        error?.let { snackbarHostState.showSnackbar(it); viewModel.clearError() }
    }

    val visibleItems = if (showHidden) items else items.filter { !it.hidden }
    val hiddenCount = items.count { it.hidden }

    fun tryTogglePower(item: PluginUiItem) {
        if (item.runtime.running) {
            viewModel.sendStop(item)
        } else {
            if (!viewModel.isIgnoringBatteryOptimizations()) {
                pendingStartItem = item
            } else {
                viewModel.sendStart(item)
            }
        }
    }

    // Tema değişikliğinde snackbar göster
    val currentTheme by app.theme.collectAsState()
    // Tema isimlerini compose dışında çözmek için context kullan
    val themeName = remember(currentTheme) {
        when (currentTheme) {
            AtolyeThemeType.KAGIT -> context.getString(R.string.theme_kagit)
            AtolyeThemeType.GECE_ATOLYESI -> context.getString(R.string.theme_gece)
            AtolyeThemeType.BAKIR -> context.getString(R.string.theme_bakir)
            AtolyeThemeType.DEVRE_KARTI -> context.getString(R.string.theme_devre)
            AtolyeThemeType.DYNAMIC -> context.getString(R.string.theme_dynamic)
        }
    }
    LaunchedEffect(currentTheme) {
        snackbarHostState.showSnackbar(message = "Tema: $themeName", duration = SnackbarDuration.Short)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            HexBackground(modifier = Modifier.matchParentSize(), tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                HeaderSection(
                    pluginCount = visibleItems.size,
                    hiddenCount = hiddenCount,
                    showHidden = showHidden,
                    onToggleHidden = { showHidden = !showHidden },
                    onAddClick = { showAddDialog = true },
                    onThemeClick = { app.cycleTheme() },
                    onRefreshClick = { viewModel.loadPlugins() }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.plugins_title),
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.installed_plugins_count, visibleItems.size),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 22.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
                Spacer(Modifier.height(6.dp))

                when {
                    isLoading && items.isEmpty() -> LoadingGrid(modifier = Modifier.weight(1f))
                    items.isEmpty() -> EmptyPluginsState(onAddClick = { showAddDialog = true }, modifier = Modifier.weight(1f))
                    else -> PluginGrid(
                        items = visibleItems,
                        onPluginClick = onPluginClick,
                        onPowerClick = { tryTogglePower(it) },
                        onHideToggle = { viewModel.setPluginHidden(it.plugin.metadata.pluginId, !it.hidden) },
                        onEditCard = { editItem = it },
                        onDeleteClick = { item ->
                            // TODO: Implement delete confirmation dialog and uninstall logic
                            // viewModel.uninstallPlugin(item.plugin.packageName)
                        },
                        onQuickSettingChange = { item, k, v -> viewModel.setQuickSetting(item, k, v) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(stringResource(R.string.add_plugin_dialog_title)) },
            text = { Text(stringResource(R.string.add_plugin_dialog_text)) },
            confirmButton = { TextButton(onClick = { showAddDialog = false }) { Text(stringResource(R.string.close)) } }
        )
    }
    editItem?.let { item ->
        EditCardDialog(
            item = item,
            onDismiss = { editItem = null },
            onSave = { keys -> viewModel.setVisibleQuickSettings(item.plugin.metadata.pluginId, keys); editItem = null }
        )
    }
    pendingStartItem?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingStartItem = null },
            title = { Text(stringResource(R.string.battery_opt_title)) },
            text = { Text(stringResource(R.string.battery_opt_message)) },
            confirmButton = {
                TextButton(onClick = {
                    val toStart = item
                    pendingStartItem = null
                    runCatching { context.startActivity(viewModel.requestIgnoreBatteryOptimizationsIntent()) }
                    viewModel.sendStart(toStart)
                }) { Text(stringResource(R.string.go_to_settings)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    val toStart = item
                    pendingStartItem = null
                    viewModel.sendStart(toStart)
                }) { Text(stringResource(R.string.not_now)) }
            }
        )
    }
}

@Composable
private fun HeaderSection(
    pluginCount: Int,
    hiddenCount: Int,
    showHidden: Boolean,
    onToggleHidden: () -> Unit,
    onAddClick: () -> Unit,
    onThemeClick: () -> Unit,
    onRefreshClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 10.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.weight(1f)) {
            AtolyeAppLogo(size = 54.dp)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Atölye", fontSize = 44.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, lineHeight = 46.sp)
                Text("Araçlarını tek yerde topla", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (hiddenCount > 0) {
                TextButton(onClick = onToggleHidden) {
                    Text(if (showHidden) stringResource(R.string.hide_hidden) else stringResource(R.string.show_hidden), fontSize = 12.sp)
                }
            }
            CircleActionButton(onClick = onThemeClick, contentDescription = stringResource(R.string.change_theme)) {
                Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            CircleActionButton(onClick = onRefreshClick, contentDescription = stringResource(R.string.refresh)) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            CircleActionButton(onClick = onAddClick, contentDescription = "Eklenti ekle") {
                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun CircleActionButton(onClick: () -> Unit, contentDescription: String, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun HexBackground(modifier: Modifier = Modifier, tint: Color) {
    Canvas(modifier = modifier) {
        val r = 34.dp.toPx()
        val w = r * 1.732f
        val hStep = r * 1.5f
        var row = 0
        var y = -r
        while (y < size.height + r) {
            val xOffset = if (row % 2 == 0) 0f else w / 2f
            var x = -w + xOffset
            while (x < size.width + w) {
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

@Composable
fun EditCardDialog(item: PluginUiItem, onDismiss: () -> Unit, onSave: (List<String>) -> Unit) {
    val all = item.plugin.metadata.quickSettings
    val initial = if (item.visibleQuickSettingKeys.isEmpty()) all.take(3).map { it.key } else item.visibleQuickSettingKeys
    val selected = remember { mutableStateListOf<String>().apply { addAll(initial.take(3)) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.card_edit_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.card_edit_desc), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                all.forEach { qs ->
                    val checked = selected.contains(qs.key)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { c ->
                                if (c) {
                                    if (selected.size < 3 && !selected.contains(qs.key)) selected.add(qs.key)
                                } else selected.remove(qs.key)
                            },
                            enabled = checked || selected.size < 3
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(qs.title.ifBlank { qs.key }, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(selected.toList()) }, enabled = selected.isNotEmpty()) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginDetailScreen(
    item: PluginUiItem,
    onBack: () -> Unit,
    onLaunch: () -> Unit,
    onSettings: () -> Unit,
    onPowerToggle: () -> Unit
) {
    val plugin = item.plugin
    val metadata = plugin.metadata
    val context = LocalContext.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(metadata.name, maxLines = 1, fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back)) } },
                actions = { IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.plugin_settings)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AtolyePluginIcon(pluginId = metadata.pluginId, size = 64.dp)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(metadata.name, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(categoryLabel(metadata.category), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(R.string.version_format, metadata.version), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(if (item.runtime.running) stringResource(R.string.plugin_running) else stringResource(R.string.plugin_stopped), fontSize = 12.sp, color = if (item.runtime.running) Color(0xFF159764) else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Text(metadata.description, fontSize = 15.sp, lineHeight = 22.sp)

            Button(onClick = onLaunch, modifier = Modifier.fillMaxWidth().height(50.dp), shape = AtolyeShapes.ButtonShape) {
                Text(stringResource(R.string.open_plugin), fontWeight = FontWeight.Medium)
            }
            OutlinedButton(onClick = onPowerToggle, modifier = Modifier.fillMaxWidth().height(48.dp), shape = AtolyeShapes.ButtonShape) {
                Text(if (item.runtime.running) stringResource(R.string.stop_action) else stringResource(R.string.start_action))
            }
            OutlinedButton(onClick = onSettings, modifier = Modifier.fillMaxWidth().height(48.dp), shape = AtolyeShapes.ButtonShape) {
                Text(stringResource(R.string.plugin_settings))
            }

            ReliableWorkSection(pluginPackage = plugin.packageName)

            Text(stringResource(R.string.required_permissions), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            if (metadata.requiredPermissions.isEmpty()) {
                Text(stringResource(R.string.no_permissions), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                metadata.requiredPermissions.forEach { PermissionRow(it) }
            }
            Text(stringResource(R.string.installed_version_format, plugin.installedVersion), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ReliableWorkSection(pluginPackage: String) {
    val context = LocalContext.current
    var ignoring by remember { mutableStateOf(isIgnoringBatteryOpt(context)) }
    OnResume { ignoring = isIgnoringBatteryOpt(context) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.reliable_section), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Text(
            "${stringResource(R.string.battery_opt_status)}: " + if (ignoring) stringResource(R.string.battery_opt_granted) else stringResource(R.string.battery_opt_not_granted),
            fontSize = 13.sp,
            color = if (ignoring) Color(0xFF159764) else MaterialTheme.colorScheme.error
        )
        OutlinedButton(onClick = { openBatteryOptSettings(context) }, shape = AtolyeShapes.ButtonShape) {
            Text(stringResource(R.string.open_battery_settings))
        }
        OutlinedButton(onClick = { openAppLaunchOrInfo(context, pluginPackage) }, shape = AtolyeShapes.ButtonShape) {
            Text(stringResource(R.string.open_app_launch))
        }
    }
}

fun isIgnoringBatteryOpt(context: android.content.Context): Boolean {
    val pm = context.getSystemService(PowerManager::class.java) ?: return true
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

fun openBatteryOptSettings(context: android.content.Context) {
    val intents = listOf(
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).setData(Uri.parse("package:${context.packageName}")),
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    )
    for (i in intents) {
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (runCatching { context.startActivity(i) }.isSuccess) return
    }
}

fun openAppLaunchOrInfo(context: android.content.Context, packageName: String) {
    val intents = listOf(
        Intent().setClassName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
        Intent().setClassName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"),
        Intent().setClassName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"),
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:$packageName"))
    )
    for (i in intents) {
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (runCatching { context.startActivity(i) }.isSuccess) return
    }
}

@Composable
private fun PermissionRow(permission: PluginPermission) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = if (permission.isRequired) Icons.Default.Check else Icons.Default.Close,
            contentDescription = null,
            tint = if (permission.isRequired) Color(0xFF159764) else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp).size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(permission.title.ifBlank { permission.permission }, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(
                    if (permission.isRequired) stringResource(R.string.permission_required) else stringResource(R.string.permission_optional),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            if (permission.description.isNotBlank()) {
                Text(permission.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
