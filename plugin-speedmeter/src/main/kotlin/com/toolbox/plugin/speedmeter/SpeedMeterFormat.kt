package com.toolbox.plugin.speedmeter

import java.util.Locale

/**
 * Hız Göstergesi biçimlendirmesi.
 *
 * - durum çubuğu: sol üstte saat boyutunda `0,1 MB/s` (overlay)
 * - bildirim paneli: `Aktif hız: 0,1 MB/s` + günlük/aylık veri
 *
 * Birim daima MB/s'dir; KB/s ve otomatik mod kaldırılmıştır.
 */
object SpeedMeterFormat {

    /** `0`, `0,1`, `0,2`, `1,3` ... */
    private fun mbValue(bytesPerSecond: Long): String {
        if (bytesPerSecond <= 0L) return "0"
        val mb = bytesPerSecond / (1024.0 * 1024.0)
        // Gerçek trafik çoğu zaman 0,1 MB/s'nin altında kalır.
        // Sıfırda kilitlenmemesi için herhangi bir hareket en az 0,1 gösterilir.
        if (mb < 0.1) return "0,1"
        return String.format(Locale.getDefault(), "%.1f", mb)
    }

    /** Tek hız, panel/ayar ekranı için: `0,1 MB/s`. */
    fun speed(bytesPerSecond: Long, unitMode: Int): String =
        "${value(bytesPerSecond, unitMode)} ${unit(unitMode)}"

    /**
     * Panel başlığı: aktif internet hızı tek değer olarak.
     * İndirme + yükleme toplanır. Örn. `Aktif hız: 0,2 MB/s`.
     */
    fun activeSpeed(download: Long, upload: Long, unitMode: Int): String =
        "Aktif hız: ${value(download + upload, unitMode)} ${unit(unitMode)}"

    private fun value(bytesPerSecond: Long, unitMode: Int): String = when (unitMode) {
        1 -> "${bytesPerSecond / 1024}"
        else -> mbValue(bytesPerSecond)
    }

    private fun unit(unitMode: Int): String = if (unitMode == 1) "KB/s" else "MB/s"

    /** Veri miktarı: `1,25 MB` veya `0,82 GB`. */
    fun data(bytes: Long, unitMode: Int): String = when (unitMode) {
        1 -> "${bytes / 1024} KB"
        2 -> String.format(Locale.getDefault(), "%.2f MB", bytes / (1024.0 * 1024.0))
        else -> when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024L * 1024L -> "${bytes / 1024} KB"
            bytes < 1024L * 1024L * 1024L -> String.format(
                Locale.getDefault(),
                "%.2f MB",
                bytes / (1024.0 * 1024.0)
            )
            else -> String.format(
                Locale.getDefault(),
                "%.2f GB",
                bytes / (1024.0 * 1024.0 * 1024.0)
            )
        }
    }

    fun duration(millis: Long): String {
        val totalSeconds = (millis / 1000).coerceAtLeast(0)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }
}
