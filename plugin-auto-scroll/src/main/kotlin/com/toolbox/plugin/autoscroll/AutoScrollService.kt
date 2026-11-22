package com.toolbox.plugin.autoscroll

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.toolbox.plugin.PluginSettingsManager
import com.toolbox.plugin.PluginStatusPublisher
import com.toolbox.plugin.StatusBarIconPolicy

/**
 * Otomatik kaydırmanın ömür döngüsünü yöneten ön plan servisi.
 * Gerçek hareketi [AutoScrollAccessibilityService] gönderir.
 */
class AutoScrollService : Service() {

    companion object {
        private const val TAG = "AutoScrollService"

        /**
         * Erişilebilirlik servisi bu kadar süre boyunca hiç bağlanamazsa dur.
         * Tick sayısına göre ölçmek yanıltıcıdır: Zamanlı modda 15 sn aralık
         * olduğu için birkaç tick çok uzun süreye karşılık gelir. Süre bazlı
         * ölçüm kısa kopmaları (örn. sistem yeniden bağlama) tolere eder.
         */
        private const val NOT_CONNECTED_TIMEOUT_MS = 10_000L

        fun start(context: Context) {
            runCatching {
                ContextCompat.startForegroundService(context, intent(context, AutoScrollKeys.ACTION_START))
            }
        }

        fun stop(context: Context) {
            runCatching { context.startService(intent(context, AutoScrollKeys.ACTION_STOP)) }
        }

        fun refresh(context: Context) {
            runCatching { context.startService(intent(context, AutoScrollKeys.ACTION_REFRESH)) }
        }

        fun isRunning(context: Context): Boolean =
            PluginSettingsManager(context).getBoolean(
                AutoScrollKeys.PLUGIN_ID, AutoScrollKeys.RUNNING, false
            )

        fun publishState(context: Context) {
            val settings = PluginSettingsManager(context)
            val pluginId = AutoScrollKeys.PLUGIN_ID
            val running = settings.getBoolean(pluginId, AutoScrollKeys.RUNNING, false)
            val overlayVisible = settings.getBoolean(
                pluginId,
                AutoScrollKeys.OVERLAY_VISIBLE,
                AutoScrollKeys.DEFAULT_OVERLAY_VISIBLE,
            )
            val mode = settings.getString(pluginId, AutoScrollKeys.MODE, AutoScrollKeys.DEFAULT_MODE)
            val direction = settings.getString(
                pluginId, AutoScrollKeys.DIRECTION, AutoScrollKeys.DEFAULT_DIRECTION
            )
            val target = settings.getString(
                pluginId, AutoScrollKeys.TARGET, AutoScrollKeys.DEFAULT_TARGET
            )
            val interval = settings.getInt(
                pluginId, AutoScrollKeys.INTERVAL_SECONDS,
                AutoScrollKeys.DEFAULT_INTERVAL_SECONDS,
            )
            val continuousInterval = settings.getInt(
                pluginId, AutoScrollKeys.CONTINUOUS_INTERVAL_SECONDS,
                AutoScrollKeys.DEFAULT_CONTINUOUS_INTERVAL_SECONDS,
            )

            val modeLabel = if (mode == AutoScrollKeys.MODE_CONTINUOUS) {
                context.getString(R.string.auto_mode_continuous)
            } else {
                context.getString(R.string.auto_mode_timed)
            }
            val directionLabel = if (direction == AutoScrollKeys.DIRECTION_DOWN) {
                context.getString(R.string.auto_direction_label_down)
            } else {
                context.getString(R.string.auto_direction_label_up)
            }
            val seconds = if (mode == AutoScrollKeys.MODE_CONTINUOUS) {
                continuousInterval
            } else {
                interval
            }
            val statusText = if (running) {
                listOf(
                    context.getString(R.string.auto_status_running),
                    modeLabel,
                    context.getString(R.string.auto_seconds, seconds),
                    directionLabel,
                ).joinToString(" • ")
            } else {
                context.getString(R.string.auto_status_ready)
            }

            PluginStatusPublisher.publish(
                context = context,
                pluginId = pluginId,
                running = running,
                quickSettings = mapOf(
                    AutoScrollKeys.MODE to mode,
                    AutoScrollKeys.INTERVAL_SECONDS to interval.toString(),
                    AutoScrollKeys.CONTINUOUS_INTERVAL_SECONDS to continuousInterval.toString(),
                    AutoScrollKeys.DIRECTION to direction,
                    AutoScrollKeys.TARGET to target,
                    AutoScrollKeys.OVERLAY_VISIBLE to overlayVisible.toString(),
                ),
                statusText = statusText,
            )
        }

        private fun intent(context: Context, action: String): Intent =
            Intent(context, AutoScrollService::class.java).setAction(action)
    }

    private lateinit var settings: PluginSettingsManager
    private var notificationManager: NotificationManager? = null
    private val handler = Handler(Looper.getMainLooper())
    private var foreground = false

    /** İlk "bağlı değil" anı; 0 ise şu anda bağlantı sorunu yok. */
    private var notConnectedSinceMs = 0L

    private val iconPolicyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (foreground) updateNotification()
        }
    }

    private val tick = object : Runnable {
        override fun run() {
            if (!settings.getBoolean(
                    AutoScrollKeys.PLUGIN_ID, AutoScrollKeys.RUNNING, false
                )
            ) return

            val direction = settings.getString(
                AutoScrollKeys.PLUGIN_ID,
                AutoScrollKeys.DIRECTION,
                AutoScrollKeys.DEFAULT_DIRECTION,
            )
            val result = AutoScrollAccessibilityService.performScroll(direction)
            Log.d(TAG, "tick direction=$direction result=$result")
            when (result) {
                AutoScrollResult.SENT -> notConnectedSinceMs = 0L // başarıyı accessibility callback yazar
                AutoScrollResult.NOT_CONNECTED -> {
                    val now = System.currentTimeMillis()
                    if (notConnectedSinceMs == 0L) notConnectedSinceMs = now
                    val elapsed = now - notConnectedSinceMs
                    Log.w(TAG, "NOT_CONNECTED elapsed=${elapsed}ms")
                    settings.putString(
                        AutoScrollKeys.PLUGIN_ID,
                        AutoScrollKeys.LAST_RESULT,
                        getString(R.string.auto_result_not_connected),
                    )
                    if (elapsed >= NOT_CONNECTED_TIMEOUT_MS) {
                        stopNow()
                        return
                    }
                    // Kısa kopma: servis açık kalır, bir sonraki tick'te tekrar dener.
                }
                AutoScrollResult.BLOCKED_APP -> settings.putString(
                    AutoScrollKeys.PLUGIN_ID,
                    AutoScrollKeys.LAST_RESULT,
                    getString(R.string.auto_result_blocked),
                )
                AutoScrollResult.FAILED -> settings.putString(
                    AutoScrollKeys.PLUGIN_ID,
                    AutoScrollKeys.LAST_RESULT,
                    getString(R.string.auto_result_failed),
                )
            }

            updateNotification()
            publishState(this@AutoScrollService)
            handler.postDelayed(this, nextDelayMs())
        }
    }

    override fun onCreate() {
        super.onCreate()
        settings = PluginSettingsManager(this)
        notificationManager = getSystemService(NotificationManager::class.java)
        createChannel()
        StatusBarIconPolicy.register(this, iconPolicyReceiver)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand action=${intent?.action}")
        when (intent?.action) {
            AutoScrollKeys.ACTION_STOP -> {
                stopNow()
                return START_NOT_STICKY
            }
            AutoScrollKeys.ACTION_START -> startNow()
            AutoScrollKeys.ACTION_REFRESH -> {
                if (isRunning(this)) {
                    if (ensureForeground()) scheduleNext(restart = true)
                } else {
                    publishState(this)
                }
            }
            else -> {
                if (isRunning(this)) {
                    if (ensureForeground()) scheduleNext(restart = true)
                } else {
                    publishState(this)
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        runCatching { StatusBarIconPolicy.unregister(this, iconPolicyReceiver) }
        publishState(this)
        super.onDestroy()
    }

    private fun startNow() {
        Log.d(TAG, "startNow")
        notConnectedSinceMs = 0L
        settings.putBoolean(AutoScrollKeys.PLUGIN_ID, AutoScrollKeys.RUNNING, true)
        if (!ensureForeground()) {
            settings.putBoolean(AutoScrollKeys.PLUGIN_ID, AutoScrollKeys.RUNNING, false)
            publishState(this)
            AutoScrollOverlayService.sync(this)
            stopSelf()
            return
        }
        publishState(this)
        AutoScrollOverlayService.sync(this)
        scheduleNext(restart = true)
    }

    private fun stopNow() {
        Log.d(TAG, "stopNow")
        handler.removeCallbacks(tick)
        settings.putBoolean(AutoScrollKeys.PLUGIN_ID, AutoScrollKeys.RUNNING, false)
        settings.putString(
            AutoScrollKeys.PLUGIN_ID,
            AutoScrollKeys.LAST_RESULT,
            getString(R.string.auto_status_stopped),
        )
        publishState(this)
        AutoScrollOverlayService.sync(this)
        if (foreground) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            foreground = false
        }
        stopSelf()
    }

    private fun ensureForeground(): Boolean {
        if (foreground) {
            updateNotification()
            return true
        }
        return try {
            startForeground(AutoScrollKeys.NOTIFICATION_ID, buildNotification())
            foreground = true
            true
        } catch (_: Throwable) {
            false
        }
    }

    private fun scheduleNext(restart: Boolean) {
        handler.removeCallbacks(tick)
        if (!settings.getBoolean(
                AutoScrollKeys.PLUGIN_ID, AutoScrollKeys.RUNNING, false
            )
        ) return
        if (restart) handler.post(tick) else handler.postDelayed(tick, nextDelayMs())
    }

    private fun nextDelayMs(): Long {
        val mode = settings.getString(
            AutoScrollKeys.PLUGIN_ID, AutoScrollKeys.MODE, AutoScrollKeys.DEFAULT_MODE
        )
        val key = if (mode == AutoScrollKeys.MODE_CONTINUOUS) {
            AutoScrollKeys.CONTINUOUS_INTERVAL_SECONDS
        } else {
            AutoScrollKeys.INTERVAL_SECONDS
        }
        val default = if (mode == AutoScrollKeys.MODE_CONTINUOUS) {
            AutoScrollKeys.DEFAULT_CONTINUOUS_INTERVAL_SECONDS
        } else {
            AutoScrollKeys.DEFAULT_INTERVAL_SECONDS
        }
        return settings.getInt(AutoScrollKeys.PLUGIN_ID, key, default)
            .coerceAtLeast(1)
            .times(1000L)
            .coerceAtLeast(300L)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = notificationManager ?: return
        val channel = NotificationChannel(
            AutoScrollKeys.CHANNEL_ID,
            getString(R.string.auto_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.auto_notification_channel_desc)
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.deleteNotificationChannel(AutoScrollKeys.LEGACY_CHANNEL_ID)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val mode = settings.getString(
            AutoScrollKeys.PLUGIN_ID, AutoScrollKeys.MODE, AutoScrollKeys.DEFAULT_MODE
        )
        val modeLabel = if (mode == AutoScrollKeys.MODE_CONTINUOUS) {
            getString(R.string.auto_mode_continuous)
        } else {
            getString(R.string.auto_mode_timed)
        }
        val seconds = if (mode == AutoScrollKeys.MODE_CONTINUOUS) {
            settings.getInt(
                AutoScrollKeys.PLUGIN_ID,
                AutoScrollKeys.CONTINUOUS_INTERVAL_SECONDS,
                AutoScrollKeys.DEFAULT_CONTINUOUS_INTERVAL_SECONDS,
            )
        } else {
            settings.getInt(
                AutoScrollKeys.PLUGIN_ID,
                AutoScrollKeys.INTERVAL_SECONDS,
                AutoScrollKeys.DEFAULT_INTERVAL_SECONDS,
            )
        }
        val detail = getString(R.string.auto_seconds, seconds)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val openIntent = PendingIntent.getActivity(
            this,
            20,
            Intent(this, AutoScrollPluginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            flags,
        )
        val stopIntent = PendingIntent.getService(
            this,
            21,
            intent(this, AutoScrollKeys.ACTION_STOP),
            flags,
        )

        return NotificationCompat.Builder(this, AutoScrollKeys.CHANNEL_ID)
            .setSmallIcon(
                StatusBarIconPolicy.smallIcon(this, R.drawable.ic_notification_autoscroll)
            )
            .setContentTitle(getString(R.string.auto_notification_title))
            .setContentText(getString(R.string.auto_notification_text, detail, modeLabel))
            .setContentIntent(openIntent)
            .addAction(
                R.drawable.ic_stop_autoscroll,
                getString(R.string.auto_notification_action_stop),
                stopIntent,
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .build()
    }

    private fun updateNotification() {
        if (!foreground) return
        runCatching {
            notificationManager?.notify(AutoScrollKeys.NOTIFICATION_ID, buildNotification())
        }
    }
}
