package com.toolbox.plugin

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.os.Build
import android.util.Log
import com.toolbox.contract.InstalledPlugin
import com.toolbox.contract.PluginCategory
import com.toolbox.contract.PluginContract
import com.toolbox.contract.PluginMetadata
import com.toolbox.contract.PluginPermission
import org.xmlpull.v1.XmlPullParser

/**
 * Kurulu eklentileri bulan ve yöneten yardımcı sınıf.
 *
 * Eklentiler `com.toolbox.PLUGIN` action'ı ile kendilerini bildirir ve
 * manifestlerinde `com.toolbox.plugin.METADATA` meta-data'sında kendi
 * `@xml/plugin_metadata` kaynaklarını gösterir.
 *
 * Kaynak ID'leri APK'lar arasında paylaşılamadığı için metadata XML'i
 * eklentinin kendi paket bağlamı (createPackageContext) üzerinden okunur
 * ve `@string/...` referansları eklentinin kaynaklarında çözümlenir.
 */
class PluginDiscovery(context: Context) {

    private val appContext: Context = context.applicationContext
    private val pm: PackageManager = appContext.packageManager
    private val settings = PluginSettingsManager(appContext)

    /** Tüm kurulu eklentileri tara. */
    fun discoverPlugins(): List<InstalledPlugin> {
        return queryPluginActivities()
            .distinctBy { it.activityInfo.packageName }
            .mapNotNull { resolveInfo ->
                try {
                    createInstalledPlugin(resolveInfo)
                } catch (e: Exception) {
                    Log.e(
                        TAG,
                        "Eklenti okunamadı: ${resolveInfo.activityInfo.packageName}",
                        e
                    )
                    null
                }
            }
            .sortedBy { it.metadata.name.lowercase() }
    }

    /** Belirli bir paket için eklenti bilgisi. */
    fun findPlugin(packageName: String): InstalledPlugin? {
        val intent = Intent(PluginContract.ACTION_PLUGIN).setPackage(packageName)
        val activities = queryIntentActivities(intent)
        return activities.firstOrNull()?.let { resolveInfo ->
            runCatching { createInstalledPlugin(resolveInfo) }.getOrNull()
        }
    }

    /** Plugin ID'ye göre eklenti bul. */
    fun findPluginById(pluginId: String): InstalledPlugin? =
        discoverPlugins().firstOrNull { it.metadata.pluginId == pluginId }

    /** Eklenti ana uygulamada etkin mi? */
    fun isPluginEnabled(pluginId: String): Boolean =
        settings.getBoolean(pluginId, PluginSettingsKeys.ENABLED, true)

    /** Eklentinin ana ekranını açar. */
    fun launchPlugin(plugin: InstalledPlugin, action: String? = null): Boolean {
        val className = plugin.metadata.mainActivity.ifBlank {
            findMainActivityName(plugin.packageName) ?: ""
        }
        return startPluginComponent(plugin, className, action)
    }

    /** Eklentinin ayarlar ekranını açar. */
    fun openPluginSettings(plugin: InstalledPlugin): Boolean {
        val className = plugin.metadata.settingsActivity ?: return false
        return startPluginComponent(plugin, className, PluginContract.ACTION_OPEN_SETTINGS)
    }

    /** Eklenti indirme bağlantısı. */
    fun getDownloadUrl(pluginId: String): String? =
        findPluginById(pluginId)?.metadata?.downloadUrl

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private fun createInstalledPlugin(resolveInfo: ResolveInfo): InstalledPlugin {
        val activityInfo = resolveInfo.activityInfo
        val packageName = activityInfo.packageName
        val metadataBundle = activityInfo.metaData
            ?: throw IllegalStateException("Eklenti meta-data'sı yok: $packageName")

        val xmlResId = metadataBundle.getInt(PluginContract.METADATA_PLUGIN_INFO, 0)
        if (xmlResId == 0) {
            throw IllegalStateException("Eklenti metadata kaynağı yok: $packageName")
        }

        val pluginContext = appContext.createPackageContext(packageName, 0)
        val metadata = parseMetadata(pluginContext, xmlResId, packageName)

        if (metadata.minHostVersion > PluginContract.CONTRACT_VERSION) {
            throw IllegalStateException(
                "Eklenti daha yeni bir ana uygulama sürümü istiyor: $packageName"
            )
        }

        val installedVersion = runCatching { packageVersionCode(packageName) }.getOrDefault(0)

        return InstalledPlugin(
            metadata = metadata,
            packageName = packageName,
            isEnabled = settings.getBoolean(
                metadata.pluginId,
                PluginSettingsKeys.ENABLED,
                true
            ),
            installedVersion = installedVersion,
            lastUpdated = runCatching { packageLastUpdate(packageName) }.getOrDefault(0L)
        )
    }

    private fun parseMetadata(
        pluginContext: Context,
        xmlResId: Int,
        packageName: String
    ): PluginMetadata {
        val resources = pluginContext.resources
        val parser = resources.getXml(xmlResId)

        var pluginId = packageName
        var version = 1
        var minHostVersion = 1
        var name = ""
        var description = ""
        var iconResName: String? = null
        var settingsActivity: String? = null
        var mainActivity = ""
        var category = PluginCategory.TOOLS
        var tags = emptyList<String>()
        var downloadUrl: String? = null
        var minSdk = 26
        var accentColor: String? = null
        val permissions = mutableListOf<PluginPermission>()

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "plugin-metadata" -> {
                        pluginId = parser.attr("pluginId") ?: packageName
                        version = parser.attr("version")?.toIntOrNull() ?: 1
                        minHostVersion = parser.attr("minHostVersion")?.toIntOrNull() ?: 1
                        name = resolveString(resources, packageName, parser, "nameRes")
                        description = resolveString(
                            resources,
                            packageName,
                            parser,
                            "descriptionRes"
                        )
                        iconResName = iconName(resources, parser, "iconRes")
                            ?: iconName(resources, parser, "iconResName")
                        settingsActivity = parser.attr("settingsActivity")
                        mainActivity = parser.attr("mainActivity") ?: ""
                        category = parseCategory(parser.attr("category"))
                        tags = parser.attr("tags")
                            ?.split(',')
                            ?.map { it.trim() }
                            ?.filter { it.isNotEmpty() }
                            ?: emptyList()
                        downloadUrl = parser.attr("downloadUrl")
                        minSdk = parser.attr("minSdk")?.toIntOrNull() ?: 26
                        accentColor = parser.attr("accentColor")
                    }

                    "permission" -> {
                        val permission = parser.attr("name")
                        if (permission != null) {
                            permissions.add(
                                PluginPermission(
                                    permission = permission,
                                    title = resolveString(
                                        resources,
                                        packageName,
                                        parser,
                                        "titleRes"
                                    ),
                                    description = resolveString(
                                        resources,
                                        packageName,
                                        parser,
                                        "descriptionRes"
                                    ),
                                    isRequired = parser.attr("isRequired")?.toBoolean() ?: true
                                )
                            )
                        }
                    }
                }
            }
            event = parser.next()
        }

        return PluginMetadata(
            pluginId = pluginId,
            version = version,
            minHostVersion = minHostVersion,
            name = name.ifBlank { packageName },
            description = description,
            iconResName = iconResName,
            settingsActivity = settingsActivity,
            mainActivity = mainActivity,
            requiredPermissions = permissions,
            category = category,
            tags = tags,
            downloadUrl = downloadUrl,
            minSdk = minSdk,
            accentColor = accentColor
        )
    }

    private fun startPluginComponent(
        plugin: InstalledPlugin,
        className: String,
        action: String?
    ): Boolean {
        if (className.isBlank()) return false

        val intent = Intent(PluginContract.ACTION_PLUGIN).apply {
            component = ComponentName(plugin.packageName, className)
            putExtra(PluginContract.EXTRA_PLUGIN_ID, plugin.metadata.pluginId)
            action?.let { putExtra(PluginContract.EXTRA_PLUGIN_ACTION, it) }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            appContext.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Eklenti açılamadı: ${plugin.packageName}/$className", e)
            false
        }
    }

    private fun findMainActivityName(packageName: String): String? {
        val intent = Intent(PluginContract.ACTION_PLUGIN).setPackage(packageName)
        return queryIntentActivities(intent)
            .firstOrNull()
            ?.activityInfo
            ?.name
    }

    private fun queryPluginActivities(): List<ResolveInfo> =
        queryIntentActivities(Intent(PluginContract.ACTION_PLUGIN))

    private fun queryIntentActivities(intent: Intent): List<ResolveInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.GET_META_DATA.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)
        }
    }

    @Suppress("NewApi")
    private fun packageVersionCode(packageName: String): Int {
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(packageName, 0)
        }
        return info.longVersionCode.toInt()
    }

    @Suppress("NewApi")
    private fun packageLastUpdate(packageName: String): Long {
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(packageName, 0)
        }
        return info.lastUpdateTime
    }

    private fun XmlPullParser.attr(name: String): String? =
        getAttributeValue(null, name)

    /**
     * Metadata XML aapt2 tarafından derlendiğinde `@string/...` değerleri
     * tip olarak REFERENCE'a dönüşür ve `getAttributeValue` null döner.
     * Bu yüzden önce ham metne, sonra resource ID'ye bakılır.
     */
    private fun resolveString(
        resources: Resources,
        packageName: String,
        parser: XmlResourceParser,
        attribute: String
    ): String {
        val raw = parser.getAttributeValue(null, attribute)
        if (!raw.isNullOrBlank()) {
            if (!raw.startsWith("@")) return raw

            val directId = parser.getAttributeResourceValue(null, attribute, 0)
            if (directId != 0) {
                return runCatching { resources.getString(directId) }.getOrDefault("")
            }

            val name = raw.substringAfter('/')
            val id = resources.getIdentifier(name, "string", packageName)
            if (id == 0) return ""
            return runCatching { resources.getString(id) }.getOrDefault("")
        }

        val resId = parser.getAttributeResourceValue(null, attribute, 0)
        if (resId == 0) return ""
        return runCatching { resources.getString(resId) }.getOrDefault("")
    }

    private fun iconName(
        resources: Resources,
        parser: XmlResourceParser,
        attribute: String
    ): String? {
        val resId = parser.getAttributeResourceValue(null, attribute, 0)
        if (resId != 0) {
            return runCatching { resources.getResourceEntryName(resId) }.getOrNull()
        }
        val raw = parser.getAttributeValue(null, attribute) ?: return null
        if (raw.isBlank()) return null
        return raw.substringAfterLast('/')
    }

    private fun parseCategory(value: String?): PluginCategory {
        return when (value?.uppercase()) {
            "NETWORK" -> PluginCategory.NETWORK
            "DISPLAY" -> PluginCategory.DISPLAY
            "SYSTEM" -> PluginCategory.SYSTEM
            "DEVELOPER" -> PluginCategory.DEVELOPER
            "MEDIA" -> PluginCategory.MEDIA
            "PRODUCTIVITY" -> PluginCategory.PRODUCTIVITY
            else -> PluginCategory.TOOLS
        }
    }

    companion object {
        private const val TAG = "PluginDiscovery"
    }
}

/**
 * Eklenti durum yayınlarını dinlemek için basit receiver.
 */
class PluginStatusReceiver : BroadcastReceiver() {

    private var listener: ((pluginId: String, running: Boolean) -> Unit)? = null

    fun setListener(listener: (pluginId: String, running: Boolean) -> Unit) {
        this.listener = listener
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pluginId = intent.getStringExtra(PluginContract.EXTRA_PLUGIN_ID) ?: return
        val running = intent.getBooleanExtra(PluginContract.EXTRA_PLUGIN_RUNNING, false)
        listener?.invoke(pluginId, running)
    }

    fun register(context: Context) {
        val filter = IntentFilter(PluginContract.ACTION_PLUGIN_STATUS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(this, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            androidx.core.content.ContextCompat.registerReceiver(
                context,
                this,
                filter,
                androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }
    }

    fun unregister(context: Context) {
        try {
            context.unregisterReceiver(this)
        } catch (_: IllegalArgumentException) {
            // Zaten kayıtlı değil.
        }
    }
}
