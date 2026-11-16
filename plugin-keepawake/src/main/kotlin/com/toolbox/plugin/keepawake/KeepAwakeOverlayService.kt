package com.toolbox.plugin.keepawake

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.toolbox.contract.PluginContract
import com.toolbox.plugin.PluginSettingsManager
import java.util.concurrent.TimeUnit

/**
 * Ekranı açık tutan ön plan servisi.
 *
 * Görünmez 1x1 piksel overlay penceresi `FLAG_KEEP_SCREEN_ON` taşır ve
 * ekranın kapanmasını engeller. Uygulama bazlı modda, kullanım
 * istatistikleriyle ön plandaki uygulama izlenir; yalnızca seçili
 * uygulamalar açıkken overlay etkinleştirilir.
 */
class KeepAwakeOverlayService : Service() {

    companion object {
        const val CHANNEL_ID = "keepawake_channel"
        const val NOTIFICATION_ID = 2001

        const val ACTION_START = "com.toolbox.plugin.keepawake.START"
        const val ACTION_STOP = "com.toolbox.plugin.keepawake.STOP"
        const val ACTION_TOGGLE = "com.toolbox.plugin.keepawake.TOGGLE"

        const val EXTRA_MODE = "mode"
        const val EXTRA_DURATION = "duration"

        fun start(context: Context, mode: Int, durationMinutes: Int) {
            val intent = Intent(context, KeepAwakeOverlayService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_MODE, mode)
                .putExtra(EXTRA_DURATION, durationMinutes)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, KeepAwakeOverlayService::class.java))
        }

        fun toggle(context: Context) {
            val intent = Intent(context, KeepAwakeOverlayService::class.java)
                .setAction(ACTION_TOGGLE)
            ContextCompat.startForegroundService(context, intent)
        }
    }

    private lateinit var settings: PluginSettingsManager
    private val handler = Handler(Looper.getMainLooper())
    private var windowManager: WindowManager? = null
    private var notificationManager: NotificationManager? = null
    private var overlayView: View? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private var foregroundStarted = false
    private var isRunning = false
    private var currentMode = KeepAwakeKeys.MODE_MANUAL
    private var timerDurationMinutes = 15
    private var timerEndTime = 0L
    private var showNotification = true
    private var monitoringActive = false
    private var lastForegroundPackage: String? = null
    private var lastUsageQueryTime = 0L

    private val timerTick = object : Runnable {
        override fun run() {
            if (currentMode != KeepAwakeKeys.MODE_TIMER || timerEndTime <= 0L) return
            val remaining = timerEndTime - System.currentTimeMillis()
            if (remaining <= 0L) {
                stopKeepingAwake()
            } else {
                updateNotification()
                handler.postDelayed(this, 1000L)
            }
        }
    }

    private val monitorTick = object : Runnable {
        override fun run() {
            if (!isRunning || currentMode != KeepAwakeKeys.MODE_APP_BASED) return
            evaluateForegroundApp()
            handler.postDelayed(this, 2000L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        settings = PluginSettingsManager(this)
        windowManager = getSystemService(WindowManager::class.java)
        notificationManager = getSystemService(NotificationManager::class.java)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopKeepingAwake()
                return START_NOT_STICKY
            }

            ACTION_TOGGLE -> {
                if (isRunning) stopKeepingAwake() else startKeepingAwake()
            }

            else -> {
                if (!isRunning) startKeepingAwake()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        cleanup()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ------------------------------------------------------------------
    // Başlatma / durdurma
    // ------------------------------------------------------------------

    private fun startKeepingAwake() {
        loadSettings()
        ensureForeground()

        if (!Settings.canDrawOverlays(this)) {
            abortStart()
            return
        }

        if (currentMode == KeepAwakeKeys.MODE_APP_BASED && !KeepAwakePermissions.hasUsageStats(this)) {
            abortStart()
            return
        }

        isRunning = true
        settings.putBoolean(KeepAwakeKeys.PLUGIN_ID, KeepAwakeKeys.SERVICE_RUNNING, true)

        handler.removeCallbacks(timerTick)
        handler.removeCallbacks(monitorTick)

        when (currentMode) {
            KeepAwakeKeys.MODE_TIMER -> {
                timerEndTime = System.currentTimeMillis() +
                    TimeUnit.MINUTES.toMillis(timerDurationMinutes.toLong())
                showOverlay()
                handler.post(timerTick)
            }

            KeepAwakeKeys.MODE_APP_BASED -> {
                monitoringActive = true
                lastForegroundPackage = null
                lastUsageQueryTime = 0L
                evaluateForegroundApp()
                handler.post(monitorTick)
            }

            else -> {
                showOverlay()
            }
        }

        updateNotification()
        broadcastStatus(true)
    }

    private fun abortStart() {
        isRunning = false
        settings.putBoolean(KeepAwakeKeys.PLUGIN_ID, KeepAwakeKeys.SERVICE_RUNNING, false)
        broadcastStatus(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun stopKeepingAwake() {
        cleanup()
        stopSelf()
    }

    private fun cleanup() {
        handler.removeCallbacks(timerTick)
        handler.removeCallbacks(monitorTick)
        monitoringActive = false
        hideOverlay()
        isRunning = false
        timerEndTime = 0L
        if (::settings.isInitialized) {
            settings.putBoolean(KeepAwakeKeys.PLUGIN_ID, KeepAwakeKeys.SERVICE_RUNNING, false)
        }
        broadcastStatus(false)
        if (foregroundStarted) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            foregroundStarted = false
        }
    }

    private fun loadSettings() {
        val pluginId = KeepAwakeKeys.PLUGIN_ID
        currentMode = settings.getInt(pluginId, KeepAwakeKeys.MODE, KeepAwakeKeys.MODE_MANUAL)
        timerDurationMinutes = settings.getInt(
            pluginId,
            KeepAwakeKeys.TIMER_DURATION,
            15
        )
        showNotification = settings.getBoolean(
            pluginId,
            KeepAwakeKeys.SHOW_NOTIFICATION,
            true
        )
    }

    // ------------------------------------------------------------------
    // Uygulama bazlı izleme
    // ------------------------------------------------------------------

    private fun evaluateForegroundApp() {
        val selected = settings.getStringSet(
            KeepAwakeKeys.PLUGIN_ID,
            KeepAwakeKeys.SELECTED_APPS,
            emptySet()
        )
        val foreground = foregroundPackage()
        val shouldKeep = foreground != null && selected.contains(foreground)

        if (shouldKeep) {
            if (overlayView == null) {
                showOverlay()
                updateNotification()
            }
        } else {
            if (overlayView != null) {
                hideOverlay()
                updateNotification()
            }
        }
    }

    /**
     * Son ön plana çıkan paketi döndürür.
     *
     * Kullanım olayları yalnızca son 15 saniyede değil, son sorgudan bu yana
     * okunur; aksi halde seçili uygulama 15 saniyeden uzun süre açık kaldığında
     * pencere kapanır. İlk sorguda son 10 dakika taranır ve son bilinen paket
     * önbellekte tutulur.
     */
    private fun foregroundPackage(): String? {
        if (!KeepAwakePermissions.hasUsageStats(this)) return null

        val usageStatsManager = getSystemService(UsageStatsManager::class.java) ?: return null
        val now = System.currentTimeMillis()
        val start = if (lastUsageQueryTime == 0L) {
            now - TimeUnit.MINUTES.toMillis(10)
        } else {
            lastUsageQueryTime
        }
        if (start >= now) return lastForegroundPackage

        val events = usageStatsManager.queryEvents(start, now)
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                lastForegroundPackage = event.packageName
            }
        }
        lastUsageQueryTime = now
        return lastForegroundPackage
    }

    // ------------------------------------------------------------------
    // Overlay ve uyku kilidi
    // ------------------------------------------------------------------

    private fun showOverlay() {
        if (overlayView != null) return

        val view = View(this).apply { setBackgroundColor(0x00000000) }
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val params = WindowManager.LayoutParams(
            1,
            1,
            type,
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.START or Gravity.TOP
            x = 0
            y = 0
        }

        val added = runCatching {
            windowManager?.addView(view, params)
        }.isSuccess
        if (added) {
            overlayView = view
            acquireWakeLock()
        }
    }

    private fun hideOverlay() {
        overlayView?.let { view ->
            runCatching { windowManager?.removeView(view) }
        }
        overlayView = null
        releaseWakeLock()
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val powerManager = getSystemService(PowerManager::class.java) ?: return
        wakeLock = powerManager
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Atolye::KeepAwake")
            .apply {
                setReferenceCounted(false)
                acquire()
            }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { lock ->
            if (lock.isHeld) runCatching { lock.release() }
        }
        wakeLock = null
    }

    // ------------------------------------------------------------------
    // Bildirim
    // ------------------------------------------------------------------

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.keepawake_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.keepawake_channel_desc)
                setShowBadge(false)
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun ensureForeground() {
        val notification = buildNotification()
        if (foregroundStarted) {
            notificationManager?.notify(NOTIFICATION_ID, notification)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        foregroundStarted = true
    }

    private fun updateNotification() {
        if (!foregroundStarted) {
            ensureForeground()
            return
        }
        notificationManager?.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, KeepAwakePluginActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, KeepAwakeOverlayService::class.java)
            .setAction(ACTION_STOP)
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val text = when {
            !showNotification -> getString(R.string.keepawake_notification_generic)
            currentMode == KeepAwakeKeys.MODE_TIMER && timerEndTime > 0L -> getString(
                R.string.keepawake_notification_timer,
                KeepAwakeFormat.duration(timerEndTime - System.currentTimeMillis())
            )

            currentMode == KeepAwakeKeys.MODE_APP_BASED -> {
                if (overlayView != null) {
                    getString(R.string.keepawake_notification_app_active)
                } else {
                    getString(R.string.keepawake_notification_app_waiting)
                }
            }

            currentMode == KeepAwakeKeys.MODE_MANUAL ->
                getString(R.string.keepawake_notification_manual)

            else -> getString(R.string.keepawake_notification_infinite)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_keepawake)
            .setContentTitle(getString(R.string.keepawake_notification_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setContentIntent(contentIntent)
            .addAction(
                R.drawable.ic_stop,
                getString(R.string.stop_keeping_awake),
                stopPendingIntent
            )
            .build()
    }

    private fun broadcastStatus(running: Boolean) {
        val intent = Intent(PluginContract.ACTION_PLUGIN_STATUS)
            .putExtra(PluginContract.EXTRA_PLUGIN_ID, KeepAwakeKeys.PLUGIN_ID)
            .putExtra(PluginContract.EXTRA_PLUGIN_RUNNING, running)
        sendBroadcast(intent)
    }
}
