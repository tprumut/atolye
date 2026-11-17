package com.atolye.host.github

import android.content.Context
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.atolye.design.AtolyeShapes
import com.atolye.host.R
import com.toolbox.contract.PluginCategory
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginBrowserBottomSheet(
    onDismiss: () -> Unit,
    viewModel: PluginBrowserViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()

    val currentState = state
    val filteredPlugins = remember(filter, currentState) {
        if (currentState is PluginBrowserState.Success) {
            viewModel.getFilteredPlugins()
        } else emptyList()
    }

    val categories = if (currentState is PluginBrowserState.Success) currentState.categories else emptyList<PluginCategory>()

    // Bottom sheet overlay using Box
    Box(modifier = Modifier.fillMaxSize()) {
        // Dimmed background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            // Bottom sheet content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .padding(16.dp)
                    .height(500.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Eklenti Mağazası", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Search field
                TextField(
                    value = filter.searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    placeholder = { Text(stringResource(R.string.search_plugins)) },
                    singleLine = true,
                    shape = AtolyeShapes.ButtonShape
                )

                Spacer(Modifier.height(8.dp))

                // Category filter chips
                if (categories.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = filter.showInstalledOnly,
                                onClick = { viewModel.toggleShowInstalledOnly() },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                label = { Text("Kurulu") }
                            )
                            categories.forEach { category ->
                                FilterChip(
                                    selected = filter.selectedCategory == category,
                                    onClick = { viewModel.updateCategory(category) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    label = { Text(categoryLabel(category)) }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // Plugin list
                when (currentState) {
                    is PluginBrowserState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    is PluginBrowserState.Error -> {
                        Text(currentState.message, color = MaterialTheme.colorScheme.error, modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(200.dp))
                    }
                    is PluginBrowserState.Success -> {
                        if (filteredPlugins.isEmpty()) {
                            Text(
                                stringResource(R.string.no_plugins_found),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth().padding(16.dp).height(200.dp)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().height(300.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(filteredPlugins) { plugin ->
                                    PluginBrowserItem(
                                        plugin = plugin,
                                        state = currentState,
                                        onInstallClick = { viewModel.installPlugin(plugin) },
                                        onOpenGitHubClick = { viewModel.openGitHubPage(plugin) }
                                    )
                                }
                            }
                        }
                    }
                    is PluginBrowserState.Downloading -> {
                        if (currentState.pluginId in filteredPlugins.map { it.pluginId }) {
                            DownloadingProgress(pluginId = currentState.pluginId, progress = currentState.progress)
                        } else {
                            // Show list with loading indicator on specific item
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().height(300.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(filteredPlugins) { plugin ->
                                    PluginBrowserItem(
                                        plugin = plugin,
                                        state = currentState,
                                        onInstallClick = { viewModel.installPlugin(plugin) },
                                        onOpenGitHubClick = { viewModel.openGitHubPage(plugin) }
                                    )
                                }
                            }
                        }
                    }
                    is PluginBrowserState.Installing -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Text("Kuruluyor...", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    is PluginBrowserState.SuccessMessage -> {
                        Text(currentState.message, color = Color(0xFF159764), modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(100.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun PluginBrowserItem(
    plugin: GitHubPluginInfo,
    state: PluginBrowserState,
    onInstallClick: () -> Unit,
    onOpenGitHubClick: () -> Unit
) {
    val isDownloading = state is PluginBrowserState.Downloading && state.pluginId == plugin.pluginId
    val isInstalling = state is PluginBrowserState.Installing && state.pluginId == plugin.pluginId
    val isInstalled = plugin.isInstalled

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isInstalled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        shape = AtolyeShapes.CardShape
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Plugin icon placeholder
                Box(
                    modifier = Modifier.size(48.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, AtolyeShapes.CardShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getCategoryIcon(plugin.category),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(plugin.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        if (isInstalled) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Kurulu", tint = Color(0xFF159764), modifier = Modifier.size(20.dp))
                        }
                    }
                    Text(plugin.description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = true,
                            onClick = {},
                            modifier = Modifier.padding(top = 4.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            label = { Text(categoryLabel(plugin.category), fontSize = 11.sp, fontWeight = FontWeight.Medium) }
                        )
                        Text("v${plugin.version}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isDownloading) {
                    OutlinedButton(
                        onClick = {},
                        modifier = Modifier.weight(1f),
                        enabled = false,
                        shape = AtolyeShapes.ButtonShape
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("%${((state as PluginBrowserState.Downloading).progress * 100).toInt()}")
                    }
                } else if (isInstalling) {
                    OutlinedButton(
                        onClick = {},
                        modifier = Modifier.weight(1f),
                        enabled = false,
                        shape = AtolyeShapes.ButtonShape
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Kuruluyor...")
                    }
                } else if (isInstalled) {
                    OutlinedButton(
                        onClick = onOpenGitHubClick,
                        modifier = Modifier.weight(1f),
                        shape = AtolyeShapes.ButtonShape
                    ) {
                        Text("GitHub'a Git")
                    }
                    Button(
                        onClick = onInstallClick, // Reinstall
                        modifier = Modifier.weight(1f),
                        shape = AtolyeShapes.ButtonShape
                    ) {
                        Text("Yeniden Kur")
                    }
                } else {
                    OutlinedButton(
                        onClick = onOpenGitHubClick,
                        modifier = Modifier.weight(1f),
                        shape = AtolyeShapes.ButtonShape
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("GitHub")
                    }
                    Button(
                        onClick = onInstallClick,
                        modifier = Modifier.weight(1f),
                        shape = AtolyeShapes.ButtonShape
                    ) {
                        Text("Kur")
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadingProgress(pluginId: String, progress: Float) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("İndiriliyor...", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth())
        Text("%${(progress * 100).toInt()}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

fun getCategoryIcon(category: PluginCategory) = when (category) {
    PluginCategory.NETWORK -> Icons.Default.Wifi
    PluginCategory.DISPLAY -> Icons.Default.ScreenRotation
    PluginCategory.SYSTEM -> Icons.Default.Settings
    PluginCategory.DEVELOPER -> Icons.Default.Code
    PluginCategory.MEDIA -> Icons.Default.VolumeUp
    PluginCategory.PRODUCTIVITY -> Icons.Default.Timer
    else -> Icons.Default.Build
}