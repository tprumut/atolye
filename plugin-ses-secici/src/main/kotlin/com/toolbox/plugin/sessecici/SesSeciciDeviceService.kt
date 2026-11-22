package com.toolbox.plugin.sessecici

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.toolbox.plugin.PluginSettingsManager
import com.toolbox.plugin.PluginStatusPublisher
import com.toolbox.plugin.StatusBarIconPolicy

class SesSeciciDeviceService : Service() {

    companion object {
        const val CHANNEL_ID = "sessecici_channel_v2"
        const val LEGACY_CHANNEL_ID = "sessecici_channel"
        const val NOTIF_ID = 4001
        const val ACTION_START = "com.toolbox.plugin.sessecici.START"
        const val ACTION_REFRESH = "com.toolbox.plugin.sessecici.REFRESH"
        const val ACTION_STOP_NOTIF = "com.toolbox.plugin.sessecici.STOP_NOTIF"

        fun start(context: Context) {
            val i = Intent(context, SesSeciciDeviceService::class.java).setAction(ACTION_START)
            try { ContextCompat.startForegroundService(context, i) } catch (_: Throwable) {}
        }

        fun refresh(context: Context) {
            val i = Intent(context, SesSeciciDeviceService::class.java).setAction(ACTION_REFRESH)
            try { context.startService(i) } catch (_: Throwable) {}
        }
    }

    private lateinit var settings: PluginSettingsManager
    private var am: AudioManager? = null
    private var nm: NotificationManager? = null
    private var callback: AudioDeviceCallback? = null
    private val handler = Handler(Looper.getMainLooper())
    private var fg = false

    private val iconPolicyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (fg) updateNotification()
        }
    }

    override fun onCreate() {
        super.onCreate()
        settings = PluginSettingsManager(this)
        am = getSystemService(AudioManager::class.java)
        nm = getSystemService(NotificationManager::class.java)
        createChannel()
        registerCallback()
        StatusBarIconPolicy.register(this, iconPolicyReceiver)
        ensureForegroundOrStop()
        publishState(true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_NOTIF -> {
                settings.putBoolean(SesSeciciKeys.PLUGIN_ID, SesSeciciKeys.NOTIFICATION_ENABLED, false)
                stopForegroundClean()
                return START_NOT_STICKY
            }
            ACTION_REFRESH -> { updateNotification(); publishState(fg) }
            else -> { ensureForegroundOrStop(); publishState(fg) }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        publishState(false)
        unregisterCallback()
        StatusBarIconPolicy.unregister(this, iconPolicyReceiver)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun registerCallback() {
        if (callback != null) return
        val cb = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                val auto = settings.getBoolean(SesSeciciKeys.PLUGIN_ID, SesSeciciKeys.AUTO_APPLY_ON_CONNECT, true)
                if (auto) {
                    addedDevices?.forEach { d ->
                        // kısa gecikme ile route otursun
                        handler.postDelayed({ SesSeciciAudio.applySavedForDevice(this@SesSeciciDeviceService, settings, d) }, 700)
                    }
                }
                updateNotification()
                publishState(fg)
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                updateNotification()
                publishState(fg)
            }
        }
        callback = cb
        try { am?.registerAudioDeviceCallback(cb, handler) } catch (_: Throwable) {}
    }

    private fun unregisterCallback() {
        try { callback?.let { am?.unregisterAudioDeviceCallback(it) } } catch (_: Throwable) {}
        callback = null
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_DEFAULT)
            ch.description = getString(R.string.notification_channel_desc)
            ch.setShowBadge(false)
            ch.setSound(null, null)
            ch.enableVibration(false)
            ch.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            nm?.deleteNotificationChannel(LEGACY_CHANNEL_ID)
            nm?.createNotificationChannel(ch)
        }
    }

    private fun ensureForegroundOrStop() {
        val enabled = settings.getBoolean(SesSeciciKeys.PLUGIN_ID, SesSeciciKeys.NOTIFICATION_ENABLED, true)
        val auto = settings.getBoolean(SesSeciciKeys.PLUGIN_ID, SesSeciciKeys.AUTO_APPLY_ON_CONNECT, true)
        if (!enabled && !auto) {
            // Otomatik uygulama için callback gerekli; bildirim kapalıysa yine de arka planda tutmak gerekebilir.
            // Ancak kullanıcı bildirimi kapattıysa FGS göstermeden callback'i tutamayız güvenilir şekilde; bu durumda sadece açıkken çalışır.
            stopForegroundClean()
            return
        }
        val n = buildNotification()
        try {
            if (!fg) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                } else startForeground(NOTIF_ID, n)
                fg = true
            } else nm?.notify(NOTIF_ID, n)
        } catch (_: Throwable) {}
    }

    private fun updateNotification() {
        val enabled = settings.getBoolean(SesSeciciKeys.PLUGIN_ID, SesSeciciKeys.NOTIFICATION_ENABLED, true)
        if (!fg && enabled) ensureForegroundOrStop() else if (fg) nm?.notify(NOTIF_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val devices = SesSeciciAudio.listOutputDevices(this, settings)
        val current = devices.firstOrNull { it.isCurrent }?.name ?: devices.firstOrNull()?.name ?: getString(R.string.device_speaker)
        val openIntent = Intent(this, SesSeciciPluginActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pi = PendingIntent.getActivity(this, 0, openIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(StatusBarIconPolicy.smallIcon(this, R.drawable.ic_notification_sessecici))
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text, current))
            .setContentIntent(pi)
            .setSortKey("300_atolye_sessecici")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setGroup("atolye_sessecici")
            .build()
    }

    private fun stopForegroundClean() {
        try { stopForeground(STOP_FOREGROUND_REMOVE) } catch (_: Throwable) {}
        fg = false
        publishState(false)
        stopSelf()
    }

    private fun publishState(running: Boolean) {
        try {
            val qs = mapOf(
                SesSeciciKeys.AUTO_APPLY_ON_CONNECT to settings.getBoolean(SesSeciciKeys.PLUGIN_ID, SesSeciciKeys.AUTO_APPLY_ON_CONNECT, true).toString(),
                SesSeciciKeys.NOTIFICATION_ENABLED to settings.getBoolean(SesSeciciKeys.PLUGIN_ID, SesSeciciKeys.NOTIFICATION_ENABLED, true).toString(),
                SesSeciciKeys.EXPERIMENTAL_APP_AUDIO to settings.getBoolean(SesSeciciKeys.PLUGIN_ID, SesSeciciKeys.EXPERIMENTAL_APP_AUDIO, false).toString()
            )
            val text = try {
                val devices = SesSeciciAudio.listOutputDevices(this, settings)
                devices.firstOrNull { it.isCurrent }?.name ?: devices.firstOrNull()?.name
            } catch (_: Exception) { null }
            PluginStatusPublisher.publish(this, SesSeciciKeys.PLUGIN_ID, running, qs, statusText = text)
        } catch (_: Exception) {}
    }
}
