package com.atolye.host.plugins

import com.toolbox.plugin.PluginProvider
import com.toolbox.plugin.autoscroll.AutoScrollPluginProvider
import com.toolbox.plugin.izin.IzinPluginProvider
import com.toolbox.plugin.keepawake.KeepAwakePluginProvider
import com.toolbox.plugin.sessecici.SesSeciciPluginProvider
import com.toolbox.plugin.speedmeter.SpeedMeterPluginProvider
import com.toolbox.plugin.uykucu.UykucuPluginProvider

/**
 * Atölye APK'sına dahil edilen eklentilerin tek kayıt noktası.
 *
 * Yeni bir eklenti eklemek için:
 * 1. `plugin-*` kütüphane modülünde bir [PluginProvider] tanımla.
 * 2. Modülü `:toolbox-host` bağımlılığına ekle.
 * 3. Sağlayıcıyı aşağıdaki listeye ekle.
 *
 * Harici APK keşfi (PackageManager / `com.toolbox.PLUGIN` intent'i) artık
 * kullanılmıyor; eklentiler uygulamanın içinde derlenir.
 */
object BuiltInPlugins {
    val all: List<PluginProvider> = listOf(
        SpeedMeterPluginProvider,
        KeepAwakePluginProvider,
        UykucuPluginProvider,
        SesSeciciPluginProvider,
        IzinPluginProvider,
        AutoScrollPluginProvider
    )
}
