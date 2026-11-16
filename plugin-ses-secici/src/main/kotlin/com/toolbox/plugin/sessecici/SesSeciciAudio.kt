package com.toolbox.plugin.sessecici

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import com.toolbox.plugin.PluginSettingsManager

data class OutputDeviceUi(
    val id: Int,
    val key: String,
    val name: String,
    val typeLabel: String,
    val isCurrent: Boolean,
    val isCommunication: Boolean,
    val savedVolume: Int?,
    val remember: Boolean,
    val maxVolume: Int,
    val info: AudioDeviceInfo
)

object SesSeciciAudio {

    fun audioManager(context: Context): AudioManager =
        context.getSystemService(AudioManager::class.java)

    fun listOutputDevices(context: Context, settings: PluginSettingsManager): List<OutputDeviceUi> {
        val am = audioManager(context)
        val devices = try { am.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList() } catch (_: Throwable) { emptyList() }
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val comm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try { am.communicationDevice } catch (_: Throwable) { null }
        } else null
        return devices
            .filter { isRelevant(it) }
            .distinctBy { deviceKey(it) }
            .map { d ->
                val key = deviceKey(d)
                val remember = settings.getBoolean(SesSeciciKeys.PLUGIN_ID, SesSeciciKeys.rememberKey(key), false)
                val saved = if (remember) settings.getInt(SesSeciciKeys.PLUGIN_ID, SesSeciciKeys.volumeKey(key), -1).takeIf { it >= 0 } else null
                OutputDeviceUi(
                    id = d.id,
                    key = key,
                    name = deviceName(d),
                    typeLabel = deviceTypeLabel(context, d),
                    isCurrent = (comm != null && comm.id == d.id),
                    isCommunication = isCommunicationCapable(d),
                    savedVolume = saved,
                    remember = remember,
                    maxVolume = max,
                    info = d
                )
            }
            .sortedWith(compareBy<OutputDeviceUi> { typeOrder(it.info) }.thenBy { it.name.lowercase() })
    }

    fun deviceKey(d: AudioDeviceInfo): String {
        val addr = try { d.address ?: "" } catch (_: Throwable) { "" }
        return "${d.type}_${d.productName}_${addr}"
    }

    fun deviceName(d: AudioDeviceInfo): String {
        val pn = try { d.productName?.toString()?.trim().orEmpty() } catch (_: Throwable) { "" }
        return if (pn.isNotBlank()) pn else typeFallback(d)
    }

    fun deviceTypeLabel(context: Context, d: AudioDeviceInfo): String = when (d.type) {
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> context.getString(R.string.device_speaker)
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_USB_HEADSET -> context.getString(R.string.device_wired)
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO, AudioDeviceInfo.TYPE_BLE_HEADSET, AudioDeviceInfo.TYPE_BLE_SPEAKER, AudioDeviceInfo.TYPE_HEARING_AID -> context.getString(R.string.device_bluetooth)
        AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_ACCESSORY -> context.getString(R.string.device_usb)
        else -> context.getString(R.string.device_other)
    }

    private fun typeFallback(d: AudioDeviceInfo): String = when (d.type) {
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Hoparlör"
        AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "Ahize"
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Kablolu Kulaklık"
        AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Kablolu Kulaklık"
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth A2DP"
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth SCO"
        AudioDeviceInfo.TYPE_USB_DEVICE -> "USB Ses"
        else -> "Ses Cihazı (${d.type})"
    }

    private fun isRelevant(d: AudioDeviceInfo): Boolean = when (d.type) {
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
        AudioDeviceInfo.TYPE_BUILTIN_EARPIECE,
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
        AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_USB_HEADSET,
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
        AudioDeviceInfo.TYPE_BLE_HEADSET,
        AudioDeviceInfo.TYPE_BLE_SPEAKER,
        AudioDeviceInfo.TYPE_USB_DEVICE,
        AudioDeviceInfo.TYPE_USB_ACCESSORY,
        AudioDeviceInfo.TYPE_HEARING_AID -> true
        else -> false
    }

    private fun isCommunicationCapable(d: AudioDeviceInfo): Boolean = when (d.type) {
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
        AudioDeviceInfo.TYPE_BUILTIN_EARPIECE,
        AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_USB_HEADSET,
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
        AudioDeviceInfo.TYPE_BLE_HEADSET -> true
        else -> false
    }

    private fun typeOrder(d: AudioDeviceInfo): Int = when (d.type) {
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_USB_HEADSET -> 0
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO, AudioDeviceInfo.TYPE_BLE_HEADSET, AudioDeviceInfo.TYPE_BLE_SPEAKER -> 1
        AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_ACCESSORY -> 2
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> 3
        else -> 4
    }

    fun currentMusicVolume(context: Context): Int =
        try { audioManager(context).getStreamVolume(AudioManager.STREAM_MUSIC) } catch (_: Throwable) { 0 }

    fun maxMusicVolume(context: Context): Int =
        try { audioManager(context).getStreamMaxVolume(AudioManager.STREAM_MUSIC) } catch (_: Throwable) { 15 }

    fun setMusicVolume(context: Context, volume: Int) {
        try {
            val am = audioManager(context)
            val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            am.setStreamVolume(AudioManager.STREAM_MUSIC, volume.coerceIn(0, max), 0)
        } catch (_: Throwable) {}
    }

    fun saveDeviceVolume(settings: PluginSettingsManager, key: String, volume: Int, remember: Boolean) {
        settings.putBoolean(SesSeciciKeys.PLUGIN_ID, SesSeciciKeys.rememberKey(key), remember)
        if (remember) settings.putInt(SesSeciciKeys.PLUGIN_ID, SesSeciciKeys.volumeKey(key), volume)
    }

    fun applySavedForDevice(context: Context, settings: PluginSettingsManager, d: AudioDeviceInfo) {
        val auto = settings.getBoolean(SesSeciciKeys.PLUGIN_ID, SesSeciciKeys.AUTO_APPLY_ON_CONNECT, true)
        if (!auto) return
        val key = deviceKey(d)
        val remember = settings.getBoolean(SesSeciciKeys.PLUGIN_ID, SesSeciciKeys.rememberKey(key), false)
        if (!remember) return
        val vol = settings.getInt(SesSeciciKeys.PLUGIN_ID, SesSeciciKeys.volumeKey(key), -1)
        if (vol >= 0) setMusicVolume(context, vol)
    }

    /**
     * Çıkış geçişini dener.
     * Android 12+ (API 31): iletişim cihazı (SCO/kulaklık/ahize/hoparlör) için setCommunicationDevice/clearCommunicationDevice.
     * Diğer durumlarda sistem medya yönlendirmesi genellikle 3. taraf uygulamalardan doğrudan değiştirilemez.
     * Dönen Pair(success, messageResNullable): kullanıcıya net durum gösterimi için.
     */
    fun trySwitchOutput(context: Context, device: AudioDeviceInfo): Pair<Boolean, String?> {
        val am = audioManager(context)
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Hoparlöre dönmek için iletişim cihazını temizlemeyi dene
                if (device.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
                    try { am.clearCommunicationDevice() } catch (_: Throwable) {}
                    return true to null
                }
                val ok = am.setCommunicationDevice(device)
                if (ok) true to null else false to "unsupported"
            } else {
                // Eski sürümlerde sınırlı: Bluetooth SCO aç/kapat denemesi
                when (device.type) {
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO, AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> {
                        try {
                            @Suppress("DEPRECATION")
                            am.mode = AudioManager.MODE_NORMAL
                            @Suppress("DEPRECATION")
                            am.startBluetoothSco()
                            @Suppress("DEPRECATION")
                            am.isBluetoothScoOn = true
                            true to null
                        } catch (_: Throwable) { false to "unsupported" }
                    }
                    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> {
                        try {
                            @Suppress("DEPRECATION")
                            am.stopBluetoothSco()
                            @Suppress("DEPRECATION")
                            am.isBluetoothScoOn = false
                            @Suppress("DEPRECATION")
                            am.isSpeakerphoneOn = true
                            true to null
                        } catch (_: Throwable) { false to "unsupported" }
                    }
                    else -> false to "unsupported"
                }
            }
        } catch (_: Throwable) {
            false to "unsupported"
        }
    }

    fun getActivePlaybackAppLabels(context: Context): List<String> {
        return try {
            val am = audioManager(context)
            val configs = am.activePlaybackConfigurations
            val pm = context.packageManager
            configs.mapNotNull { cfg ->
                try {
                    val attr = cfg.audioAttributes
                    val usage = attr?.usage
                    // Paket adı doğrudan erişim API seviyesine göre değişir; en güvenlisi boş dönmek yerine genel bilgi vermek
                    null as String?
                } catch (_: Throwable) { null }
            }
        } catch (_: Throwable) { emptyList() }
    }
}
