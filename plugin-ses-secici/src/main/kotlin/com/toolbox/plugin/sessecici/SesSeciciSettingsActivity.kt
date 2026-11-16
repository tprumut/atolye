package com.toolbox.plugin.sessecici

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.toolbox.plugin.PluginSettingsManager

class SesSeciciSettingsActivity : ComponentActivity() {
    private val settings by lazy { PluginSettingsManager(this) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { SesSeciciTheme { SesSeciciSettingsScreen(settings = settings, onBack = { finish() }) } }
    }
}
