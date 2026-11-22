package com.toolbox.plugin

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.toolbox.contract.InstalledPlugin
import com.toolbox.contract.PluginContract

/**
 * Atölye içindeki eklentileri keşfeder ve yönetir.
 *
 * Eski yapıda her eklenti ayrı bir APK'ydi ve host onları
 * `PackageManager.queryIntentActivities("com.toolbox.PLUGIN")` ile bulur,
 * metadata XML'lerini eklentinin paket bağlamından okurdu.
 *
 * Yeni yapıda tüm eklentiler aynı APK içinde derlendiği için keşif,
 * [PluginProvider] listesi üzerinden yapılır. Paket adı daima host paketidir;
 * eklentiler kendi ID'leriyle birbirinden ayrılır.
 */
class PluginDiscovery(
    context: Context,
    private val providers: List<PluginProvider>
) {

    private val appContext: Context = context.applicationContext
    private val packageManager: PackageManager = appContext.packageManager
    private val settings = PluginSettingsManager(appContext)
    private val hostPackageName: String = appContext.packageName

    /** APK'ya dahil edilen tüm eklentileri listeler. */
    fun discoverPlugins(): List<InstalledPlugin> {
        return providers.mapNotNull { provider ->
            try {
                createInstalledPlugin(provider)
            } catch (e: Exception) {
                Log.e(TAG, "Eklenti okunamadı: ${provider.pluginId}", e)
                null
            }
        }.sortedBy { it.metadata.name.lowercase() }
    }

    /** Eklenti ID'sine göre eklenti bilgisi. */
    fun findPlugin(pluginId: String): InstalledPlugin? =
        discoverPlugins().firstOrNull { it.metadata.pluginId == pluginId }

    /** [findPlugin] ile aynı; eski çağrılar için takma ad. */
    fun findPluginById(pluginId: String): InstalledPlugin? = findPlugin(pluginId)

    /** Eklentinin host içinde etkin olup olmadığı. */
    fun isPluginEnabled(pluginId: String): Boolean =
        settings.getBoolean(pluginId, PluginSettingsKeys.ENABLED, true)

    /** Eklentinin ana ekranını açar. */
    fun launchPlugin(plugin: InstalledPlugin, action: String? = null, from: Context? = null): Boolean {
        return startPluginComponent(
            pluginId = plugin.metadata.pluginId,
            className = plugin.metadata.mainActivity,
            action = action,
            from = from
        )
    }

    /** Eklentinin ayarlar ekranını açar. */
    fun openPluginSettings(plugin: InstalledPlugin, from: Context? = null): Boolean {
        val provider = providers.firstOrNull { it.pluginId == plugin.metadata.pluginId }
            ?: return false
        val className = provider.settingsActivityClassName ?: return false
        return startPluginComponent(
            pluginId = plugin.metadata.pluginId,
            className = className,
            action = PluginContract.ACTION_OPEN_SETTINGS,
            from = from
        )
    }

    /** Eklenti indirme bağlantısı (artık harici APK kullanılmıyor). */
    fun getDownloadUrl(pluginId: String): String? =
        findPlugin(pluginId)?.metadata?.downloadUrl

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private fun createInstalledPlugin(provider: PluginProvider): InstalledPlugin {
        // Provider zaten receiver sınıfını biliyor. Metadata'da commandReceiver
        // boş bırakılırsa host komutu pakete genel yayınlar ve TEK eklentiyi
        // açıp kapatmak yerine HEPSİNİ etkiler. Burada zorunlu olarak doldur.
        val metadata = provider.metadata(appContext).copy(
            commandReceiver = provider.commandReceiverClassName
        )

        if (metadata.minHostVersion > PluginContract.CONTRACT_VERSION) {
            throw IllegalStateException(
                "Eklenti daha yeni bir ana uygulama sözleşmesi istiyor: ${provider.pluginId}"
            )
        }

        return InstalledPlugin(
            metadata = metadata,
            packageName = hostPackageName,
            isEnabled = settings.getBoolean(metadata.pluginId, PluginSettingsKeys.ENABLED, true),
            installedVersion = hostVersionCode,
            lastUpdated = hostLastUpdated
        )
    }

    @Suppress("NewApi")
    private val hostPackageInfo by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(hostPackageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(hostPackageName, 0)
        }
    }

    private val hostVersionCode: Int
        get() = hostPackageInfo.longVersionCode.toInt()

    private val hostLastUpdated: Long
        get() = hostPackageInfo.lastUpdateTime

    private fun startPluginComponent(
        pluginId: String,
        className: String,
        action: String?,
        from: Context? = null
    ): Boolean {
        if (className.isBlank()) return false

        val context = from ?: appContext
        val intent = Intent(PluginContract.ACTION_PLUGIN).apply {
            component = ComponentName(hostPackageName, className)
            putExtra(PluginContract.EXTRA_PLUGIN_ID, pluginId)
            action?.let { putExtra(PluginContract.EXTRA_PLUGIN_ACTION, it) }
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Eklenti açılamadı: $hostPackageName/$className", e)
            false
        }
    }

    companion object {
        private const val TAG = "PluginDiscovery"
    }
}

/**
 * Eklenti durum yayınlarını dinlemek için receiver.
 */
class PluginStatusReceiver : BroadcastReceiver() {

    data class Status(
        val pluginId: String,
        val running: Boolean,
        val quickSettings: Map<String, String> = emptyMap(),
        val remainingMs: Long = -1L,
        val statusText: String? = null
    )

    private var listener: ((Status) -> Unit)? = null
    private var simpleListener: ((pluginId: String, running: Boolean) -> Unit)? = null

    fun setListener(listener: (Status) -> Unit) {
        this.listener = listener
    }

    /** Geriye dönük uyumlu dinleyici. */
    fun setListener(listener: (pluginId: String, running: Boolean) -> Unit) {
        this.simpleListener = listener
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pluginId = intent.getStringExtra(PluginContract.EXTRA_PLUGIN_ID) ?: return
        val running = intent.getBooleanExtra(PluginContract.EXTRA_PLUGIN_RUNNING, false)
        val remainingMs = intent.getLongExtra(PluginContract.EXTRA_REMAINING_MS, -1L)
        val statusText = intent.getStringExtra(PluginContract.EXTRA_STATUS_TEXT)
        val qsBundle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getBundleExtra(PluginContract.EXTRA_QUICK_SETTINGS)
        } else {
            @Suppress("DEPRECATION")
            intent.getBundleExtra(PluginContract.EXTRA_QUICK_SETTINGS)
        }
        val quickSettings = buildMap {
            qsBundle?.keySet()?.forEach { k ->
                val v = qsBundle.getString(k)
                if (v != null) put(k, v)
            }
        }
        val status = Status(pluginId, running, quickSettings, remainingMs, statusText)
        listener?.invoke(status)
        simpleListener?.invoke(pluginId, running)
    }

    fun register(context: Context) {
        val filter = IntentFilter(PluginContract.ACTION_PLUGIN_STATUS)
        androidx.localbroadcastmanager.content.LocalBroadcastManager
            .getInstance(context)
            .registerReceiver(this, filter)
    }

    fun unregister(context: Context) {
        try {
            androidx.localbroadcastmanager.content.LocalBroadcastManager
                .getInstance(context)
                .unregisterReceiver(this)
        } catch (_: IllegalArgumentException) {
            // Zaten kayıtlı değil.
        }
    }
}
