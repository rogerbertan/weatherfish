package dev.rogerbertan.weatherfish.data.openmeteo

import dev.rogerbertan.weatherfish.domain.CurrentWeather
import dev.rogerbertan.weatherfish.domain.DailyForecast
import dev.rogerbertan.weatherfish.domain.GeoPoint
import dev.rogerbertan.weatherfish.domain.HourlyForecast
import dev.rogerbertan.weatherfish.domain.Place
import dev.rogerbertan.weatherfish.domain.WeatherCondition
import dev.rogerbertan.weatherfish.domain.WeatherError
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt
import kotlinx.serialization.json.Json

val openMeteoJson = Json { ignoreUnknownKeys = true }

fun parseOpenMeteoCurrent(json: String): CurrentWeather =
    openMeteoJson.decodeFromString<OpenMeteoForecastResponse>(json).current?.toDomain()
        ?: throw WeatherError.NotFound

fun parseOpenMeteoHourly(json: String): List<HourlyForecast> =
    openMeteoJson.decodeFromString<OpenMeteoForecastResponse>(json).hourly?.toDomain().orEmpty()

fun parseOpenMeteoDaily(json: String): List<DailyForecast> =
    openMeteoJson.decodeFromString<OpenMeteoForecastResponse>(json).daily?.toDomain().orEmpty()

fun parseOpenMeteoPlaces(json: String): List<Place> =
    openMeteoJson.decodeFromString<GeocodingResponse>(json).results.map { it.toDomain() }

fun parseOpenMeteoTimeZone(json: String): ZoneId =
    ZoneId.of(openMeteoJson.decodeFromString<OpenMeteoForecastResponse>(json).timezone)

fun Current.toDomain(): CurrentWeather {
    val wmo = wmoCode(weatherCode)
    return CurrentWeather(
        observedAt = Instant.ofEpochSecond(time),
        condition = wmo.condition,
        description = wmo.description,
        temperatureC = temperature,
        feelsLikeC = apparentTemperature,
        humidityPercent = humidity,
        dewPointC = dewPoint,
        windSpeedKph = windSpeed,
        windDirection = compassDirection(windDirection),
        pressureMb = pressure,
        visibilityKm = visibility / 1000,
        uvIndex = uvIndex.roundToInt(),
    )
}

fun Hourly.toDomain(): List<HourlyForecast> = time.indices.map { i ->
    HourlyForecast(
        time = Instant.ofEpochSecond(time[i]),
        condition = wmoCode(weatherCode[i]).condition,
        temperatureC = temperature[i],
        precipitationChancePercent = precipitationProbability[i] ?: 0,
    )
}

fun Daily.toDomain(): List<DailyForecast> = time.indices.map { i ->
    val wmo = wmoCode(weatherCode[i])
    DailyForecast(
        date = LocalDate.parse(time[i]),
        condition = wmo.condition,
        description = wmo.description,
        minTemperatureC = minTemperature[i],
        maxTemperatureC = maxTemperature[i],
        precipitationChancePercent = precipitationProbability[i] ?: 0,
    )
}

fun GeocodingResult.toDomain() = Place(
    name = name,
    region = admin1?.ifBlank { null },
    country = country,
    point = GeoPoint(latitude, longitude),
    timeZone = ZoneId.of(timezone),
)

private val compassPoints = listOf(
    "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
    "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW",
)

fun compassDirection(degrees: Double): String =
    compassPoints[(degrees / 22.5).roundToInt().mod(compassPoints.size)]

data class WmoCode(val condition: WeatherCondition, val description: String)

fun wmoCode(code: Int): WmoCode = when (code) {
    0 -> WmoCode(WeatherCondition.Clear, "Céu limpo")
    1 -> WmoCode(WeatherCondition.Clear, "Predominantemente limpo")
    2 -> WmoCode(WeatherCondition.PartlyCloudy, "Parcialmente nublado")
    3 -> WmoCode(WeatherCondition.Cloudy, "Nublado")
    45, 48 -> WmoCode(WeatherCondition.Fog, "Nevoeiro")
    51, 53, 55 -> WmoCode(WeatherCondition.Drizzle, "Garoa")
    56, 57 -> WmoCode(WeatherCondition.Drizzle, "Garoa congelante")
    61, 63, 65 -> WmoCode(WeatherCondition.Rain, "Chuva")
    66, 67 -> WmoCode(WeatherCondition.Rain, "Chuva congelante")
    71, 73, 75, 77 -> WmoCode(WeatherCondition.Snow, "Neve")
    80, 81, 82 -> WmoCode(WeatherCondition.Rain, "Pancadas de chuva")
    85, 86 -> WmoCode(WeatherCondition.Snow, "Pancadas de neve")
    95 -> WmoCode(WeatherCondition.Thunderstorm, "Trovoada")
    96, 99 -> WmoCode(WeatherCondition.Thunderstorm, "Trovoada com granizo")
    else -> WmoCode(WeatherCondition.Unknown, "Condição desconhecida")
}
