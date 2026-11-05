package uz.nucleo.ui.components

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor

/** Formatting helpers for numbers shown in the UI. */
object Format {
    private val grouped = NumberFormat.getIntegerInstance(Locale.US)

    /** 8420 → "8,420". */
    fun int(value: Number): String = grouped.format(floor(value.toDouble()).toLong())

    /** Big amounts with K/M/B suffixes: 8420 → "8,420", 1_250_000 → "1.25M". */
    fun amount(value: Double): String {
        val v = floor(value)
        return when {
            abs(v) >= 1e9 -> "%.2fB".format(v / 1e9)
            abs(v) >= 1e6 -> "%.2fM".format(v / 1e6)
            abs(v) >= 100_000 -> "%.1fK".format(v / 1e3)
            else -> int(v)
        }
    }

    /** Rates like "+1.96" or "+6". */
    fun rate(value: Double): String =
        if (value >= 10 || value == floor(value)) "+" + int(value) else "+%.2f".format(value).trimEnd('0').trimEnd('.')

    /** 125 s → "2 min", 7300 s → "2 h 1 min", 8 s → "8 s". */
    fun duration(seconds: Double): String {
        val s = seconds.toLong()
        return when {
            s < 60 -> "$s s"
            s < 3600 -> "${s / 60} min"
            else -> {
                val m = (s % 3600) / 60
                if (m == 0L) "${s / 3600} h" else "${s / 3600} h $m min"
            }
        }
    }

    /** 125 → "2:05". */
    fun clock(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)

    fun percent(fraction: Double): String = "${(fraction * 100).toInt()}%"
}
