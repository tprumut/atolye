package com.toolbox.plugin

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Eklenti ayarları yönetimi.
 *
 * Ayarlar SharedPreferences üzerinde saklanır. Her eklentinin kendi
 * tercih dosyası vardır. Tüm okuma/yazma işlemleri senkron olduğundan
 * hem Activity hem de Service içinden güvenle kullanılabilir.
 *
 * `observe*` fonksiyonları, aynı dosyaya yapılan değişiklikleri dinleyen
 * StateFlow'lar döndürür. Böylece Compose ekranları ayarları canlı izler.
 */
class PluginSettingsManager(context: Context) {

    private val appContext: Context = context.applicationContext

    private val flows = HashMap<String, MutableStateFlow<Any>>()
    private val readers = HashMap<String, () -> Any>()
    private val listeners = HashMap<String, SharedPreferences.OnSharedPreferenceChangeListener>()

    private fun prefs(pluginId: String): SharedPreferences =
        appContext.getSharedPreferences("atolye_plugin_$pluginId", Context.MODE_PRIVATE)

    private fun flowKey(pluginId: String, key: String): String = "$pluginId\u0000$key"

    private fun registerListener(pluginId: String) {
        if (listeners.containsKey(pluginId)) return

        val sharedPreferences = prefs(pluginId)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key != null) {
                val flowKey = flowKey(pluginId, key)
                val reader = readers[flowKey]
                val flow = flows[flowKey]
                if (reader != null && flow != null) {
                    flow.value = reader()
                }
            }
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
        listeners[pluginId] = listener
    }

    private fun <T : Any> observe(
        pluginId: String,
        key: String,
        default: T,
        read: () -> T
    ): StateFlow<T> {
        registerListener(pluginId)
        val flowKey = flowKey(pluginId, key)

        @Suppress("UNCHECKED_CAST")
        val existing = flows[flowKey] as? MutableStateFlow<T>
        if (existing != null) return existing.asStateFlow()

        val flow = MutableStateFlow(read())
        flows[flowKey] = flow as MutableStateFlow<Any>
        readers[flowKey] = { read() }
        return flow.asStateFlow()
    }

    private fun publish(pluginId: String, key: String, value: Any) {
        val flow = flows[flowKey(pluginId, key)]
        flow?.value = value
    }

    // ------------------------------------------------------------------
    // Boolean
    // ------------------------------------------------------------------

    fun getBoolean(pluginId: String, key: String, default: Boolean = false): Boolean =
        prefs(pluginId).getBoolean(key, default)

    fun putBoolean(pluginId: String, key: String, value: Boolean) {
        prefs(pluginId).edit().putBoolean(key, value).apply()
        publish(pluginId, key, value)
    }

    fun observeBoolean(
        pluginId: String,
        key: String,
        default: Boolean = false
    ): StateFlow<Boolean> = observe(pluginId, key, default) {
        getBoolean(pluginId, key, default)
    }

    // ------------------------------------------------------------------
    // Int
    // ------------------------------------------------------------------

    fun getInt(pluginId: String, key: String, default: Int = 0): Int =
        prefs(pluginId).getInt(key, default)

    fun putInt(pluginId: String, key: String, value: Int) {
        prefs(pluginId).edit().putInt(key, value).apply()
        publish(pluginId, key, value)
    }

    fun observeInt(pluginId: String, key: String, default: Int = 0): StateFlow<Int> =
        observe(pluginId, key, default) { getInt(pluginId, key, default) }

    // ------------------------------------------------------------------
    // Long
    // ------------------------------------------------------------------

    fun getLong(pluginId: String, key: String, default: Long = 0L): Long =
        prefs(pluginId).getLong(key, default)

    fun putLong(pluginId: String, key: String, value: Long) {
        prefs(pluginId).edit().putLong(key, value).apply()
        publish(pluginId, key, value)
    }

    // ------------------------------------------------------------------
    // String
    // ------------------------------------------------------------------

    fun getString(pluginId: String, key: String, default: String = ""): String =
        prefs(pluginId).getString(key, default) ?: default

    fun putString(pluginId: String, key: String, value: String) {
        prefs(pluginId).edit().putString(key, value).apply()
        publish(pluginId, key, value)
    }

    fun observeString(pluginId: String, key: String, default: String = ""): StateFlow<String> =
        observe(pluginId, key, default) { getString(pluginId, key, default) }

    // ------------------------------------------------------------------
    // String set
    // ------------------------------------------------------------------

    fun getStringSet(
        pluginId: String,
        key: String,
        default: Set<String> = emptySet()
    ): Set<String> {
        val stored = prefs(pluginId).getStringSet(key, null) ?: return default
        return HashSet(stored)
    }

    fun putStringSet(pluginId: String, key: String, value: Set<String>) {
        val copy = HashSet(value)
        prefs(pluginId).edit().putStringSet(key, copy).apply()
        publish(pluginId, key, copy)
    }

    fun observeStringSet(
        pluginId: String,
        key: String,
        default: Set<String> = emptySet()
    ): StateFlow<Set<String>> = observe(pluginId, key, default) {
        getStringSet(pluginId, key, default)
    }

    // ------------------------------------------------------------------
    // Cleanup
    // ------------------------------------------------------------------

    fun clearAll(pluginId: String) {
        prefs(pluginId).edit().clear().apply()
    }
}

/**
 * Yaygın ayar anahtarları
 */
object PluginSettingsKeys {
    const val ENABLED = "enabled"
    const val FIRST_RUN = "first_run"
    const val LAST_UPDATED = "last_updated"

    const val NOTIFICATION_ENABLED = "notification_enabled"
    const val NOTIFICATION_PRIORITY = "notification_priority"

    const val THEME_MODE = "theme_mode" // 0=system, 1=light, 2=dark
}
