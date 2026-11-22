package com.toolbox.plugin.speedmeter

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.TrafficStats
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.toolbox.plugin.PluginSettingsManager
import com.toolbox.plugin.StatusBarIconPolicy
import com.toolbox.plugin.PluginStatusPublisher

/**
 * Ağ hızını saniyede bir ölçen ön plan servisi.
 *
 * Hızlar [TrafficStats] üzerinden okunur. Toplam, Wi-Fi (toplam - mobil)
 * veya mobil mod seçilebilir. Günlük/aylık kullanım [SpeedMeterStats]
 * içinde saklanır. Servis ayarları her turda yeniden okur; böylece ayar
 * ekranındaki değişiklikler anında uygulanır.
 *
 * Canlı hız, ekranda popup olarak değil **durum çubuğundaki küçük ikonun
 * yerine** gösterilir. Her saniye üretilen bitmap, örn. `250K` / `1.2M`
 * değerini taşır; bildirim panelinde de aynı anlık değerler yazılır.
 */
class SpeedMeterService : Service() {

    companion object {
        // Eski kanal IMPORTANCE_LOW olduğu için bazı OEM arayüzlerinde status bar
        // ikonu bastırılıyordu. Yeni ID ile kanal DEFAULT önemde yeniden oluşturulur.
        const val CHANNEL_ID = "speedmeter_channel_v2"
        const val LEGACY_CHANNEL_ID = "speedmeter_channel"
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
    private var windowManager: WindowManager? = null
    private var speedOverlay: LinearLayout? = null
    private var speedOverlayText: TextView? = null
    private var speedOverlayIcons: LinearLayout? = null
    private var overlayTextColor: Int = Color.WHITE

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
        windowManager = getSystemService(WindowManager::class.java)

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

        // Durum çubuğunda hız sayısı öncelikli; diğer eklenti ikonları gizlenir.
        StatusBarIconPolicy.setSpeedVisible(this, true)

        // Sol üstteki durum çubuğunda hız, saat puntousunda gösterilir.
        // Bildirim kendisi standart kalır.
        showSpeedOverlay()

        settings.putBoolean(SpeedMeterKeys.PLUGIN_ID, SpeedMeterKeys.SERVICE_RUNNING, true)

        // Ön plan bildirimi hemen gösterilmeli (5 saniye kuralı).
        loadSettings()
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
        hideSpeedOverlay()
        unregisterNetworkCallback()
        SpeedMeterStats.writeCounters(this, lastRx, lastTx)
        settings.putBoolean(SpeedMeterKeys.PLUGIN_ID, SpeedMeterKeys.SERVICE_RUNNING, false)
        // Hız kapanınca diğer eklentiler normal ikonlarına döner.
        StatusBarIconPolicy.setSpeedVisible(this, false)
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
        showUpload = settings.getBoolean(pluginId, SpeedMeterKeys.SHOW_UPLOAD, true)
        networkMode = settings.getInt(pluginId, SpeedMeterKeys.NETWORK_MODE, 0)
        // KB/s ve otomatik birimler kaldırıldı: her zaman MB/s kullanılır.
        unitMode = 2
        if (settings.getInt(pluginId, SpeedMeterKeys.UNIT, 2) != 2) {
            settings.putInt(pluginId, SpeedMeterKeys.UNIT, 2)
        }
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
            // Sessiz ama status bar'da görünür olması için DEFAULT önem; ses kapalı.
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.speedmeter_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.speedmeter_channel_desc)
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager?.deleteNotificationChannel(LEGACY_CHANNEL_ID)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun notifyIfNeeded(download: Long, upload: Long) {
        notificationManager?.notify(NOTIFICATION_ID, buildNotification(download, upload))
        speedOverlayText?.text = SpeedMeterFormat.speed(download + upload, unitMode)
        updateOverlayIcons()
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

        // Panelde aktif internet hızı tek değer olarak gösterilir.
        // Durum çubuğundaki sayı ayrı bir overlay ile çizilir; bildirimin
        // small icon'u sabit simgedir, böylece hız iki kez görünmez.
        val speedText = SpeedMeterFormat.activeSpeed(download, upload, unitMode)

        // Standart bildirim: başlıkta aktif hız, genişletilmişte günlük/aylık veri.
        val bigText = speedText + "\n" + getString(
            R.string.speedmeter_notification_big,
            SpeedMeterFormat.data(usage.dailyTotal, unitMode),
            SpeedMeterFormat.data(usage.monthlyTotal, unitMode)
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            // Hız açıkken sistem ikonu saydam; hız metni ve diğer eklenti
            // ikonları overlay içinde "saat | hız | ikonlar" sırasıyla çizilir.
            .setSmallIcon(StatusBarIconPolicy.smallIcon(this, R.drawable.ic_notification_speed))
            .setContentTitle(speedText)
            .setContentText(speedText)
            .setSubText(getString(R.string.plugin_speedmeter_name))
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            // Hız bildirimi her saniye güncellenir; sıralama anahtarı olmazsa
            // diğer güncellenen bildirimlerle yer değiştirir. En küçük anahtar
            // ve yüksek önem ile daima en üstte tutulur.
            .setSortKey("000_atolye_speedmeter")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // 4+ bildirimde sistemin otomatik "ranker_group" grubu kurup tüm ikonları
            // tek genel ikonda gizlemesini engelle: her eklentinin kendi grubu olsun.
            .setGroup("atolye_speedmeter")
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
    // Sol üst durum çubuğu (saat boyutunda hız)
    // ------------------------------------------------------------------

    /**
     * Saatin hemen sağına yerleşen sabit hız göstergesi:
     * "saat | hız | aktif eklenti ikonları".
     * Sistem bildirim ikonları hız açıkken saydam olduğu için eklenti
     * ikonlarını kendi overlay'imizde çiziyoruz; böylece ikonlar hızın
     * arkasına düşmez ve istenen sıra korunur.
     */
    private fun showSpeedOverlay() {
        if (speedOverlay != null) return
        val wm = windowManager ?: return

        val isNight = resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        overlayTextColor = if (isNight) Color.WHITE else Color.rgb(32, 33, 36)

        val statusBarHeightRes = resources.getIdentifier(
            "status_bar_height", "dimen", "android"
        )
        val statusBarHeight = if (statusBarHeightRes > 0) {
            resources.getDimensionPixelSize(statusBarHeightRes)
        } else {
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                24f,
                resources.displayMetrics
            ).toInt()
        }
        // Hız saatin hemen sağında; eklenti ikonları onun ardından çizilir.
        val startX = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            78f,
            resources.displayMetrics
        ).toInt()

        // Sağdaki sistem ikonları (Wi-Fi, batarya, ...) için güvenli pay.
        val displayWidth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            wm.currentWindowMetrics.bounds.width()
        } else {
            @Suppress("DEPRECATION")
            resources.displayMetrics.widthPixels
        }
        val rightReserve = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            120f,
            resources.displayMetrics
        ).toInt()
        val maxOverlayWidth = (displayWidth - startX - rightReserve).coerceAtLeast(0)
        Log.i(
            "SpeedMeterOverlay",
            "displayWidth=$displayWidth startX=$startX rightReserve=$rightReserve " +
                "maxWidth=$maxOverlayWidth"
        )

        val text = TextView(this).apply {
            text = SpeedMeterFormat.speed(0L, unitMode)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(overlayTextColor)
            setIncludeFontPadding(false)
            gravity = Gravity.CENTER_VERTICAL
            isSingleLine = true
            maxWidth = maxOverlayWidth
            ellipsize = TextUtils.TruncateAt.END
        }
        val icons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val view = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(
                text,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
            )
            addView(
                icons,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
            )
        }
        speedOverlayText = text
        speedOverlayIcons = icons

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            statusBarHeight,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.START or Gravity.TOP
            x = startX
            // Saatin baseline'ına hizalamak için status bar merkezinden biraz yukarı.
            y = -TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                2f,
                resources.displayMetrics
            ).toInt()
        }

        runCatching {
            wm.addView(view, params)
            speedOverlay = view
            updateOverlayIcons()
        }
    }

    private fun hideSpeedOverlay() {
        speedOverlay?.let { view ->
            runCatching { windowManager?.removeView(view) }
        }
        speedOverlay = null
        speedOverlayText = null
        speedOverlayIcons = null
    }

    /** Aktif eklenti bildirimlerinin ikonlarını hız metninin sağına çizer. */
    private fun updateOverlayIcons() {
        val container = speedOverlayIcons ?: return
        val nm = notificationManager ?: return
        val active = nm.activeNotifications
            .filter { it.packageName == packageName && it.id != NOTIFICATION_ID }
            .sortedBy { it.notification.sortKey ?: "" }
        val resIds = active.map { sbn ->
            when (sbn.id) {
                2001 -> resources.getIdentifier(
                    "ic_notification_keepawake", "drawable", packageName
                )
                3001, 3002 -> resources.getIdentifier(
                    "ic_notification_uykucu", "drawable", packageName
                )
                4001 -> resources.getIdentifier(
                    "ic_notification_sessecici", "drawable", packageName
                )
                5001 -> resources.getIdentifier(
                    "ic_notification_autoscroll", "drawable", packageName
                )
                else -> 0
            }
        }
        val key = active.joinToString(",") { it.id.toString() }
        if (container.tag == key) return
        container.tag = key
        Log.i(
            "SpeedMeterOverlay",
            "icons ids=${active.map { it.id }} resIds=$resIds key=$key"
        )
        container.removeAllViews()

        val size = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 18f, resources.displayMetrics
        ).toInt()
        val margin = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 4f, resources.displayMetrics
        ).toInt()
        for ((index, sbn) in active.withIndex()) {
            val resId = resIds[index]
            if (resId == 0) continue
            val icon = ImageView(this).apply {
                setImageResource(resId)
                setColorFilter(overlayTextColor, PorterDuff.Mode.SRC_ATOP)
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
            val lp = LinearLayout.LayoutParams(size, LinearLayout.LayoutParams.MATCH_PARENT)
            lp.marginStart = margin
            container.addView(icon, lp)
        }
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
            val speedText = SpeedMeterFormat.activeSpeed(download, upload, unitMode)
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
                SpeedMeterKeys.UNIT to settings.getInt(SpeedMeterKeys.PLUGIN_ID, SpeedMeterKeys.UNIT, 2).toString(),
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
