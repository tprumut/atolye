package com.atolye.design

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Atölye Ana Tema Bileşeni
 *
 * Kullanım:
 * AtolyeTheme {
 *     // Uygulama içeriği
 * }
 *
 * Tema değiştirme:
 * val themeType = rememberAtolyeThemeType()
 * AtolyeTheme(themeType = themeType) { ... }
 */

@Composable
fun AtolyeTheme(
    themeType: AtolyeThemeType = AtolyeThemeType.KAGIT,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    // Dynamic color desteği (Android 12+)
    val dynamicColorScheme = rememberDynamicColorScheme(configuration)

    // Seçili tema için renk şeması
    val colorScheme = rememberAtolyeColorScheme(themeType, dynamicColorScheme)

    // Material3 tema
    MaterialTheme(
        colorScheme = colorScheme.toMaterial3ColorScheme(),
        typography = AtolyeTypography.AtolyeTypography,
        shapes = AtolyeShapes.AtolyeShapes,
        content = content
    )
}

/**
 * AtolyeColorScheme -> Material3 ColorScheme dönüşümü
 */
private fun AtolyeColorScheme.toMaterial3ColorScheme(): androidx.compose.material3.ColorScheme {
    val isDark = this is GeceAtolyesiColorScheme || this is BakirColorScheme || this is DevreKartiColorScheme

    return if (isDark) {
        darkColorScheme(
            primary = primary,
            primaryContainer = primaryContainer,
            onPrimary = onPrimary,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            secondaryContainer = secondaryContainer,
            onSecondary = onSecondary,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            tertiaryContainer = tertiaryContainer,
            onTertiary = onTertiary,
            onTertiaryContainer = onTertiaryContainer,
            error = error,
            errorContainer = errorContainer,
            onError = onError,
            onErrorContainer = onErrorContainer,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            outline = outline,
            outlineVariant = outlineVariant,
            inverseSurface = inverseOnSurface,
            inverseOnSurface = onBackground,
            inversePrimary = primaryContainer
        )
    } else {
        lightColorScheme(
            primary = primary,
            primaryContainer = primaryContainer,
            onPrimary = onPrimary,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            secondaryContainer = secondaryContainer,
            onSecondary = onSecondary,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            tertiaryContainer = tertiaryContainer,
            onTertiary = onTertiary,
            onTertiaryContainer = onTertiaryContainer,
            error = error,
            errorContainer = errorContainer,
            onError = onError,
            onErrorContainer = onErrorContainer,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            outline = outline,
            outlineVariant = outlineVariant,
            inverseSurface = inverseOnSurface,
            inverseOnSurface = onBackground,
            inversePrimary = primaryContainer
        )
    }
}

/**
 * Dynamic Color Scheme (Android 12+)
 */
@Composable
fun rememberDynamicColorScheme(configuration: Configuration): androidx.compose.material3.ColorScheme? {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        val context = LocalContext.current
        // Material3 dynamic color APIs
        // return MaterialTheme.colorScheme if using dynamic
        return null // Placeholder - actual implementation uses Material3 dynamic APIs
    }
    return null
}

/**
 * Tema tipi ve renk şeması state yönetimi
 */
@Composable
fun rememberAtolyeThemeType(): androidx.compose.runtime.MutableState<AtolyeThemeType> {
    return remember { mutableStateOf(AtolyeThemeType.KAGIT) }
}

@Composable
fun rememberAtolyeColorScheme(
    themeType: AtolyeThemeType,
    dynamicColors: androidx.compose.material3.ColorScheme?
): AtolyeColorScheme {
    return remember(themeType) {
        when (themeType) {
            AtolyeThemeType.KAGIT -> KagitColorScheme()
            AtolyeThemeType.GECE_ATOLYESI -> GeceAtolyesiColorScheme()
            AtolyeThemeType.BAKIR -> BakirColorScheme()
            AtolyeThemeType.DEVRE_KARTI -> DevreKartiColorScheme()
            AtolyeThemeType.DYNAMIC -> DynamicColorScheme(null)
        }
    }
}

/**
 * Tema seçici dialog/menü için yardımcı
 */
@Composable
fun AtolyeThemeSelector(
    currentTheme: AtolyeThemeType,
    onThemeChange: (AtolyeThemeType) -> Unit,
    onDismiss: () -> Unit = {}
) {
    val themes = listOf(
        AtolyeThemeType.KAGIT to "Kâğıt",
        AtolyeThemeType.GECE_ATOLYESI to "Gece Atölyesi",
        AtolyeThemeType.BAKIR to "Bakır",
        AtolyeThemeType.DEVRE_KARTI to "Devre Kartı",
        AtolyeThemeType.DYNAMIC to "Dinamik (Sistem)"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tema Seç") },
        text = {
            Column(
                modifier = androidx.compose.ui.Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                themes.forEach { (type, name) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = name)
                        RadioButton(
                            selected = currentTheme == type,
                            onClick = {
                                onThemeChange(type)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Tamam") } }
    )
}