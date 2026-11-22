package com.toolbox.plugin.izin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atolye.design.AtolyeShapes
import com.atolye.design.AtolyeTheme
import com.atolye.design.AtolyeThemeStore
import com.toolbox.plugin.PluginSettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun IzinTheme(content: @Composable () -> Unit) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IzinScreen(
    settings: PluginSettingsManager,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val includeSystem by settings.observeBoolean(
        IzinKeys.PLUGIN_ID, IzinKeys.INCLUDE_SYSTEM, false
    ).collectAsState()
    val onlyGranted by settings.observeBoolean(
        IzinKeys.PLUGIN_ID, IzinKeys.ONLY_GRANTED, true
    ).collectAsState()

    var apps by remember { mutableStateOf(emptyList<IzinApp>()) }
    var loading by remember { mutableStateOf(true) }
    var refreshTick by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var expandedPackages by remember { mutableStateOf(emptySet<String>()) }

    LaunchedEffect(includeSystem, refreshTick) {
        loading = true
        val loaded = withContext(Dispatchers.IO) {
            IzinAppData.load(context, includeSystem)
        }
        apps = loaded
        loading = false
    }

    val search = query.trim()
    val visibleApps = remember(apps, search, onlyGranted) {
        apps.mapNotNull { app ->
            val permissionMatches = app.permissions.filter { permission ->
                val matchesQuery = search.isBlank() ||
                    permission.label.contains(search, ignoreCase = true) ||
                    permission.name.contains(search, ignoreCase = true) ||
                    permission.group.contains(search, ignoreCase = true)
                (!onlyGranted || permission.granted) && matchesQuery
            }
            val appMatches = search.isNotBlank() &&
                (app.label.contains(search, ignoreCase = true) ||
                    app.packageName.contains(search, ignoreCase = true))

            val finalPermissions = if (appMatches) {
                app.permissions.filter { !onlyGranted || it.granted }
            } else {
                permissionMatches
            }

            if (search.isBlank() && finalPermissions.isEmpty()) return@mapNotNull null
            if (search.isNotBlank() && !appMatches && finalPermissions.isEmpty()) return@mapNotNull null
            app.copy(permissions = finalPermissions)
        }
    }
    val grantedTotal = visibleApps.sumOf { it.grantedCount }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.plugin_izin_name),
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                actions = {
                    IconButton(onClick = { refreshTick++ }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.izin_refresh),
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.izin_settings),
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
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.izin_search_hint)) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = onlyGranted,
                        onClick = {
                            settings.putBoolean(
                                IzinKeys.PLUGIN_ID, IzinKeys.ONLY_GRANTED, !onlyGranted
                            )
                        },
                        label = { Text(stringResource(R.string.izin_only_granted)) },
                    )
                    FilterChip(
                        selected = includeSystem,
                        onClick = {
                            settings.putBoolean(
                                IzinKeys.PLUGIN_ID, IzinKeys.INCLUDE_SYSTEM, !includeSystem
                            )
                        },
                        label = { Text(stringResource(R.string.izin_include_system)) },
                    )
                }
            }

            item {
                Text(
                    text = stringResource(
                        R.string.izin_app_count, visibleApps.size
                    ) + " • " + stringResource(R.string.izin_granted_summary, grantedTotal, visibleApps.sumOf { it.permissions.size }),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (loading) {
                item {
                    Text(
                        stringResource(R.string.izin_loading),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (!loading && visibleApps.isEmpty()) {
                item {
                    InfoCard(stringResource(R.string.izin_no_apps))
                }
            }

            items(visibleApps, key = { it.packageName }) { app ->
                AppCard(
                    app = app,
                    expanded = app.packageName in expandedPackages,
                    onToggleExpand = {
                        expandedPackages = if (app.packageName in expandedPackages) {
                            expandedPackages - app.packageName
                        } else {
                            expandedPackages + app.packageName
                        }
                    },
                )
            }

            item {
                InfoCard(
                    text = stringResource(R.string.izin_note),
                    title = stringResource(R.string.izin_note_title),
                )
            }
        }
    }
}

@Composable
private fun AppCard(
    app: IzinApp,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(app.label, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(
                        app.packageName,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (app.isSystem) {
                    Text(
                        stringResource(R.string.izin_system_app),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Text(
                stringResource(R.string.izin_granted_summary, app.grantedCount, app.permissions.size),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val shown = if (expanded) app.permissions else app.permissions.take(5)
            shown.forEach { permission ->
                PermissionRow(permission)
            }

            if (app.permissions.size > 5) {
                TextButton(onClick = onToggleExpand) {
                    Text(
                        if (expanded) stringResource(R.string.izin_show_less)
                        else stringResource(R.string.izin_show_all),
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(permission: IzinPermission) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (permission.granted) Icons.Default.CheckCircle else Icons.Default.Block,
            contentDescription = null,
            tint = if (permission.granted) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                permission.label,
                fontSize = 13.sp,
                color = if (permission.granted) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Text(
                permission.group + " • " + permission.name,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            stringResource(
                if (permission.granted) R.string.izin_granted else R.string.izin_denied
            ),
            fontSize = 10.sp,
            color = if (permission.granted) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
private fun InfoCard(text: String, title: String? = null) {
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
            if (title != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                    )
                }
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
fun IzinSettingsScreen(
    settings: PluginSettingsManager,
    onBack: () -> Unit,
) {
    val onlyGranted by settings.observeBoolean(
        IzinKeys.PLUGIN_ID, IzinKeys.ONLY_GRANTED, true
    ).collectAsState()
    val includeSystem by settings.observeBoolean(
        IzinKeys.PLUGIN_ID, IzinKeys.INCLUDE_SYSTEM, false
    ).collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.izin_settings_title),
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.izin_back),
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
                Text(
                    stringResource(R.string.izin_section_filters),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    stringResource(R.string.izin_section_filters_desc),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            item {
                Card(
                    shape = AtolyeShapes.CardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        SettingSwitch(
                            title = stringResource(R.string.izin_only_granted),
                            description = stringResource(R.string.izin_only_granted_desc),
                            checked = onlyGranted,
                        ) {
                            settings.putBoolean(
                                IzinKeys.PLUGIN_ID, IzinKeys.ONLY_GRANTED, it
                            )
                        }
                        HorizontalDivider()
                        SettingSwitch(
                            title = stringResource(R.string.izin_include_system),
                            description = stringResource(R.string.izin_include_system_desc),
                            checked = includeSystem,
                        ) {
                            settings.putBoolean(
                                IzinKeys.PLUGIN_ID, IzinKeys.INCLUDE_SYSTEM, it
                            )
                        }
                    }
                }
            }

            item {
                InfoCard(
                    text = stringResource(R.string.izin_note),
                    title = stringResource(R.string.izin_note_title),
                )
            }
        }
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(
                description,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
