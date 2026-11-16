package com.toolbox.plugin.speedmeter

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.TrafficStats
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.toolbox.plugin.PluginSettingsManager
import com.toolbox.plugin.PluginStatusPublisher

/**
 * Ağ hızını saniyede bir ölçen ön plan servisi.
 *
 * Hızlar [TrafficStats] üzerinden okunur. Toplam, Wi-Fi (toplam - mobil)
 * veya mobil mod seçilebilir. Günlük/aylık kullanım [SpeedMeterStats]
 * içinde saklanır. Servis ayarları her turda yeniden okur; böylece ayar
 * ekranındaki değişiklikler anında uygulanır.
 */
class SpeedMeterService : Service() {

    companion object {
        const val CHANNEL_ID = "speedmeter_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.toolbox.plugin.speedmeter.START"
        const val ACTION_STOP = "com.toolbox.plugin.speedmeter.STOP"
        const val ACTION_RESET = "com.toolbox.plugin.speedmeter.RESET"

        const val ACTION_SPEED_UPDATE = "com.toolbox.plugin.speedmeter.SPEED_UPDATE"
        const val EXTRA_DOWNLOAD_SPEED = "download_speed"
        const val EXTRA_UPLOAD_SPEED = "upload_speed"
        const val EXTRA_DAILY_USAGE = "daily_usage"
        const val EXTRA_MONTHLY_USAGE = "monthly_usage"
        const val EXTRA_IS_RUNNING = "is_running"

        fun start(context: Context) {
            val intent = Intent(context, SpeedMeterService::class.java)
                .setAction(ACTION_START)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, SpeedMeterService::class.java))
        }

        fun resetStats(context: Context) {
            SpeedMeterStats.reset(context)
            val settings = PluginSettingsManager(context)
            val running = settings.getBoolean(
                SpeedMeterKeys.PLUGIN_ID,
                SpeedMeterKeys.SERVICE_RUNNING,
                false
            )
            if (running) {
                context.startService(
                    Intent(context, SpeedMeterService::class.java).setAction(ACTION_RESET)
                )
            }
        }
    }

    private lateinit var settings: PluginSettingsManager
    private val handler = Handler(Looper.getMainLooper())
    private var notificationManager: NotificationManager? = null
    private var connectivityManager: ConnectivityManager? = null

    private var showNotification = true
    private var showUpload = true
    private var networkMode = 0
    private var unitMode = 0

    private var usage = SpeedMeterUsage()
    private var lastRx = 0L
    private var lastTx = 0L
    private var lastDownload = 0L
    private var lastUpload = 0L

    private val tick = object : Runnable {
        override fun run() {
            calculateSpeed()
            handler.postDelayed(this, 1000L)
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            // Arayüz değiştiğinde sayaç farkı negatif olabilir; sıfırla.
            lastRx = currentRxBytes()
            lastTx = currentTxBytes()
        }
    }

    override fun onCreate() {
        super.onCreate()
        settings = PluginSettingsManager(this)
        notificationManager = getSystemService(NotificationManager::class.java)
        connectivityManager = getSystemService(ConnectivityManager::class.java)

        usage = SpeedMeterStats.read(this)
        val (savedRx, savedTx) = SpeedMeterStats.readCounters(this)
        lastRx = savedRx
        lastTx = savedTx
        if (savedRx == 0L && savedTx == 0L) {
            lastRx = currentRxBytes()
            lastTx = currentTxBytes()
            SpeedMeterStats.writeCounters(this, lastRx, lastTx)
        }

        createNotificationChannel()
        registerNetworkCallback()

        settings.putBoolean(SpeedMeterKeys.PLUGIN_ID, SpeedMeterKeys.SERVICE_RUNNING, true)

        // Ön plan bildirimi hemen gösterilmeli (5 saniye kuralı).
        startForegroundCompat(buildNotification(0L, 0L))
        publishStatus(0L, 0L)
        handler.post(tick)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_RESET -> {
                usage = SpeedMeterUsage()
                SpeedMeterStats.reset(this)
                lastRx = currentRxBytes()
                lastTx = currentTxBytes()
                SpeedMeterStats.writeCounters(this, lastRx, lastTx)
                notifyIfNeeded(0L, 0L)
            }

            else -> {
                settings.putBoolean(
                    SpeedMeterKeys.PLUGIN_ID,
                    SpeedMeterKeys.SERVICE_RUNNING,
                    true
                )
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        unregisterNetworkCallback()
        SpeedMeterStats.writeCounters(this, lastRx, lastTx)
        settings.putBoolean(SpeedMeterKeys.PLUGIN_ID, SpeedMeterKeys.SERVICE_RUNNING, false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        publishStoppedStatus()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ------------------------------------------------------------------
    // Ölçüm
    // ------------------------------------------------------------------

    private fun calculateSpeed() {
        loadSettings()

        val rx = currentRxBytes()
        val tx = currentTxBytes()

        val deltaRx = (rx - lastRx).coerceAtLeast(0L)
        val deltaTx = (tx - lastTx).coerceAtLeast(0L)

        lastRx = rx
        lastTx = tx
        lastDownload = deltaRx
        lastUpload = deltaTx

        usage = usage.copy(
            dailyRx = usage.dailyRx + deltaRx,
            dailyTx = usage.dailyTx + deltaTx,
            monthlyRx = usage.monthlyRx + deltaRx,
            monthlyTx = usage.monthlyTx + deltaTx
        )
        SpeedMeterStats.write(this, usage)
        SpeedMeterStats.writeCounters(this, lastRx, lastTx)

        broadcastSpeeds(deltaRx, deltaTx)
        notifyIfNeeded(deltaRx, deltaTx)
        publishStatus(deltaRx, deltaTx)
    }

    private fun loadSettings() {
        val pluginId = SpeedMeterKeys.PLUGIN_ID
        showNotification = settings.getBoolean(
            pluginId,
            SpeedMeterKeys.NOTIFICATION_ENABLED,
            true
        )
        showUpload = settings.getBoolean(pluginId, SpeedMeterKeys.SHOW_UPLOAD, true)
        networkMode = settings.getInt(pluginId, SpeedMeterKeys.NETWORK_MODE, 0)
        unitMode = settings.getInt(pluginId, SpeedMeterKeys.UNIT, 0)
    }

    private fun currentRxBytes(): Long = when (networkMode) {
        1 -> (TrafficStats.getTotalRxBytes() - TrafficStats.getMobileRxBytes())
            .coerceAtLeast(0L)
        2 -> TrafficStats.getMobileRxBytes()
        else -> TrafficStats.getTotalRxBytes()
    }

    private fun currentTxBytes(): Long = when (networkMode) {
        1 -> (TrafficStats.getTotalTxBytes() - TrafficStats.getMobileTxBytes())
            .coerceAtLeast(0L)
        2 -> TrafficStats.getMobileTxBytes()
        else -> TrafficStats.getTotalTxBytes()
    }

    private fun broadcastSpeeds(download: Long, upload: Long) {
        val intent = Intent(ACTION_SPEED_UPDATE)
            .setPackage(packageName)
            .putExtra(EXTRA_DOWNLOAD_SPEED, download)
            .putExtra(EXTRA_UPLOAD_SPEED, upload)
            .putExtra(EXTRA_DAILY_USAGE, usage.dailyTotal)
            .putExtra(EXTRA_MONTHLY_USAGE, usage.monthlyTotal)
            .putExtra(EXTRA_IS_RUNNING, true)
        sendBroadcast(intent)
    }

    // ------------------------------------------------------------------
    // Bildirim
    // ------------------------------------------------------------------

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.speedmeter_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.speedmeter_channel_desc)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun notifyIfNeeded(download: Long, upload: Long) {
        notificationManager?.notify(NOTIFICATION_ID, buildNotification(download, upload))
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(download: Long, upload: Long): Notification {
        val openIntent = Intent(this, SpeedMeterPluginActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, SpeedMeterService::class.java).setAction(ACTION_STOP)
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val text = when {
            !showNotification -> getString(R.string.speedmeter_notification_measuring)
            showUpload -> getString(
                R.string.speedmeter_notification_both,
                SpeedMeterFormat.speed(download, unitMode),
                SpeedMeterFormat.speed(upload, unitMode)
            )

            else -> getString(
                R.string.speedmeter_notification_download,
                SpeedMeterFormat.speed(download, unitMode)
            )
        }

        val bigText = if (showNotification) {
            getString(
                R.string.speedmeter_notification_big,
                SpeedMeterFormat.data(usage.dailyTotal, unitMode),
                SpeedMeterFormat.data(usage.monthlyTotal, unitMode)
            )
        } else {
            getString(R.string.speedmeter_notification_measuring)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_speed)
            .setContentTitle(getString(R.string.plugin_speedmeter_name))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setContentIntent(contentIntent)
            .addAction(
                R.drawable.ic_stop,
                getString(R.string.stop_service),
                stopPendingIntent
            )
            .build()
    }

    // ------------------------------------------------------------------
    // Ağ
    // ------------------------------------------------------------------

    private fun registerNetworkCallback() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        runCatching {
            connectivityManager?.registerNetworkCallback(request, networkCallback)
        }
    }

    private fun unregisterNetworkCallback() {
        runCatching {
            connectivityManager?.unregisterNetworkCallback(networkCallback)
        }
    }

    private fun publishStatus(download: Long, upload: Long) {
        try {
            val speedText = "↓ ${SpeedMeterFormat.speed(download, unitMode)}  ↑ ${SpeedMeterFormat.speed(upload, unitMode)}"
            val qs = mapOf(
                SpeedMeterKeys.NETWORK_MODE to networkMode.toString(),
                SpeedMeterKeys.UNIT to unitMode.toString(),
                SpeedMeterKeys.AUTOSTART to settings.getBoolean(SpeedMeterKeys.PLUGIN_ID, SpeedMeterKeys.AUTOSTART, true).toString()
            )
            PluginStatusPublisher.publish(
                context = this,
                pluginId = SpeedMeterKeys.PLUGIN_ID,
                running = true,
                quickSettings = qs,
                statusText = speedText
            )
        } catch (_: Exception) {
        }
    }

    private fun publishStoppedStatus() {
        try {
            val qs = mapOf(
                SpeedMeterKeys.NETWORK_MODE to settings.getInt(SpeedMeterKeys.PLUGIN_ID, SpeedMeterKeys.NETWORK_MODE, 0).toString(),
                SpeedMeterKeys.UNIT to settings.getInt(SpeedMeterKeys.PLUGIN_ID, SpeedMeterKeys.UNIT, 0).toString(),
                SpeedMeterKeys.AUTOSTART to settings.getBoolean(SpeedMeterKeys.PLUGIN_ID, SpeedMeterKeys.AUTOSTART, true).toString()
            )
            PluginStatusPublisher.publish(
                context = this,
                pluginId = SpeedMeterKeys.PLUGIN_ID,
                running = false,
                quickSettings = qs,
                statusText = null
            )
        } catch (_: Exception) {
        }
    }
}
