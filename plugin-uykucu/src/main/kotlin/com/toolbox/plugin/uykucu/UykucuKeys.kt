package com.toolbox.plugin.uykucu

import java.util.Locale
import java.util.concurrent.TimeUnit

object UykucuKeys {
    const val PLUGIN_ID = "com.toolbox.plugin.uykucu"

    const val TIMER_RUNNING = "timer_running"
    const val TIMER_END_TIME = "timer_end_time"
    const val LAST_DURATION_MINUTES = "last_duration_minutes"
    const val WARNING_SHOWN = "warning_shown"

    const val CLOSE_APPS_ENABLED = "close_apps_enabled"
    const val STOP_MEDIA_ENABLED = "stop_media_enabled"
    const val REMEMBER_LAST_DURATION = "remember_last_duration"

    const val DEFAULT_DURATION_MINUTES = 30
    val PRESET_MINUTES = listOf(5, 10, 15, 30, 45, 60, 90)
}

object UykucuFormat {
    fun remaining(ms: Long): String {
        val total = TimeUnit.MILLISECONDS.toSeconds(ms.coerceAtLeast(0L))
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) String.format(Locale.getDefault(), "%d:%02d:%02d", h, m, s)
        else String.format(Locale.getDefault(), "%02d:%02d", m, s)
    }
}
