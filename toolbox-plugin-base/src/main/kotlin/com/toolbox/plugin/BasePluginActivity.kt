package com.toolbox.plugin

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import com.toolbox.contract.PluginContract
import com.toolbox.contract.PluginMetadata

/**
 * Eklentilerin temel Activity sınıfı.
 *
 * Ana uygulama eklentiyi `com.toolbox.PLUGIN` action'ı ile başlatır.
 * Alt sınıflar [pluginMetadata] ve [PluginContent] sağlamalıdır.
 *
 * Kullanım:
 * ```
 * class MyPluginActivity : BasePluginActivity() {
 *     override val pluginMetadata: PluginMetadata by lazy { ... }
 *     @Composable override fun PluginContent() { MyScreen() }
 * }
 * ```
 */
abstract class BasePluginActivity : ComponentActivity() {

    /** Eklenti meta verisi. Context gerektiren metinler için `by lazy` kullanın. */
    abstract val pluginMetadata: PluginMetadata

    /** Eklentinin Compose içeriği. */
    @Composable
    protected abstract fun PluginContent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PluginContent()
        }

        handlePluginAction(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePluginAction(intent)
    }

    private fun handlePluginAction(intent: Intent?) {
        when (intent?.getStringExtra(PluginContract.EXTRA_PLUGIN_ACTION)) {
            PluginContract.ACTION_OPEN_SETTINGS -> openSettings()
            PluginContract.ACTION_GET_STATUS -> sendStatusBroadcast()
            else -> Unit
        }
    }

    /** Ayarlar ekranını açar. İstenirse alt sınıflar override edebilir. */
    open fun openSettings() {
        val className = pluginMetadata.settingsActivity ?: return
        try {
            val component = ComponentName(packageName, className)
            startActivity(Intent().setComponent(component))
        } catch (e: Exception) {
            Log.e("BasePluginActivity", "Ayarlar açılamadı: $className", e)
        }
    }

    /** Ana uygulamaya durum bilgisi gönderir. */
    open fun sendStatusBroadcast(running: Boolean = true) {
        val intent = Intent(PluginContract.ACTION_PLUGIN_STATUS)
            .setPackage(packageName)
            .putExtra(PluginContract.EXTRA_PLUGIN_ID, pluginMetadata.pluginId)
            .putExtra(PluginContract.EXTRA_PLUGIN_RUNNING, running)
        sendBroadcast(intent)
    }
}
