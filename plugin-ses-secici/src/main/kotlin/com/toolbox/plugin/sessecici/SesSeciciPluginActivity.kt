package com.toolbox.plugin.sessecici

import android.content.Intent
import androidx.compose.runtime.Composable
import com.toolbox.contract.PluginCategory
import com.toolbox.contract.PluginMetadata
import com.toolbox.contract.PluginPermission
import com.toolbox.plugin.BasePluginActivity
import com.toolbox.plugin.PluginSettingsManager

class SesSeciciPluginActivity : BasePluginActivity() {
    private val settings by lazy { PluginSettingsManager(this) }

    override val pluginMetadata: PluginMetadata by lazy {
        PluginMetadata(
            pluginId = SesSeciciKeys.PLUGIN_ID,
            version = 1,
            minHostVersion = 1,
            name = getString(R.string.plugin_sessecici_name),
            description = getString(R.string.plugin_sessecici_desc),
            settingsActivity = SesSeciciSettingsActivity::class.java.name,
            mainActivity = SesSeciciPluginActivity::class.java.name,
            requiredPermissions = listOf(
                PluginPermission("android.permission.MODIFY_AUDIO_SETTINGS", getString(R.string.permission_modify_audio_title), getString(R.string.permission_modify_audio_desc), true),
                PluginPermission("android.permission.POST_NOTIFICATIONS", getString(R.string.permission_post_notifications_title), getString(R.string.permission_post_notifications_desc), false),
                PluginPermission("android.permission.BLUETOOTH_CONNECT", getString(R.string.permission_bluetooth_connect_title), getString(R.string.permission_bluetooth_connect_desc), false)
            ),
            category = PluginCategory.MEDIA,
            tags = listOf("ses", "çıkış", "bluetooth", "kulaklık", "hoparlör", "usb"),
            accentColor = "#00897B",
            downloadUrl = "https://github.com/tprumut/atolye/releases/latest/download/plugin-ses-secici-debug.apk",
            minSdk = 26
        )
    }

    @Composable
    override fun PluginContent() {
        SesSeciciTheme { SesSeciciScreen(settings = settings, onOpenSettings = { openSettings() }) }
    }

    override fun openSettings() { startActivity(Intent(this, SesSeciciSettingsActivity::class.java)) }
}
