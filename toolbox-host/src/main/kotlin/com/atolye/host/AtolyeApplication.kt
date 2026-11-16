package com.atolye.host

import android.app.Application
import android.content.Context
import com.atolye.design.AtolyeThemeType
import com.toolbox.plugin.PluginDiscovery
import com.toolbox.plugin.PluginSettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Atölye ana uygulaması.
 *
 * Bağımlılıklar Hilt yerine basit tekil (singleton) örnekler olarak
 * burada tutulur. Eklenti keşfi ve ayar yönetimi uygulama ömrü boyunca
 * tek bir örnek üzerinden çalışır.
 */
class AtolyeApplication : Application() {

    val pluginDiscovery: PluginDiscovery by lazy { PluginDiscovery(this) }

    val pluginSettings: PluginSettingsManager by lazy { PluginSettingsManager(this) }

    private val hostPreferences by lazy {
        getSharedPreferences("host_settings", Context.MODE_PRIVATE)
    }

    private val _theme = MutableStateFlow(AtolyeThemeType.KAGIT)

    val theme: StateFlow<AtolyeThemeType> = _theme.asStateFlow()

    // Tema geçiş sırası (DYNAMIC hariç, kullanıcı isteği üzerine eklenebilir)
    private val themeCycleOrder = listOf(
        AtolyeThemeType.KAGIT,
        AtolyeThemeType.GECE_ATOLYESI,
        AtolyeThemeType.BAKIR,
        AtolyeThemeType.DEVRE_KARTI
    )

    override fun onCreate() {
        super.onCreate()
        _theme.value = loadTheme()
    }

    fun setTheme(themeType: AtolyeThemeType) {
        hostPreferences.edit().putString(KEY_THEME, themeType.name).apply()
        _theme.value = themeType
    }

    /**
     * Mevcut temadan bir sonrakine geçer (döngüsel).
     * Palet butonuna her basışta çağrılır.
     */
    fun cycleTheme(): AtolyeThemeType {
        val current = _theme.value
        val currentIndex = themeCycleOrder.indexOf(current)
        val nextIndex = if (currentIndex >= 0) (currentIndex + 1) % themeCycleOrder.size else 0
        val nextTheme = themeCycleOrder[nextIndex]
        setTheme(nextTheme)
        return nextTheme
    }

    private fun loadTheme(): AtolyeThemeType {
        val stored = hostPreferences.getString(KEY_THEME, AtolyeThemeType.KAGIT.name)
        return runCatching { AtolyeThemeType.valueOf(stored ?: "") }
            .getOrDefault(AtolyeThemeType.KAGIT)
    }

    companion object {
        private const val KEY_THEME = "theme"
    }
}
