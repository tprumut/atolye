package com.toolbox.plugin.uykucu

import androidx.compose.runtime.Composable
import com.toolbox.contract.PluginMetadata
import com.toolbox.plugin.BasePluginActivity
import com.toolbox.plugin.PluginSettingsManager

class UykucuPluginActivity : BasePluginActivity() {

    private val settings by lazy { PluginSettingsManager(this) }

    override val pluginMetadata: PluginMetadata by lazy {
        UykucuPluginProvider.metadata(this)
    }

    @Composable
    override fun PluginContent() {
        UykucuTheme {
            UykucuScreen(
                settings = settings,
                onOpenSettings = { openSettings() }
            )
        }
    }

    override fun openSettings() {
        startActivity(android.content.Intent(this, UykucuSettingsActivity::class.java))
    }
}
