package com.atolye.design

import android.content.Context

/**
 * Ana Atölye uygulamasının seçtiği temanın tek kaynağı.
 *
 * Hem host hem de tek APK içindeki eklentiler aynı `host_settings`
 * dosyasını okur; böylece eklenti sayfaları ana uygulamayla aynı temada açılır.
 */
object AtolyeThemeStore {
    const val PREFERENCES_NAME = "host_settings"
    const val KEY_THEME = "theme"

    fun read(context: Context): AtolyeThemeType {
        val stored = context.applicationContext
            .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getString(KEY_THEME, AtolyeThemeType.KAGIT.name)
        return runCatching { AtolyeThemeType.valueOf(stored ?: AtolyeThemeType.KAGIT.name) }
            .getOrDefault(AtolyeThemeType.KAGIT)
    }

    fun save(context: Context, themeType: AtolyeThemeType) {
        context.applicationContext
            .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME, themeType.name)
            .apply()
    }
}
