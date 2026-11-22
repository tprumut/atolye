package com.toolbox.plugin.autoscroll

import android.content.Context
import com.toolbox.plugin.BasePluginCommandReceiver
import com.toolbox.plugin.PluginSettingsManager

class AutoScrollCommandReceiver : BasePluginCommandReceiver() {
    override val pluginId: String = AutoScrollKeys.PLUGIN_ID

    override fun onStart(context: Context) {
        AutoScrollService.start(context)
    }

    override fun onStop(context: Context) {
        AutoScrollService.stop(context)
    }

    override fun onGetState(context: Context) {
        AutoScrollService.publishState(context)
    }

    override fun onSetQuickSetting(context: Context, key: String, value: String) {
        val settings = PluginSettingsManager(context)
        when (key) {
            AutoScrollKeys.MODE -> {
                val mode = if (value == AutoScrollKeys.MODE_CONTINUOUS) {
                    AutoScrollKeys.MODE_CONTINUOUS
                } else {
                    AutoScrollKeys.MODE_TIMED
                }
                settings.putString(AutoScrollKeys.PLUGIN_ID, AutoScrollKeys.MODE, mode)
            }
            AutoScrollKeys.DIRECTION -> {
                val direction = if (value == AutoScrollKeys.DIRECTION_DOWN) {
                    AutoScrollKeys.DIRECTION_DOWN
                } else {
                    AutoScrollKeys.DIRECTION_UP
                }
                settings.putString(AutoScrollKeys.PLUGIN_ID, AutoScrollKeys.DIRECTION, direction)
            }
            AutoScrollKeys.INTERVAL_SECONDS -> {
                val seconds = value.toIntOrNull()
                    ?: AutoScrollKeys.DEFAULT_INTERVAL_SECONDS
                settings.putInt(
                    AutoScrollKeys.PLUGIN_ID,
                    AutoScrollKeys.INTERVAL_SECONDS,
                    seconds.coerceIn(1, 600),
                )
            }
            AutoScrollKeys.CONTINUOUS_INTERVAL_SECONDS -> {
                val seconds = value.toIntOrNull()
                    ?: AutoScrollKeys.DEFAULT_CONTINUOUS_INTERVAL_SECONDS
                settings.putInt(
                    AutoScrollKeys.PLUGIN_ID,
                    AutoScrollKeys.CONTINUOUS_INTERVAL_SECONDS,
                    seconds.coerceIn(1, 30),
                )
            }
            AutoScrollKeys.SWIPE_DURATION_MS -> {
                val duration = value.toIntOrNull()
                    ?: AutoScrollKeys.DEFAULT_SWIPE_DURATION_MS
                settings.putInt(
                    AutoScrollKeys.PLUGIN_ID,
                    AutoScrollKeys.SWIPE_DURATION_MS,
                    duration.coerceIn(100, 900),
                )
            }
            AutoScrollKeys.OVERLAY_VISIBLE -> {
                AutoScrollOverlayService.setEnabled(context, value == "true")
            }
            else -> settings.putString(AutoScrollKeys.PLUGIN_ID, key, value)
        }

        if (AutoScrollService.isRunning(context)) {
            AutoScrollService.refresh(context)
        }
        AutoScrollService.publishState(context)
    }
}
