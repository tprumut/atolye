package com.toolbox.plugin.keepawake

import androidx.compose.runtime.Composable
import com.toolbox.contract.PluginMetadata
import com.toolbox.plugin.BasePluginActivity
import com.toolbox.plugin.PluginSettingsManager

/**
 * Keep Awake ana ekranı.
 *
 * Host uygulaması bu Activity'yi `com.toolbox.PLUGIN` action'ı ile açar.
 * Ekran açık tutma servisi ve tüm ayarlar aynı ekrandan yönetilir.
 */
class KeepAwakePluginActivity : BasePluginActivity() {

    private val settings by lazy { PluginSettingsManager(this) }

    override val pluginMetadata: PluginMetadata by lazy {
        KeepAwakePluginProvider.metadata(this)
    }

    @Composable
    override fun PluginContent() {
        KeepAwakeTheme {
            KeepAwakeScreen(
                settings = settings,
                onOpenSettings = { openKeepAwakeSettings(this) }
            )
        }
    }
}
