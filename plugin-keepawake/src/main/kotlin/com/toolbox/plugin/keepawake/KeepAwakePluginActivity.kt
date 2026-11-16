package com.toolbox.plugin.keepawake

import androidx.compose.runtime.Composable
import com.toolbox.contract.PluginCategory
import com.toolbox.contract.PluginMetadata
import com.toolbox.contract.PluginPermission
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
        PluginMetadata(
            pluginId = KeepAwakeKeys.PLUGIN_ID,
            version = 1,
            minHostVersion = 1,
            name = getString(R.string.plugin_keepawake_name),
            description = getString(R.string.plugin_keepawake_desc),
            settingsActivity = KeepAwakeSettingsActivity::class.java.name,
            mainActivity = KeepAwakePluginActivity::class.java.name,
            requiredPermissions = listOf(
                PluginPermission(
                    permission = "android.permission.SYSTEM_ALERT_WINDOW",
                    title = getString(R.string.permission_system_alert_window_title),
                    description = getString(R.string.permission_system_alert_window_desc),
                    isRequired = true
                ),
                PluginPermission(
                    permission = "android.permission.WAKE_LOCK",
                    title = getString(R.string.permission_wake_lock_title),
                    description = getString(R.string.permission_wake_lock_desc),
                    isRequired = true
                ),
                PluginPermission(
                    permission = "android.permission.PACKAGE_USAGE_STATS",
                    title = getString(R.string.permission_usage_stats_title),
                    description = getString(R.string.permission_usage_stats_desc),
                    isRequired = false
                ),
                PluginPermission(
                    permission = "android.permission.FOREGROUND_SERVICE",
                    title = getString(R.string.permission_foreground_service_title),
                    description = getString(R.string.permission_foreground_service_desc),
                    isRequired = true
                ),
                PluginPermission(
                    permission = "android.permission.FOREGROUND_SERVICE_SPECIAL_USE",
                    title = getString(R.string.permission_foreground_service_title),
                    description = getString(R.string.permission_foreground_service_desc),
                    isRequired = true
                ),
                PluginPermission(
                    permission = "android.permission.POST_NOTIFICATIONS",
                    title = getString(R.string.permission_post_notifications_title),
                    description = getString(R.string.permission_post_notifications_desc),
                    isRequired = true
                )
            ),
            category = PluginCategory.DISPLAY,
            tags = listOf("ekran", "açık", "tut", "wake", "keep", "screen", "uyanık"),
            downloadUrl = "https://github.com/tprumut/atolye/releases/latest/download/plugin-keepawake-debug.apk",
            minSdk = 26
        )
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
