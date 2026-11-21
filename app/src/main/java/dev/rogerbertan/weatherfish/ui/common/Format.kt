package dev.rogerbertan.weatherfish.ui.common

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

private val hourFormatter = DateTimeFormatter.ofPattern("HH'h'")

fun formatTemperature(celsius: Double): String = "${celsius.roundToInt()}°"

fun formatHour(time: Instant, zone: ZoneId): String = hourFormatter.format(time.atZone(zone))

fun formatWeekday(date: LocalDate, locale: Locale = Locale.getDefault()): String =
    date.dayOfWeek.getDisplayName(TextStyle.FULL, locale).replaceFirstChar { it.titlecase(locale) }

fun formatDayMonth(date: LocalDate): String = "%02d/%02d".format(date.dayOfMonth, date.monthValue)