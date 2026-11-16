package com.toolbox.plugin.uykucu

import android.content.Context
import com.toolbox.plugin.BasePluginCommandReceiver
import com.toolbox.plugin.PluginSettingsManager
import com.toolbox.plugin.PluginStatusPublisher

class UykucuCommandReceiver : BasePluginCommandReceiver() {
    override val pluginId: String = UykucuKeys.PLUGIN_ID

    override fun onStart(context: Context) {
        val settings = PluginSettingsManager(context)
        val running = settings.getBoolean(pluginId, UykucuKeys.TIMER_RUNNING, false)
        val end = settings.getLong(pluginId, UykucuKeys.TIMER_END_TIME, 0L)
        if (running && end > System.currentTimeMillis()) {
            UykucuTimerService.start(context, UykucuKeys.DEFAULT_DURATION_MINUTES) // restore path handled in service
            return
        }
        val remember = settings.getBoolean(pluginId, UykucuKeys.REMEMBER_LAST_DURATION, true)
        val minutes = if (remember) settings.getInt(pluginId, UykucuKeys.LAST_DURATION_MINUTES, UykucuKeys.DEFAULT_DURATION_MINUTES)
        else UykucuKeys.DEFAULT_DURATION_MINUTES
        UykucuTimerService.start(context, minutes.coerceAtLeast(1))
    }

    override fun onStop(context: Context) {
        UykucuTimerService.cancel(context)
    }

    override fun onGetState(context: Context) {
        publish(context)
    }

    override fun onSetQuickSetting(context: Context, key: String, value: String) {
        val settings = PluginSettingsManager(context)
        val b = value == "true" || value == "1"
        when (key) {
            UykucuKeys.CLOSE_APPS_ENABLED, "close_apps_enabled" -> settings.putBoolean(pluginId, UykucuKeys.CLOSE_APPS_ENABLED, b)
            UykucuKeys.STOP_MEDIA_ENABLED, "stop_media_enabled" -> settings.putBoolean(pluginId, UykucuKeys.STOP_MEDIA_ENABLED, b)
            UykucuKeys.REMEMBER_LAST_DURATION, "remember_last_duration" -> settings.putBoolean(pluginId, UykucuKeys.REMEMBER_LAST_DURATION, b)
            else -> settings.putString(pluginId, key, value)
        }
        publish(context)
    }

    companion object {
        fun publish(context: Context) {
            val settings = PluginSettingsManager(context)
            val running = settings.getBoolean(UykucuKeys.PLUGIN_ID, UykucuKeys.TIMER_RUNNING, false)
            val end = settings.getLong(UykucuKeys.PLUGIN_ID, UykucuKeys.TIMER_END_TIME, 0L)
            val remaining = if (running && end > 0L) (end - System.currentTimeMillis()).coerceAtLeast(0L) else -1L
            val qs = mapOf(
                UykucuKeys.STOP_MEDIA_ENABLED to settings.getBoolean(UykucuKeys.PLUGIN_ID, UykucuKeys.STOP_MEDIA_ENABLED, true).toString(),
                UykucuKeys.CLOSE_APPS_ENABLED to settings.getBoolean(UykucuKeys.PLUGIN_ID, UykucuKeys.CLOSE_APPS_ENABLED, true).toString(),
                UykucuKeys.REMEMBER_LAST_DURATION to settings.getBoolean(UykucuKeys.PLUGIN_ID, UykucuKeys.REMEMBER_LAST_DURATION, true).toString()
            )
            PluginStatusPublisher.publish(
                context = context,
                pluginId = UykucuKeys.PLUGIN_ID,
                running = running && end > System.currentTimeMillis(),
                quickSettings = qs,
                remainingMs = remaining,
                statusText = null
            )
        }
    }
}
