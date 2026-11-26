package dev.rogerbertan.weatherfish.data.xweather

import dev.rogerbertan.weatherfish.domain.CurrentWeather
import dev.rogerbertan.weatherfish.domain.DailyForecast
import dev.rogerbertan.weatherfish.domain.GeoPoint
import dev.rogerbertan.weatherfish.domain.HourlyForecast
import dev.rogerbertan.weatherfish.domain.Place
import dev.rogerbertan.weatherfish.domain.WeatherCondition
import dev.rogerbertan.weatherfish.domain.WeatherError
import java.text.Normalizer
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.Locale
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

val xweatherJson = Json { ignoreUnknownKeys = true }

fun <T> XweatherResponse<T>.orThrow(): List<T> =
    if (success) response else throw weatherErrorFor(error?.code)

fun weatherErrorFor(code: String?): WeatherError = when {
    code == null -> WeatherError.Unknown("unexpected response")
    code == "IOException" -> WeatherError.Network
    code == "invalid_client" || code == "unauthorized_namespace" -> WeatherError.Unauthorized
    code.startsWith("maxhits") -> WeatherError.QuotaExceeded
    code == "invalid_location" || code == "invalid_query" || code == "no_data" -> WeatherError.NotFound
    else -> WeatherError.Unknown(code)
}

fun xweatherErrorIn(json: String): WeatherError? =
    try {
        xweatherJson.decodeFromString<XweatherResponse<JsonElement>>(json).error?.code?.let(::weatherErrorFor)
    } catch (e: IllegalArgumentException) {
        null
    }

val GeoPoint.xweatherId: String get() = String.format(Locale.ROOT, "%.5f,%.5f", latitude, longitude)

private val combiningMarks = Regex("\\p{M}")

fun String.foldForSearch(): String =
    Normalizer.normalize(trim().lowercase(), Normalizer.Form.NFD).replace(combiningMarks, "")

fun searchTerm(query: String): String =
    query.foldForSearch().filter { it.isLetterOrDigit() || it in " -'" }

fun parseConditions(json: String): CurrentWeather =
    xweatherJson.decodeFromString<XweatherResponse<ConditionsResponse>>(json).orThrow()
        .firstOrNull()?.periods?.firstOrNull()?.toDomain()
        ?: throw WeatherError.NotFound

fun parseHourly(json: String, hours: Int): List<HourlyForecast> =
    xweatherJson.decodeFromString<XweatherResponse<ForecastResponse>>(json).orThrow()
        .flatMap { it.periods }.take(hours).map { it.toHourly() }

fun parseDaily(json: String, days: Int): List<DailyForecast> =
    xweatherJson.decodeFromString<XweatherResponse<ForecastResponse>>(json).orThrow()
        .flatMap { it.periods }.take(days).map { it.toDaily() }

fun parsePlaces(json: String): List<Place> =
    xweatherJson.decodeFromString<XweatherResponse<PlaceResponse>>(json).orThrow().map { it.toDomain() }

fun parseClosestPlace(json: String): Place =
    parsePlaces(json).firstOrNull() ?: throw WeatherError.NotFound

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