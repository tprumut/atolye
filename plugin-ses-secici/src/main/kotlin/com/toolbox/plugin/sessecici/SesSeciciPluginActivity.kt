package com.toolbox.plugin.sessecici

import android.content.Intent
import androidx.compose.runtime.Composable
import com.toolbox.contract.PluginMetadata
import com.toolbox.plugin.BasePluginActivity
import com.toolbox.plugin.PluginSettingsManager

class SesSeciciPluginActivity : BasePluginActivity() {
    private val settings by lazy { PluginSettingsManager(this) }

    override val pluginMetadata: PluginMetadata by lazy {
        SesSeciciPluginProvider.metadata(this)
    }

    @Composable
    override fun PluginContent() {
        SesSeciciTheme { SesSeciciScreen(settings = settings, onOpenSettings = { openSettings() }) }
    }

    override fun openSettings() { startActivity(Intent(this, SesSeciciSettingsActivity::class.java)) }
}
