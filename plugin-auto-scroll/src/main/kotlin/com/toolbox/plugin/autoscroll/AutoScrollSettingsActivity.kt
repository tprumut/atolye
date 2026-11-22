package com.toolbox.plugin.autoscroll

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.toolbox.plugin.PluginSettingsManager

class AutoScrollSettingsActivity : ComponentActivity() {
    private val settings by lazy { PluginSettingsManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AutoScrollTheme {
                AutoScrollSettingsScreen(
                    settings = settings,
                    onBack = { finish() },
                )
            }
        }
    }
}
