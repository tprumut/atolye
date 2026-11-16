package com.toolbox.plugin.speedmeter

import java.util.Locale

/**
 * Hız ve veri boyutu biçimlendirme yardımcıları.
 *
 * Hem arayüz hem de servis tarafından kullanılır; bu yüzden tek bir
 * yerde tutulur ve yinelenen üst düzey fonksiyon adları oluşmaz.
 */
object SpeedMeterFormat {

    /** 0 = otomatik, 1 = KB/s, 2 = MB/s */
    fun speed(bytesPerSecond: Long, unitMode: Int): String = when (unitMode) {
        1 -> "${bytesPerSecond / 1024} KB/s"
        2 -> String.format(Locale.getDefault(), "%.2f MB/s", bytesPerSecond / (1024.0 * 1024.0))
        else -> when {
            bytesPerSecond < 1024 -> "$bytesPerSecond B/s"
            bytesPerSecond < 1024 * 1024 -> "${bytesPerSecond / 1024} KB/s"
            else -> String.format(
                Locale.getDefault(),
                "%.2f MB/s",
                bytesPerSecond / (1024.0 * 1024.0)
            )
        }
    }

    /** Bildirimde kullanılan kısa biçim. */
    fun speedShort(bytesPerSecond: Long, unitMode: Int): String = when (unitMode) {
        1 -> "${bytesPerSecond / 1024}K"
        2 -> String.format(Locale.getDefault(), "%.1fM", bytesPerSecond / (1024.0 * 1024.0))
        else -> when {
            bytesPerSecond < 1024 -> "$bytesPerSecond"
            bytesPerSecond < 1024 * 1024 -> "${bytesPerSecond / 1024}K"
            else -> String.format(
                Locale.getDefault(),
                "%.1fM",
                bytesPerSecond / (1024.0 * 1024.0)
            )
        }
    }

    /** 0 = otomatik, 1 = KB, 2 = MB */
    fun data(bytes: Long, unitMode: Int): String = when (unitMode) {
        1 -> "${bytes / 1024} KB"
        2 -> String.format(Locale.getDefault(), "%.2f MB", bytes / (1024.0 * 1024.0))
        else -> when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024L * 1024 * 1024 -> String.format(
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
