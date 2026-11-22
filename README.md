# Atölye - Modüler Android Araç Uygulaması

Atölye, tüm araçların **tek bir APK** içinde eklenti (plugin) olarak derlendiği bir Android uygulamasıdır. Eklentiler ayrı uygulama değildir; Atölye'nin içinde modül olarak yaşar, kendi arayüzünü ve servislerini kendi paketinde tutar.

**Design Concept:** Atölye Tezgâhı (Workbench) - Varsayılan tema **Kâğıt** (F6F1E7/FCFAF5), diğer temalar: **Gece Atölyesi**, **Bakır**, **Devre Kartı**. Android 12+ dinamik renk destekli.

## 🏗 Mimari

```
atölye/
├── atolye-design/          # Tasarım sistemi (renk, tipografi, şekil, ikon)
├── toolbox-contract/       # Paylaşılan sözleşme (PluginMetadata, InstalledPlugin)
├── toolbox-host/           # Ana uygulama (Atölye) - tek APK
├── toolbox-plugin-base/    # Eklenti altyapısı (PluginProvider, Discovery, Control)
├── plugin-speedmeter/      # Eklenti: Hız Göstergesi (Camgöbeği) - library
├── plugin-keepawake/       # Eklenti: Ekranı Açık Tut (Kehribar) - library
├── plugin-uykucu/          # Eklenti: Uyku Zamanlayıcı - library
├── plugin-ses-secici/      # Eklenti: Ses Çıkış Seçici - library
├── plugin-izin-yoneticisi/ # Eklenti: İzin Yöneticisi - library
└── plugin-auto-scroll/     # Eklenti: Otomatik Kaydırma - library
```

### Eklenti kaydı ve keşfi

- Her eklenti modülü bir `PluginProvider` tanımlar; ad, açıklama, izinler, hızlı ayarlar ve Activity/receiver sınıf adları burada bildirilir.
- `:toolbox-host` modülü bu modüllere `implementation(project(...))` ile bağımlıdır.
- `com.atolye.host.plugins.BuiltInPlugins` listesi, APK'ya dahil eklentilerin **tek kayıt noktasıdır**.
- `PluginDiscovery`, `PackageManager` taraması yapmaz; kayıt listesindeki provider'lar üzerinden `InstalledPlugin` listesi üretir.
- Açılış ve komutlar aynı paket içindeki açık `ComponentName` ve broadcast kanallarıyla yapılır.
- Eski `com.toolbox.PLUGIN` keşif intent'i, metadata XML'leri ve harici APK indirme akışı kaldırılmıştır.

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

## 📱 Eklentiler

### 1. Hız Göstergesi (`plugin-speedmeter`) - *Camgöbeği*
- **Foreground Service** ile dinamik bildirim (saniyede bir güncelleme)
- **TrafficStats** tabanlı hesaplama
- **Dinamik bildirim ikonu** - hız metni bitmap üzerine çizilir
- **Ağ Modu**: Toplam / Sadece Wi-Fi / Sadece Mobil
- **Birim**: Yalnızca MB/s, tek ondalık (örn. `1,2 MB/s`)
- **Günlük/Aylık** veri kullanım istatistikleri
- **Açılışta auto-start** (`RECEIVE_BOOT_COMPLETED`)
- **Android 13+** bildirim izni yönetimi

### 2. Ekranı Açık Tut (`plugin-keepawake`) - *Kehribar*
- **Overlay pencere** (1x1 px şeffaf) + `FLAG_KEEP_SCREEN_ON`
- **Quick Settings Tile** - bildirim panelinden toggle
- **Dört mod**: Manuel, Zamanlı, Süresiz, Uygulama bazlı
- **Uygulama bazlı mod**: `UsageStatsManager`
- **Bildirim** ile durdurma aksiyonu
- **Wake lock** (PARTIAL_WAKE_LOCK) CPU için

### 3. Uykucu (`plugin-uykucu`)
- Geri sayım zamanlayıcı, bitişte ekran kilitleme
- Accessibility servisi ve exact alarm desteği
- Bildirim üzerinden uzatma/durdurma

### 4. Ses Seçici (`plugin-ses-secici`)
- Bluetooth / kablolu / USB / hoparlör çıkış seçimi
- Cihaz bazında ses ve yönlendirme tercihleri
- Quick Settings Tile desteği

### 5. İzin Yöneticisi (`plugin-izin-yoneticisi`)
- Salt okunur izin/paket listesi; okunabilir izin ve paket adları
- Kurulu uygulamaların izin durumunu tek ekranda gösterir

### 6. Otomatik Kaydırma (`plugin-auto-scroll`)
- X'te Zamanlı 15 sn, Reddit/Instagram'da Sürekli mod
- Video/reels içermez; yalnızca dokunmatik kaydırma hareketi gönderir
- Bölünmüş ekranda **Aktif / Sol-Üst / Sağ-Alt** hedef seçimi
- Kaydırma açıkken sürüklenebilir yüzen durdurma tuşu; kapanınca kendiliğinden kaybolur

## 🛠 Gereksinimler

- **JDK 17+**
- **Android SDK** API 34
- **Gradle wrapper** (projede mevcut)

## 🚀 Derleme ve kurulum

```bash
# Tek APK (tüm eklentiler dahil)
./gradlew :toolbox-host:assembleDebug

# Release (imzalama gerektirir)
./gradlew :toolbox-host:assembleRelease
```

APK konumu:

```
toolbox-host/build/outputs/apk/debug/toolbox-host-debug.apk
```

Cihaza kurulum:

```bash
./gradlew :toolbox-host:installDebug
```

> Eklentiler artık ayrı APK olarak derlenmez veya kurulmaz; hepsi yukarıdaki tek APK'nın içindedir.
>
> Geliştirme, test ve kurulum **USB ADB** ile yapılır; kablosuz ADB kullanılmaz.

## 🔧 Yeni eklenti ekleme

1. Yeni bir `plugin-*/` kütüphane modülü oluştur (`com.android.library`).
2. `settings.gradle.kts` içine `include(":plugin-yeni")` ekle.
3. `:toolbox-host` bağımlılıklarına `implementation(project(":plugin-yeni"))` ekle.
4. Modülde `PluginProvider` arayüzünü uygulayan bir `object` tanımla; `metadata(context)` içinde adı, açıklamayı, izinleri, kategoriyi ve hızlı ayarları bildir.
5. Eklenti Activity'si `BasePluginActivity`'den türesin ve `pluginMetadata`'yı provider'dan alsın.
6. Manifest'e Activity/servis/receiver'ları ekle (harici keşif intent'i veya meta-data gerekmez).
7. Provider'ı `BuiltInPlugins.all` listesine ekle.
8. `AtolyeTheme` ve tasarım sistemi bileşenlerini kullanarak arayüzü yaz.
9. Derle ve test et.

## 📝 Lisans

GPL-3.0-only - Özgür ve açık kaynak, Google servisleri gerektirmez.
