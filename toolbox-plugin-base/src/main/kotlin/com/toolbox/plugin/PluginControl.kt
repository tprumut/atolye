package com.toolbox.plugin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.toolbox.contract.PluginContract

/**
 * Ana uygulamadan eklentiye komut gönderir.
 * İmza seviyesi izin [PluginContract.PERMISSION_CONTROL_PLUGIN] ile korunmalıdır.
 */
object PluginCommandSender {

    fun sendCommand(
        context: Context,
        pluginPackage: String,
        receiverClass: String?,
        command: String,
        settingKey: String? = null,
        settingValue: String? = null
    ): Boolean {
        val intent = Intent(PluginContract.ACTION_PLUGIN_COMMAND).apply {
            if (!receiverClass.isNullOrBlank()) {
                setClassName(pluginPackage, receiverClass)
            } else {
                setPackage(pluginPackage)
            }
            putExtra(PluginContract.EXTRA_COMMAND, command)
            settingKey?.let { putExtra(PluginContract.EXTRA_SETTING_KEY, it) }
            settingValue?.let { putExtra(PluginContract.EXTRA_SETTING_VALUE, it) }
        }
        return try {
            context.sendBroadcast(intent, PluginContract.PERMISSION_CONTROL_PLUGIN)
            true
        } catch (e: Exception) {
            try {
                // Bazı durumlarda izin parametresi hedef pakette sorun çıkarabilir; yine de dene
                context.sendBroadcast(intent)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    fun requestState(context: Context, pluginPackage: String, receiverClass: String?): Boolean =
        sendCommand(context, pluginPackage, receiverClass, PluginContract.COMMAND_GET_STATE)

    fun start(context: Context, pluginPackage: String, receiverClass: String?): Boolean =
        sendCommand(context, pluginPackage, receiverClass, PluginContract.COMMAND_START)

    fun stop(context: Context, pluginPackage: String, receiverClass: String?): Boolean =
        sendCommand(context, pluginPackage, receiverClass, PluginContract.COMMAND_STOP)

    fun setQuickSetting(
        context: Context,
        pluginPackage: String,
        receiverClass: String?,
        key: String,
        value: String
    ): Boolean = sendCommand(
        context, pluginPackage, receiverClass,
        PluginContract.COMMAND_SET_QUICK_SETTING, key, value
    )
}

/** Eklentiden host'a zengin durum yayını gönderir. */
object PluginStatusPublisher {

    fun publish(
        context: Context,
        pluginId: String,
        running: Boolean,
        quickSettings: Map<String, String> = emptyMap(),
        remainingMs: Long = -1L,
        statusText: String? = null
    ) {
        // Android 14 (targetSdk 34) paket hedefli sistem broadcast'lerinde
        // ekstraları STRIPPED yapıyor. Eklentiler ve host aynı süreçte olduğu için
        // durum yayını süreç içi LocalBroadcastManager ile gönderilir.
        val intent = Intent(PluginContract.ACTION_PLUGIN_STATUS).apply {
            putExtra(PluginContract.EXTRA_PLUGIN_ID, pluginId)
            putExtra(PluginContract.EXTRA_PLUGIN_RUNNING, running)
            putExtra(PluginContract.EXTRA_REMAINING_MS, remainingMs)
            if (!statusText.isNullOrBlank()) putExtra(PluginContract.EXTRA_STATUS_TEXT, statusText)
            if (quickSettings.isNotEmpty()) {
                val b = Bundle()
                quickSettings.forEach { (k, v) -> b.putString(k, v) }
                putExtra(PluginContract.EXTRA_QUICK_SETTINGS, b)
            }
        }
        try {
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        } catch (_: Exception) {
        }
    }
}

/**
 * Eklentilerin komut alması için temel receiver.
 * Alt sınıflar [pluginId], [onStart], [onStop], [onGetState], [onSetQuickSetting] sağlar.
 */
abstract class BasePluginCommandReceiver : BroadcastReceiver() {

    abstract val pluginId: String

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != PluginContract.ACTION_PLUGIN_COMMAND) return
        val command = intent.getStringExtra(PluginContract.EXTRA_COMMAND) ?: return
        when (command) {
            PluginContract.COMMAND_START -> onStart(context)
            PluginContract.COMMAND_STOP -> onStop(context)
            PluginContract.COMMAND_GET_STATE -> onGetState(context)
            PluginContract.COMMAND_SET_QUICK_SETTING -> {
                val key = intent.getStringExtra(PluginContract.EXTRA_SETTING_KEY) ?: return
                val value = intent.getStringExtra(PluginContract.EXTRA_SETTING_VALUE) ?: ""
                onSetQuickSetting(context, key, value)
            }
        }
    }

    open fun onStart(context: Context) {}
    open fun onStop(context: Context) {}
    open fun onGetState(context: Context) {
        // Varsayılan: sadece çalışmıyor bilgisi gönder
        PluginStatusPublisher.publish(context, pluginId, running = false)
    }
    open fun onSetQuickSetting(context: Context, key: String, value: String) {}
}
