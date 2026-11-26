package dev.rogerbertan.weatherfish.data.xweather

import com.aerisweather.aeris.model.AerisDataJSON
import com.aerisweather.aeris.model.AerisResponse
import com.aerisweather.aeris.model.ConditionPeriod as AerisConditionPeriod
import com.aerisweather.aeris.model.ForecastPeriod as AerisForecastPeriod
import com.aerisweather.aeris.response.ConditionsResponse as AerisConditionsResponse
import com.aerisweather.aeris.response.ForecastsResponse as AerisForecastsResponse
import com.aerisweather.aeris.response.PlacesResponse as AerisPlacesResponse
import dev.rogerbertan.weatherfish.domain.CurrentWeather
import dev.rogerbertan.weatherfish.domain.DailyForecast
import dev.rogerbertan.weatherfish.domain.GeoPoint
import dev.rogerbertan.weatherfish.domain.HourlyForecast
import dev.rogerbertan.weatherfish.domain.Place
import dev.rogerbertan.weatherfish.domain.WeatherError
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

fun AerisResponse.orThrow(): List<AerisDataJSON> =
    if (isSuccessful) listOfResponse.orEmpty() else throw weatherErrorFor(error?.code)

fun parseAerisConditions(data: List<AerisDataJSON>): CurrentWeather =
    data.firstOrNull()?.let { AerisConditionsResponse(it).periods.firstOrNull() }?.toDomain()
        ?: throw WeatherError.NotFound

fun parseAerisHourly(data: List<AerisDataJSON>, hours: Int): List<HourlyForecast> =
    data.asSequence().flatMap { AerisForecastsResponse(it).periods }.take(hours).map { it.toHourly() }.toList()

fun parseAerisDaily(data: List<AerisDataJSON>, days: Int): List<DailyForecast> =
    data.asSequence().flatMap { AerisForecastsResponse(it).periods }.take(days).map { it.toDaily() }.toList()

fun parseAerisPlaces(data: List<AerisDataJSON>): List<Place> =
    data.map { AerisPlacesResponse(it).toDomain() }

private fun Number?.required(field: String): Number = requireNotNull(this) { "missing $field" }

private fun AerisConditionPeriod.toDomain() = CurrentWeather(
    observedAt = Instant.ofEpochSecond(timestamp.required("timestamp").toLong()),
    condition = conditionFromCoded(weatherPrimaryCoded.orEmpty()),
    description = weather.orEmpty(),
    temperatureC = tempC.required("tempC").toDouble(),
    feelsLikeC = feelslikeC.required("feelslikeC").toDouble(),
    humidityPercent = humidity.required("humidity").toInt(),
    dewPointC = dewpointC.required("dewpointC").toDouble(),
    windSpeedKph = windSpeedKPH.required("windSpeedKPH").toDouble(),
    windDirection = windDir.orEmpty(),
    pressureMb = pressureMB.required("pressureMB").toDouble(),
    visibilityKm = visibilityKM.required("visibilityKM").toDouble(),
    uvIndex = uvi.required("uvi").toInt(),
)

private fun AerisForecastPeriod.toHourly() = HourlyForecast(
    time = Instant.ofEpochSecond(timestamp.required("timestamp").toLong()),
    condition = conditionFromCoded(weatherPrimaryCoded.orEmpty()),
    temperatureC = tempC.required("tempC").toDouble(),
    precipitationChancePercent = pop.required("pop").toInt(),
)

private fun AerisForecastPeriod.toDaily() = DailyForecast(
    date = OffsetDateTime.parse(requireNotNull(dateTimeISO) { "missing dateTimeISO" }).toLocalDate(),
    condition = conditionFromCoded(weatherPrimaryCoded.orEmpty()),
    description = weather.orEmpty(),
    minTemperatureC = minTempC.required("minTempC").toDouble(),
    maxTemperatureC = maxTempC.required("maxTempC").toDouble(),
    precipitationChancePercent = pop.required("pop").toInt(),
)

private fun AerisPlacesResponse.toDomain(): Place {
    val name = requireNotNull(place) { "missing place" }
    val loc = requireNotNull(location) { "missing loc" }
    return Place(
        name = titleCasePlaceName(name.name.orEmpty()),
        region = name.stateFull?.ifBlank { null },
        country = name.countryFull?.ifBlank { null } ?: name.country.orEmpty().uppercase(),
        point = GeoPoint(loc.lat, loc.lon),
        timeZone = ZoneId.of(requireNotNull(profile?.tz) { "missing profile.tz" }),
    )
}