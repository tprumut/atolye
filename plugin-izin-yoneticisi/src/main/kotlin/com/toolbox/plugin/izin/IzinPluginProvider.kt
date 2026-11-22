package com.toolbox.plugin.izin

import android.content.Context
import com.toolbox.contract.PluginCategory
import com.toolbox.contract.PluginMetadata
import com.toolbox.contract.PluginPermission
import com.toolbox.plugin.PluginProvider

object IzinPluginProvider : PluginProvider {

    override val pluginId: String = IzinKeys.PLUGIN_ID
    override val mainActivityClassName: String = IzinPluginActivity::class.java.name
    override val settingsActivityClassName: String = IzinSettingsActivity::class.java.name
    override val commandReceiverClassName: String = IzinCommandReceiver::class.java.name

    override fun metadata(context: Context): PluginMetadata = PluginMetadata(
        pluginId = pluginId,
        version = 1,
        minHostVersion = 1,
        name = context.getString(R.string.plugin_izin_name),
        description = context.getString(R.string.plugin_izin_desc),
        settingsActivity = settingsActivityClassName,
        mainActivity = mainActivityClassName,
        commandReceiver = commandReceiverClassName,
        iconResName = "ic_izin",
        requiredPermissions = listOf(
            PluginPermission(
                permission = "android.permission.QUERY_ALL_PACKAGES",
                title = context.getString(R.string.izin_permission_query_title),
                description = context.getString(R.string.izin_permission_query_desc),
                isRequired = true,
            )
        ),
        category = PluginCategory.TOOLS,
        tags = listOf("izin", "permission", "güvenlik", "uygulama"),
        accentColor = "#3F6FB5",
        minSdk = 26,
    )
}
