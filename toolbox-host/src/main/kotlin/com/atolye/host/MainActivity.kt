package com.atolye.host

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.atolye.design.AtolyeTheme
import com.atolye.design.AtolyeThemeType
import com.atolye.host.ui.MainScreen
import com.atolye.host.ui.MainViewModel
import com.atolye.host.ui.PluginDetailScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as AtolyeApplication

        setContent {
            val themeType by app.theme.collectAsState()

            AtolyeTheme(themeType = themeType) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AtolyeApp(
                        themeType = themeType,
                        onThemeChange = app::setTheme
                    )
                }
            }
        }
    }
}

@Composable
private fun AtolyeApp(
    themeType: AtolyeThemeType,
    onThemeChange: (AtolyeThemeType) -> Unit
) {
    val context = LocalContext.current
    val viewModel: MainViewModel = viewModel()
    val items by viewModel.items.collectAsState()
    val app = context.applicationContext as AtolyeApplication
    val discovery = app.pluginDiscovery

    var selectedPluginId by remember { mutableStateOf<String?>(null) }
    val selectedItem = selectedPluginId?.let { id -> items.firstOrNull { it.plugin.metadata.pluginId == id } }

    if (selectedItem == null) {
        if (selectedPluginId != null) selectedPluginId = null
        MainScreen(
            themeType = themeType,
            onThemeChange = onThemeChange,
            onPluginClick = { item -> selectedPluginId = item.plugin.metadata.pluginId },
            viewModel = viewModel
        )
    } else {
        BackHandler { selectedPluginId = null }
        val launchFailed = stringResource(R.string.launch_failed)
        val settingsUnavailable = stringResource(R.string.settings_unavailable)

        PluginDetailScreen(
            item = selectedItem,
            onBack = { selectedPluginId = null },
            onLaunch = {
                if (!discovery.launchPlugin(selectedItem.plugin, from = context)) {
                    Toast.makeText(context, launchFailed, Toast.LENGTH_SHORT).show()
                }
            },
            onSettings = {
                if (!discovery.openPluginSettings(selectedItem.plugin, from = context)) {
                    Toast.makeText(context, settingsUnavailable, Toast.LENGTH_SHORT).show()
                }
            },
            onPowerToggle = {
                if (selectedItem.runtime.running) viewModel.sendStop(selectedItem) else viewModel.sendStart(selectedItem)
            }
        )
    }
}