package com.toolbox.plugin.izin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.toolbox.plugin.PluginSettingsManager

class IzinSettingsActivity : ComponentActivity() {
    private val settings by lazy { PluginSettingsManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IzinTheme {
                IzinSettingsScreen(
                    settings = settings,
                    onBack = { finish() },
                )
            }
        }
    }
}
