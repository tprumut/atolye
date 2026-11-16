package com.atolye.host.ui

import com.toolbox.contract.InstalledPlugin

data class PluginRuntimeState(
    val running: Boolean = false,
    val quickSettingValues: Map<String, String> = emptyMap(),
    val remainingMs: Long = -1L,
    val statusText: String? = null,
)

data class PluginUiItem(
    val plugin: InstalledPlugin,
    val runtime: PluginRuntimeState = PluginRuntimeState(),
    val hidden: Boolean = false,
    /** Kartta gösterilecek hızlı ayar anahtarları (en fazla 3). Boşsa metadata ilk 3 varsayılan. */
    val visibleQuickSettingKeys: List<String> = emptyList(),
)

object HostPrefs {
    const val HOST_PREFS = "host_settings"
    fun hiddenKey(pluginId: String) = "hidden_$pluginId"
    fun qsVisibleKey(pluginId: String) = "qs_visible_$pluginId" // comma-separated keys
    const val PLUGIN_ORDER_KEY = "plugin_order" // comma-separated pluginIds
}