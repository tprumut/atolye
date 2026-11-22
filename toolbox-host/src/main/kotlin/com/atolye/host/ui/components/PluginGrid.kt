package com.atolye.host.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atolye.host.R
import com.atolye.host.ui.PluginUiItem

@Composable
fun PluginGrid(
    items: List<PluginUiItem>,
    onPluginClick: (PluginUiItem) -> Unit,
    onPowerClick: (PluginUiItem) -> Unit,
    onHideToggle: (PluginUiItem) -> Unit,
    onEditCard: (PluginUiItem) -> Unit,
    onQuickSettingChange: (PluginUiItem, String, String) -> Unit,
    onReorder: ((Int, Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // Kartlarda hızlı ayar satırları ve durum metni olduğu için dar kolonlar
        // içeriği kırpıyor. Eşikler yükseltildi: bu cihaz (941dp) artık 2 kolon.
        val columns = when {
            maxWidth >= 1200.dp -> 3
            maxWidth >= 640.dp -> 2
            else -> 1
        }
        LazyVerticalGrid(
            modifier = Modifier.fillMaxSize(),
            columns = GridCells.Fixed(columns),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(items, key = { it.plugin.metadata.pluginId }) { item ->
                val index = items.indexOf(item)
                PluginCard(
                    item = item,
                    onCardClick = { onPluginClick(item) },
                    onPowerClick = { onPowerClick(item) },
                    onHideToggle = { onHideToggle(item) },
                    onEditCard = { onEditCard(item) },
                    onQuickSettingChange = { k, v -> onQuickSettingChange(item, k, v) },
                    onDragStart = onReorder?.let { reorder ->
                        { reorder(index, index) } // placeholder - actual drag logic in PluginCard
                    }
                )
            }
        }
    }
}

@Composable
fun EmptyPluginsState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(88.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Build, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(44.dp))
            }
            Text(stringResource(R.string.welcome_title), fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(stringResource(R.string.welcome_description), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun LoadingGrid(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}
