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
import com.atolye.host.ui.PluginSettingsScreen
import com.atolye.host.ui.PluginUiItem
import com.toolbox.plugin.autoscroll.AutoScrollOverlayService
import com.toolbox.plugin.keepawake.KeepAwakeOverlayService

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as AtolyeApplication

        // Yeniden kurulum/force-stop sonrası Uyanık servisi ölü kalmasın.
        // Kayıtlı mod ve süre ile ekranı açık tutma servisi tekrar başlar.
        KeepAwakeOverlayService.ensureRunning(this)
        // Yüzen tuş yalnızca kaydırma hâlâ çalışıyorsa geri yüklenir.
        AutoScrollOverlayService.ensureRunning(this)

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

    // Kart tıklaması doğrudan eklentiyi açar. Ayarlar düğmesi ise tüm
    // eklentiler için paylaşılan tek ayar ekranını açar.
    var settingsOpen by remember { mutableStateOf(false) }
    var settingsPluginId by remember { mutableStateOf<String?>(null) }

    val launchFailed = stringResource(R.string.launch_failed)
    val settingsUnavailable = stringResource(R.string.settings_unavailable)

    fun launch(item: PluginUiItem) {
        if (!discovery.launchPlugin(item.plugin, from = context)) {
            Toast.makeText(context, launchFailed, Toast.LENGTH_SHORT).show()
        }
    }

    if (!settingsOpen) {
        MainScreen(
            themeType = themeType,
            onThemeChange = onThemeChange,
            onPluginClick = { item -> launch(item) },
            onSettingsClick = { settingsOpen = true },
            viewModel = viewModel
        )
    } else {
        BackHandler { settingsOpen = false }
        PluginSettingsScreen(
            items = items,
            selectedId = settingsPluginId,
            onSelect = { settingsPluginId = it },
            onBack = { settingsOpen = false },
            onLaunch = { item -> launch(item) },
            onSettings = { item ->
                if (!discovery.openPluginSettings(item.plugin, from = context)) {
                    Toast.makeText(context, settingsUnavailable, Toast.LENGTH_SHORT).show()
                }
            },
            onPowerToggle = { item ->
                if (item.runtime.running) viewModel.sendStop(item) else viewModel.sendStart(item)
            }
        )
    }
}