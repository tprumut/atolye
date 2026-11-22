package com.toolbox.plugin.izin

import android.content.Context
import com.toolbox.plugin.BasePluginCommandReceiver
import com.toolbox.plugin.PluginStatusPublisher

/**
 * İzin Yöneticisi salt okunur bir araçtır; arka plan servisi yoktur.
 * Host'un kartı "çalışıyor" gibi göstermemesi için her durumda kapalı
 * durumunu ve açıklayıcı metni yayınlarız.
 */
class IzinCommandReceiver : BasePluginCommandReceiver() {
    override val pluginId: String = IzinKeys.PLUGIN_ID

    override fun onStart(context: Context) = publish(context)
    override fun onStop(context: Context) = publish(context)
    override fun onGetState(context: Context) = publish(context)

    companion object {
        fun publish(context: Context) {
            PluginStatusPublisher.publish(
                context = context,
                pluginId = IzinKeys.PLUGIN_ID,
                running = false,
                statusText = context.getString(R.string.izin_read_only),
            )
        }
    }
}
