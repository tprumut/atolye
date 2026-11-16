package com.toolbox.plugin.uykucu

import androidx.compose.runtime.Composable
import com.toolbox.contract.PluginCategory
import com.toolbox.contract.PluginMetadata
import com.toolbox.contract.PluginPermission
import com.toolbox.plugin.BasePluginActivity
import com.toolbox.plugin.PluginSettingsManager

class UykucuPluginActivity : BasePluginActivity() {

    private val settings by lazy { PluginSettingsManager(this) }

    override val pluginMetadata: PluginMetadata by lazy {
        PluginMetadata(
            pluginId = UykucuKeys.PLUGIN_ID,
            version = 1,
            minHostVersion = 1,
            name = getString(R.string.plugin_uykucu_name),
            description = getString(R.string.plugin_uykucu_desc),
            settingsActivity = UykucuSettingsActivity::class.java.name,
            mainActivity = UykucuPluginActivity::class.java.name,
            requiredPermissions = listOf(
                PluginPermission(
                    permission = "android.permission.BIND_ACCESSIBILITY_SERVICE",
                    title = getString(R.string.permission_accessibility_title),
                    description = getString(R.string.permission_accessibility_desc),
                    isRequired = true
                ),
                PluginPermission(
                    permission = "android.permission.FOREGROUND_SERVICE",
                    title = getString(R.string.permission_foreground_service_title),
                    description = getString(R.string.permission_foreground_service_desc),
                    isRequired = true
                ),
                PluginPermission(
                    permission = "android.permission.POST_NOTIFICATIONS",
                    title = getString(R.string.permission_post_notifications_title),
                    description = getString(R.string.permission_post_notifications_desc),
                    isRequired = true
                ),
                PluginPermission(
                    permission = "android.permission.SCHEDULE_EXACT_ALARM",
                    title = getString(R.string.permission_exact_alarm_title),
                    description = getString(R.string.permission_exact_alarm_desc),
                    isRequired = true
                )
            ),
            category = PluginCategory.TOOLS,
            tags = listOf("uyku", "zamanlayıcı", "timer", "sleep", "kilit"),
            accentColor = "#5C6BC0",
            downloadUrl = "https://github.com/tprumut/atolye/releases/latest/download/plugin-uykucu-debug.apk",
            minSdk = 26
        )
    }

    @Composable
    override fun PluginContent() {
        UykucuTheme {
            UykucuScreen(
                settings = settings,
                onOpenSettings = { openSettings() }
            )
        }
    }

    override fun openSettings() {
        startActivity(android.content.Intent(this, UykucuSettingsActivity::class.java))
    }
}
