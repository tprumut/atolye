# Atölye - Modular Android Utility App

A modular Android application framework where each tool is a separate plugin APK. Designed for F-Droid distribution and devices without Google Play Services.

**Design Concept:** Atölye Tezgâhı (Workbench) - Varsayılan tema **Kâğıt** (F6F1E7/FCFAF5), diğer temalar: **Gece Atölyesi**, **Bakır**, **Devre Kartı**. Android 12+ dinamik renk destekli.

## 🏗 Architecture

```
toolbox/
├── atolye-design/          # Tasarım sistemi (renk, tipografi, şekil, ikon)
├── toolbox-contract/       # Paylaşılan plugin sözleşmesi
├── toolbox-host/           # Ana uygulama (Atölye)
├── toolbox-plugin-base/    # Plugin base class'ları
├── plugin-speedmeter/      # Plugin 1: İnternet Hızı (Camgöbeği)
└── plugin-keepawake/       # Plugin 2: Ekranı Açık Tut (Kehribar)
```

### Plugin Discovery
- Host app uses `PackageManager` to find activities with `com.toolbox.PLUGIN` intent action
- Plugins declare metadata in `AndroidManifest.xml` via `<meta-data>`
- Contract versioning ensures compatibility

## 🎨 Tasarım Sistemi (atolye-design)

| Bileşen | Detay |
|---------|-------|
| **Temalar** | Kâğıt (varsayılan), Gece Atölyesi, Bakır, Devre Kartı, Dinamik (Android 12+) |
| **Yazı Tipleri** | Inter (genel), JetBrains Mono (sayılar/veri) - Variable font, uygulama içinde gömülü |
| **Köşe Yuvarlaklığı** | 16dp standart (Card, Button, Chip, Dialog) |
| **Kart Stili** | Gölge yok, ince çerçeve (1dp outlineVariant) |
| **İkon** | Altıgen yuvalı (uyarlanabilir ikon standardı), ortada delik |
| **Eklenti Renkleri** | Speed Meter: Camgöbeği (00B5C4), Keep Awake: Kehribar (FFB300) |

### Renk Paleti - Kâğıt Tema
- **Zemin**: F6F1E7 | **Kart**: FCFAF5 | **Çerçeve**: DDD5C4
- **Metin**: 22262E | **İkincil Metin**: 6B6657 | **Vurgu**: 2B4C7E

## 📱 Plugins

### 1. İnternet Hızı Göstergesi (`plugin-speedmeter`) - *Camgöbeği*
- **Foreground Service** ile dinamik bildirim (saniyede bir güncelleme)
- **TrafficStats** tabanlı hesaplama
- **Dinamik bildirim ikonu** - hız metni bitmap üzerine çizilir
- **Ağ Modu**: Toplam / Sadece Wi-Fi / Sadece Mobil
- **Birim**: Otomatik (KB/s/MB/s), KB/s, MB/s
- **Günlük/Aylık** veri kullanım istatistikleri
- **Açılışta auto-start** (`RECEIVE_BOOT_COMPLETED`)
- **Android 13+** bildirim izni yönetimi
- Düşük pil tüketimi

### 2. Ekranı Açık Tut (`plugin-keepawake`) - *Kehribar*
- **Overlay pencere** (1x1 px şeffaf) + `FLAG_KEEP_SCREEN_ON`
- **Quick Settings Tile** - bildirim panelinden toggle
- **Dört mod**:
  - Manuel (dokunarak aç/kapa)
  - Zamanlı (15dk, 30dk, 1sa, 2sa)
  - Süresiz (manuel kapatılana kadar)
  - Uygulama bazlı (seçili uygulamalarda ekran açık)
- **Uygulama bazlı mod**: `UsageStatsManager` (Kullanım İstatistikleri izni)
- **Bildirim** ile durdurma aksiyonu
- **Overlay izni** açıklama dialog'u ile yönetim
- **Wake lock** (PARTIAL_WAKE_LOCK) CPU için

## 🛠 Requirements

- **JDK 17+**
- **Android SDK** with API 34
- **Gradle 8.5+** (wrapper will be generated)

## 🚀 Build Instructions

### 1. Generate Gradle Wrapper
```bash
cd toolbox
gradle wrapper --gradle-version 8.5
```

### 2. Build All Modules
```bash
# Debug builds
./gradlew assembleDebug

# Release builds
./gradlew assembleRelease
```

### 3. Install on Device
```bash
# Host app (Atölye)
./gradlew :toolbox-host:installDebug

# Speed Meter plugin
./gradlew :plugin-speedmeter:installDebug

# Keep Awake plugin
./gradlew :plugin-keepawake:installDebug
```

### 4. Build APKs for Distribution
```bash
# All debug APKs
./gradlew assembleDebug

# All release APKs (requires signing config)
./gradlew assembleRelease
```

APKs will be in:
- `toolbox-host/build/outputs/apk/debug/toolbox-host-debug.apk`
- `plugin-speedmeter/build/outputs/apk/debug/plugin-speedmeter-debug.apk`
- `plugin-keepawake/build/outputs/apk/debug/plugin-keepawake-debug.apk`

## 📋 F-Droid Metadata

Each plugin needs separate F-Droid metadata. Example for `plugin-speedmeter`:

```yaml
# fdroid/metadata/com.toolbox.plugin.speedmeter.yml
Categories:
  - Network
License: GPL-3.0-only
WebSite: https://github.com/toolbox/plugins
SourceCode: https://github.com/toolbox/plugins
IssueTracker: https://github.com/toolbox/plugins/issues
Donate: https://github.com/sponsors/toolbox

AutoName: Internet Speed Meter
RepoType: git
Repo: https://github.com/toolbox/plugins.git
Builds:
  - versionName: '1.0'
    versionCode: 1
    commit: v1.0
    subdir: plugin-speedmeter
    gradle:
      - yes
```

## 🔧 Yeni Plugin Ekleme

1. Create new module directory (e.g., `plugin-newtool/`)
2. Add `build.gradle.kts` with `com.android.application` plugin
3. Add to `settings.gradle.kts`: `include(":plugin-newtool")`
4. Add `implementation(project(":atolye-design"))` dependency
5. Create plugin Activity extending `BasePluginActivity`
6. Implement `pluginMetadata` with unique `pluginId`
7. Add plugin metadata XML in `res/xml/plugin_metadata.xml`
8. Declare `com.toolbox.PLUGIN` intent filter in Manifest
9. Use `AtolyeTheme` in ComposeView
10. Use `AtolyeCard`, `DataStyles`, plugin-specific icons
11. Build and test!

## 📝 License

GPL-3.0-only - Free and open source, no Google services required.

## 🤝 Contributing

1. Fork the repository
2. Create feature branch
3. Add plugin or improve existing
4. Submit PR with description

## 📱 Tested On

- Android 8.0 (API 26) - Minimum
- Android 14 (API 34) - Target
- Tablet layouts optimized
- No Google Play Services required
- Works on /e/OS, LineageOS, GrapheneOS, etc.