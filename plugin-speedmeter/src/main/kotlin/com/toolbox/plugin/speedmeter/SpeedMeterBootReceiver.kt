package com.toolbox.plugin.speedmeter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.toolbox.plugin.PluginSettingsManager

/**
 * Cihaz açılışında veya uygulama güncellendiğinde servisi yeniden başlatır.
 *
 * Açılışta otomatik başlatma ayarı ve güncelleme öncesi çalışma durumu
 * [PluginSettingsManager] üzerinden okunur; böylece servis ve arayüz
 * aynı tercihleri kullanır.
 */
class SpeedMeterBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val settings = PluginSettingsManager(context)
        val pluginId = SpeedMeterKeys.PLUGIN_ID

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                val autostart = settings.getBoolean(
                    pluginId,
                    SpeedMeterKeys.AUTOSTART,
                    true
                )
                if (autostart) {
                    SpeedMeterService.start(context)
                }
            }

            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                val wasRunning = settings.getBoolean(
                    pluginId,
                    SpeedMeterKeys.SERVICE_RUNNING,
                    false
                )
                if (wasRunning) {
                    SpeedMeterService.start(context)
                }
            }
        }
    }

    companion object {
        /**
         * Otomatik başlatma ayarını kaydeder.
         *
         * Yalnızca cihaz açılışındaki davranışı etkiler; çalışan servis
         * durdurulmaz. Kullanıcı servisi ana ekrandan ayrıca durdurabilir.
         */
        fun setAutostart(context: Context, enabled: Boolean) {
            PluginSettingsManager(context).putBoolean(
                SpeedMeterKeys.PLUGIN_ID,
                SpeedMeterKeys.AUTOSTART,
                enabled
            )
        }
    }
}
