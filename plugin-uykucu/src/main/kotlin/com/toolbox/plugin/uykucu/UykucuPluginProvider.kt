package com.toolbox.plugin.uykucu

import android.content.Context
import com.toolbox.contract.PluginCategory
import com.toolbox.contract.PluginMetadata
import com.toolbox.contract.PluginPermission
import com.toolbox.contract.PluginQuickSetting
import com.toolbox.contract.QuickSettingType
import com.toolbox.plugin.PluginProvider

/**
 * Uykucu eklentisinin Atölye içindeki kaydı.
 */
object UykucuPluginProvider : PluginProvider {

    override val pluginId: String = UykucuKeys.PLUGIN_ID
    override val mainActivityClassName: String = UykucuPluginActivity::class.java.name
    override val settingsActivityClassName: String = UykucuSettingsActivity::class.java.name
    override val commandReceiverClassName: String = UykucuCommandReceiver::class.java.name

    override fun metadata(context: Context): PluginMetadata = PluginMetadata(
        pluginId = pluginId,
        version = 1,
        minHostVersion = 1,
        name = context.getString(R.string.plugin_uykucu_name),
        description = context.getString(R.string.plugin_uykucu_desc),
        settingsActivity = settingsActivityClassName,
        mainActivity = mainActivityClassName,
        requiredPermissions = listOf(
            PluginPermission(
                permission = "android.permission.BIND_ACCESSIBILITY_SERVICE",
                title = context.getString(R.string.permission_accessibility_title),
                description = context.getString(R.string.permission_accessibility_desc),
                isRequired = true
            ),
            PluginPermission(
                permission = "android.permission.FOREGROUND_SERVICE",
                title = context.getString(R.string.permission_foreground_service_title),
                description = context.getString(R.string.uykucu_permission_foreground_service_desc),
                isRequired = true
            ),
            PluginPermission(
                permission = "android.permission.POST_NOTIFICATIONS",
                title = context.getString(R.string.permission_post_notifications_title),
                description = context.getString(R.string.uykucu_permission_post_notifications_desc),
                isRequired = true
            ),
            PluginPermission(
                permission = "android.permission.SCHEDULE_EXACT_ALARM",
                title = context.getString(R.string.permission_exact_alarm_title),
                description = context.getString(R.string.permission_exact_alarm_desc),
                isRequired = true
            )
        ),
        category = PluginCategory.TOOLS,
        tags = listOf("uyku", "zamanlayıcı", "timer", "sleep", "kilit"),
        accentColor = "#5C6BC0",
        quickSettings = listOf(
            PluginQuickSetting(
                key = UykucuKeys.STOP_MEDIA_ENABLED,
                title = context.getString(R.string.uykucu_stop_media),
                type = QuickSettingType.TOGGLE,
                defaultValue = "true"
            ),
            PluginQuickSetting(
                key = UykucuKeys.CLOSE_APPS_ENABLED,
                title = context.getString(R.string.uykucu_close_apps),
                type = QuickSettingType.TOGGLE,
                defaultValue = "true"
            ),
            PluginQuickSetting(
                key = UykucuKeys.REMEMBER_LAST_DURATION,
                title = context.getString(R.string.uykucu_remember_last),
                type = QuickSettingType.TOGGLE,
                defaultValue = "true"
            )
        ),
        minSdk = 26
    )
}
