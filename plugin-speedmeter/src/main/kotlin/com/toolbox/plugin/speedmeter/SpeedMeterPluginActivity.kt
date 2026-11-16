package com.toolbox.plugin.speedmeter

import androidx.compose.runtime.Composable
import com.toolbox.contract.PluginCategory
import com.toolbox.contract.PluginMetadata
import com.toolbox.contract.PluginPermission
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
        PluginMetadata(
            pluginId = SpeedMeterKeys.PLUGIN_ID,
            version = 1,
            minHostVersion = 1,
            name = getString(R.string.plugin_speedmeter_name),
            description = getString(R.string.plugin_speedmeter_desc),
            settingsActivity = SpeedMeterSettingsActivity::class.java.name,
            mainActivity = SpeedMeterPluginActivity::class.java.name,
            requiredPermissions = listOf(
                PluginPermission(
                    permission = "android.permission.FOREGROUND_SERVICE",
                    title = getString(R.string.permission_foreground_service_title),
                    description = getString(R.string.permission_foreground_service_desc),
                    isRequired = true
                ),
                PluginPermission(
                    permission = "android.permission.FOREGROUND_SERVICE_DATA_SYNC",
                    title = getString(R.string.permission_foreground_data_sync_title),
                    description = getString(R.string.permission_foreground_data_sync_desc),
                    isRequired = true
                ),
                PluginPermission(
                    permission = "android.permission.INTERNET",
                    title = getString(R.string.permission_network_title),
                    description = getString(R.string.permission_network_desc),
                    isRequired = false
                ),
                PluginPermission(
                    permission = "android.permission.RECEIVE_BOOT_COMPLETED",
                    title = getString(R.string.permission_receive_boot_title),
                    description = getString(R.string.permission_receive_boot_desc),
                    isRequired = true
                ),
                PluginPermission(
                    permission = "android.permission.POST_NOTIFICATIONS",
                    title = getString(R.string.permission_post_notifications_title),
                    description = getString(R.string.permission_post_notifications_desc),
                    isRequired = true
                )
            ),
            category = PluginCategory.NETWORK,
            tags = listOf("internet", "hız", "veri", "kullanım", "traffic"),
            downloadUrl = "https://github.com/toolbox/plugins/releases/latest/download/speedmeter.apk",
            minSdk = 26
        )
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
