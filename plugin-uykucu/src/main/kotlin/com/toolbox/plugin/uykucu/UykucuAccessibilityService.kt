package com.toolbox.plugin.uykucu

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Uykucu erişilebilirlik servisi.
 *
 * - GLOBAL_ACTION_LOCK_SCREEN ile ekranı kilitler.
 * - İsteğe bağlı: HOME -> RECENTS -> "Tümünü temizle" benzeri düğmeyi bulup tıklar.
 *   Cihaza göre arayüz farkları olabileceği için bu adım try/catch içinde tutulur
 *   ve başarısız olursa sessizce ana ekrana dönülür.
 */
class UykucuAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: UykucuAccessibilityService? = null
            private set

        fun lockScreenNow(): Boolean {
            return try {
                val svc = instance ?: return false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    svc.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                } else {
                    false
                }
            } catch (_: Throwable) {
                false
            }
        }

        fun goHome() {
            try { instance?.performGlobalAction(GLOBAL_ACTION_HOME) } catch (_: Throwable) {}
        }

        fun tryCloseAppsThen(onDone: () -> Unit) {
            val svc = instance
            if (svc == null) {
                onDone()
                return
            }
            try {
                svc.startCloseAppsSequence(onDone)
            } catch (_: Throwable) {
                try { svc.performGlobalAction(GLOBAL_ACTION_HOME) } catch (_: Throwable) {}
                onDone()
            }
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var closeAppsCallback: (() -> Unit)? = null
    private var clearAttempted = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        serviceInfo = serviceInfo?.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        instance = null
        closeAppsCallback = null
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (closeAppsCallback == null || clearAttempted) return
        try {
            val root = rootInActiveWindow ?: return
            val clicked = findAndClickClearAll(root)
            root.recycle()
            if (clicked) {
                clearAttempted = true
                handler.postDelayed({ finishCloseApps() }, 500)
            }
        } catch (_: Throwable) {
            // sessizce yut
        }
    }

    override fun onInterrupt() {}

    private fun startCloseAppsSequence(onDone: () -> Unit) {
        closeAppsCallback = onDone
        clearAttempted = false
        try {
            performGlobalAction(GLOBAL_ACTION_HOME)
        } catch (_: Throwable) {}
        handler.postDelayed({
            try {
                performGlobalAction(GLOBAL_ACTION_RECENTS)
            } catch (_: Throwable) {
                finishCloseApps()
                return@postDelayed
            }
            // Recents açıldıktan sonra içerik taraması onAccessibilityEvent ile yapılır.
            // Belirli süre içinde bulunamazsa ana ekrana dön ve devam et.
            handler.postDelayed({ finishCloseApps() }, 2200)
        }, 450)
    }

    private fun finishCloseApps() {
        if (closeAppsCallback == null) return
        val cb = closeAppsCallback
        closeAppsCallback = null
        clearAttempted = false
        try { performGlobalAction(GLOBAL_ACTION_HOME) } catch (_: Throwable) {}
        handler.postDelayed({ cb?.invoke() }, 300)
    }

    /** "Tümünü temizle" benzeri düğmeyi bulup tıklar. */
    private fun findAndClickClearAll(node: AccessibilityNodeInfo): Boolean {
        try {
            val texts = listOfNotNull(node.text?.toString(), node.contentDescription?.toString())
            val match = texts.any { t ->
                val s = t.lowercase()
                s.contains("clear all") || s.contains("close all") ||
                    s.contains("tümünü temizle") || s.contains("tumunu temizle") ||
                    s.contains("tümünü kapat") || s.contains("tumunu kapat") ||
                    s.contains("hepsini temizle") || s.contains("hepsini kapat") ||
                    s.contains("tümünü sil") || s.contains("temizle") && s.contains("tüm")
            }
            if (match && node.isClickable) {
                return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            if (match) {
                var parent: AccessibilityNodeInfo? = node.parent
                while (parent != null) {
                    if (parent.isClickable) {
                        val ok = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        parent.recycle()
                        return ok
                    }
                    val next = parent.parent
                    parent.recycle()
                    parent = next
                }
            }
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                val found = findAndClickClearAll(child)
                child.recycle()
                if (found) return true
            }
        } catch (_: Throwable) {
        }
        return false
    }
}
