package com.toolbox.plugin.sessecici

import android.content.Context
import com.toolbox.contract.PluginCategory
import com.toolbox.contract.PluginMetadata
import com.toolbox.contract.PluginPermission
import com.toolbox.contract.PluginQuickSetting
import com.toolbox.contract.QuickSettingType
import com.toolbox.plugin.PluginProvider

/**
 * Ses Seçici eklentisinin Atölye içindeki kaydı.
 */
object SesSeciciPluginProvider : PluginProvider {

    override val pluginId: String = SesSeciciKeys.PLUGIN_ID
    override val mainActivityClassName: String = SesSeciciPluginActivity::class.java.name
    override val settingsActivityClassName: String = SesSeciciSettingsActivity::class.java.name
    override val commandReceiverClassName: String = SesSeciciCommandReceiver::class.java.name

    override fun metadata(context: Context): PluginMetadata = PluginMetadata(
        pluginId = pluginId,
        version = 1,
        minHostVersion = 1,
        name = context.getString(R.string.plugin_sessecici_name),
        description = context.getString(R.string.plugin_sessecici_desc),
        settingsActivity = settingsActivityClassName,
        mainActivity = mainActivityClassName,
        requiredPermissions = listOf(
            PluginPermission(
                permission = "android.permission.MODIFY_AUDIO_SETTINGS",
                title = context.getString(R.string.permission_modify_audio_title),
                description = context.getString(R.string.permission_modify_audio_desc),
                isRequired = true
            ),
            PluginPermission(
                permission = "android.permission.POST_NOTIFICATIONS",
                title = context.getString(R.string.permission_post_notifications_title),
                description = context.getString(R.string.sessecici_permission_post_notifications_desc),
                isRequired = false
            ),
            PluginPermission(
                permission = "android.permission.BLUETOOTH_CONNECT",
                title = context.getString(R.string.permission_bluetooth_connect_title),
                description = context.getString(R.string.permission_bluetooth_connect_desc),
                isRequired = false
            )
        ),
        category = PluginCategory.MEDIA,
        tags = listOf("ses", "çıkış", "bluetooth", "kulaklık", "hoparlör", "usb"),
        accentColor = "#00897B",
        quickSettings = listOf(
            PluginQuickSetting(
                key = SesSeciciKeys.NOTIFICATION_ENABLED,
                title = context.getString(R.string.notification_enabled),
                type = QuickSettingType.TOGGLE,
                defaultValue = "true"
            ),
            PluginQuickSetting(
                key = SesSeciciKeys.AUTO_APPLY_ON_CONNECT,
                title = context.getString(R.string.auto_apply_on_connect),
                type = QuickSettingType.TOGGLE,
                defaultValue = "true"
            )
        ),
        minSdk = 26
    )
}
