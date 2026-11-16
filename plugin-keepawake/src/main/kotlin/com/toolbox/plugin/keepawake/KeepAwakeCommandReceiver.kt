package com.toolbox.plugin.keepawake

import android.content.Context
import android.content.Intent
import com.toolbox.contract.PluginContract
import com.toolbox.plugin.BasePluginCommandReceiver
import com.toolbox.plugin.PluginSettingsManager
import com.toolbox.plugin.PluginStatusPublisher

class KeepAwakeCommandReceiver : BasePluginCommandReceiver() {
    override val pluginId: String = KeepAwakeKeys.PLUGIN_ID

    override fun onStart(context: Context) {
        val settings = PluginSettingsManager(context)
        val mode = settings.getInt(pluginId, KeepAwakeKeys.MODE, KeepAwakeKeys.MODE_TIMER)
        // Uygulama modunda kullanım istatistiği yoksa izin yönlendirmesi
        if (mode == KeepAwakeKeys.MODE_APP_BASED && !KeepAwakePermissions.hasUsageStats(context)) {
            KeepAwakePermissions.openUsageStatsSettings(context)
            KeepAwakeOverlayService.publishCurrentStatus(context)
            return
        }
        if (!KeepAwakePermissions.canDrawOverlays(context)) {
            KeepAwakePermissions.openOverlaySettings(context)
            KeepAwakeOverlayService.publishCurrentStatus(context)
            return
        }
        val duration = settings.getInt(pluginId, KeepAwakeKeys.TIMER_DURATION, 15)
        KeepAwakeOverlayService.start(context, mode, duration)
    }

    override fun onStop(context: Context) {
        KeepAwakeOverlayService.stop(context)
    }

    override fun onGetState(context: Context) {
        KeepAwakeOverlayService.publishCurrentStatus(context)
    }

    override fun onSetQuickSetting(context: Context, key: String, value: String) {
        val settings = PluginSettingsManager(context)
        when (key) {
            KeepAwakeKeys.MODE, "mode" -> {
                val v = value.toIntOrNull() ?: return
                settings.putInt(pluginId, KeepAwakeKeys.MODE, v)
            }
            KeepAwakeKeys.TIMER_DURATION, "timer_duration", "duration" -> {
                val v = value.toIntOrNull() ?: return
                settings.putInt(pluginId, KeepAwakeKeys.TIMER_DURATION, v)
            }
            KeepAwakeKeys.SHOW_NOTIFICATION, "show_notification", "notification" -> {
                val b = value == "true" || value == "1"
                settings.putBoolean(pluginId, KeepAwakeKeys.SHOW_NOTIFICATION, b)
            }
            else -> {
                settings.putString(pluginId, key, value)
            }
        }
        // Çalışıyorsa servise anında uygulat
        KeepAwakeOverlayService.applySettings(context)
        KeepAwakeOverlayService.publishCurrentStatus(context)
    }
}
