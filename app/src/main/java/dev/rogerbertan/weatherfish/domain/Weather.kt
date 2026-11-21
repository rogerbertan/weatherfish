package dev.rogerbertan.weatherfish.domain

import java.time.Instant
import java.time.LocalDate

enum class WeatherCondition {
    Clear,
    PartlyCloudy,
    Cloudy,
    Fog,
    Drizzle,
    Rain,
    Thunderstorm,
    Snow,
    Wind,
    Unknown,
}

data class CurrentWeather(
    val observedAt: Instant,
    val condition: WeatherCondition,
    val description: String,
    val temperatureC: Double,
    val feelsLikeC: Double,
    val humidityPercent: Int,
    val dewPointC: Double,
    val windSpeedKph: Double,
    val windDirection: String,
    val pressureMb: Double,
    val visibilityKm: Double,
    val uvIndex: Int,
)

data class HourlyForecast(
    val time: Instant,
    val condition: WeatherCondition,
    val temperatureC: Double,
    val precipitationChancePercent: Int,
)

data class DailyForecast(
    val date: LocalDate,
    val condition: WeatherCondition,
    val description: String,
    val minTemperatureC: Double,
    val maxTemperatureC: Double,
    val precipitationChancePercent: Int,
)