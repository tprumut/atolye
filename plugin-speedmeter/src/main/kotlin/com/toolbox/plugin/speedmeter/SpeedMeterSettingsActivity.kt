package com.toolbox.plugin.speedmeter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.toolbox.plugin.PluginSettingsManager

/**
 * Speed Meter ayarlar ekranı.
 *
 * Host uygulaması tarafından ayrı bir Activity olarak açılır. Ana
 * ekrandaki ayar kartlarıyla aynı bileşenleri kullanır.
 */
class SpeedMeterSettingsActivity : ComponentActivity() {

    private val settings by lazy { PluginSettingsManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SpeedMeterTheme {
                SpeedMeterSettingsScreen(
                    settings = settings,
                    onBack = { finish() }
                )
            }
        }
    }
}
