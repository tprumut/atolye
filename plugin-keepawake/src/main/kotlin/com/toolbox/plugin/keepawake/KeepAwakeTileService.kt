package com.toolbox.plugin.keepawake

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.toolbox.plugin.PluginSettingsManager

/**
 * Hızlı ayarlar kartosu.
 *
 * Kartoya dokunmak ekranı açık tutmayı başlatır/durdurur. Karto
 * ayarlardan kapatıldığında devre dışı bırakılır.
 */
class KeepAwakeTileService : TileService() {

    private val settings by lazy { PluginSettingsManager(this) }

    override fun onTileAdded() {
        super.onTileAdded()
        updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()

        val pluginId = KeepAwakeKeys.PLUGIN_ID
        if (!settings.getBoolean(pluginId, KeepAwakeKeys.QUICK_SETTINGS, true)) {
            updateTile()
            return
        }

        val running = settings.getBoolean(pluginId, KeepAwakeKeys.SERVICE_RUNNING, false)

        if (running) {
            KeepAwakeOverlayService.stop(this)
            applyTileState(running = false)
        } else {
            if (!KeepAwakePermissions.canDrawOverlays(this)) {
                updateTile()
                return
            }
            val mode = settings.getInt(pluginId, KeepAwakeKeys.MODE, KeepAwakeKeys.MODE_MANUAL)
            val duration = settings.getInt(pluginId, KeepAwakeKeys.TIMER_DURATION, 15)
            KeepAwakeOverlayService.start(this, mode, duration)
            applyTileState(running = true)
        }
    }

    private fun updateTile() {
        val tile = getQsTile() ?: return

        val pluginId = KeepAwakeKeys.PLUGIN_ID
        val enabled = settings.getBoolean(pluginId, KeepAwakeKeys.QUICK_SETTINGS, true)
        if (!enabled) {
            tile.state = Tile.STATE_UNAVAILABLE
            tile.label = getString(R.string.screen_normal)
            tile.icon = Icon.createWithResource(this, R.drawable.ic_qs_screen_off)
            tile.updateTile()
            return
        }

        val running = settings.getBoolean(pluginId, KeepAwakeKeys.SERVICE_RUNNING, false)
        applyTileState(running)
    }

    private fun applyTileState(running: Boolean) {
        val tile = getQsTile() ?: return
        tile.state = if (running) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(
            if (running) R.string.screen_kept_awake else R.string.screen_normal
        )
        tile.icon = Icon.createWithResource(
            this,
            if (running) R.drawable.ic_qs_screen_on else R.drawable.ic_qs_screen_off
        )
        tile.updateTile()
    }
}
