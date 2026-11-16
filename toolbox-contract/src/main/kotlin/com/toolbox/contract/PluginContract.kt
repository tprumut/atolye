package com.toolbox.contract

import kotlinx.serialization.Serializable

/**
 * Toolbox / Atölye Plugin Sözleşmesi - Version 2
 *
 * Her eklenti AndroidManifest.xml'inde `com.toolbox.PLUGIN` action'ı ile
 * kendini bildirir ve `com.toolbox.plugin.METADATA` meta-data'sında
 * `@xml/plugin_metadata` kaynağını gösterir.
 *
 * Keşif: Ana uygulama PackageManager.queryIntentActivities() ile eklentileri
 * bulur, eklentinin kendi resource'larından metadata XML'ini okur ve
 * görünen metinleri eklentinin diline göre çözer.
 *
 * Not: Kaynak ID'leri APK'lar arasında paylaşılamaz. Bu yüzden metadata'da
 * resource *isimleri* taşınır, ID değil.
 */
@Serializable
data class PluginMetadata(
    /** Benzersiz eklenti ID'si (örn: "com.toolbox.plugin.speedmeter") */
    val pluginId: String,

    /** Eklenti sürüm kodu (artan tam sayı) */
    val version: Int = 1,

    /** Minimum ana uygulama sözleşme sürümü */
    val minHostVersion: Int = 1,

    /** Görünen ad (çözümlenmiş, yerelleştirilmiş metin) */
    val name: String = "",

    /** Açıklama (çözümlenmiş, yerelleştirilmiş metin) */
    val description: String = "",

    /** Eklenti paketindeki drawable kaynağının adı (örn: "ic_speedmeter") */
    val iconResName: String? = null,

    /** Ayarlar Activity'si (tam sınıf adı) */
    val settingsActivity: String? = null,

    /** Ana Activity (tam sınıf adı) */
    val mainActivity: String = "",

    /** İstenen izinler ve açıklamaları */
    val requiredPermissions: List<PluginPermission> = emptyList(),

    /** Eklenti kategorisi */
    val category: PluginCategory = PluginCategory.TOOLS,

    /** Eklenti etiketleri */
    val tags: List<String> = emptyList(),

    /** İndirme URL'si (F-Droid, GitHub Releases, vb.) */
    val downloadUrl: String? = null,

    /** Minimum Android SDK */
    val minSdk: Int = 26,

    /** Eklenti vurgu rengi (#RRGGBB), opsiyonel */
    val accentColor: String? = null,
)

/** Eklenti kategorileri */
enum class PluginCategory {
    TOOLS,
    NETWORK,
    DISPLAY,
    SYSTEM,
    DEVELOPER,
    MEDIA,
    PRODUCTIVITY,
}

/** Eklenti izni ve açıklaması (metinler keşif sırasında çözümlenir) */
@Serializable
data class PluginPermission(
    val permission: String,
    val title: String = "",
    val description: String = "",
    val isRequired: Boolean = true,
)

/** Kurulu eklenti bilgisi (ana uygulamada kullanılır) */
@Serializable
data class InstalledPlugin(
    val metadata: PluginMetadata,
    val packageName: String,
    val isEnabled: Boolean = true,
    val installedVersion: Int = 0,
    val lastUpdated: Long = 0L,
)

/** Plugin sözleşmesi sabitleri */
object PluginContract {
    /** Intent action for plugin discovery */
    const val ACTION_PLUGIN = "com.toolbox.PLUGIN"

    /** Metadata key for plugin info in AndroidManifest */
    const val METADATA_PLUGIN_INFO = "com.toolbox.plugin.METADATA"

    /** Bu sözleşmenin sürümü */
    const val CONTRACT_VERSION = 2

    /** Intent extra keys */
    const val EXTRA_PLUGIN_ID = "plugin_id"
    const val EXTRA_PLUGIN_ACTION = "plugin_action"

    /** Plugin actions */
    const val ACTION_OPEN_MAIN = "open_main"
    const val ACTION_OPEN_SETTINGS = "open_settings"
    const val ACTION_GET_STATUS = "get_status"

    /** Eklenti durum yayını */
    const val ACTION_PLUGIN_STATUS = "com.toolbox.PLUGIN_STATUS"
    const val EXTRA_PLUGIN_RUNNING = "plugin_running"
}
