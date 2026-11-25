package dev.rogerbertan.weatherfish.data.xweather

import android.util.Log
import dev.rogerbertan.weatherfish.data.rest.getText
import dev.rogerbertan.weatherfish.data.rest.parseOrThrow
import dev.rogerbertan.weatherfish.domain.CurrentWeather
import dev.rogerbertan.weatherfish.domain.DailyForecast
import dev.rogerbertan.weatherfish.domain.GeoPoint
import dev.rogerbertan.weatherfish.domain.HourlyForecast
import dev.rogerbertan.weatherfish.domain.MapCredentialsProvider
import dev.rogerbertan.weatherfish.domain.Place
import dev.rogerbertan.weatherfish.domain.WeatherError
import dev.rogerbertan.weatherfish.domain.WeatherRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.parameter
import io.ktor.http.Headers
import javax.inject.Inject

class XweatherRestRepository @Inject constructor(
    private val client: HttpClient,
    private val credentials: MapCredentialsProvider,
) : WeatherRepository {

    override suspend fun currentWeather(point: GeoPoint): CurrentWeather =
        fetch("conditions/${point.xweatherId}", parse = ::parseConditions)

    override suspend fun hourlyForecast(point: GeoPoint, hours: Int): List<HourlyForecast> =
        fetch("forecasts/${point.xweatherId}", mapOf("filter" to "1hr", "limit" to hours)) { parseHourly(it, hours) }

    override suspend fun dailyForecast(point: GeoPoint, days: Int): List<DailyForecast> =
        fetch("forecasts/${point.xweatherId}", mapOf("filter" to "day", "limit" to days)) { parseDaily(it, days) }

    override suspend fun searchPlaces(query: String): List<Place> {
        val term = searchTerm(query)
        if (term.isBlank()) return emptyList()
        val params = mapOf("query" to "name:^$term", "sort" to "pop:-1", "limit" to 10)
        return fetch("places/search", params, ::parsePlaces)
    }

    override suspend fun placeAt(point: GeoPoint): Place =
        fetch("places/${point.xweatherId}", parse = ::parseClosestPlace)

    private suspend fun <T> fetch(path: String, params: Map<String, Any> = emptyMap(), parse: (String) -> T): T {
        val keys = credentials.mapCredentials ?: throw WeatherError.Unauthorized
        val response = client.getText("$BASE_URL/$path") {
            parameter("client_id", keys.clientId)
            parameter("client_secret", keys.clientSecret)
            params.forEach { (name, value) -> parameter(name, value) }
        }
        logRateLimit(response.headers)
        return response.parseOrThrow(::xweatherErrorIn, parse)
    }

    private fun logRateLimit(headers: Headers) {
        val limit = headers["x-ratelimit-limit-minute"] ?: return
        val remaining = headers["x-ratelimit-remaining-minute"]
        val reset = headers["x-ratelimit-reset-minute"]
        Log.d(TAG, "rate limit $remaining/$limit per minute, reset $reset")
    }

    private companion object {
        const val BASE_URL = "https://data.api.xweather.com"
        const val TAG = "XweatherRest"
    }
}
