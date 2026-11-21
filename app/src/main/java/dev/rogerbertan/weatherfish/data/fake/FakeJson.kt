package dev.rogerbertan.weatherfish.data.fake

import dev.rogerbertan.weatherfish.data.xweather.ConditionsResponse
import dev.rogerbertan.weatherfish.data.xweather.ForecastResponse
import dev.rogerbertan.weatherfish.data.xweather.PlaceResponse
import dev.rogerbertan.weatherfish.data.xweather.XweatherResponse
import dev.rogerbertan.weatherfish.data.xweather.foldForSearch
import dev.rogerbertan.weatherfish.data.xweather.orThrow
import dev.rogerbertan.weatherfish.data.xweather.toDaily
import dev.rogerbertan.weatherfish.data.xweather.toDomain
import dev.rogerbertan.weatherfish.data.xweather.toHourly
import dev.rogerbertan.weatherfish.data.xweather.xweatherJson
import dev.rogerbertan.weatherfish.domain.CurrentWeather
import dev.rogerbertan.weatherfish.domain.DailyForecast
import dev.rogerbertan.weatherfish.domain.HourlyForecast
import dev.rogerbertan.weatherfish.domain.Place
import dev.rogerbertan.weatherfish.domain.WeatherError

enum class FakeEndpoint(val file: String) {
    Conditions("fake/conditions.json"),
    HourlyForecast("fake/forecasts_hourly.json"),
    DailyForecast("fake/forecasts_daily.json"),
    PlacesSearch("fake/places_search.json"),
    PlacesClosest("fake/places_closest.json"),
}

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

fun parseClosestPlace(json: String): Place =
    xweatherJson.decodeFromString<XweatherResponse<PlaceResponse>>(json).orThrow()
        .firstOrNull()?.toDomain()
        ?: throw WeatherError.NotFound

fun parsePlaces(json: String, query: String): List<Place> {
    val needle = query.foldForSearch()
    return xweatherJson.decodeFromString<XweatherResponse<PlaceResponse>>(json).orThrow()
        .map { it.toDomain() }
        .filter { it.name.foldForSearch().contains(needle) }
}