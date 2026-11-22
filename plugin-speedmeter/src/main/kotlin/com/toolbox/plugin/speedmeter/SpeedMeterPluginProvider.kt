package com.toolbox.plugin.speedmeter

import android.content.Context
import com.toolbox.contract.PluginCategory
import com.toolbox.contract.PluginMetadata
import com.toolbox.contract.PluginPermission
import com.toolbox.contract.PluginQuickSetting
import com.toolbox.contract.QuickSettingOption
import com.toolbox.contract.QuickSettingType
import com.toolbox.plugin.PluginProvider

/**
 * Hız Göstergesi eklentisinin Atölye içindeki kaydı.
 */
object SpeedMeterPluginProvider : PluginProvider {

    override val pluginId: String = SpeedMeterKeys.PLUGIN_ID
    override val mainActivityClassName: String = SpeedMeterPluginActivity::class.java.name
    override val settingsActivityClassName: String = SpeedMeterSettingsActivity::class.java.name
    override val commandReceiverClassName: String = SpeedMeterCommandReceiver::class.java.name

    override fun metadata(context: Context): PluginMetadata = PluginMetadata(
        pluginId = pluginId,
        version = 1,
        minHostVersion = 1,
        name = context.getString(R.string.plugin_speedmeter_name),
        description = context.getString(R.string.plugin_speedmeter_desc),
        settingsActivity = settingsActivityClassName,
        mainActivity = mainActivityClassName,
        requiredPermissions = listOf(
            PluginPermission(
                permission = "android.permission.FOREGROUND_SERVICE",
                title = context.getString(R.string.permission_foreground_service_title),
                description = context.getString(R.string.speedmeter_permission_foreground_service_desc),
                isRequired = true
            ),
            PluginPermission(
                permission = "android.permission.FOREGROUND_SERVICE_DATA_SYNC",
                title = context.getString(R.string.permission_foreground_data_sync_title),
                description = context.getString(R.string.permission_foreground_data_sync_desc),
                isRequired = true
            ),
            PluginPermission(
                permission = "android.permission.INTERNET",
                title = context.getString(R.string.permission_network_title),
                description = context.getString(R.string.permission_network_desc),
                isRequired = false
            ),
            PluginPermission(
                permission = "android.permission.RECEIVE_BOOT_COMPLETED",
                title = context.getString(R.string.permission_receive_boot_title),
                description = context.getString(R.string.permission_receive_boot_desc),
                isRequired = true
            ),
            PluginPermission(
                permission = "android.permission.POST_NOTIFICATIONS",
                title = context.getString(R.string.permission_post_notifications_title),
                description = context.getString(R.string.speedmeter_permission_post_notifications_desc),
                isRequired = true
            )
        ),
        category = PluginCategory.NETWORK,
        tags = listOf("internet", "hız", "veri", "kullanım", "traffic"),
        quickSettings = listOf(
            PluginQuickSetting(
                key = SpeedMeterKeys.NETWORK_MODE,
                title = context.getString(R.string.speedmeter_network_mode),
                type = QuickSettingType.CHOICE,
                options = listOf(
                    QuickSettingOption("0", context.getString(R.string.speedmeter_network_mode_total)),
                    QuickSettingOption("1", context.getString(R.string.speedmeter_network_mode_wifi)),
                    QuickSettingOption("2", context.getString(R.string.speedmeter_network_mode_mobile))
                ),
                defaultValue = "0"
            ),
            PluginQuickSetting(
                key = SpeedMeterKeys.UNIT,
                title = context.getString(R.string.speedmeter_unit),
                type = QuickSettingType.CHOICE,
                // Kullanıcı yalnızca MB/s istiyor; otomatik ve KB/s seçenekleri
                // kaldırıldı.
                options = listOf(
                    QuickSettingOption("2", context.getString(R.string.speedmeter_unit_mbps))
                ),
                defaultValue = "2"
            ),
            PluginQuickSetting(
                key = SpeedMeterKeys.AUTOSTART,
                title = context.getString(R.string.speedmeter_autostart),
                type = QuickSettingType.TOGGLE,
                defaultValue = "true"
            )
        ),
        minSdk = 26
    )
}
