package dev.rogerbertan.weatherfish.data.fake

import dev.rogerbertan.weatherfish.data.xweather.foldForSearch
import dev.rogerbertan.weatherfish.data.xweather.parsePlaces
import dev.rogerbertan.weatherfish.domain.Place

enum class FakeEndpoint(val file: String) {
    Conditions("fake/conditions.json"),
    HourlyForecast("fake/forecasts_hourly.json"),
    DailyForecast("fake/forecasts_daily.json"),
    PlacesSearch("fake/places_search.json"),
    PlacesClosest("fake/places_closest.json"),
}

fun matchingPlaces(json: String, query: String): List<Place> {
    val needle = query.foldForSearch()
    return parsePlaces(json).filter { it.name.foldForSearch().contains(needle) }
}