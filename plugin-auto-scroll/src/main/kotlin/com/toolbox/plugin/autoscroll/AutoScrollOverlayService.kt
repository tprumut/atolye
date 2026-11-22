package com.toolbox.plugin.autoscroll

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import com.toolbox.plugin.PluginSettingsManager
import kotlin.math.abs

/**
 * Otomatik kaydırma açıkken görünen küçük, sürüklenebilir durdurma düğmesi.
 * Kaydırma durduğunda tuş kendiliğinden kaldırılır.
 *
 * Ayrı bildirim/ön plan servisi yoktur: kaydırma servisi zaten ön planda olduğu
 * için bu servis yalnızca pencereyi yönetir.
 */
class AutoScrollOverlayService : Service() {

    companion object {
        private const val TAG = "AutoScrollOverlay"

        /**
         * Tercihi değiştirir ve tuşun görünürlüğünü kaydırma durumuna göre
         * yeniden hesaplar. İzin isteği göstermez; host hızlı ayarları için kullanılır.
         */
        fun setEnabled(context: Context, enabled: Boolean) {
            PluginSettingsManager(context).putBoolean(
                AutoScrollKeys.PLUGIN_ID, AutoScrollKeys.OVERLAY_VISIBLE, enabled
            )
            sync(context)
        }

        /** Ayar ekranındaki anahtar: izin yoksa izin ekranını açar. */
        fun enable(context: Context): Boolean {
            if (!AutoScrollPermissions.canDrawOverlays(context)) {
                AutoScrollPermissions.openOverlaySettings(context)
                return false
            }
            setEnabled(context, true)
            return true
        }

        fun disable(context: Context) = setEnabled(context, false)

        /**
         * Tuş yalnızca üç koşul birden sağlanıyorsa açılır:
         * kullanıcı tercihi açık, kaydırma çalışıyor ve ekran üstü izni var.
         */
        fun sync(context: Context) {
            val settings = PluginSettingsManager(context)
            val shouldShow =
                settings.getBoolean(
                    AutoScrollKeys.PLUGIN_ID,
                    AutoScrollKeys.OVERLAY_VISIBLE,
                    AutoScrollKeys.DEFAULT_OVERLAY_VISIBLE,
                ) && settings.getBoolean(
                    AutoScrollKeys.PLUGIN_ID, AutoScrollKeys.RUNNING, false
                ) && AutoScrollPermissions.canDrawOverlays(context)

            val intent = Intent(context, AutoScrollOverlayService::class.java)
            runCatching {
                if (shouldShow) context.startService(intent.setAction(AutoScrollKeys.ACTION_OVERLAY_SHOW))
                else context.stopService(intent)
            }
        }

        /** Uygulama açıldığında kayıtlı durum geri yüklenir. */
        fun ensureRunning(context: Context) = sync(context)
    }

    private lateinit var settings: PluginSettingsManager
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var iconView: ImageView? = null
    private var params: WindowManager.LayoutParams? = null
    private var screenW = 0
    private var screenH = 0
    private var lastRunning: Boolean? = null

    private val handler = Handler(Looper.getMainLooper())
    private val poll = object : Runnable {
        override fun run() {
            if (!shouldBeVisible()) {
                Log.d(TAG, "overlay no longer needed -> hiding")
                shutdown()
                return
            }
            val running = isRunning()
            if (lastRunning != running) {
                lastRunning = running
                updateIcon(running)
            }
            if (overlayView == null) addOverlay()
            handler.postDelayed(this, 1_000L)
        }
    }

    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var downAt = 0L
    private var moved = false

    override fun onCreate() {
        super.onCreate()
        settings = PluginSettingsManager(this)
        windowManager = getSystemService(WindowManager::class.java)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            AutoScrollKeys.ACTION_OVERLAY_HIDE -> {
                // Bildirimdeki "Yüzen tuşu kapat" eylemi tercihi de kapatır.
                settings.putBoolean(
                    AutoScrollKeys.PLUGIN_ID, AutoScrollKeys.OVERLAY_VISIBLE, false
                )
                shutdown()
                return START_NOT_STICKY
            }

            AutoScrollKeys.ACTION_OVERLAY_TOGGLE_SCROLL -> {
                toggleScroll()
                return START_NOT_STICKY
            }
        }

        if (!shouldBeVisible()) {
            Log.d(TAG, "start ignored: enabled=${overlayEnabled()} running=${isRunning()}")
            shutdown()
            return START_NOT_STICKY
        }
        if (overlayView == null) addOverlay()
        startPolling()
        return START_STICKY
    }

    override fun onDestroy() {
        hideView()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun overlayEnabled(): Boolean = settings.getBoolean(
        AutoScrollKeys.PLUGIN_ID,
        AutoScrollKeys.OVERLAY_VISIBLE,
        AutoScrollKeys.DEFAULT_OVERLAY_VISIBLE,
    )

    private fun isRunning(): Boolean = settings.getBoolean(
        AutoScrollKeys.PLUGIN_ID, AutoScrollKeys.RUNNING, false
    )

    private fun shouldBeVisible(): Boolean =
        overlayEnabled() && isRunning() && AutoScrollPermissions.canDrawOverlays(this)

    private fun startPolling() {
        handler.removeCallbacks(poll)
        handler.post(poll)
    }

    private fun toggleScroll() {
        when {
            isRunning() -> AutoScrollService.stop(this)
            AutoScrollPermissions.isAccessibilityEnabled(this) -> AutoScrollService.start(this)
            else -> AutoScrollPermissions.openAccessibilitySettings(this)
        }
    }

    private fun screenSize(): Pair<Int, Int> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = getSystemService(WindowManager::class.java)
                .currentWindowMetrics.bounds
            return bounds.width() to bounds.height()
        }
        @Suppress("DEPRECATION")
        val metrics = resources.displayMetrics
        return metrics.widthPixels to metrics.heightPixels
    }

    private fun addOverlay() {
        val density = resources.displayMetrics.density
        val size = (56 * density).toInt().coerceAtLeast(96)
        val (width, height) = screenSize()
        screenW = width
        screenH = height

        val icon = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding((12 * density).toInt(), (12 * density).toInt(),
                (12 * density).toInt(), (12 * density).toInt())
        }
        iconView = icon

        val view = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(size, size)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xFF6B7280.toInt())
            }
            elevation = 8f * density
            addView(icon)
        }

        val savedX = settings.getInt(AutoScrollKeys.PLUGIN_ID, AutoScrollKeys.OVERLAY_X, -1)
        val savedY = settings.getInt(AutoScrollKeys.PLUGIN_ID, AutoScrollKeys.OVERLAY_Y, -1)
        val defaultX = (width - size - 32 * density).toInt().coerceAtLeast(0)
        val defaultY = (height * 0.42f).toInt().coerceAtLeast(0)

        params = WindowManager.LayoutParams(
            size,
            size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = if (savedX >= 0) savedX.coerceAtMost((width - size).coerceAtLeast(0)) else defaultX
            y = if (savedY >= 0) savedY.coerceAtMost((height - size).coerceAtLeast(0)) else defaultY
        }

        view.setOnTouchListener { v, event ->
            val p = params ?: return@setOnTouchListener false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    lastX = event.rawX
                    lastY = event.rawY
                    downAt = System.currentTimeMillis()
                    moved = false
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    p.x += (event.rawX - lastX).toInt()
                    p.y += (event.rawY - lastY).toInt()
                    lastX = event.rawX
                    lastY = event.rawY
                    if (abs(event.rawX - downX) > 8 || abs(event.rawY - downY) > 8) moved = true
                    runCatching { windowManager?.updateViewLayout(v, p) }
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val isTap = !moved && System.currentTimeMillis() - downAt < 600
                    if (event.actionMasked == MotionEvent.ACTION_CANCEL) {
                        savePosition(p)
                    } else if (isTap) {
                        toggleScroll()
                    } else {
                        savePosition(p)
                    }
                    true
                }

                else -> false
            }
        }

        val added = runCatching { windowManager?.addView(view, params) }.isSuccess
        if (added) {
            overlayView = view
            val running = isRunning()
            lastRunning = running
            updateIcon(running)
            Log.d(TAG, "overlay added at (${params?.x}, ${params?.y}) size=$size")
        } else {
            Log.w(TAG, "addView failed")
            shutdown()
        }
    }

    private fun savePosition(p: WindowManager.LayoutParams) {
        p.x = p.x.coerceIn(0, (screenW - p.width).coerceAtLeast(0))
        p.y = p.y.coerceIn(0, (screenH - p.height).coerceAtLeast(0))
        runCatching { windowManager?.updateViewLayout(overlayView, p) }
        settings.putInt(AutoScrollKeys.PLUGIN_ID, AutoScrollKeys.OVERLAY_X, p.x)
        settings.putInt(AutoScrollKeys.PLUGIN_ID, AutoScrollKeys.OVERLAY_Y, p.y)
    }

    private fun updateIcon(running: Boolean) {
        val view = overlayView ?: return
        val color = if (running) 0xFF159764.toInt() else 0xFF6B7280.toInt()
        (view.background as? GradientDrawable)?.setColor(color)
        iconView?.setImageResource(
            if (running) R.drawable.ic_stop_autoscroll else R.drawable.ic_play_autoscroll
        )
        view.contentDescription = getString(
            if (running) R.string.auto_overlay_desc_running else R.string.auto_overlay_desc_stopped
        )
    }

    private fun shutdown() {
        hideView()
        stopSelf()
    }

    private fun hideView() {
        handler.removeCallbacks(poll)
        overlayView?.let { view -> runCatching { windowManager?.removeView(view) } }
        overlayView = null
        iconView = null
        params = null
        lastRunning = null
    }
}
