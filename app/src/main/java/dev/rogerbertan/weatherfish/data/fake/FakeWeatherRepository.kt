package dev.rogerbertan.weatherfish.data.fake

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.rogerbertan.weatherfish.data.parsingOrThrow
import dev.rogerbertan.weatherfish.data.xweather.parseClosestPlace
import dev.rogerbertan.weatherfish.data.xweather.parseConditions
import dev.rogerbertan.weatherfish.data.xweather.parseDaily
import dev.rogerbertan.weatherfish.data.xweather.parseHourly
import dev.rogerbertan.weatherfish.domain.CurrentWeather
import dev.rogerbertan.weatherfish.domain.DailyForecast
import dev.rogerbertan.weatherfish.domain.GeoPoint
import dev.rogerbertan.weatherfish.domain.HourlyForecast
import dev.rogerbertan.weatherfish.domain.Place
import dev.rogerbertan.weatherfish.domain.WeatherError
import dev.rogerbertan.weatherfish.domain.WeatherRepository
import javax.inject.Inject
import kotlin.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

data class FakeConfig(val latency: Duration, val simulatedError: WeatherError?)

class FakeWeatherRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val config: FakeConfig,
) : WeatherRepository {

    override suspend fun currentWeather(point: GeoPoint): CurrentWeather =
        load(FakeEndpoint.Conditions, ::parseConditions)

    override suspend fun hourlyForecast(point: GeoPoint, hours: Int): List<HourlyForecast> =
        load(FakeEndpoint.HourlyForecast) { parseHourly(it, hours) }

    override suspend fun dailyForecast(point: GeoPoint, days: Int): List<DailyForecast> =
        load(FakeEndpoint.DailyForecast) { parseDaily(it, days) }

    override suspend fun searchPlaces(query: String): List<Place> =
        load(FakeEndpoint.PlacesSearch) { matchingPlaces(it, query) }

    override suspend fun placeAt(point: GeoPoint): Place =
        load(FakeEndpoint.PlacesClosest, ::parseClosestPlace)

    private suspend fun <T> load(endpoint: FakeEndpoint, parse: (String) -> T): T {
        delay(config.latency)
        config.simulatedError?.let { throw it }
        val json = withContext(Dispatchers.IO) {
            context.assets.open(endpoint.file).bufferedReader().use { it.readText() }
        }
        return parsingOrThrow { parse(json) }
    }
}