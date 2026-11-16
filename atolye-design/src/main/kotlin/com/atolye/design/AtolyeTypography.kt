package com.atolye.design

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Typography

/**
 * Atölye Tasarım Sistemi - Tipografi
 *
 * Genel metin: Inter Variable Font
 * Sayısal veriler: JetBrains Mono Variable Font
 *
 * Font dosyaları res/font/ altında olmalı:
 * - Inter[opsz,wght].ttf (Variable font)
 * - JetBrainsMono[opsz,wght].ttf (Variable font)
 */
object AtolyeTypography {

    // FontFamily tanımları (variable font desteği için)
    val InterFontFamily = FontFamily(
        Font(R.font.inter_variable, FontWeight.Normal),
        Font(R.font.inter_variable, FontWeight.Medium),
        Font(R.font.inter_variable, FontWeight.SemiBold),
        Font(R.font.inter_variable, FontWeight.Bold)
    )

    val JetBrainsMonoFontFamily = FontFamily(
        Font(R.font.jetbrains_mono_variable, FontWeight.Normal),
        Font(R.font.jetbrains_mono_variable, FontWeight.Medium),
        Font(R.font.jetbrains_mono_variable, FontWeight.SemiBold),
        Font(R.font.jetbrains_mono_variable, FontWeight.Bold)
    )

    // Monospace sayı stili için helper
    private val monoNumbers = TextStyle(
        fontFamily = JetBrainsMonoFontFamily,
        fontFeatureSettings = "tnum=1, cv02=1" // Tabular numbers, slashed zero
    )

    /**
     * Atölye Material3 Typography
     * 16dp köşe yuvarlaklığı ile uyumlu boyutlar
     */
    val AtolyeTypography = Typography(
        // Display - Büyük başlıklar
        displayLarge = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 57.sp,
            lineHeight = 64.sp,
            letterSpacing = -0.25.sp
        ),
        displayMedium = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 45.sp,
            lineHeight = 52.sp,
            letterSpacing = 0.sp
        ),
        displaySmall = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 36.sp,
            lineHeight = 44.sp,
            letterSpacing = 0.sp
        ),

        // Headline - Ekran başlıkları
        headlineLarge = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            letterSpacing = 0.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = 0.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            letterSpacing = 0.sp
        ),

        // Title - Kart ve bölüm başlıkları
        titleLarge = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp
        ),
        titleSmall = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),

        // Body - Genel metin
        bodyLarge = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp
        ),
        bodySmall = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp
        ),

        // Label - Buton, chip, menü etiketleri
        labelLarge = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        labelMedium = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        ),
        labelSmall = TextStyle(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        )
    )

    /**
     * Sayı/Veri görüntüleme için özel stiller (JetBrains Mono)
     */
    object DataStyles {
        // Hız gösterimi, sayaçlar - Büyük
        val DisplayNumber = TextStyle(
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 48.sp,
            lineHeight = 56.sp,
            letterSpacing = 0.sp,
            fontFeatureSettings = "tnum=1, cv02=1"
        )

        // Kartlardaki metriks - Orta
        val MetricNumber = TextStyle(
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            letterSpacing = 0.sp,
            fontFeatureSettings = "tnum=1, cv02=1"
        )

        // İnce detay sayıları - Küçük
        val DetailNumber = TextStyle(
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.sp,
            fontFeatureSettings = "tnum=1, cv02=1"
        )

        // Monospace body (kod, log, vs.)
        val MonoBody = TextStyle(
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.sp,
            fontFeatureSettings = "tnum=1, cv02=1"
        )
    }

    /**
     * TextStyle'a mono sayı özelliği ekleyen extension
     */
    fun TextStyle.withMonoNumbers(): TextStyle = this.merge(monoNumbers)
}