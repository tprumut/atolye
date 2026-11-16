package com.atolye.host.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.atolye.host.AtolyeApplication
import com.toolbox.contract.InstalledPlugin
import com.toolbox.contract.PluginContract
import com.toolbox.plugin.PluginCommandSender
import com.toolbox.plugin.PluginSettingsKeys
import com.toolbox.plugin.PluginStatusReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val atolye = application as AtolyeApplication
    private val discovery = atolye.pluginDiscovery
    private val hostPrefs = appContext.getSharedPreferences(HostPrefs.HOST_PREFS, Context.MODE_PRIVATE)

    private val _items = MutableStateFlow<List<PluginUiItem>>(emptyList())
    val items: StateFlow<List<PluginUiItem>> = _items.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val runtime = mutableMapOf<String, PluginRuntimeState>()

    private val statusReceiver = PluginStatusReceiver().apply {
        setListener { status ->
            val prev = runtime[status.pluginId] ?: PluginRuntimeState()
            val mergedQs = prev.quickSettingValues.toMutableMap().apply { putAll(status.quickSettings) }
            runtime[status.pluginId] = prev.copy(
                running = status.running,
                quickSettingValues = mergedQs,
                remainingMs = status.remainingMs,
                statusText = status.statusText
            )
            _items.update { list ->
                list.map { if (it.plugin.metadata.pluginId == status.pluginId) it.copy(runtime = runtime[status.pluginId]!!) else it }
            }
        }
    }

    init {
        statusReceiver.register(appContext)
        loadPlugins()
    }

    override fun onCleared() {
        statusReceiver.unregister(appContext)
        super.onCleared()
    }

    fun loadPlugins(requestState: Boolean = true) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val discovered = withContext(Dispatchers.IO) { discovery.discoverPlugins() }
                val newItems = discovered.map { p ->
                    val id = p.metadata.pluginId
                    val hidden = hostPrefs.getBoolean(HostPrefs.hiddenKey(id), false)
                    val visibleQs = hostPrefs.getString(HostPrefs.qsVisibleKey(id), "")
                        ?.split(',')?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
                    PluginUiItem(
                        plugin = p,
                        runtime = runtime[id] ?: PluginRuntimeState(),
                        hidden = hidden,
                        visibleQuickSettingKeys = visibleQs
                    )
                }
                _items.value = newItems
                if (requestState) requestStates(newItems)
            } catch (e: Exception) {
                _error.value = e.message ?: "Eklentiler yüklenemedi"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun requestStates(items: List<PluginUiItem> = _items.value) {
        items.forEach { item ->
            val meta = item.plugin.metadata
            PluginCommandSender.requestState(appContext, item.plugin.packageName, meta.commandReceiver)
        }
    }

    fun setPluginHidden(pluginId: String, hidden: Boolean) {
        hostPrefs.edit().putBoolean(HostPrefs.hiddenKey(pluginId), hidden).apply()
        _items.update { list -> list.map { if (it.plugin.metadata.pluginId == pluginId) it.copy(hidden = hidden) else it } }
    }

    fun setVisibleQuickSettings(pluginId: String, keys: List<String>) {
        val value = keys.take(3).joinToString(",")
        hostPrefs.edit().putString(HostPrefs.qsVisibleKey(pluginId), value).apply()
        _items.update { list -> list.map { if (it.plugin.metadata.pluginId == pluginId) it.copy(visibleQuickSettingKeys = keys.take(3)) else it } }
    }

    fun sendStart(item: PluginUiItem): Boolean {
        val meta = item.plugin.metadata
        return PluginCommandSender.start(appContext, item.plugin.packageName, meta.commandReceiver)
    }

    fun sendStop(item: PluginUiItem): Boolean {
        val meta = item.plugin.metadata
        return PluginCommandSender.stop(appContext, item.plugin.packageName, meta.commandReceiver)
    }

    fun setQuickSetting(item: PluginUiItem, key: String, value: String): Boolean {
        val meta = item.plugin.metadata
        // optimistic update
        runtime[item.plugin.metadata.pluginId] = (runtime[item.plugin.metadata.pluginId] ?: PluginRuntimeState()).let { r ->
            r.copy(quickSettingValues = r.quickSettingValues.toMutableMap().apply { put(key, value) })
        }
        _items.update { list ->
            list.map { if (it.plugin.metadata.pluginId == item.plugin.metadata.pluginId) it.copy(runtime = runtime[item.plugin.metadata.pluginId]!!) else it }
        }
        return PluginCommandSender.setQuickSetting(appContext, item.plugin.packageName, meta.commandReceiver, key, value)
    }

    fun launchPlugin(item: PluginUiItem): Boolean = discovery.launchPlugin(item.plugin)
    fun openPluginSettings(item: PluginUiItem): Boolean = discovery.openPluginSettings(item.plugin)

    fun isIgnoringBatteryOptimizations(): Boolean {
        val pm = appContext.getSystemService(PowerManager::class.java) ?: return true
        return pm.isIgnoringBatteryOptimizations(appContext.packageName)
    }

    fun requestIgnoreBatteryOptimizationsIntent(): Intent {
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${appContext.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun clearError() { _error.value = null }
}
