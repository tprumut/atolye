package com.toolbox.plugin.speedmeter

import android.content.Context
import com.toolbox.plugin.BasePluginCommandReceiver
import com.toolbox.plugin.PluginSettingsManager
import com.toolbox.plugin.PluginStatusPublisher

class SpeedMeterCommandReceiver : BasePluginCommandReceiver() {
    override val pluginId: String = SpeedMeterKeys.PLUGIN_ID

    override fun onStart(context: Context) {
        SpeedMeterService.start(context)
    }

    override fun onStop(context: Context) {
        SpeedMeterService.stop(context)
    }

    override fun onGetState(context: Context) {
        publishCurrent(context)
    }

    override fun onSetQuickSetting(context: Context, key: String, value: String) {
        val settings = PluginSettingsManager(context)
        when (key) {
            SpeedMeterKeys.NETWORK_MODE, "network_mode" -> {
                value.toIntOrNull()?.let { settings.putInt(pluginId, SpeedMeterKeys.NETWORK_MODE, it) }
            }
            SpeedMeterKeys.UNIT, "unit" -> {
                value.toIntOrNull()?.let { settings.putInt(pluginId, SpeedMeterKeys.UNIT, it) }
            }
            SpeedMeterKeys.AUTOSTART, "autostart", "start_on_boot" -> {
                val b = value == "true" || value == "1"
                settings.putBoolean(pluginId, SpeedMeterKeys.AUTOSTART, b)
                SpeedMeterBootReceiver.setAutostart(context, b)
            }
            else -> settings.putString(pluginId, key, value)
        }
        // Ayarlar servisin sonraki tick'inde okunur; durumu da yayınla
        publishCurrent(context)
    }

    companion object {
        fun publishCurrent(context: Context, runningOverride: Boolean? = null, speedText: String? = null) {
            val settings = PluginSettingsManager(context)
            val running = runningOverride ?: settings.getBoolean(SpeedMeterKeys.PLUGIN_ID, SpeedMeterKeys.SERVICE_RUNNING, false)
            val qs = mapOf(
                SpeedMeterKeys.NETWORK_MODE to settings.getInt(SpeedMeterKeys.PLUGIN_ID, SpeedMeterKeys.NETWORK_MODE, 0).toString(),
                SpeedMeterKeys.UNIT to settings.getInt(SpeedMeterKeys.PLUGIN_ID, SpeedMeterKeys.UNIT, 2).toString(),
                SpeedMeterKeys.AUTOSTART to settings.getBoolean(SpeedMeterKeys.PLUGIN_ID, SpeedMeterKeys.AUTOSTART, true).toString()
            )
            PluginStatusPublisher.publish(
                context = context,
                pluginId = SpeedMeterKeys.PLUGIN_ID,
                running = running,
                quickSettings = qs,
                statusText = speedText
            )
        }
    }
}
