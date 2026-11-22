package com.toolbox.plugin.keepawake

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
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
import com.toolbox.plugin.PluginStatusPublisher
import com.toolbox.plugin.StatusBarIconPolicy
import java.util.concurrent.TimeUnit

/**
 * Ekranı açık tutan ön plan servisi (Uyanık).
 *
 * Görünmez 1x1 piksel overlay penceresi `FLAG_KEEP_SCREEN_ON` taşır ve
 * ekranın kapanmasını engeller. Modlar: süreli, süresiz, uygulama bazlı.
 * Çalışırken ayar değişiklikleri servisi yeniden başlatmadan uygulanır.
 */
class KeepAwakeOverlayService : Service() {

    companion object {
        /** Gerçek servis durumu; shared_prefs'deki service_running bayrağı bayat kalabiliyor. */
        @Volatile
        var isAlive: Boolean = false

        const val CHANNEL_ID = "keepawake_channel_v2"
        const val LEGACY_CHANNEL_ID = "keepawake_channel"
        const val NOTIFICATION_ID = 2001

        const val ACTION_START = "com.toolbox.plugin.keepawake.START"
        const val ACTION_STOP = "com.toolbox.plugin.keepawake.STOP"
        const val ACTION_TOGGLE = "com.toolbox.plugin.keepawake.TOGGLE"
        const val ACTION_APPLY = "com.toolbox.plugin.keepawake.APPLY"

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
            // Servis zaten ölmüşse onDestroy çalışmaz; bayrağı burada temizle ki
            // kart güç düğmesi bir sonraki basışta START göndersin.
            val settings = PluginSettingsManager(context)
            settings.putBoolean(KeepAwakeKeys.PLUGIN_ID, KeepAwakeKeys.SERVICE_RUNNING, false)
            settings.putLong(KeepAwakeKeys.PLUGIN_ID, "timer_end_time", 0L)
            context.stopService(Intent(context, KeepAwakeOverlayService::class.java))
            publishCurrentStatus(context)
        }

        fun toggle(context: Context) {
            val intent = Intent(context, KeepAwakeOverlayService::class.java)
                .setAction(ACTION_TOGGLE)
            ContextCompat.startForegroundService(context, intent)
        }

        fun applySettings(context: Context) {
            val intent = Intent(context, KeepAwakeOverlayService::class.java)
                .setAction(ACTION_APPLY)
            try {
                context.startService(intent)
            } catch (_: Exception) {
            }
        }

        /**
         * Hızlı ayardan süre/mot değiştirildiğinde çağrılır. Servis gerçekten
         * çalışmıyorsa ACTION_APPLY yalnızca durum yayınlar, ekranı açık tutmaz;
         * bu yüzden burada doğrudan ön plan servisini başlatıyoruz.
         */
        fun ensureRunning(context: Context) {
            if (isAlive) {
                applySettings(context)
                return
            }
            // Extra yok: onStartCommand mevcut mod/süreyi shared_prefs'den okur.
            start(context, -1, -1)
        }

        fun publishCurrentStatus(context: Context) {
            val settings = PluginSettingsManager(context)
            val running = settings.getBoolean(KeepAwakeKeys.PLUGIN_ID, KeepAwakeKeys.SERVICE_RUNNING, false)
            val mode = settings.getInt(KeepAwakeKeys.PLUGIN_ID, KeepAwakeKeys.MODE, KeepAwakeKeys.MODE_TIMER)
            val duration = settings.getInt(KeepAwakeKeys.PLUGIN_ID, KeepAwakeKeys.TIMER_DURATION, 15)
            val showNotif = settings.getBoolean(KeepAwakeKeys.PLUGIN_ID, KeepAwakeKeys.SHOW_NOTIFICATION, true)
            val end = settings.getLong(KeepAwakeKeys.PLUGIN_ID, "timer_end_time", 0L)
            val remaining = if (running && mode == KeepAwakeKeys.MODE_TIMER && end > 0L) {
                (end - System.currentTimeMillis()).coerceAtLeast(0L)
            } else -1L
            val qs = mapOf(
                KeepAwakeKeys.MODE to mode.toString(),
                KeepAwakeKeys.TIMER_DURATION to duration.toString(),
                KeepAwakeKeys.SHOW_NOTIFICATION to showNotif.toString()
            )
            PluginStatusPublisher.publish(
                context = context,
                pluginId = KeepAwakeKeys.PLUGIN_ID,
                running = running,
                quickSettings = qs,
                remainingMs = remaining,
                statusText = null
            )
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

    // Hız göstergesi açılıp kapanınca durum çubuğu ikonunu yeniler.
    private val iconPolicyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (foregroundStarted) updateNotification()
        }
    }

    private val timerTick = object : Runnable {
        override fun run() {
            if (!isRunning) return
            if (currentMode == KeepAwakeKeys.MODE_TIMER) {
                if (timerEndTime <= 0L) {
                    stopKeepingAwake()
                    return
                }
                val remaining = timerEndTime - System.currentTimeMillis()
                if (remaining <= 0L) {
                    stopKeepingAwake()
                    return
                }
            }
            updateNotification()
            publishStatus()
            handler.postDelayed(this, 1000L)
        }
    }

    private val monitorTick = object : Runnable {
        override fun run() {
            if (!isRunning || currentMode != KeepAwakeKeys.MODE_APP_BASED) return
            evaluateForegroundApp()
            publishStatus()
            handler.postDelayed(this, 2000L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        isAlive = true
        settings = PluginSettingsManager(this)
        windowManager = getSystemService(WindowManager::class.java)
        notificationManager = getSystemService(NotificationManager::class.java)
        createNotificationChannel()
        StatusBarIconPolicy.register(this, iconPolicyReceiver)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopKeepingAwake()
                return START_NOT_STICKY
            }

            ACTION_TOGGLE -> {
                if (isRunning) stopKeepingAwake() else startKeepingAwakeFromPrefs()
            }

            ACTION_APPLY -> {
                if (isRunning) {
                    applyRuntimeSettings()
                } else {
                    // Servis yaşıyorsa ama başlatılmadıysa ayarları uygula;
                    // aksi halde sadece durum yayınlamak ekranı açık tutmuyor.
                    startKeepingAwakeFromPrefs()
                }
            }

            ACTION_START -> {
                val mode = intent.getIntExtra(EXTRA_MODE, -1)
                val dur = intent.getIntExtra(EXTRA_DURATION, -1)
                if (mode >= 0) settings.putInt(KeepAwakeKeys.PLUGIN_ID, KeepAwakeKeys.MODE, mode)
                if (dur > 0) settings.putInt(KeepAwakeKeys.PLUGIN_ID, KeepAwakeKeys.TIMER_DURATION, dur)
                if (!isRunning) startKeepingAwake() else applyRuntimeSettings()
            }

            else -> {
                if (!isRunning) startKeepingAwakeFromPrefs()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        isAlive = false
        cleanup()
        StatusBarIconPolicy.unregister(this, iconPolicyReceiver)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ------------------------------------------------------------------
    // Başlatma / durdurma / yeniden yapılandırma
    // ------------------------------------------------------------------

    private fun startKeepingAwakeFromPrefs() {
        loadSettings()
        startKeepingAwake()
    }

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

        // Ekran kilidi yalnızca overlay gerçekten eklendiğinde alınır;
        // uygulama bazlı modda seçili uygulama yoksa ekran açık kalmaz.
        handler.removeCallbacks(timerTick)
        handler.removeCallbacks(monitorTick)

        configureForCurrentMode(resetTimer = true)

        updateNotification()
        publishStatus()
    }

    private fun configureForCurrentMode(resetTimer: Boolean) {
        when (currentMode) {
            KeepAwakeKeys.MODE_TIMER -> {
                monitoringActive = false
                if (resetTimer || timerEndTime <= 0L || timerEndTime < System.currentTimeMillis()) {
                    timerEndTime = System.currentTimeMillis() +
                        TimeUnit.MINUTES.toMillis(timerDurationMinutes.toLong())
                    settings.putLong(KeepAwakeKeys.PLUGIN_ID, "timer_end_time", timerEndTime)
                }
                showOverlay()
                handler.post(timerTick)
            }

            KeepAwakeKeys.MODE_APP_BASED -> {
                timerEndTime = 0L
                settings.putLong(KeepAwakeKeys.PLUGIN_ID, "timer_end_time", 0L)
                monitoringActive = true
                lastForegroundPackage = null
                lastUsageQueryTime = 0L
                evaluateForegroundApp()
                handler.removeCallbacks(monitorTick)
                handler.post(monitorTick)
                // periyodik durum güncellemesi için timerTick'i de kullan
                handler.removeCallbacks(timerTick)
                handler.post(timerTick)
            }

            KeepAwakeKeys.MODE_INFINITE -> {
                monitoringActive = false
                timerEndTime = 0L
                settings.putLong(KeepAwakeKeys.PLUGIN_ID, "timer_end_time", 0L)
                showOverlay()
                handler.removeCallbacks(timerTick)
                handler.post(timerTick)
            }

            else -> { // MANUAL -> süresiz gibi davran
                monitoringActive = false
                timerEndTime = 0L
                settings.putLong(KeepAwakeKeys.PLUGIN_ID, "timer_end_time", 0L)
                showOverlay()
                handler.removeCallbacks(timerTick)
                handler.post(timerTick)
            }
        }
    }

    private fun applyRuntimeSettings() {
        val prevMode = currentMode
        val prevDuration = timerDurationMinutes
        loadSettings()

        // Bildirim görünürlüğü/ içeriği güncelle
        updateNotification()

        if (currentMode != prevMode) {
            // Mod değişti: yeniden yapılandır (süreliğe geçişte süreyi sıfırdan başlat)
            configureForCurrentMode(resetTimer = true)
            publishStatus()
            return
        }

        when (currentMode) {
            KeepAwakeKeys.MODE_TIMER -> {
                if (prevDuration != timerDurationMinutes) {
                    // Süre değişti: kalan süreyi yeni süreye göre güncelle (çalışırken yeniden başlatmadan)
                    val remaining = (timerEndTime - System.currentTimeMillis()).coerceAtLeast(0L)
                    // Eğer henüz bitmediyse yeni süreyi şu andan itibaren uygula
                    timerEndTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(timerDurationMinutes.toLong())
                    settings.putLong(KeepAwakeKeys.PLUGIN_ID, "timer_end_time", timerEndTime)
                }
                if (overlayView == null) showOverlay()
            }

            KeepAwakeKeys.MODE_APP_BASED -> {
                if (!KeepAwakePermissions.hasUsageStats(this)) {
                    // izin yoksa güvenli durdur
                    stopKeepingAwake()
                    return
                }
                evaluateForegroundApp()
            }

            else -> {
                if (overlayView == null) showOverlay()
            }
        }
        publishStatus()
    }

    private fun abortStart() {
        isRunning = false
        settings.putBoolean(KeepAwakeKeys.PLUGIN_ID, KeepAwakeKeys.SERVICE_RUNNING, false)
        releaseWakeLock()
        publishStatus()
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
        releaseWakeLock()
        isRunning = false
        timerEndTime = 0L
        if (::settings.isInitialized) {
            settings.putBoolean(KeepAwakeKeys.PLUGIN_ID, KeepAwakeKeys.SERVICE_RUNNING, false)
            settings.putLong(KeepAwakeKeys.PLUGIN_ID, "timer_end_time", 0L)
        }
        publishStatus()
        if (foregroundStarted) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            foregroundStarted = false
        }
    }

    private fun loadSettings() {
        val pluginId = KeepAwakeKeys.PLUGIN_ID
        currentMode = settings.getInt(pluginId, KeepAwakeKeys.MODE, KeepAwakeKeys.MODE_TIMER)
        timerDurationMinutes = settings.getInt(pluginId, KeepAwakeKeys.TIMER_DURATION, 15)
        showNotification = settings.getBoolean(pluginId, KeepAwakeKeys.SHOW_NOTIFICATION, true)
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
        // PARTIAL_WAKE_LOCK yalnızca CPU'yu açık tutar; ekran yine 30 sn'de
        // kapanır. Ekranın gerçekten açık kalması için ekran kilidi gerekli.
        val level = PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
            PowerManager.ACQUIRE_CAUSES_WAKEUP
        val lock = powerManager
            .newWakeLock(level, "Atolye::KeepAwake:Screen")
            .apply { setReferenceCounted(false) }
        runCatching { lock.acquire() }
        wakeLock = lock
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
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.keepawake_channel_desc)
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager?.deleteNotificationChannel(LEGACY_CHANNEL_ID)
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
                KeepAwakeFormat.duration((timerEndTime - System.currentTimeMillis()).coerceAtLeast(0L))
            )

            currentMode == KeepAwakeKeys.MODE_APP_BASED -> {
                if (overlayView != null) {
                    getString(R.string.keepawake_notification_app_active)
                } else {
                    getString(R.string.keepawake_notification_app_waiting)
                }
            }

            currentMode == KeepAwakeKeys.MODE_INFINITE || currentMode == KeepAwakeKeys.MODE_MANUAL ->
                getString(R.string.keepawake_notification_infinite)

            else -> getString(R.string.keepawake_notification_generic)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(StatusBarIconPolicy.smallIcon(this, R.drawable.ic_notification_keepawake))
            .setContentTitle(getString(R.string.keepawake_notification_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setSortKey("100_atolye_keepawake")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setGroup("atolye_keepawake")
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

    private fun publishStatus() {
        val remaining = if (isRunning && currentMode == KeepAwakeKeys.MODE_TIMER && timerEndTime > 0L) {
            (timerEndTime - System.currentTimeMillis()).coerceAtLeast(0L)
        } else -1L
        val qs = mapOf(
            KeepAwakeKeys.MODE to currentMode.toString(),
            KeepAwakeKeys.TIMER_DURATION to timerDurationMinutes.toString(),
            KeepAwakeKeys.SHOW_NOTIFICATION to showNotification.toString()
        )
        val statusText = when {
            isRunning && currentMode == KeepAwakeKeys.MODE_TIMER && remaining >= 0 -> null
            isRunning && currentMode == KeepAwakeKeys.MODE_APP_BASED -> if (overlayView != null) "Uygulama etkin" else "Bekleniyor"
            isRunning -> "Açık"
            else -> null
        }
        PluginStatusPublisher.publish(
            context = this,
            pluginId = KeepAwakeKeys.PLUGIN_ID,
            running = isRunning,
            quickSettings = qs,
            remainingMs = remaining,
            statusText = statusText
        )
    }
}
