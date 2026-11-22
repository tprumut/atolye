package com.toolbox.plugin.keepawake

import android.content.Context
import com.toolbox.contract.PluginCategory
import com.toolbox.contract.PluginMetadata
import com.toolbox.contract.PluginPermission
import com.toolbox.contract.PluginQuickSetting
import com.toolbox.contract.QuickSettingOption
import com.toolbox.contract.QuickSettingType
import com.toolbox.plugin.PluginProvider

/**
 * Ekranı Açık Tut eklentisinin Atölye içindeki kaydı.
 */
object KeepAwakePluginProvider : PluginProvider {

    override val pluginId: String = KeepAwakeKeys.PLUGIN_ID
    override val mainActivityClassName: String = KeepAwakePluginActivity::class.java.name
    override val settingsActivityClassName: String = KeepAwakeSettingsActivity::class.java.name
    override val commandReceiverClassName: String = KeepAwakeCommandReceiver::class.java.name

    override fun metadata(context: Context): PluginMetadata = PluginMetadata(
        pluginId = pluginId,
        version = 1,
        minHostVersion = 1,
        name = context.getString(R.string.plugin_keepawake_name),
        description = context.getString(R.string.plugin_keepawake_desc),
        settingsActivity = settingsActivityClassName,
        mainActivity = mainActivityClassName,
        requiredPermissions = listOf(
            PluginPermission(
                permission = "android.permission.SYSTEM_ALERT_WINDOW",
                title = context.getString(R.string.permission_system_alert_window_title),
                description = context.getString(R.string.permission_system_alert_window_desc),
                isRequired = true
            ),
            PluginPermission(
                permission = "android.permission.WAKE_LOCK",
                title = context.getString(R.string.permission_wake_lock_title),
                description = context.getString(R.string.permission_wake_lock_desc),
                isRequired = true
            ),
            PluginPermission(
                permission = "android.permission.PACKAGE_USAGE_STATS",
                title = context.getString(R.string.permission_usage_stats_title),
                description = context.getString(R.string.permission_usage_stats_desc),
                isRequired = false
            ),
            PluginPermission(
                permission = "android.permission.FOREGROUND_SERVICE",
                title = context.getString(R.string.permission_foreground_service_title),
                description = context.getString(R.string.keepawake_permission_foreground_service_desc),
                isRequired = true
            ),
            PluginPermission(
                permission = "android.permission.FOREGROUND_SERVICE_SPECIAL_USE",
                title = context.getString(R.string.permission_foreground_service_title),
                description = context.getString(R.string.keepawake_permission_foreground_service_desc),
                isRequired = true
            ),
            PluginPermission(
                permission = "android.permission.POST_NOTIFICATIONS",
                title = context.getString(R.string.permission_post_notifications_title),
                description = context.getString(R.string.keepawake_permission_post_notifications_desc),
                isRequired = true
            )
        ),
        category = PluginCategory.DISPLAY,
        tags = listOf("ekran", "açık", "tut", "wake", "keep", "screen", "uyanık"),
        quickSettings = listOf(
            PluginQuickSetting(
                key = KeepAwakeKeys.MODE,
                title = context.getString(R.string.keepawake_mode),
                type = QuickSettingType.CHOICE,
                options = listOf(
                    QuickSettingOption("0", context.getString(R.string.keepawake_mode_manual)),
                    QuickSettingOption("1", context.getString(R.string.keepawake_mode_timer)),
                    QuickSettingOption("2", context.getString(R.string.keepawake_mode_infinite)),
                    QuickSettingOption("3", context.getString(R.string.keepawake_mode_app_based))
                ),
                defaultValue = "1"
            ),
            PluginQuickSetting(
                key = KeepAwakeKeys.TIMER_DURATION,
                title = context.getString(R.string.keepawake_timer_duration),
                type = QuickSettingType.CHOICE,
                options = listOf(
                    QuickSettingOption("15", context.getString(R.string.keepawake_timer_15min)),
                    QuickSettingOption("30", context.getString(R.string.keepawake_timer_30min)),
                    QuickSettingOption("60", context.getString(R.string.keepawake_timer_1hour)),
                    QuickSettingOption("120", context.getString(R.string.keepawake_timer_2hour))
                ),
                defaultValue = "15"
            ),
            PluginQuickSetting(
                key = KeepAwakeKeys.SHOW_NOTIFICATION,
                title = context.getString(R.string.keepawake_show_notification),
                type = QuickSettingType.TOGGLE,
                defaultValue = "true"
            )
        ),
        minSdk = 26
    )
}
