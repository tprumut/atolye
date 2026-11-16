package com.toolbox.plugin.speedmeter

import android.content.Context
import java.util.Calendar

/**
 * Günlük/aylık veri kullanımı sayaçları.
 *
 * Değerler özel bir SharedPreferences dosyasında tutulur. Gün veya ay
 * değiştiğinde ilgili sayaçlar otomatik olarak sıfırlanır.
 */
data class SpeedMeterUsage(
    val dailyRx: Long = 0L,
    val dailyTx: Long = 0L,
    val monthlyRx: Long = 0L,
    val monthlyTx: Long = 0L
) {
    val dailyTotal: Long get() = dailyRx + dailyTx
    val monthlyTotal: Long get() = monthlyRx + monthlyTx
}

object SpeedMeterStats {

    private const val PREFS = "speedmeter_stats"
    private const val KEY_DAILY_RX = "daily_rx"
    private const val KEY_DAILY_TX = "daily_tx"
    private const val KEY_MONTHLY_RX = "monthly_rx"
    private const val KEY_MONTHLY_TX = "monthly_tx"
    private const val KEY_DAY = "last_day"
    private const val KEY_MONTH = "last_month"
    private const val KEY_LAST_RX = "last_rx"
    private const val KEY_LAST_TX = "last_tx"

    fun read(context: Context): SpeedMeterUsage {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val today = dayStamp()
        val month = monthStamp()

        var dailyRx = prefs.getLong(KEY_DAILY_RX, 0L)
        var dailyTx = prefs.getLong(KEY_DAILY_TX, 0L)
        var monthlyRx = prefs.getLong(KEY_MONTHLY_RX, 0L)
        var monthlyTx = prefs.getLong(KEY_MONTHLY_TX, 0L)

        val editor = prefs.edit()
        var changed = false

        if (prefs.getInt(KEY_DAY, 0) != today) {
            dailyRx = 0L
            dailyTx = 0L
            editor.putInt(KEY_DAY, today)
            changed = true
        }
        if (prefs.getInt(KEY_MONTH, 0) != month) {
            monthlyRx = 0L
            monthlyTx = 0L
            editor.putInt(KEY_MONTH, month)
            changed = true
        }
        if (changed) editor.apply()

        return SpeedMeterUsage(dailyRx, dailyTx, monthlyRx, monthlyTx)
    }

    fun write(context: Context, usage: SpeedMeterUsage) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_DAILY_RX, usage.dailyRx)
            .putLong(KEY_DAILY_TX, usage.dailyTx)
            .putLong(KEY_MONTHLY_RX, usage.monthlyRx)
            .putLong(KEY_MONTHLY_TX, usage.monthlyTx)
            .putInt(KEY_DAY, dayStamp())
            .putInt(KEY_MONTH, monthStamp())
            .apply()
    }

    fun reset(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_DAILY_RX, 0L)
            .putLong(KEY_DAILY_TX, 0L)
            .putLong(KEY_MONTHLY_RX, 0L)
            .putLong(KEY_MONTHLY_TX, 0L)
            .putInt(KEY_DAY, dayStamp())
            .putInt(KEY_MONTH, monthStamp())
            .apply()
    }

    fun readCounters(context: Context): Pair<Long, Long> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LAST_RX, 0L) to prefs.getLong(KEY_LAST_TX, 0L)
    }

    fun writeCounters(context: Context, rx: Long, tx: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_RX, rx)
            .putLong(KEY_LAST_TX, tx)
            .apply()
    }

    private fun dayStamp(): Int {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.YEAR) * 1000 +
            calendar.get(Calendar.DAY_OF_YEAR)
    }

    private fun monthStamp(): Int {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.YEAR) * 100 + calendar.get(Calendar.MONTH)
    }
}
