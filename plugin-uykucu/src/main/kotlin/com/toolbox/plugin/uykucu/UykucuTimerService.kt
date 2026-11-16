package com.toolbox.plugin.uykucu

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.toolbox.contract.PluginContract
import com.toolbox.plugin.PluginSettingsManager
import com.toolbox.plugin.PluginStatusPublisher
import java.util.concurrent.TimeUnit

class UykucuTimerService : Service() {

    companion object {
        const val CHANNEL_ID = "uykucu_timer_channel"
        const val WARNING_CHANNEL_ID = "uykucu_warning_channel"
        const val NOTIFICATION_ID = 3001
        const val WARNING_NOTIFICATION_ID = 3002

        const val ACTION_START = "com.toolbox.plugin.uykucu.START"
        const val ACTION_CANCEL = "com.toolbox.plugin.uykucu.CANCEL"
        const val ACTION_EXTEND = "com.toolbox.plugin.uykucu.EXTEND"
        const val ACTION_ALARM_FINISH = "com.toolbox.plugin.uykucu.ALARM_FINISH"
        const val ACTION_ALARM_WARNING = "com.toolbox.plugin.uykucu.ALARM_WARNING"
        const val ACTION_REFRESH = "com.toolbox.plugin.uykucu.REFRESH"

        const val EXTRA_DURATION_MINUTES = "duration_minutes"

        fun start(context: Context, durationMinutes: Int) {
            val i = Intent(context, UykucuTimerService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_DURATION_MINUTES, durationMinutes)
            ContextCompat.startForegroundService(context, i)
        }

        fun cancel(context: Context) {
            val i = Intent(context, UykucuTimerService::class.java).setAction(ACTION_CANCEL)
            ContextCompat.startForegroundService(context, i)
        }

        fun extend(context: Context) {
            val i = Intent(context, UykucuTimerService::class.java).setAction(ACTION_EXTEND)
            ContextCompat.startForegroundService(context, i)
        }
    }

    private lateinit var settings: PluginSettingsManager
    private val handler = Handler(Looper.getMainLooper())
    private var notificationManager: NotificationManager? = null
    private var alarmManager: AlarmManager? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private var isRunning = false
    private var endTimeMs: Long = 0L
    private var finishing = false
    private var foregroundStarted = false

    private val tick = object : Runnable {
        override fun run() {
            if (!isRunning) return
            val remaining = endTimeMs - System.currentTimeMillis()
            if (remaining <= 0L) {
                handleFinish()
            } else {
                updateNotification()
                publishRichStatus(true)
                handler.postDelayed(this, 1000L)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        settings = PluginSettingsManager(this)
        notificationManager = getSystemService(NotificationManager::class.java)
        alarmManager = getSystemService(AlarmManager::class.java)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val minutes = intent.getIntExtra(EXTRA_DURATION_MINUTES, UykucuKeys.DEFAULT_DURATION_MINUTES)
                startTimer(minutes)
            }
            ACTION_CANCEL -> cancelTimer()
            ACTION_EXTEND -> extendTimer()
            ACTION_ALARM_WARNING -> showWarningIfNeeded()
            ACTION_ALARM_FINISH -> handleFinish()
            ACTION_REFRESH -> if (isRunning) updateNotification() else restoreOrStop()
            else -> restoreOrStop()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        releaseWakeLock()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ---- Timer control ----

    private fun startTimer(minutes: Int) {
        finishing = false
        val durMin = minutes.coerceIn(1, 24 * 60)
        endTimeMs = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(durMin.toLong())
        isRunning = true
        persistState(true)
        rememberDurationIfNeeded(durMin)
        acquireWakeLock()
        ensureForeground()
        scheduleAlarms()
        handler.removeCallbacks(tick)
        handler.post(tick)
        updateNotification()
        broadcastStatus(true)
    }

    private fun restoreOrStop() {
        val running = settings.getBoolean(UykucuKeys.PLUGIN_ID, UykucuKeys.TIMER_RUNNING, false)
        val end = settings.getLong(UykucuKeys.PLUGIN_ID, UykucuKeys.TIMER_END_TIME, 0L)
        if (running && end > System.currentTimeMillis()) {
            isRunning = true
            endTimeMs = end
            finishing = false
            acquireWakeLock()
            ensureForeground()
            handler.removeCallbacks(tick)
            handler.post(tick)
            updateNotification()
            broadcastStatus(true)
        } else if (running && end <= System.currentTimeMillis() && end > 0L) {
            isRunning = true
            endTimeMs = end
            ensureForeground()
            handleFinish()
        } else {
            stopForegroundClean()
        }
    }

    private fun cancelTimer() {
        if (!isRunning && !settings.getBoolean(UykucuKeys.PLUGIN_ID, UykucuKeys.TIMER_RUNNING, false)) {
            stopForegroundClean()
            return
        }
        isRunning = false
        finishing = false
        endTimeMs = 0L
        handler.removeCallbacks(tick)
        cancelAlarms()
        cancelWarningNotification()
        persistState(false)
        settings.putBoolean(UykucuKeys.PLUGIN_ID, UykucuKeys.WARNING_SHOWN, false)
        broadcastStatus(false)
        releaseWakeLock()
        stopForegroundClean()
    }

    private fun extendTimer() {
        if (!isRunning) return
        val now = System.currentTimeMillis()
        val base = maxOf(endTimeMs, now)
        endTimeMs = base + TimeUnit.MINUTES.toMillis(5)
        settings.putLong(UykucuKeys.PLUGIN_ID, UykucuKeys.TIMER_END_TIME, endTimeMs)
        settings.putBoolean(UykucuKeys.PLUGIN_ID, UykucuKeys.WARNING_SHOWN, false)
        cancelWarningNotification()
        scheduleAlarms()
        updateNotification()
    }

    private fun handleFinish() {
        if (finishing) return
        finishing = true
        if (!isRunning) {
            val running = settings.getBoolean(UykucuKeys.PLUGIN_ID, UykucuKeys.TIMER_RUNNING, false)
            if (!running) { stopForegroundClean(); return }
            isRunning = true
        }
        handler.removeCallbacks(tick)
        cancelAlarms()
        cancelWarningNotification()

        val stopMedia = settings.getBoolean(UykucuKeys.PLUGIN_ID, UykucuKeys.STOP_MEDIA_ENABLED, true)
        val closeApps = settings.getBoolean(UykucuKeys.PLUGIN_ID, UykucuKeys.CLOSE_APPS_ENABLED, true)

        if (stopMedia) stopMediaPlayback()

        var finalized = false
        fun finalizeOnce() {
            if (finalized) return
            finalized = true
            try { lockScreen() } catch (_: Throwable) {}
            handler.postDelayed({ completeFinish() }, 400)
        }

        if (closeApps && UykucuPermissions.isAccessibilityEnabled(this)) {
            // Ayrı fonksiyonda; hata olursa sessizce atla ve kilitle.
            tryCloseAppsSequenceSafe { handler.post { finalizeOnce() } }
            // Güvenlik zaman aşımı (tek seferlik)
            handler.postDelayed({ finalizeOnce() }, 5000)
        } else {
            if (closeApps) {
                UykucuAccessibilityService.goHome()
            }
            handler.postDelayed({ finalizeOnce() }, 400)
        }
    }

    private fun completeFinish() {
        if (!finishing && !isRunning) return
        isRunning = false
        finishing = false
        endTimeMs = 0L
        persistState(false)
        settings.putBoolean(UykucuKeys.PLUGIN_ID, UykucuKeys.WARNING_SHOWN, false)
        broadcastStatus(false)
        releaseWakeLock()
        stopForegroundClean()
    }

    // ---- Actions ----

    private fun stopMediaPlayback() {
        try {
            val am = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            // Medya tuşları
            for (code in listOf(KeyEvent.KEYCODE_MEDIA_PAUSE, KeyEvent.KEYCODE_MEDIA_STOP)) {
                try {
                    val down = KeyEvent(KeyEvent.ACTION_DOWN, code)
                    val up = KeyEvent(KeyEvent.ACTION_UP, code)
                    am.dispatchMediaKeyEvent(down)
                    am.dispatchMediaKeyEvent(up)
                } catch (_: Throwable) {}
            }
            // Ses odağını alarak çalan medyayı durdurmayı dene
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val attrs = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                    val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                        .setAudioAttributes(attrs)
                        .setOnAudioFocusChangeListener { }
                        .build()
                    am.requestAudioFocus(req)
                    am.abandonAudioFocusRequest(req)
                } else {
                    @Suppress("DEPRECATION")
                    val listener = AudioManager.OnAudioFocusChangeListener { }
                    @Suppress("DEPRECATION")
                    am.requestAudioFocus(listener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    @Suppress("DEPRECATION")
                    am.abandonAudioFocus(listener)
                }
            } catch (_: Throwable) {}
        } catch (_: Throwable) {}
    }

    private fun tryCloseAppsSequenceSafe(onDone: () -> Unit) {
        try {
            UykucuAccessibilityService.tryCloseAppsThen(onDone)
        } catch (_: Throwable) {
            UykucuAccessibilityService.goHome()
            onDone()
        }
    }

    private fun lockScreen() {
        try {
            val ok = UykucuAccessibilityService.lockScreenNow()
            if (!ok) {
                // Erişilebilirlik yoksa kilitleyemeyiz; sessizce geç.
            }
        } catch (_: Throwable) {}
    }

    // ---- Alarms ----

    private fun scheduleAlarms() {
        cancelAlarms()
        val am = alarmManager ?: return
        val now = System.currentTimeMillis()
        if (endTimeMs <= now) return

        // Uyarı: 30 sn önce
        val warnAt = endTimeMs - 30_000L
        if (warnAt > now) {
            setExactAlarm(am, warnAt, ACTION_ALARM_WARNING, 101)
        } else {
            showWarningIfNeeded()
        }
        // Bitiş
        setExactAlarm(am, endTimeMs, ACTION_ALARM_FINISH, 102)
    }

    private fun setExactAlarm(am: AlarmManager, triggerAt: Long, action: String, reqCode: Int) {
        val pi = alarmPendingIntent(action, reqCode)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (am.canScheduleExactAlarms()) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                } else {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
        } catch (_: SecurityException) {
            try { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi) } catch (_: Throwable) {}
        } catch (_: Throwable) {}
    }

    private fun cancelAlarms() {
        val am = alarmManager ?: return
        try { am.cancel(alarmPendingIntent(ACTION_ALARM_WARNING, 101)) } catch (_: Throwable) {}
        try { am.cancel(alarmPendingIntent(ACTION_ALARM_FINISH, 102)) } catch (_: Throwable) {}
    }

    private fun alarmPendingIntent(action: String, reqCode: Int): PendingIntent {
        val i = Intent(this, UykucuAlarmReceiver::class.java).setAction(action)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(this, reqCode, i, flags)
    }

    // ---- Notifications ----

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timer = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.uykucu_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.uykucu_channel_desc)
                setShowBadge(false)
            }
            val warn = NotificationChannel(
                WARNING_CHANNEL_ID,
                getString(R.string.uykucu_warning_title),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.uykucu_channel_desc)
                setShowBadge(true)
            }
            notificationManager?.createNotificationChannel(timer)
            notificationManager?.createNotificationChannel(warn)
        }
    }

    private fun ensureForeground() {
        val n = buildNotification()
        if (!foregroundStarted) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(
                        NOTIFICATION_ID,
                        n,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } else {
                    startForeground(NOTIFICATION_ID, n)
                }
                foregroundStarted = true
            } catch (_: Throwable) {
                startForeground(NOTIFICATION_ID, n)
                foregroundStarted = true
            }
        } else {
            notificationManager?.notify(NOTIFICATION_ID, n)
        }
    }

    private fun updateNotification() {
        if (!foregroundStarted) ensureForeground()
        else notificationManager?.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val remaining = (endTimeMs - System.currentTimeMillis()).coerceAtLeast(0L)
        val text = getString(R.string.uykucu_notification_remaining, UykucuFormat.remaining(remaining))

        val openIntent = Intent(this, UykucuPluginActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val contentPi = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val cancelPi = PendingIntent.getService(
            this, 1, Intent(this, UykucuTimerService::class.java).setAction(ACTION_CANCEL),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val extendPi = PendingIntent.getService(
            this, 2, Intent(this, UykucuTimerService::class.java).setAction(ACTION_EXTEND),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_uykucu)
            .setContentTitle(getString(R.string.uykucu_notification_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentIntent(contentPi)
            .addAction(R.drawable.ic_extend, getString(R.string.uykucu_extend_5), extendPi)
            .addAction(R.drawable.ic_stop, getString(R.string.uykucu_cancel), cancelPi)
            .build()
    }

    private fun showWarningIfNeeded() {
        val shown = settings.getBoolean(UykucuKeys.PLUGIN_ID, UykucuKeys.WARNING_SHOWN, false)
        val running = settings.getBoolean(UykucuKeys.PLUGIN_ID, UykucuKeys.TIMER_RUNNING, false)
        if (shown || !running) return
        if (UykucuPermissions.hasNotificationPermission(this)) {
            val n = NotificationCompat.Builder(this, WARNING_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_uykucu)
                .setContentTitle(getString(R.string.uykucu_warning_title))
                .setContentText(getString(R.string.uykucu_warning_text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .build()
            try { NotificationManagerCompat.from(this).notify(WARNING_NOTIFICATION_ID, n) } catch (_: SecurityException) {}
        }
        settings.putBoolean(UykucuKeys.PLUGIN_ID, UykucuKeys.WARNING_SHOWN, true)
    }

    private fun cancelWarningNotification() {
        notificationManager?.cancel(WARNING_NOTIFICATION_ID)
    }

    // ---- State/power ----

    private fun persistState(running: Boolean) {
        settings.putBoolean(UykucuKeys.PLUGIN_ID, UykucuKeys.TIMER_RUNNING, running)
        settings.putLong(UykucuKeys.PLUGIN_ID, UykucuKeys.TIMER_END_TIME, if (running) endTimeMs else 0L)
    }

    private fun rememberDurationIfNeeded(minutes: Int) {
        val remember = settings.getBoolean(UykucuKeys.PLUGIN_ID, UykucuKeys.REMEMBER_LAST_DURATION, true)
        if (remember) {
            settings.putInt(UykucuKeys.PLUGIN_ID, UykucuKeys.LAST_DURATION_MINUTES, minutes)
        }
    }

    private fun acquireWakeLock() {
        try {
            if (wakeLock?.isHeld == true) return
            val pm = getSystemService(PowerManager::class.java) ?: return
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Atolye::UykucuTimer").apply {
                setReferenceCounted(false)
                acquire(10 * 60 * 60 * 1000L) // safety
            }
        } catch (_: Throwable) {}
    }

    private fun releaseWakeLock() {
        try { if (wakeLock?.isHeld == true) wakeLock?.release() } catch (_: Throwable) {}
        wakeLock = null
    }

    private fun broadcastStatus(running: Boolean) {
        val i = Intent(PluginContract.ACTION_PLUGIN_STATUS)
            .putExtra(PluginContract.EXTRA_PLUGIN_ID, UykucuKeys.PLUGIN_ID)
            .putExtra(PluginContract.EXTRA_PLUGIN_RUNNING, running)
        sendBroadcast(i)
        publishRichStatus(running)
    }

    private fun publishRichStatus(running: Boolean) {
        try {
            val actualRunning = running && isRunning && endTimeMs > System.currentTimeMillis()
            val remaining = if (actualRunning) (endTimeMs - System.currentTimeMillis()).coerceAtLeast(0L) else -1L
            val qs = mapOf(
                UykucuKeys.STOP_MEDIA_ENABLED to settings.getBoolean(UykucuKeys.PLUGIN_ID, UykucuKeys.STOP_MEDIA_ENABLED, true).toString(),
                UykucuKeys.CLOSE_APPS_ENABLED to settings.getBoolean(UykucuKeys.PLUGIN_ID, UykucuKeys.CLOSE_APPS_ENABLED, true).toString(),
                UykucuKeys.REMEMBER_LAST_DURATION to settings.getBoolean(UykucuKeys.PLUGIN_ID, UykucuKeys.REMEMBER_LAST_DURATION, true).toString()
            )
            PluginStatusPublisher.publish(this, UykucuKeys.PLUGIN_ID, actualRunning, qs, remaining, null)
        } catch (_: Exception) {}
    }

    private fun stopForegroundClean() {
        isRunning = false
        foregroundStarted = false
        try { stopForeground(STOP_FOREGROUND_REMOVE) } catch (_: Throwable) {}
        stopSelf()
    }
}
