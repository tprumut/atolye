package com.toolbox.contract

import kotlinx.serialization.Serializable

/**
 * Toolbox / Atölye Plugin Sözleşmesi - Version 3
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
 *
 * v3: Ana uygulama ↔ eklenti kontrol kanalı (imza korumalı BroadcastReceiver),
 * hızlı ayar (quick setting) metadata ve zengin durum yayını eklendi.
 * Hızlı ayarı olmayan/eski eklentiler geriye dönük uyumludur.
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

    /** Komut alıcı BroadcastReceiver (tam sınıf adı), v3 kontrol kanalı */
    val commandReceiver: String? = null,

    /** İstenen izinler ve açıklamaları */
    val requiredPermissions: List<PluginPermission> = emptyList(),

    /** Eklenti kategorisi */
    val category: PluginCategory = PluginCategory.TOOLS,

    /** Eklenti etiketleri */
    val tags: List<String> = emptyList(),

    /** İndirme URL'si (GitHub Releases vb.) */
    val downloadUrl: String? = null,

    /** Minimum Android SDK */
    val minSdk: Int = 26,

    /** Eklenti vurgu rengi (#RRGGBB), opsiyonel */
    val accentColor: String? = null,

    /** Kartta gösterilebilen hızlı ayarlar (en fazla 6). */
    val quickSettings: List<PluginQuickSetting> = emptyList(),
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

/** Hızlı ayar tipi */
@Serializable
enum class QuickSettingType {
    /** Birden çok seçenek (çipler) */
    CHOICE,
    /** Aç/Kapat */
    TOGGLE,
}

/** Seçenekli hızlı ayar seçeneği */
@Serializable
data class QuickSettingOption(
    /** Saklanacak/değer olarak gönderilecek anahtar */
    val value: String,
    /** Görünen başlık (çözümlenmiş) */
    val label: String = "",
)

/** Kart üzerinde gösterilen hızlı ayar tanımı */
@Serializable
data class PluginQuickSetting(
    /** Ayar anahtarı (eklentinin kendi ayar dosyasında saklanır) */
    val key: String,
    /** Görünen başlık (çözümlenmiş) */
    val title: String = "",
    /** Tip: CHOICE veya TOGGLE */
    val type: QuickSettingType = QuickSettingType.TOGGLE,
    /** CHOICE için seçenekler */
    val options: List<QuickSettingOption> = emptyList(),
    /** Varsayılan değer (TOGGLE: "true"/"false", CHOICE: option value) */
    val defaultValue: String = "",
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
    const val CONTRACT_VERSION = 3

    /** Host ↔ eklenti kontrolü için imza seviyesi izin (host ve eklentilerde tanımlı olmalı). */
    const val PERMISSION_CONTROL_PLUGIN = "com.toolbox.permission.CONTROL_PLUGIN"

    /** Intent extra keys */
    const val EXTRA_PLUGIN_ID = "plugin_id"
    const val EXTRA_PLUGIN_ACTION = "plugin_action"

    /** Ana uygulamadan eklentiye komut action'ı */
    const val ACTION_PLUGIN_COMMAND = "com.toolbox.PLUGIN_COMMAND"
    const val EXTRA_COMMAND = "plugin_command"
    const val EXTRA_SETTING_KEY = "setting_key"
    const val EXTRA_SETTING_VALUE = "setting_value"

    /** Komutlar */
    const val COMMAND_START = "START"
    const val COMMAND_STOP = "STOP"
    const val COMMAND_GET_STATE = "GET_STATE"
    const val COMMAND_SET_QUICK_SETTING = "SET_QUICK_SETTING"

    /** Eski/uyumluluk eylemleri (Activity intent extra) */
    const val ACTION_OPEN_MAIN = "open_main"
    const val ACTION_OPEN_SETTINGS = "open_settings"
    const val ACTION_GET_STATUS = "get_status"

    /** Eklenti durum yayını (eklentiden host'a) */
    const val ACTION_PLUGIN_STATUS = "com.toolbox.PLUGIN_STATUS"
    const val EXTRA_PLUGIN_RUNNING = "plugin_running"

    /**
     * Hızlı ayar değerleri. Bundle içinde String key -> String value eşlemesi
     * (TOGGLE: "true"/"false", CHOICE: seçenek değeri).
     */
    const val EXTRA_QUICK_SETTINGS = "plugin_quick_settings"
    /** Kalan süre (ms), varsa; yoksa -1. */
    const val EXTRA_REMAINING_MS = "plugin_remaining_ms"
    /** Eklentinin kartta gösterebileceği kısa durum metni (opsiyonel). */
    const val EXTRA_STATUS_TEXT = "plugin_status_text"
}
