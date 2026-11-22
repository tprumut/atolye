package com.toolbox.plugin.autoscroll

object AutoScrollKeys {
    const val PLUGIN_ID = "com.toolbox.plugin.autoscroll"

    const val RUNNING = "running"
    const val MODE = "mode"
    const val INTERVAL_SECONDS = "interval_seconds"
    const val CONTINUOUS_INTERVAL_SECONDS = "continuous_interval_seconds"
    const val DIRECTION = "direction"
    /** Bölünmüş ekranda gesture'ın gönderileceği pencere. */
    const val TARGET = "target"
    const val SWIPE_DURATION_MS = "swipe_duration_ms"
    const val LAST_APP = "last_app"
    const val LAST_RESULT = "last_result"

    /** Kaydırma açıkken gösterilen sürüklenebilir kısayol tuşu. */
    const val OVERLAY_VISIBLE = "overlay_visible"
    const val OVERLAY_X = "overlay_x"
    const val OVERLAY_Y = "overlay_y"
    const val DEFAULT_OVERLAY_VISIBLE = true

    const val MODE_TIMED = "timed"
    const val MODE_CONTINUOUS = "continuous"

    const val DIRECTION_UP = "up"
    const val DIRECTION_DOWN = "down"

    const val DEFAULT_MODE = MODE_TIMED
    const val DEFAULT_INTERVAL_SECONDS = 15
    const val DEFAULT_CONTINUOUS_INTERVAL_SECONDS = 1
    const val DEFAULT_DIRECTION = DIRECTION_UP

    const val TARGET_ACTIVE = "active"
    const val TARGET_PRIMARY = "primary"   // sol / üst bölme
    const val TARGET_SECONDARY = "secondary" // sağ / alt bölme
    const val DEFAULT_TARGET = TARGET_ACTIVE

    const val DEFAULT_SWIPE_DURATION_MS = 320

    val INTERVAL_OPTIONS = listOf(3, 5, 10, 15, 30, 60)
    val CONTINUOUS_INTERVAL_OPTIONS = listOf(1, 2, 3, 5)

    const val CHANNEL_ID = "autoscroll_channel_v2"
    const val LEGACY_CHANNEL_ID = "autoscroll_channel"
    const val NOTIFICATION_ID = 5001
    const val OVERLAY_NOTIFICATION_ID = 5002

    const val ACTION_START = "com.toolbox.plugin.autoscroll.START"
    const val ACTION_STOP = "com.toolbox.plugin.autoscroll.STOP"
    const val ACTION_REFRESH = "com.toolbox.plugin.autoscroll.REFRESH"
    const val ACTION_OVERLAY_SHOW = "com.toolbox.plugin.autoscroll.OVERLAY_SHOW"
    const val ACTION_OVERLAY_HIDE = "com.toolbox.plugin.autoscroll.OVERLAY_HIDE"
    const val ACTION_OVERLAY_TOGGLE_SCROLL =
        "com.toolbox.plugin.autoscroll.OVERLAY_TOGGLE_SCROLL"
}
