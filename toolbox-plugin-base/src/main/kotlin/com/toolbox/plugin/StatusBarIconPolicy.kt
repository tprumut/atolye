package com.toolbox.plugin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.drawable.IconCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager

/**
 * Durum çubuğu eklenti ikonları.
 *
 * Hız göstergesi açıkken sistemin çizdiği küçük ikonlar saydamlaştırılır;
 * eklenti ikonları hız metninin sağına kendi overlay'imiz içinde çizilir.
 * Böylece düzen "saat | hız | eklenti ikonları" olur ve sistem ikonları
 * hız metninin altına düşmez. Hız kapanınca [ACTION_CHANGED] ile servisler
 * kendi ikonlarına döner.
 */
object StatusBarIconPolicy {

    const val ACTION_CHANGED = "com.toolbox.STATUSBAR_ICON_POLICY_CHANGED"

    @Volatile
    private var speedVisible = false

    fun setSpeedVisible(context: Context, visible: Boolean) {
        if (speedVisible == visible) return
        speedVisible = visible
        LocalBroadcastManager.getInstance(context.applicationContext)
            .sendBroadcast(Intent(ACTION_CHANGED))
    }

    fun isSpeedVisible(): Boolean = speedVisible

    /** Hız açıksa görünmez ikon, kapalıysa eklentinin kendi ikonu. */
    fun smallIcon(context: Context, normalResId: Int): IconCompat =
        if (speedVisible) {
            IconCompat.createWithBitmap(TRANSPARENT_ICON)
        } else {
            IconCompat.createWithResource(context, normalResId)
        }

    fun register(context: Context, receiver: BroadcastReceiver) {
        LocalBroadcastManager.getInstance(context.applicationContext)
            .registerReceiver(receiver, IntentFilter(ACTION_CHANGED))
    }

    fun unregister(context: Context, receiver: BroadcastReceiver) {
        runCatching {
            LocalBroadcastManager.getInstance(context.applicationContext)
                .unregisterReceiver(receiver)
        }
    }

    private val TRANSPARENT_ICON: Bitmap by lazy {
        Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.TRANSPARENT)
        }
    }
}
