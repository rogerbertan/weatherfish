package dev.rogerbertan.weatherfish.ui

import dev.rogerbertan.weatherfish.domain.CurrentWeather
import dev.rogerbertan.weatherfish.domain.GeoPoint
import dev.rogerbertan.weatherfish.domain.HourlyForecast
import dev.rogerbertan.weatherfish.domain.Place
import dev.rogerbertan.weatherfish.domain.WeatherCondition
import java.time.Instant
import java.time.ZoneId

val testPlace = Place("Porto Alegre", "Rio Grande do Sul", "Brasil", GeoPoint(-30.0, -51.2), ZoneId.of("America/Sao_Paulo"))

val testCurrent = CurrentWeather(
    observedAt = Instant.EPOCH,
    condition = WeatherCondition.Clear,
    description = "Céu limpo",
    temperatureC = 21.0,
    feelsLikeC = 22.0,
    humidityPercent = 50,
    dewPointC = 10.0,
    windSpeedKph = 12.0,
    windDirection = "S",
    pressureMb = 1015.0,
    visibilityKm = 10.0,
    uvIndex = 5,
)

val testHourly = listOf(HourlyForecast(Instant.EPOCH, WeatherCondition.Clear, 21.0, 0))