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

/**
 * Ana gezinme: eklenti listesi ve eklenti ayrıntısı.
 *
 * Navigasyon kütüphanesi kullanılmaz; seçili eklenti basit bir yerel
 * durumda tutulur ve geri tuşu [BackHandler] ile yakalanır.
 */
@Composable
private fun AtolyeApp(
    themeType: AtolyeThemeType,
    onThemeChange: (AtolyeThemeType) -> Unit
) {
    val context = LocalContext.current
    val viewModel: MainViewModel = viewModel()
    val plugins by viewModel.plugins.collectAsState()

    var selectedPluginId by remember { mutableStateOf<String?>(null) }

    val selectedPlugin = selectedPluginId?.let { id ->
        plugins.firstOrNull { it.metadata.pluginId == id }
    }

    if (selectedPlugin == null) {
        if (selectedPluginId != null) {
            // Eklenti kaldırılmış ya da yenilenirken kaybolmuş olabilir.
            selectedPluginId = null
        }

        MainScreen(
            themeType = themeType,
            onThemeChange = onThemeChange,
            onPluginClick = { plugin ->
                selectedPluginId = plugin.metadata.pluginId
            },
            viewModel = viewModel
        )
    } else {
        BackHandler { selectedPluginId = null }

        val launchFailed = stringResource(R.string.launch_failed)
        val settingsUnavailable = stringResource(R.string.settings_unavailable)

        PluginDetailScreen(
            plugin = selectedPlugin,
            onBack = { selectedPluginId = null },
            onLaunch = {
                if (!viewModel.launchPlugin(selectedPlugin)) {
                    Toast.makeText(context, launchFailed, Toast.LENGTH_SHORT).show()
                }
            },
            onSettings = {
                if (!viewModel.openPluginSettings(selectedPlugin)) {
                    Toast.makeText(context, settingsUnavailable, Toast.LENGTH_SHORT).show()
                }
            },
            onToggle = { enabled ->
                viewModel.setPluginEnabled(selectedPlugin, enabled)
            }
        )
    }
}
