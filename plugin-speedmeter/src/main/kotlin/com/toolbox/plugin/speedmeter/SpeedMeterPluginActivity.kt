package com.toolbox.plugin.speedmeter

import androidx.compose.runtime.Composable
import com.toolbox.contract.PluginMetadata
import com.toolbox.plugin.BasePluginActivity
import com.toolbox.plugin.PluginSettingsManager

/**
 * Speed Meter ana ekranı.
 *
 * Host uygulaması bu Activity'yi `com.toolbox.PLUGIN` action'ı ile açar.
 * Ayarlar ve servis durumu [PluginSettingsManager] üzerinden canlı izlenir.
 */
class SpeedMeterPluginActivity : BasePluginActivity() {

    private val settings by lazy { PluginSettingsManager(this) }

    override val pluginMetadata: PluginMetadata by lazy {
        SpeedMeterPluginProvider.metadata(this)
    }

    @Composable
    override fun PluginContent() {
        SpeedMeterTheme {
            SpeedMeterScreen(
                settings = settings,
                onOpenSettings = { openSpeedMeterSettings(this) }
            )
        }
    }
}
