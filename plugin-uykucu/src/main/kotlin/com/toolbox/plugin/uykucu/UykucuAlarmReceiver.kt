package com.toolbox.plugin.uykucu

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class UykucuAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val svc = Intent(context, UykucuTimerService::class.java).setAction(action)
        try {
            ContextCompat.startForegroundService(context, svc)
        } catch (_: Exception) {
            // Servis başlatılamazsa sessizce geç.
        }
    }
}
