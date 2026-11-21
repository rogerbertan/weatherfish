package dev.rogerbertan.weatherfish.data.xweather

import dev.rogerbertan.weatherfish.domain.CurrentWeather
import dev.rogerbertan.weatherfish.domain.DailyForecast
import dev.rogerbertan.weatherfish.domain.GeoPoint
import dev.rogerbertan.weatherfish.domain.HourlyForecast
import dev.rogerbertan.weatherfish.domain.Place
import dev.rogerbertan.weatherfish.domain.WeatherCondition
import dev.rogerbertan.weatherfish.domain.WeatherError
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import kotlinx.serialization.json.Json

val xweatherJson = Json { ignoreUnknownKeys = true }

fun <T> XweatherResponse<T>.orThrow(): List<T> =
    if (success) response else throw error?.toDomain() ?: WeatherError.Unknown("unexpected response")

private fun XweatherError.toDomain(): WeatherError = when (code) {
    "invalid_client", "unauthorized_namespace", "maxhits_daily" -> WeatherError.Unauthorized
    "invalid_location", "invalid_query", "no_data" -> WeatherError.NotFound
    else -> WeatherError.Unknown(description.ifBlank { code })
}

fun PlaceResponse.toDomain() = Place(
    name = titleCasePlaceName(place.name),
    region = place.stateFull.ifBlank { null },
    country = place.countryFull.ifBlank { place.country.uppercase() },
    point = GeoPoint(loc.lat, loc.long),
    timeZone = ZoneId.of(profile.tz),
)

private val lowercaseParticles = setOf("de", "da", "do", "das", "dos", "e")

fun titleCasePlaceName(name: String): String =
    name.split(' ').mapIndexed { index, word ->
        if (index > 0 && word in lowercaseParticles) word else word.replaceFirstChar { it.uppercaseChar() }
    }.joinToString(" ")

fun ConditionsPeriod.toDomain() = CurrentWeather(
    observedAt = Instant.ofEpochSecond(timestamp),
    condition = conditionFromCoded(weatherPrimaryCoded),
    description = weather,
    temperatureC = tempC,
    feelsLikeC = feelslikeC,
    humidityPercent = humidity,
    dewPointC = dewpointC,
    windSpeedKph = windSpeedKPH,
    windDirection = windDir,
    pressureMb = pressureMB,
    visibilityKm = visibilityKM,
    uvIndex = uvi,
)

fun ForecastPeriod.toHourly() = HourlyForecast(
    time = Instant.ofEpochSecond(timestamp),
    condition = conditionFromCoded(weatherPrimaryCoded),
    temperatureC = requireNotNull(tempC) { "hourly period without tempC" },
    precipitationChancePercent = pop,
)

fun ForecastPeriod.toDaily() = DailyForecast(
    date = OffsetDateTime.parse(dateTimeISO).toLocalDate(),
    condition = conditionFromCoded(weatherPrimaryCoded),
    description = weather,
    minTemperatureC = requireNotNull(minTempC) { "daily period without minTempC" },
    maxTemperatureC = requireNotNull(maxTempC) { "daily period without maxTempC" },
    precipitationChancePercent = pop,
)

private val weatherCodes = mapOf(
    "T" to WeatherCondition.Thunderstorm,
    "A" to WeatherCondition.Thunderstorm,
    "R" to WeatherCondition.Rain,
    "RW" to WeatherCondition.Rain,
    "ZR" to WeatherCondition.Rain,
    "L" to WeatherCondition.Drizzle,
    "ZL" to WeatherCondition.Drizzle,
    "S" to WeatherCondition.Snow,
    "SW" to WeatherCondition.Snow,
    "RS" to WeatherCondition.Snow,
    "SI" to WeatherCondition.Snow,
    "WM" to WeatherCondition.Snow,
    "IP" to WeatherCondition.Snow,
    "BS" to WeatherCondition.Snow,
    "F" to WeatherCondition.Fog,
    "ZF" to WeatherCondition.Fog,
    "IF" to WeatherCondition.Fog,
    "BR" to WeatherCondition.Fog,
    "H" to WeatherCondition.Fog,
    "K" to WeatherCondition.Fog,
    "BD" to WeatherCondition.Wind,
    "BN" to WeatherCondition.Wind,
    "CL" to WeatherCondition.Clear,
    "FW" to WeatherCondition.Clear,
    "SC" to WeatherCondition.PartlyCloudy,
    "BK" to WeatherCondition.Cloudy,
    "OV" to WeatherCondition.Cloudy,
)

fun conditionFromCoded(coded: String): WeatherCondition =
    weatherCodes[coded.substringAfterLast(':')] ?: WeatherCondition.Unknown