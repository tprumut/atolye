package com.atolye.host.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.atolye.host.AtolyeApplication
import com.toolbox.contract.InstalledPlugin
import com.toolbox.plugin.PluginSettingsKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val atolye = application as AtolyeApplication
    private val discovery = atolye.pluginDiscovery
    private val settings = atolye.pluginSettings

    private val _plugins = MutableStateFlow<List<InstalledPlugin>>(emptyList())
    val plugins: StateFlow<List<InstalledPlugin>> = _plugins.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadPlugins()
    }

    fun loadPlugins() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _plugins.value = withContext(Dispatchers.IO) {
                    discovery.discoverPlugins()
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Eklentiler yüklenemedi"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setPluginEnabled(plugin: InstalledPlugin, enabled: Boolean) {
        settings.putBoolean(
            plugin.metadata.pluginId,
            PluginSettingsKeys.ENABLED,
            enabled
        )
        _plugins.value = _plugins.value.map {
            if (it.metadata.pluginId == plugin.metadata.pluginId) {
                it.copy(isEnabled = enabled)
            } else {
                it
            }
        }
    }

    /** @return true eklenti açılabildiyse */
    fun launchPlugin(plugin: InstalledPlugin): Boolean =
        discovery.launchPlugin(plugin)

    /** @return true ayarlar açılabildiyse */
    fun openPluginSettings(plugin: InstalledPlugin): Boolean =
        discovery.openPluginSettings(plugin)

    fun clearError() {
        _error.value = null
    }
}
