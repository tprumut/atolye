package com.atolye.host.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.atolye.design.AtolyeShapes
import com.atolye.design.AtolyeThemeSelector
import com.atolye.design.AtolyeThemeType
import com.atolye.host.R
import com.atolye.host.ui.components.EmptyPluginsState
import com.atolye.host.ui.components.LoadingGrid
import com.atolye.host.ui.components.PluginGrid
import com.atolye.host.ui.components.PluginIcon
import com.atolye.host.ui.components.categoryLabel
import com.toolbox.contract.InstalledPlugin
import com.toolbox.contract.PluginPermission

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    themeType: AtolyeThemeType,
    onThemeChange: (AtolyeThemeType) -> Unit,
    onPluginClick: (InstalledPlugin) -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val plugins by viewModel.plugins.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.app_tagline),
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
                    IconButton(onClick = { showThemeDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = stringResource(R.string.change_theme)
                        )
                    }
                    IconButton(onClick = { viewModel.loadPlugins() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.refresh)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.plugins_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (plugins.isNotEmpty()) {
                    Text(
                        text = stringResource(
                            R.string.installed_plugins_count,
                            plugins.size
                        ),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            when {
                isLoading && plugins.isEmpty() -> LoadingGrid(modifier = Modifier.weight(1f))
                plugins.isEmpty() -> EmptyPluginsState(
                    onAddClick = { showAddDialog = true },
                    modifier = Modifier.weight(1f)
                )
                else -> PluginGrid(
                    plugins = plugins,
                    onPluginClick = onPluginClick,
                    onPluginToggle = viewModel::setPluginEnabled,
                    onAddPluginClick = { showAddDialog = true },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (showThemeDialog) {
        AtolyeThemeSelector(
            currentTheme = themeType,
            onThemeChange = onThemeChange,
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(stringResource(R.string.add_plugin_dialog_title)) },
            text = { Text(stringResource(R.string.add_plugin_dialog_text)) },
            confirmButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginDetailScreen(
    plugin: InstalledPlugin,
    onBack: () -> Unit,
    onLaunch: () -> Unit,
    onSettings: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    val metadata = plugin.metadata

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = metadata.name,
                        maxLines = 1,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.plugin_settings)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PluginIcon(
                    metadata = metadata,
                    colors = com.atolye.design.KagitColorScheme(),
                    size = 64.dp
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = metadata.name,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = categoryLabel(metadata.category),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.version_format, metadata.version),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = metadata.description,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )

            if (metadata.tags.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    metadata.tags.take(4).forEach { tag ->
                        Text(
                            text = "#$tag",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.enable_plugin),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (plugin.isEnabled) {
                            stringResource(R.string.plugin_enabled)
                        } else {
                            stringResource(R.string.plugin_disabled)
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = plugin.isEnabled,
                    onCheckedChange = onToggle
                )
            }

            Button(
                onClick = onLaunch,
                enabled = plugin.isEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = AtolyeShapes.ButtonShape
            ) {
                Text(
                    text = stringResource(R.string.open_plugin),
                    fontWeight = FontWeight.Medium
                )
            }

            OutlinedButton(
                onClick = onSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = AtolyeShapes.ButtonShape
            ) {
                Text(stringResource(R.string.plugin_settings))
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.required_permissions),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )

            if (metadata.requiredPermissions.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_permissions),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                metadata.requiredPermissions.forEach { permission ->
                    PermissionRow(permission)
                }
            }

            Text(
                text = stringResource(
                    R.string.installed_version_format,
                    plugin.installedVersion
                ),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PermissionRow(permission: PluginPermission) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = if (permission.isRequired) {
                Icons.Default.Check
            } else {
                Icons.Default.Close
            },
            contentDescription = null,
            tint = if (permission.isRequired) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier
                .padding(top = 2.dp)
                .size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = permission.title.ifBlank { permission.permission },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (permission.isRequired) {
                        stringResource(R.string.permission_required)
                    } else {
                        stringResource(R.string.permission_optional)
                    },
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            if (permission.description.isNotBlank()) {
                Text(
                    text = permission.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
