package com.toolbox.plugin.izin

import android.content.Intent
import androidx.compose.runtime.Composable
import com.toolbox.contract.PluginMetadata
import com.toolbox.plugin.BasePluginActivity
import com.toolbox.plugin.PluginSettingsManager

class IzinPluginActivity : BasePluginActivity() {

    private val settings by lazy { PluginSettingsManager(this) }

    override val pluginMetadata: PluginMetadata by lazy {
        IzinPluginProvider.metadata(this)
    }

    @Composable
    override fun PluginContent() {
        IzinTheme {
            IzinScreen(
                settings = settings,
                onOpenSettings = { openSettings() },
            )
        }
    }

    override fun openSettings() {
        startActivity(Intent(this, IzinSettingsActivity::class.java))
    }
}
