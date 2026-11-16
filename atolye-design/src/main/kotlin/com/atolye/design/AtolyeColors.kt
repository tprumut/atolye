package com.atolye.design

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Atölye Tasarım Sistemi - Renk Paleti
 * Konsept: Atölye Tezgâhı
 */

// ============================================================================
// KÂĞIT TEMA (Varsayılan - Light)
// ============================================================================
object KagitColors {
    // Temel yüzeyler
    val Background = Color(0xFFF6F1E7)      // Kâğıt zemin
    val Surface = Color(0xFFFCFAF5)          // Kart yüzeyi
    val SurfaceContainer = Color(0xFFF8F3EB) // Kapsayıcı
    val SurfaceVariant = Color(0xFFF0EBE0)   // Varyant
    val Outline = Color(0xFFDDD5C4)          // Çerçeve/kenarlık
    val OutlineVariant = Color(0xFFE5DDCE)   // İnce çerçeve

    // Metin
    val OnBackground = Color(0xFF22262E)     // Ana metin
    val OnSurface = Color(0xFF22262E)        // Yüzey metni
    val OnSurfaceVariant = Color(0xFF6B6657) // İkincil metin
    val InverseOnSurface = Color(0xFFF6F1E7) // Ters metin

    // Vurgu
    val Primary = Color(0xFF2B4C7E)          // Ana vurgu - Koyu mavi
    val PrimaryContainer = Color(0xFFD6E4F0) // Açık mavi konteyner
    val OnPrimary = Color(0xFFFFFFFF)
    val OnPrimaryContainer = Color(0xFF1A2D54)

    val Secondary = Color(0xFF5C6B4A)        // İkincil - Yeşilimsi
    val SecondaryContainer = Color(0xFFE8ECD6)
    val OnSecondary = Color(0xFFFFFFFF)
    val OnSecondaryContainer = Color(0xFF3D4A2F)

    val Tertiary = Color(0xFF8B5E3C)         // Üçüncül - Toprak
    val TertiaryContainer = Color(0xFFF5E8DD)
    val OnTertiary = Color(0xFFFFFFFF)
    val OnTertiaryContainer = Color(0xFF5D3E24)

    // Durum
    val Error = Color(0xFFB33A3A)
    val ErrorContainer = Color(0xFFFBE9E9)
    val OnError = Color(0xFFFFFFFF)
    val OnErrorContainer = Color(0xFF6B1A1A)

    val Success = Color(0xFF3A7D44)
    val SuccessContainer = Color(0xFFE8F5EA)

    // Gölge (kullanılmaz - çerçeve tercih edilir)
    val Shadow = Color(0x1A000000)
    val Scrim = Color(0x7F000000)

    // Eklenti renkleri
    val PluginSpeedMeter = Color(0xFF00B5C4)      // Camgöbeği - İnternet hızı
    val PluginSpeedMeterContainer = Color(0xFFE0F7FA)
    val PluginKeepAwake = Color(0xFFFFB300)       // Kehribar - Ekran açık tut
    val PluginKeepAwakeContainer = Color(0xFFFFF3E0)
}

// ============================================================================
// GECE ATÖLYESİ TEMA (Dark)
// ============================================================================
object GeceAtolyesiColors {
    val Background = Color(0xFF1A1D1E)
    val Surface = Color(0xFF23272A)
    val SurfaceContainer = Color(0xFF2A2F32)
    val SurfaceVariant = Color(0xFF33383C)
    val Outline = Color(0xFF4A4F54)
    val OutlineVariant = Color(0xFF3D4247)

    val OnBackground = Color(0xFFE8E6E0)
    val OnSurface = Color(0xFFE8E6E0)
    val OnSurfaceVariant = Color(0xFFA8A090)
    val InverseOnSurface = Color(0xFF1A1D1E)

    val Primary = Color(0xFF9DBEF5)
    val PrimaryContainer = Color(0xFF1E3A5F)
    val OnPrimary = Color(0xFF0D1B2E)
    val OnPrimaryContainer = Color(0xFFD6E4F0)

    val Secondary = Color(0xFFC5D6A8)
    val SecondaryContainer = Color(0xFF3D4A2F)
    val OnSecondary = Color(0xFF1F2618)
    val OnSecondaryContainer = Color(0xFFE8ECD6)

    val Tertiary = Color(0xFFE8C4A0)
    val TertiaryContainer = Color(0xFF5D3E24)
    val OnTertiary = Color(0xFF2E1D10)
    val OnTertiaryContainer = Color(0xFFF5E8DD)

    val Error = Color(0xFFFFB4AB)
    val ErrorContainer = Color(0xFF6B1A1A)
    val OnError = Color(0xFF3D0A0A)
    val OnErrorContainer = Color(0xFFFBE9E9)

    val Success = Color(0xFFA5D6A7)
    val SuccessContainer = Color(0xFF1B4D20)

    val Shadow = Color(0x33000000)
    val Scrim = Color(0xBF000000)

    val PluginSpeedMeter = Color(0xFF40E0D0)
    val PluginSpeedMeterContainer = Color(0xFF004D4A)
    val PluginKeepAwake = Color(0xFFFFD54F)
    val PluginKeepAwakeContainer = Color(0xFF5D4000)
}

// ============================================================================
// BAKIR TEMA (Copper)
// ============================================================================
object BakirColors {
    val Background = Color(0xFF2D1F1A)
    val Surface = Color(0xFF3D2A22)
    val SurfaceContainer = Color(0xFF4A352A)
    val SurfaceVariant = Color(0xFF5C4433)
    val Outline = Color(0xFFB87333)
    val OutlineVariant = Color(0xFF8B5A2B)

    val OnBackground = Color(0xFFF5E6D8)
    val OnSurface = Color(0xFFF5E6D8)
    val OnSurfaceVariant = Color(0xFFD4A57A)
    val InverseOnSurface = Color(0xFF2D1F1A)

    val Primary = Color(0xFFE87A2E)
    val PrimaryContainer = Color(0xFF5C2D0E)
    val OnPrimary = Color(0xFF2D1305)
    val OnPrimaryContainer = Color(0xFFFFDAB3)

    val Secondary = Color(0xFFCD9A5C)
    val SecondaryContainer = Color(0xFF4A2E1A)
    val OnSecondary = Color(0xFF2D180A)
    val OnSecondaryContainer = Color(0xFFF5E0C8)

    val Tertiary = Color(0xFF4A7C59)
    val TertiaryContainer = Color(0xFF1A3D22)
    val OnTertiary = Color(0xFFFFFFFF)
    val OnTertiaryContainer = Color(0xFFA5D6A7)

    val Error = Color(0xFFFF8A80)
    val ErrorContainer = Color(0xFF5C1A1A)
    val OnError = Color(0xFFFFFFFF)
    val OnErrorContainer = Color(0xFFFBE9E9)

    val Shadow = Color(0x33000000)
    val Scrim = Color(0xBF000000)

    val PluginSpeedMeter = Color(0xFF00D4C8)
    val PluginSpeedMeterContainer = Color(0xFF004D4A)
    val PluginKeepAwake = Color(0xFFFFB800)
    val PluginKeepAwakeContainer = Color(0xFF5D3E00)
}

// ============================================================================
// DEVRE KARTI TEMA (Circuit Board - High Contrast)
// ============================================================================
object DevreKartiColors {
    val Background = Color(0xFF0A1F0A)
    val Surface = Color(0xFF0F2A0F)
    val SurfaceContainer = Color(0xFF143514)
    val SurfaceVariant = Color(0xFF1A421A)
    val Outline = Color(0xFF00FF41)
    val OutlineVariant = Color(0xFF00CC33)

    val OnBackground = Color(0xFFB8F5B8)
    val OnSurface = Color(0xFFB8F5B8)
    val OnSurfaceVariant = Color(0xFF66CC66)
    val InverseOnSurface = Color(0xFF0A1F0A)

    val Primary = Color(0xFF00FF41)
    val PrimaryContainer = Color(0xFF003D0A)
    val OnPrimary = Color(0xFF001A00)
    val OnPrimaryContainer = Color(0xFFB8F5B8)

    val Secondary = Color(0xFF00CCFF)
    val SecondaryContainer = Color(0xFF003D4D)
    val OnSecondary = Color(0xFF001A1F)
    val OnSecondaryContainer = Color(0xFFB8F0FF)

    val Tertiary = Color(0xFFFFD700)
    val TertiaryContainer = Color(0xFF3D3200)
    val OnTertiary = Color(0xFF1A1500)
    val OnTertiaryContainer = Color(0xFFFFF0B8)

    val Error = Color(0xFFFF3333)
    val ErrorContainer = Color(0xFF3D0A0A)
    val OnError = Color(0xFFFFFFFF)
    val OnErrorContainer = Color(0xFFFFCCCC)

    val Shadow = Color(0x4D00FF41)
    val Scrim = Color(0xBF000000)

    val PluginSpeedMeter = Color(0xFF00FFFF)
    val PluginSpeedMeterContainer = Color(0xFF003D3D)
    val PluginKeepAwake = Color(0xFFFFCC00)
    val PluginKeepAwakeContainer = Color(0xFF3D2E00)
}

// ============================================================================
// TEMA SEÇİCİ
// ============================================================================
enum class AtolyeThemeType {
    KAGIT,           // Varsayılan - Kâğıt
    GECE_ATOLYESI,   // Gece Atölyesi
    BAKIR,           // Bakır
    DEVRE_KARTI,     // Devre Kartı
    DYNAMIC          // Android 12+ Dynamic Color
}

@Composable
fun rememberAtolyeColors(themeType: AtolyeThemeType = AtolyeThemeType.KAGIT, dynamicColors: android.content.res.Resources.Theme? = null): AtolyeColorScheme {
    return when (themeType) {
        AtolyeThemeType.KAGIT -> KagitColorScheme()
        AtolyeThemeType.GECE_ATOLYESI -> GeceAtolyesiColorScheme()
        AtolyeThemeType.BAKIR -> BakirColorScheme()
        AtolyeThemeType.DEVRE_KARTI -> DevreKartiColorScheme()
        AtolyeThemeType.DYNAMIC -> DynamicColorScheme(dynamicColors)
    }
}

// ============================================================================
// RENK ŞEMASI ARAYÜZÜ
// ============================================================================
interface AtolyeColorScheme {
    val background: Color
    val surface: Color
    val surfaceContainer: Color
    val surfaceVariant: Color
    val outline: Color
    val outlineVariant: Color
    val onBackground: Color
    val onSurface: Color
    val onSurfaceVariant: Color
    val inverseOnSurface: Color
    val primary: Color
    val primaryContainer: Color
    val onPrimary: Color
    val onPrimaryContainer: Color
    val secondary: Color
    val secondaryContainer: Color
    val onSecondary: Color
    val onSecondaryContainer: Color
    val tertiary: Color
    val tertiaryContainer: Color
    val onTertiary: Color
    val onTertiaryContainer: Color
    val error: Color
    val errorContainer: Color
    val onError: Color
    val onErrorContainer: Color
    val pluginSpeedMeter: Color
    val pluginSpeedMeterContainer: Color
    val pluginKeepAwake: Color
    val pluginKeepAwakeContainer: Color
}

class KagitColorScheme : AtolyeColorScheme {
    override val background = KagitColors.Background
    override val surface = KagitColors.Surface
    override val surfaceContainer = KagitColors.SurfaceContainer
    override val surfaceVariant = KagitColors.SurfaceVariant
    override val outline = KagitColors.Outline
    override val outlineVariant = KagitColors.OutlineVariant
    override val onBackground = KagitColors.OnBackground
    override val onSurface = KagitColors.OnSurface
    override val onSurfaceVariant = KagitColors.OnSurfaceVariant
    override val inverseOnSurface = KagitColors.InverseOnSurface
    override val primary = KagitColors.Primary
    override val primaryContainer = KagitColors.PrimaryContainer
    override val onPrimary = KagitColors.OnPrimary
    override val onPrimaryContainer = KagitColors.OnPrimaryContainer
    override val secondary = KagitColors.Secondary
    override val secondaryContainer = KagitColors.SecondaryContainer
    override val onSecondary = KagitColors.OnSecondary
    override val onSecondaryContainer = KagitColors.OnSecondaryContainer
    override val tertiary = KagitColors.Tertiary
    override val tertiaryContainer = KagitColors.TertiaryContainer
    override val onTertiary = KagitColors.OnTertiary
    override val onTertiaryContainer = KagitColors.OnTertiaryContainer
    override val error = KagitColors.Error
    override val errorContainer = KagitColors.ErrorContainer
    override val onError = KagitColors.OnError
    override val onErrorContainer = KagitColors.OnErrorContainer
    override val pluginSpeedMeter = KagitColors.PluginSpeedMeter
    override val pluginSpeedMeterContainer = KagitColors.PluginSpeedMeterContainer
    override val pluginKeepAwake = KagitColors.PluginKeepAwake
    override val pluginKeepAwakeContainer = KagitColors.PluginKeepAwakeContainer
}

class GeceAtolyesiColorScheme : AtolyeColorScheme {
    override val background = GeceAtolyesiColors.Background
    override val surface = GeceAtolyesiColors.Surface
    override val surfaceContainer = GeceAtolyesiColors.SurfaceContainer
    override val surfaceVariant = GeceAtolyesiColors.SurfaceVariant
    override val outline = GeceAtolyesiColors.Outline
    override val outlineVariant = GeceAtolyesiColors.OutlineVariant
    override val onBackground = GeceAtolyesiColors.OnBackground
    override val onSurface = GeceAtolyesiColors.OnSurface
    override val onSurfaceVariant = GeceAtolyesiColors.OnSurfaceVariant
    override val inverseOnSurface = GeceAtolyesiColors.InverseOnSurface
    override val primary = GeceAtolyesiColors.Primary
    override val primaryContainer = GeceAtolyesiColors.PrimaryContainer
    override val onPrimary = GeceAtolyesiColors.OnPrimary
    override val onPrimaryContainer = GeceAtolyesiColors.OnPrimaryContainer
    override val secondary = GeceAtolyesiColors.Secondary
    override val secondaryContainer = GeceAtolyesiColors.SecondaryContainer
    override val onSecondary = GeceAtolyesiColors.OnSecondary
    override val onSecondaryContainer = GeceAtolyesiColors.OnSecondaryContainer
    override val tertiary = GeceAtolyesiColors.Tertiary
    override val tertiaryContainer = GeceAtolyesiColors.TertiaryContainer
    override val onTertiary = GeceAtolyesiColors.OnTertiary
    override val onTertiaryContainer = GeceAtolyesiColors.OnTertiaryContainer
    override val error = GeceAtolyesiColors.Error
    override val errorContainer = GeceAtolyesiColors.ErrorContainer
    override val onError = GeceAtolyesiColors.OnError
    override val onErrorContainer = GeceAtolyesiColors.OnErrorContainer
    override val pluginSpeedMeter = GeceAtolyesiColors.PluginSpeedMeter
    override val pluginSpeedMeterContainer = GeceAtolyesiColors.PluginSpeedMeterContainer
    override val pluginKeepAwake = GeceAtolyesiColors.PluginKeepAwake
    override val pluginKeepAwakeContainer = GeceAtolyesiColors.PluginKeepAwakeContainer
}

class BakirColorScheme : AtolyeColorScheme {
    override val background = BakirColors.Background
    override val surface = BakirColors.Surface
    override val surfaceContainer = BakirColors.SurfaceContainer
    override val surfaceVariant = BakirColors.SurfaceVariant
    override val outline = BakirColors.Outline
    override val outlineVariant = BakirColors.OutlineVariant
    override val onBackground = BakirColors.OnBackground
    override val onSurface = BakirColors.OnSurface
    override val onSurfaceVariant = BakirColors.OnSurfaceVariant
    override val inverseOnSurface = BakirColors.InverseOnSurface
    override val primary = BakirColors.Primary
    override val primaryContainer = BakirColors.PrimaryContainer
    override val onPrimary = BakirColors.OnPrimary
    override val onPrimaryContainer = BakirColors.OnPrimaryContainer
    override val secondary = BakirColors.Secondary
    override val secondaryContainer = BakirColors.SecondaryContainer
    override val onSecondary = BakirColors.OnSecondary
    override val onSecondaryContainer = BakirColors.OnSecondaryContainer
    override val tertiary = BakirColors.Tertiary
    override val tertiaryContainer = BakirColors.TertiaryContainer
    override val onTertiary = BakirColors.OnTertiary
    override val onTertiaryContainer = BakirColors.OnTertiaryContainer
    override val error = BakirColors.Error
    override val errorContainer = BakirColors.ErrorContainer
    override val onError = BakirColors.OnError
    override val onErrorContainer = BakirColors.OnErrorContainer
    override val pluginSpeedMeter = BakirColors.PluginSpeedMeter
    override val pluginSpeedMeterContainer = BakirColors.PluginSpeedMeterContainer
    override val pluginKeepAwake = BakirColors.PluginKeepAwake
    override val pluginKeepAwakeContainer = BakirColors.PluginKeepAwakeContainer
}

class DevreKartiColorScheme : AtolyeColorScheme {
    override val background = DevreKartiColors.Background
    override val surface = DevreKartiColors.Surface
    override val surfaceContainer = DevreKartiColors.SurfaceContainer
    override val surfaceVariant = DevreKartiColors.SurfaceVariant
    override val outline = DevreKartiColors.Outline
    override val outlineVariant = DevreKartiColors.OutlineVariant
    override val onBackground = DevreKartiColors.OnBackground
    override val onSurface = DevreKartiColors.OnSurface
    override val onSurfaceVariant = DevreKartiColors.OnSurfaceVariant
    override val inverseOnSurface = DevreKartiColors.InverseOnSurface
    override val primary = DevreKartiColors.Primary
    override val primaryContainer = DevreKartiColors.PrimaryContainer
    override val onPrimary = DevreKartiColors.OnPrimary
    override val onPrimaryContainer = DevreKartiColors.OnPrimaryContainer
    override val secondary = DevreKartiColors.Secondary
    override val secondaryContainer = DevreKartiColors.SecondaryContainer
    override val onSecondary = DevreKartiColors.OnSecondary
    override val onSecondaryContainer = DevreKartiColors.OnSecondaryContainer
    override val tertiary = DevreKartiColors.Tertiary
    override val tertiaryContainer = DevreKartiColors.TertiaryContainer
    override val onTertiary = DevreKartiColors.OnTertiary
    override val onTertiaryContainer = DevreKartiColors.OnTertiaryContainer
    override val error = DevreKartiColors.Error
    override val errorContainer = DevreKartiColors.ErrorContainer
    override val onError = DevreKartiColors.OnError
    override val onErrorContainer = DevreKartiColors.OnErrorContainer
    override val pluginSpeedMeter = DevreKartiColors.PluginSpeedMeter
    override val pluginSpeedMeterContainer = DevreKartiColors.PluginSpeedMeterContainer
    override val pluginKeepAwake = DevreKartiColors.PluginKeepAwake
    override val pluginKeepAwakeContainer = DevreKartiColors.PluginKeepAwakeContainer
}

class DynamicColorScheme(
    private val theme: android.content.res.Resources.Theme?
) : AtolyeColorScheme {
    // Dynamic color implementation would use Material3's dynamic color APIs
    // For now, fallback to Kagit
    private val fallback = KagitColorScheme()

    override val background: Color = fallback.background
    override val surface: Color = fallback.surface
    override val surfaceContainer: Color = fallback.surfaceContainer
    override val surfaceVariant: Color = fallback.surfaceVariant
    override val outline: Color = fallback.outline
    override val outlineVariant: Color = fallback.outlineVariant
    override val onBackground: Color = fallback.onBackground
    override val onSurface: Color = fallback.onSurface
    override val onSurfaceVariant: Color = fallback.onSurfaceVariant
    override val inverseOnSurface: Color = fallback.inverseOnSurface
    override val primary: Color = fallback.primary
    override val primaryContainer: Color = fallback.primaryContainer
    override val onPrimary: Color = fallback.onPrimary
    override val onPrimaryContainer: Color = fallback.onPrimaryContainer
    override val secondary: Color = fallback.secondary
    override val secondaryContainer: Color = fallback.secondaryContainer
    override val onSecondary: Color = fallback.onSecondary
    override val onSecondaryContainer: Color = fallback.onSecondaryContainer
    override val tertiary: Color = fallback.tertiary
    override val tertiaryContainer: Color = fallback.tertiaryContainer
    override val onTertiary: Color = fallback.onTertiary
    override val onTertiaryContainer: Color = fallback.onTertiaryContainer
    override val error: Color = fallback.error
    override val errorContainer: Color = fallback.errorContainer
    override val onError: Color = fallback.onError
    override val onErrorContainer: Color = fallback.onErrorContainer
    override val pluginSpeedMeter: Color = fallback.pluginSpeedMeter
    override val pluginSpeedMeterContainer: Color = fallback.pluginSpeedMeterContainer
    override val pluginKeepAwake: Color = fallback.pluginKeepAwake
    override val pluginKeepAwakeContainer: Color = fallback.pluginKeepAwakeContainer
}