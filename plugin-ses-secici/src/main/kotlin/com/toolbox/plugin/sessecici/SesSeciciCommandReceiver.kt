package com.toolbox.plugin.sessecici

import android.content.Context
import com.toolbox.plugin.BasePluginCommandReceiver
import com.toolbox.plugin.PluginSettingsManager
import com.toolbox.plugin.PluginStatusPublisher

class SesSeciciCommandReceiver : BasePluginCommandReceiver() {
    override val pluginId: String = SesSeciciKeys.PLUGIN_ID

    override fun onStart(context: Context) {
        SesSeciciDeviceService.start(context)
        publish(context, running = true)
    }

    override fun onStop(context: Context) {
        try {
            context.stopService(android.content.Intent(context, SesSeciciDeviceService::class.java))
        } catch (_: Exception) {}
        publish(context, running = false)
    }

    override fun onGetState(context: Context) {
        publish(context, running = null)
    }

    override fun onSetQuickSetting(context: Context, key: String, value: String) {
        val settings = PluginSettingsManager(context)
        val b = value == "true" || value == "1"
        when (key) {
            SesSeciciKeys.NOTIFICATION_ENABLED, "notification_enabled" -> settings.putBoolean(pluginId, SesSeciciKeys.NOTIFICATION_ENABLED, b)
            SesSeciciKeys.AUTO_APPLY_ON_CONNECT, "auto_apply_on_connect" -> settings.putBoolean(pluginId, SesSeciciKeys.AUTO_APPLY_ON_CONNECT, b)
            SesSeciciKeys.EXPERIMENTAL_APP_AUDIO, "experimental_app_audio" -> settings.putBoolean(pluginId, SesSeciciKeys.EXPERIMENTAL_APP_AUDIO, b)
            else -> settings.putString(pluginId, key, value)
        }
        SesSeciciDeviceService.start(context)
        publish(context, running = true)
    }

    companion object {
        fun publish(context: Context, running: Boolean?) {
            val settings = PluginSettingsManager(context)
            val qs = mapOf(
                SesSeciciKeys.AUTO_APPLY_ON_CONNECT to settings.getBoolean(SesSeciciKeys.PLUGIN_ID, SesSeciciKeys.AUTO_APPLY_ON_CONNECT, true).toString(),
                SesSeciciKeys.NOTIFICATION_ENABLED to settings.getBoolean(SesSeciciKeys.PLUGIN_ID, SesSeciciKeys.NOTIFICATION_ENABLED, true).toString(),
                SesSeciciKeys.EXPERIMENTAL_APP_AUDIO to settings.getBoolean(SesSeciciKeys.PLUGIN_ID, SesSeciciKeys.EXPERIMENTAL_APP_AUDIO, false).toString()
            )
            val isRunning = running ?: true
            val text = try {
                val devices = SesSeciciAudio.listOutputDevices(context, settings)
                devices.firstOrNull { it.isCurrent }?.name ?: devices.firstOrNull()?.name
            } catch (_: Exception) { null }
            PluginStatusPublisher.publish(
                context = context,
                pluginId = SesSeciciKeys.PLUGIN_ID,
                running = isRunning,
                quickSettings = qs,
                statusText = text
            )
        }
    }
}
