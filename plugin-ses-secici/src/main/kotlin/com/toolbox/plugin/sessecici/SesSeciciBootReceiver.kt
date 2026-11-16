package com.toolbox.plugin.sessecici

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.toolbox.plugin.PluginSettingsManager

class SesSeciciBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val settings = PluginSettingsManager(context)
        val auto = settings.getBoolean(SesSeciciKeys.PLUGIN_ID, SesSeciciKeys.AUTO_APPLY_ON_CONNECT, true)
        val notif = settings.getBoolean(SesSeciciKeys.PLUGIN_ID, SesSeciciKeys.NOTIFICATION_ENABLED, true)
        if (auto || notif) SesSeciciDeviceService.start(context)
    }
}
