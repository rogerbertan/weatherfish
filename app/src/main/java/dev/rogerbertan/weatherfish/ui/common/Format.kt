package dev.rogerbertan.weatherfish.ui.common

import dev.rogerbertan.weatherfish.domain.TemperatureUnit
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

private val hourFormatter = DateTimeFormatter.ofPattern("HH'h'")

fun formatTemperature(celsius: Double, unit: TemperatureUnit): String {
    val value = when (unit) {
        TemperatureUnit.Celsius -> celsius
        TemperatureUnit.Fahrenheit -> celsius * 9 / 5 + 32
    }
    return "${value.roundToInt()}°"
}

fun formatHour(time: Instant, zone: ZoneId): String = hourFormatter.format(time.atZone(zone))

fun formatWeekday(date: LocalDate, locale: Locale = Locale.getDefault()): String =
    date.dayOfWeek.getDisplayName(TextStyle.FULL, locale).replaceFirstChar { it.titlecase(locale) }

fun formatDayMonth(date: LocalDate): String = "%02d/%02d".format(date.dayOfMonth, date.monthValue)