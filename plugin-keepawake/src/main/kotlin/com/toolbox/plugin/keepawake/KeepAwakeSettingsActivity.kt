package com.toolbox.plugin.keepawake

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.toolbox.plugin.PluginSettingsManager

/**
 * Keep Awake ayarlar ekranı.
 *
 * Mod seçimi, uygulama listesi, bildirim ayarları ve izin durumları
 * burada yönetilir. Host uygulaması tarafından ayrı Activity olarak açılır.
 */
class KeepAwakeSettingsActivity : ComponentActivity() {

    private val settings by lazy { PluginSettingsManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            KeepAwakeTheme {
                KeepAwakeSettingsScreen(
                    settings = settings,
                    onBack = { finish() }
                )
            }
        }
    }
}
