package com.toolbox.plugin.autoscroll

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import com.toolbox.plugin.PluginSettingsManager

enum class AutoScrollResult {
    SENT,
    NOT_CONNECTED,
    BLOCKED_APP,
    FAILED,
}

/** Gesture'ın gönderileceği pencere ve o pencerenin uygulaması. */
private data class ScrollTarget(val bounds: Rect?, val packageName: String?)

/** Pencere seçimi sırasında kullanılan basit aday bilgisi. */
private data class Candidate(
    val bounds: Rect,
    val packageName: String?,
    val isActive: Boolean,
)

private fun isBlockedPackage(packageName: String?, selfPackage: String): Boolean {
    if (packageName.isNullOrBlank()) return false
    if (packageName == selfPackage) return true
    if (packageName == "com.android.systemui") return true
    val launcherHints = listOf("launcher3", "launcher", "lawnchair", "nova", "home")
    return launcherHints.any { packageName.contains(it, ignoreCase = true) }
}

/**
 * Aktif uygulamaya gerçek dokunmatik kaydırma hareketi gönderen servis.
 *
 * Video/reels oynatmaz; yalnızca ekranı kaydırır. canPerformGestures
 * accessibility_service_config.xml içinde açılır.
 *
 * Bölünmüş ekranda tüm ekran boyutları yerine hedef pencerenin `bounds`
 * değeri kullanılır; aksi halde gesture ayırıcıya veya yanlış bölmeye gider.
 */
class AutoScrollAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "AutoScrollAccessibility"

        @Volatile
        var instance: AutoScrollAccessibilityService? = null
            private set

        /** Son görülen uygulama paketi; ana ekran/systemui atlanır. */
        @Volatile
        var currentPackage: String? = null
            private set

        @Volatile
        var lastGestureOk: Boolean = false
            private set

        fun performScroll(direction: String): AutoScrollResult {
            val service = instance ?: return AutoScrollResult.NOT_CONNECTED
            val target = service.resolveTarget()
            val packageName = target.packageName
            if (isBlockedPackage(packageName, service.packageName)) {
                Log.d(TAG, "performScroll BLOCKED_APP pkg=$packageName")
                return AutoScrollResult.BLOCKED_APP
            }
            // Durum metni gerçek hedefi göstersin (bölünmüş ekranda olay
            // kaynaklı paket yanıltıcı olabiliyordu).
            if (!packageName.isNullOrBlank()) {
                service.settings.putString(
                    AutoScrollKeys.PLUGIN_ID,
                    AutoScrollKeys.LAST_APP,
                    packageName,
                )
            }
            service.handler.post { service.dispatchScroll(direction, target.bounds) }
            return AutoScrollResult.SENT
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private val settings by lazy { PluginSettingsManager(this) }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "onServiceConnected")
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        Log.d(TAG, "onUnbind")
        instance = null
        currentPackage = null
        AutoScrollService.publishState(applicationContext)
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        if (instance === this) instance = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString()?.takeIf { it.isNotBlank() } ?: return
        if (packageName == currentPackage) return
        currentPackage = packageName
        val resolved = resolveTarget().packageName ?: packageName
        settings.putString(AutoScrollKeys.PLUGIN_ID, AutoScrollKeys.LAST_APP, resolved)
        AutoScrollService.publishState(applicationContext)
    }

    override fun onInterrupt() {}

    /**
     * Hedef pencereyi seçer. `flagRetrieveInteractiveWindows` açık olduğu için
     * `windows` bölünmüş ekrandaki tüm görünür pencereleri içerir.
     *
     * - Aktif: odaktaki uygulama penceresi.
     * - Sol/Üst – Sağ/Alt: pencerelerin konumuna göre sıralanmış ilk/son pencere.
     * - Uygulama penceresi yoksa tam ekran boyutlarına düşülür.
     */
    private fun resolveTarget(): ScrollTarget {
        val candidates = windows
            .filter { window -> window.type == AccessibilityWindowInfo.TYPE_APPLICATION }
            .mapNotNull { window ->
                val rect = Rect().also { window.getBoundsInScreen(it) }
                val pkg = window.root?.packageName?.toString()
                if (rect.width() >= 200 && rect.height() >= 200 &&
                    !pkg.isNullOrBlank() && !isBlockedPackage(pkg, packageName)
                ) {
                    Candidate(rect, pkg, window.isActive)
                } else {
                    null
                }
            }

        if (candidates.isEmpty()) {
            val pkg = try {
                rootInActiveWindow?.packageName?.toString()
            } catch (_: Throwable) {
                null
            }
            return ScrollTarget(null, pkg)
        }

        val target = settings.getString(
            AutoScrollKeys.PLUGIN_ID,
            AutoScrollKeys.TARGET,
            AutoScrollKeys.DEFAULT_TARGET,
        )

        val selected = when (target) {
            AutoScrollKeys.TARGET_PRIMARY, AutoScrollKeys.TARGET_SECONDARY -> {
                val minLeft = candidates.minOf { it.bounds.left }
                val sideBySide = candidates.any { it.bounds.left > minLeft + 100 }
                when {
                    sideBySide && target == AutoScrollKeys.TARGET_PRIMARY ->
                        candidates.minWith(compareBy<Candidate> { it.bounds.left })
                    sideBySide ->
                        candidates.maxWith(compareBy<Candidate> { it.bounds.left })
                    target == AutoScrollKeys.TARGET_PRIMARY ->
                        candidates.minWith(compareBy<Candidate> { it.bounds.top })
                    else ->
                        candidates.maxWith(compareBy<Candidate> { it.bounds.top })
                }
            }

            else -> candidates.firstOrNull { it.isActive }
                ?: candidates.maxWith(
                    compareBy<Candidate> { it.bounds.width() * it.bounds.height() }
                )
        }

        Log.d(
            TAG,
            "resolve target=$target count=${candidates.size} " +
                "picked=${selected.bounds} pkg=${selected.packageName} active=${selected.isActive}",
        )
        return ScrollTarget(selected.bounds, selected.packageName)
    }

    /**
     * Tam ekran için gerçek piksel boyutları. `resources.displayMetrics`
     * erişilebilirlik servisinde cihaz yatayken dikey (doğal) boyutları
     * döndürebilir; `currentWindowMetrics` mevcut rotasyona göre doğru sınırları
     * verir. Bölünmüş ekranda ise hedef pencerenin `bounds` değeri kullanılır.
     */
    private fun screenSize(): Pair<Int, Int> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = getSystemService(WindowManager::class.java)
                .currentWindowMetrics.bounds
            return bounds.width() to bounds.height()
        }
        @Suppress("DEPRECATION")
        val metrics = android.util.DisplayMetrics()
        @Suppress("DEPRECATION")
        getSystemService(WindowManager::class.java).defaultDisplay.getRealMetrics(metrics)
        return metrics.widthPixels to metrics.heightPixels
    }

    private fun dispatchScroll(direction: String, targetBounds: Rect?) {
        val (widthInt, heightInt) = targetBounds?.let { it.width() to it.height() }
            ?: screenSize()
        val offsetX = targetBounds?.left ?: 0
        val offsetY = targetBounds?.top ?: 0
        val width = widthInt.toFloat().coerceAtLeast(1f)
        val height = heightInt.toFloat().coerceAtLeast(1f)
        val centerX = offsetX + width / 2f
        val scrollUp = direction != AutoScrollKeys.DIRECTION_DOWN
        val startY = offsetY + if (scrollUp) height * 0.76f else height * 0.24f
        val endY = offsetY + if (scrollUp) height * 0.24f else height * 0.76f

        val duration = settings.getInt(
            AutoScrollKeys.PLUGIN_ID,
            AutoScrollKeys.SWIPE_DURATION_MS,
            AutoScrollKeys.DEFAULT_SWIPE_DURATION_MS,
        ).coerceIn(100, 900).toLong()

        Log.d(
            TAG,
            "dispatch dir=$direction bounds=${targetBounds ?: "full"} " +
                "win=${widthInt}x${heightInt} from=($centerX,$startY) to=($centerX,$endY) dur=$duration",
        )

        val path = Path().apply {
            moveTo(centerX, startY)
            lineTo(centerX, endY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0L, duration)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        lastGestureOk = false
        val dispatched = dispatchGesture(
            gesture,
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    lastGestureOk = true
                    Log.d(TAG, "gesture onCompleted")
                    settings.putString(
                        AutoScrollKeys.PLUGIN_ID,
                        AutoScrollKeys.LAST_RESULT,
                        getString(R.string.auto_result_sent),
                    )
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    lastGestureOk = false
                    Log.w(TAG, "gesture onCancelled")
                    settings.putString(
                        AutoScrollKeys.PLUGIN_ID,
                        AutoScrollKeys.LAST_RESULT,
                        getString(R.string.auto_result_failed),
                    )
                }
            },
            handler,
        )
        if (!dispatched) {
            Log.w(TAG, "dispatchGesture returned false")
            settings.putString(
                AutoScrollKeys.PLUGIN_ID,
                AutoScrollKeys.LAST_RESULT,
                getString(R.string.auto_result_failed),
            )
        }
    }
}
