package com.toolbox.plugin.autoscroll

import android.content.Context
import com.toolbox.contract.PluginCategory
import com.toolbox.contract.PluginMetadata
import com.toolbox.contract.PluginPermission
import com.toolbox.contract.PluginQuickSetting
import com.toolbox.contract.QuickSettingOption
import com.toolbox.contract.QuickSettingType
import com.toolbox.plugin.PluginProvider

object AutoScrollPluginProvider : PluginProvider {

    override val pluginId: String = AutoScrollKeys.PLUGIN_ID
    override val mainActivityClassName: String = AutoScrollPluginActivity::class.java.name
    override val settingsActivityClassName: String = AutoScrollSettingsActivity::class.java.name
    override val commandReceiverClassName: String = AutoScrollCommandReceiver::class.java.name

    override fun metadata(context: Context): PluginMetadata = PluginMetadata(
        pluginId = pluginId,
        version = 1,
        minHostVersion = 1,
        name = context.getString(R.string.plugin_autoscroll_name),
        description = context.getString(R.string.plugin_autoscroll_desc),
        settingsActivity = settingsActivityClassName,
        mainActivity = mainActivityClassName,
        commandReceiver = commandReceiverClassName,
        iconResName = "ic_autoscroll",
        requiredPermissions = listOf(
            PluginPermission(
                permission = "android.permission.BIND_ACCESSIBILITY_SERVICE",
                title = context.getString(R.string.auto_permission_accessibility_title),
                description = context.getString(R.string.auto_permission_accessibility_desc),
                isRequired = true,
            ),
            PluginPermission(
                permission = "android.permission.FOREGROUND_SERVICE",
                title = context.getString(R.string.auto_permission_foreground_title),
                description = context.getString(R.string.auto_permission_foreground_desc),
                isRequired = true,
            ),
            PluginPermission(
                permission = "android.permission.POST_NOTIFICATIONS",
                title = context.getString(R.string.auto_permission_post_notifications_title),
                description = context.getString(R.string.auto_permission_post_notifications_desc),
                isRequired = true,
            ),
            PluginPermission(
                permission = "android.permission.SYSTEM_ALERT_WINDOW",
                title = context.getString(R.string.auto_permission_overlay_title),
                description = context.getString(R.string.auto_permission_overlay_desc),
                isRequired = false,
            ),
        ),
        category = PluginCategory.TOOLS,
        tags = listOf("kaydırma", "scroll", "otomasyon", "akış", "feed"),
        accentColor = "#C4553D",
        quickSettings = listOf(
            PluginQuickSetting(
                key = AutoScrollKeys.MODE,
                title = context.getString(R.string.auto_quick_mode),
                type = QuickSettingType.CHOICE,
                options = listOf(
                    QuickSettingOption(
                        AutoScrollKeys.MODE_TIMED,
                        context.getString(R.string.auto_mode_timed),
                    ),
                    QuickSettingOption(
                        AutoScrollKeys.MODE_CONTINUOUS,
                        context.getString(R.string.auto_mode_continuous),
                    ),
                ),
                defaultValue = AutoScrollKeys.DEFAULT_MODE,
            ),
            PluginQuickSetting(
                key = AutoScrollKeys.INTERVAL_SECONDS,
                title = context.getString(R.string.auto_quick_interval),
                type = QuickSettingType.CHOICE,
                options = AutoScrollKeys.INTERVAL_OPTIONS.map { seconds ->
                    QuickSettingOption(
                        seconds.toString(),
                        context.getString(R.string.auto_seconds, seconds),
                    )
                },
                defaultValue = AutoScrollKeys.DEFAULT_INTERVAL_SECONDS.toString(),
            ),
            PluginQuickSetting(
                key = AutoScrollKeys.DIRECTION,
                title = context.getString(R.string.auto_quick_direction),
                type = QuickSettingType.CHOICE,
                options = listOf(
                    QuickSettingOption(
                        AutoScrollKeys.DIRECTION_UP,
                        context.getString(R.string.auto_direction_label_up),
                    ),
                    QuickSettingOption(
                        AutoScrollKeys.DIRECTION_DOWN,
                        context.getString(R.string.auto_direction_label_down),
                    ),
                ),
                defaultValue = AutoScrollKeys.DEFAULT_DIRECTION,
            ),
            PluginQuickSetting(
                key = AutoScrollKeys.TARGET,
                title = context.getString(R.string.auto_quick_target),
                type = QuickSettingType.CHOICE,
                options = listOf(
                    QuickSettingOption(
                        AutoScrollKeys.TARGET_ACTIVE,
                        context.getString(R.string.auto_target_active),
                    ),
                    QuickSettingOption(
                        AutoScrollKeys.TARGET_PRIMARY,
                        context.getString(R.string.auto_target_primary),
                    ),
                    QuickSettingOption(
                        AutoScrollKeys.TARGET_SECONDARY,
                        context.getString(R.string.auto_target_secondary),
                    ),
                ),
                defaultValue = AutoScrollKeys.DEFAULT_TARGET,
            ),
            PluginQuickSetting(
                key = AutoScrollKeys.OVERLAY_VISIBLE,
                title = context.getString(R.string.auto_quick_overlay),
                type = QuickSettingType.TOGGLE,
                defaultValue = AutoScrollKeys.DEFAULT_OVERLAY_VISIBLE.toString(),
            ),
        ),
        minSdk = 26,
    )
}
