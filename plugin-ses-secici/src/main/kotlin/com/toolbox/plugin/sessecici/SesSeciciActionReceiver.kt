package com.toolbox.plugin.sessecici

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class SesSeciciActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        // Şu an için bildirimden ek aksiyon gerekmiyor; ileride genişletilebilir.
        SesSeciciDeviceService.refresh(context)
    }
}
