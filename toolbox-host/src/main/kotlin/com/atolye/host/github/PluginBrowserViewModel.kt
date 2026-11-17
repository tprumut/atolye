package com.atolye.host.github

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.atolye.host.R
import com.toolbox.contract.PluginCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class PluginBrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val json = Json { ignoreUnknownKeys = true }

    // GitHub raw URL for the index.json - can be configured
    private val indexUrl = "https://raw.githubusercontent.com/atolye-plugins/repo/main/index.json"

    private val _state = MutableStateFlow<PluginBrowserState>(PluginBrowserState.Loading())
    val state: StateFlow<PluginBrowserState> = _state.asStateFlow()

    private val _filter = MutableStateFlow(PluginBrowserFilter())
    val filter: StateFlow<PluginBrowserFilter> = _filter.asStateFlow()

    private var allPlugins = emptyList<GitHubPluginInfo>()

    init {
        loadPluginIndex()
    }

    fun updateFilter(newFilter: PluginBrowserFilter) {
        _filter.value = newFilter
    }

    fun updateSearchQuery(query: String) {
        _filter.value = _filter.value.copy(searchQuery = query)
    }

    fun updateCategory(category: PluginCategory?) {
        _filter.value = _filter.value.copy(selectedCategory = category)
    }

    fun toggleShowInstalledOnly() {
        _filter.value = _filter.value.copy(showInstalledOnly = !_filter.value.showInstalledOnly)
    }

    /** Fetch the plugin index from GitHub */
    fun loadPluginIndex() {
        viewModelScope.launch {
            _state.value = PluginBrowserState.Loading()
            try {
                val response = withContext(Dispatchers.IO) { fetchIndexJson() }
                val repo = json.decodeFromString<GitHubPluginRepo>(response)
                allPlugins = repo.plugins
                val categories = allPlugins.map { it.category }.distinct().sortedBy { it.name }
                _state.value = PluginBrowserState.Success(allPlugins, categories)
            } catch (e: Exception) {
                _state.value = PluginBrowserState.Error("Eklenti listesi yüklenemedi: ${e.message}")
            }
        }
    }

    private suspend fun fetchIndexJson(): String = withContext(Dispatchers.IO) {
        val url = URL(indexUrl)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        try {
            if (connection.responseCode == 200) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                throw Exception("HTTP ${connection.responseCode}")
            }
        } finally {
            connection.disconnect()
        }
    }

    /** Get filtered plugins based on current filter */
    fun getFilteredPlugins(): List<GitHubPluginInfo> {
        val filter = _filter.value
        return allPlugins.filter { plugin ->
            val matchesCategory = filter.selectedCategory == null || plugin.category == filter.selectedCategory
            val matchesSearch = filter.searchQuery.isBlank() ||
                plugin.name.contains(filter.searchQuery, ignoreCase = true) ||
                plugin.description.contains(filter.searchQuery, ignoreCase = true) ||
                plugin.tags.any { it.contains(filter.searchQuery, ignoreCase = true) }
            val matchesInstalled = !filter.showInstalledOnly || plugin.isInstalled
            matchesCategory && matchesSearch && matchesInstalled
        }
    }

    /** Download and install a plugin APK */
    fun installPlugin(plugin: GitHubPluginInfo) {
        viewModelScope.launch {
            _state.value = PluginBrowserState.Downloading(plugin.pluginId, 0f)
            try {
                val apkFile = withContext(Dispatchers.IO) { downloadApk(plugin) }
                _state.value = PluginBrowserState.Installing(plugin.pluginId)
                val success = withContext(Dispatchers.IO) { installApk(apkFile) }
                if (success) {
                    _state.value = PluginBrowserState.SuccessMessage("${plugin.name} kuruldu!")
                    // Reload index to update installed status
                    loadPluginIndex()
                } else {
                    _state.value = PluginBrowserState.Error("Kurulum başarısız: ${plugin.name}")
                }
            } catch (e: Exception) {
                _state.value = PluginBrowserState.Error("İndirme/kurulum hatası: ${e.message}")
            }
        }
    }

    private suspend fun downloadApk(plugin: GitHubPluginInfo): File = withContext(Dispatchers.IO) {
        val url = URL(plugin.downloadUrl)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 30000
        connection.readTimeout = 60000

        val contentLength = connection.contentLengthLong
        val downloadsDir = getDownloadsDir()
        val apkFile = File(downloadsDir, "${plugin.pluginId}-v${plugin.version}.apk")

        connection.inputStream.use { input ->
            FileOutputStream(apkFile).use { output ->
                val buffer = ByteArray(8192)
                var downloaded = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    output.write(buffer, 0, read)
                    downloaded += read
                    if (contentLength > 0) {
                        // Progress updates could be sent here via state
                    }
                }
            }
        }
        connection.disconnect()
        apkFile
    }

    private suspend fun installApk(apkFile: File): Boolean = withContext(Dispatchers.IO) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Use FileProvider for Android 7+
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    appContext,
                    "${appContext.packageName}.fileprovider",
                    apkFile
                )
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive")
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            appContext.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun getDownloadsDir(): File {
        val dir = File(appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "atolye-plugins")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /** Open GitHub releases page for manual download */
    fun openGitHubPage(plugin: GitHubPluginInfo) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(plugin.downloadUrl))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(intent)
    }
}