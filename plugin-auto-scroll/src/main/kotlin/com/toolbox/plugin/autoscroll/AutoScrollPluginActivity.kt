package com.toolbox.plugin.autoscroll

import android.content.Intent
import androidx.compose.runtime.Composable
import com.toolbox.contract.PluginMetadata
import com.toolbox.plugin.BasePluginActivity
import com.toolbox.plugin.PluginSettingsManager

class AutoScrollPluginActivity : BasePluginActivity() {

    private val settings by lazy { PluginSettingsManager(this) }

    override val pluginMetadata: PluginMetadata by lazy {
        AutoScrollPluginProvider.metadata(this)
    }

    @Composable
    override fun PluginContent() {
        AutoScrollTheme {
            AutoScrollScreen(
                settings = settings,
                onOpenSettings = { openSettings() },
            )
        }
    }

    override fun openSettings() {
        startActivity(Intent(this, AutoScrollSettingsActivity::class.java))
    }
}
