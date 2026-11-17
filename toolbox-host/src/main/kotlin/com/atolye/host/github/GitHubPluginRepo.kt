package com.atolye.host.github

import com.toolbox.contract.PluginCategory
import com.toolbox.contract.PluginMetadata
import com.toolbox.contract.PluginPermission
import com.toolbox.contract.PluginQuickSetting
import com.toolbox.contract.QuickSettingOption
import com.toolbox.contract.QuickSettingType
import kotlinx.serialization.Serializable

/**
 * GitHub Plugin Repository Model
 *
 * The index.json hosted on GitHub contains a list of available plugins
 * with their metadata for the plugin browser.
 *
 * Example index.json structure:
 * {
 *   "plugins": [
 *     {
 *       "pluginId": "com.toolbox.plugin.speedmeter",
 *       "version": 1,
 *       "minHostVersion": 1,
 *       "name": "Speedmeter",
 *       "description": "Network speed monitor",
 *       "iconResName": "ic_speedmeter",
 *       "settingsActivity": null,
 *       "mainActivity": "com.toolbox.plugin.speedmeter.MainActivity",
 *       "commandReceiver": "com.toolbox.plugin.speedmeter.PluginReceiver",
 *       "requiredPermissions": [...],
 *       "category": "NETWORK",
 *       "tags": ["network", "speed"],
 *       "downloadUrl": "https://github.com/.../speedmeter-v1.apk",
 *       "minSdk": 26,
 *       "accentColor": "#00A8B5",
 *       "quickSettings": [...]
 *     }
 *   ]
 * }
 */

@Serializable
data class GitHubPluginRepo(
    val plugins: List<GitHubPluginInfo>,
    val lastUpdated: Long = System.currentTimeMillis(),
    val version: Int = 1
)

@Serializable
data class GitHubPluginInfo(
    // Core identification
    val pluginId: String,
    val version: Int,
    val minHostVersion: Int = 1,

    // Display info (localized at build time, not resolved from resources)
    val name: String,
    val description: String,
    val iconResName: String? = null,

    // Component names
    val settingsActivity: String? = null,
    val mainActivity: String,
    val commandReceiver: String? = null,

    // Permissions & metadata
    val requiredPermissions: List<PluginPermission> = emptyList(),
    val category: PluginCategory = PluginCategory.TOOLS,
    val tags: List<String> = emptyList(),

    // Distribution
    val downloadUrl: String,
    val minSdk: Int = 26,
    val accentColor: String? = null,

    // Quick settings
    val quickSettings: List<PluginQuickSetting> = emptyList(),

    // Installation tracking
    val installedVersion: Int? = null,
    val isInstalled: Boolean = false
) {
    /** Convert to PluginMetadata for compatibility with existing discovery */
    fun toPluginMetadata(): PluginMetadata = PluginMetadata(
        pluginId = pluginId,
        version = version,
        minHostVersion = minHostVersion,
        name = name,
        description = description,
        iconResName = iconResName,
        settingsActivity = settingsActivity,
        mainActivity = mainActivity,
        commandReceiver = commandReceiver,
        requiredPermissions = requiredPermissions,
        category = category,
        tags = tags,
        downloadUrl = downloadUrl,
        minSdk = minSdk,
        accentColor = accentColor,
        quickSettings = quickSettings
    )
}

/** UI state for plugin browser */
sealed interface PluginBrowserState {
    data class Loading(val message: String = "Eklentiler yükleniyor...") : PluginBrowserState
    data class Success(val plugins: List<GitHubPluginInfo>, val categories: List<PluginCategory>) : PluginBrowserState
    data class Error(val message: String) : PluginBrowserState
    data class Downloading(val pluginId: String, val progress: Float) : PluginBrowserState
    data class Installing(val pluginId: String) : PluginBrowserState
    data class SuccessMessage(val message: String) : PluginBrowserState
}

/** Plugin browser filter state */
data class PluginBrowserFilter(
    val selectedCategory: PluginCategory? = null,
    val searchQuery: String = "",
    val showInstalledOnly: Boolean = false
)