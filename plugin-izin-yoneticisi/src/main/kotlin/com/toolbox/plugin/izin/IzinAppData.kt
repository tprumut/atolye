package com.toolbox.plugin.izin

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import java.util.Locale

/** Tek bir izin kaydı. */
data class IzinPermission(
    val name: String,
    val label: String,
    val group: String,
    val granted: Boolean,
)

/** Bir uygulamanın izin listesi. */
data class IzinApp(
    val packageName: String,
    val label: String,
    val versionName: String?,
    val isSystem: Boolean,
    val permissions: List<IzinPermission>,
) {
    val grantedCount: Int get() = permissions.count { it.granted }
}

/**
 * Cihazdaki uygulamaların PackageManager izin kayıtlarını okur.
 *
 * Android 11+ paket görünürlüğü nedeniyle modül manifestinde
 * QUERY_ALL_PACKAGES istenir; aksi halde liste yalnızca birkaç uygulamayla
 * sınırlı kalır.
 */
object IzinAppData {

    private val trLocale = Locale("tr", "TR")

    fun load(context: Context, includeSystem: Boolean): List<IzinApp> {
        val pm = context.packageManager
        val installed = try {
            pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        } catch (_: Throwable) {
            emptyList()
        }

        return installed.mapNotNull { info -> toApp(pm, info, includeSystem) }
            .sortedBy { it.label.lowercase(trLocale) }
    }

    private fun toApp(pm: PackageManager, info: PackageInfo, includeSystem: Boolean): IzinApp? {
        val appInfo = info.applicationInfo ?: return null
        val system = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
            (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        if (!includeSystem && system) return null

        val label = try {
            appInfo.loadLabel(pm).toString().ifBlank { info.packageName }
        } catch (_: Throwable) {
            info.packageName
        }

        val requested = info.requestedPermissions ?: emptyArray()
        val flags = info.requestedPermissionsFlags ?: IntArray(requested.size)
        val permissions = requested.mapIndexedNotNull { index, name ->
            if (name.isBlank()) return@mapIndexedNotNull null
            val granted = ((flags.getOrNull(index) ?: 0) and
                PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
            IzinPermission(
                name = name,
                label = permissionLabel(name),
                group = permissionGroup(name),
                granted = granted,
            )
        }.sortedWith(compareByDescending<IzinPermission> { it.granted }.thenBy { it.label })

        if (permissions.isEmpty()) return null
        return IzinApp(
            packageName = info.packageName,
            label = label,
            versionName = info.versionName,
            isSystem = system,
            permissions = permissions,
        )
    }

    private fun permissionLabel(name: String): String = PERMISSION_LABELS[name]
        ?: readableName(name.substringAfterLast('.'))

    private fun readableName(raw: String): String {
        val words = raw.replace('_', ' ')
            .lowercase(trLocale)
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
        if (words.isEmpty()) return raw
        return words.joinToString(" ") { word ->
            word[0].uppercase(trLocale) + word.substring(1)
        }
    }

    private fun permissionGroup(name: String): String = when {
        name.contains("ACCESSIBILITY") -> "Erişilebilirlik"
        name.contains("SYSTEM_ALERT_WINDOW") || name.contains("USAGE_STATS") ||
            name.contains("WRITE_SETTINGS") || name.contains("MANAGE_EXTERNAL_STORAGE") ||
            name.contains("REQUEST_INSTALL_PACKAGES") -> "Özel erişim"
        name.contains("CAMERA") -> "Kamera"
        name.contains("RECORD_AUDIO") || name.contains("MICROPHONE") -> "Mikrofon"
        name.contains("LOCATION") -> "Konum"
        name.contains("CONTACTS") -> "Kişiler"
        name.contains("SMS") -> "SMS"
        name.contains("PHONE") || name.contains("CALL") -> "Telefon"
        name.contains("CALENDAR") -> "Takvim"
        name.contains("POST_NOTIFICATIONS") || name.contains("NOTIFICATION") -> "Bildirim"
        name.contains("STORAGE") || name.contains("MEDIA") || name.contains("EXTERNAL_STORAGE") -> "Depolama"
        name.contains("BLUETOOTH") -> "Bluetooth"
        name.contains("WIFI") || name.contains("NETWORK") -> "Ağ"
        name.contains("ACTIVITY_RECOGNITION") || name.contains("BODY_SENSORS") ||
            name.contains("HEALTH") -> "Sağlık ve hareket"
        name.contains("FOREGROUND_SERVICE") -> "Arka plan"
        else -> "Diğer"
    }

    private val PERMISSION_LABELS = mapOf(
        "android.permission.INTERNET" to "İnternet erişimi",
        "android.permission.ACCESS_NETWORK_STATE" to "Ağ durumunu görüntüleme",
        "android.permission.CHANGE_NETWORK_STATE" to "Ağ durumunu değiştirme",
        "android.permission.ACCESS_WIFI_STATE" to "Wi-Fi bilgilerini görüntüleme",
        "android.permission.CHANGE_WIFI_STATE" to "Wi-Fi'yi açma/kapatma",
        "android.permission.CAMERA" to "Kamera",
        "android.permission.RECORD_AUDIO" to "Mikrofon (ses kaydı)",
        "android.permission.ACCESS_FINE_LOCATION" to "Hassas konum (GPS)",
        "android.permission.ACCESS_COARSE_LOCATION" to "Yaklaşık konum",
        "android.permission.ACCESS_BACKGROUND_LOCATION" to "Arka planda konum",
        "android.permission.READ_CONTACTS" to "Kişileri okuma",
        "android.permission.WRITE_CONTACTS" to "Kişileri düzenleme",
        "android.permission.GET_ACCOUNTS" to "Hesapları görüntüleme",
        "android.permission.READ_CALENDAR" to "Takvimi okuma",
        "android.permission.WRITE_CALENDAR" to "Takvimi düzenleme",
        "android.permission.READ_SMS" to "SMS'leri okuma",
        "android.permission.SEND_SMS" to "SMS gönderme",
        "android.permission.RECEIVE_SMS" to "SMS alma",
        "android.permission.READ_PHONE_STATE" to "Telefon durumu",
        "android.permission.READ_PHONE_NUMBERS" to "Telefon numaraları",
        "android.permission.CALL_PHONE" to "Telefon görüşmesi başlatma",
        "android.permission.READ_CALL_LOG" to "Arama kayıtlarını okuma",
        "android.permission.WRITE_CALL_LOG" to "Arama kayıtlarını düzenleme",
        "android.permission.READ_EXTERNAL_STORAGE" to "Depolamayı okuma",
        "android.permission.WRITE_EXTERNAL_STORAGE" to "Depolamaya yazma",
        "android.permission.MANAGE_EXTERNAL_STORAGE" to "Tüm dosyalara erişim",
        "android.permission.ACCESS_MEDIA_LOCATION" to "Medya konum bilgisi",
        "android.permission.READ_MEDIA_IMAGES" to "Görselleri okuma",
        "android.permission.READ_MEDIA_VIDEO" to "Videoları okuma",
        "android.permission.READ_MEDIA_AUDIO" to "Ses dosyalarını okuma",
        "android.permission.READ_MEDIA_VISUAL_USER_SELECTED" to "Seçili medyayı okuma",
        "android.permission.POST_NOTIFICATIONS" to "Bildirim gönderme",
        "android.permission.BODY_SENSORS" to "Vücut sensörleri",
        "android.permission.ACTIVITY_RECOGNITION" to "Hareket ve aktivite",
        "android.permission.BLUETOOTH_CONNECT" to "Bluetooth'a bağlanma",
        "android.permission.BLUETOOTH_SCAN" to "Bluetooth cihazlarını arama",
        "android.permission.BLUETOOTH_ADVERTISE" to "Bluetooth yayını",
        "android.permission.NFC" to "NFC",
        "android.permission.VIBRATE" to "Titreşim",
        "android.permission.WAKE_LOCK" to "Ekranı açık tutma",
        "android.permission.FOREGROUND_SERVICE" to "Ön plan servisi",
        "android.permission.FOREGROUND_SERVICE_CAMERA" to "Ön plan servisi: kamera",
        "android.permission.FOREGROUND_SERVICE_MICROPHONE" to "Ön plan servisi: mikrofon",
        "android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" to "Ön plan servisi: medya",
        "android.permission.FOREGROUND_SERVICE_SPECIAL_USE" to "Ön plan servisi: özel kullanım",
        "android.permission.RECEIVE_BOOT_COMPLETED" to "Açılışta başlatma",
        "android.permission.SCHEDULE_EXACT_ALARM" to "Kesin zamanlı alarm",
        "android.permission.USE_EXACT_ALARM" to "Kesin alarm kullanımı",
        "android.permission.SYSTEM_ALERT_WINDOW" to "Diğer uygulamaların üstünde göster",
        "android.permission.PACKAGE_USAGE_STATS" to "Kullanım erişimi",
        "android.permission.QUERY_ALL_PACKAGES" to "Tüm uygulamaları görüntüleme",
        "android.permission.REQUEST_INSTALL_PACKAGES" to "Uygulama yükleme isteği",
        "android.permission.WRITE_SETTINGS" to "Sistem ayarlarını değiştirme",
        "android.permission.BIND_ACCESSIBILITY_SERVICE" to "Erişilebilirlik servisi",
        "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE" to "Bildirimleri okuma",
        "android.permission.BIND_DEVICE_ADMIN" to "Cihaz yöneticisi",
    )
}
