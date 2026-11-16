package com.toolbox.plugin.uykucu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.toolbox.plugin.PluginSettingsManager

class UykucuSettingsActivity : ComponentActivity() {

    private val settings by lazy { PluginSettingsManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UykucuTheme {
                UykucuSettingsScreen(
                    settings = settings,
                    onBack = { finish() }
                )
            }
        }
    }
}
