package com.toolbox.plugin

import android.content.Context
import com.toolbox.contract.PluginMetadata

/**
 * Atölye APK'sının içinde derlenen bir eklentinin host'a tanıtımı.
 *
 * Eski yapıda eklentiler ayrı APK'lar olduğu için host onları
 * `PackageManager.queryIntentActivities()` ile keşfederdi. Artık tüm eklentiler
 * tek APK içinde olduğundan keşif, derleme zamanında bilinen bu sağlayıcı
 * listesi üzerinden yapılır.
 *
 * Eklentiler hâlâ kendi modüllerinde/kendi paketlerinde yaşar; sadece
 * dağıtım ve keşif uygulamanın içine taşınmıştır.
 */
interface PluginProvider {
    /** Benzersiz eklenti ID'si. */
    val pluginId: String

    /** Eklentinin ana Activity'sinin tam sınıf adı. */
    val mainActivityClassName: String

    /** Ayarlar Activity'sinin tam sınıf adı (yoksa null). */
    val settingsActivityClassName: String?

    /** Komut BroadcastReceiver'ının tam sınıf adı (yoksa null). */
    val commandReceiverClassName: String?

    /** Eklentinin yerelleştirilmiş meta verisi. */
    fun metadata(context: Context): PluginMetadata
}
