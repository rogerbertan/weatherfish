package dev.rogerbertan.weatherfish.data

import dev.rogerbertan.weatherfish.domain.CurrentWeather
import dev.rogerbertan.weatherfish.domain.DailyForecast
import dev.rogerbertan.weatherfish.domain.GeoPoint
import dev.rogerbertan.weatherfish.domain.HourlyForecast
import dev.rogerbertan.weatherfish.domain.Place
import dev.rogerbertan.weatherfish.domain.WeatherError
import dev.rogerbertan.weatherfish.domain.WeatherRepository
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CachingWeatherRepository(
    private val delegate: WeatherRepository,
    private val timeSource: TimeSource,
    private val scope: CoroutineScope,
) : WeatherRepository {

    private data class Key(val endpoint: String, val point: GeoPoint, val size: Int)

    private class Entry(val result: Deferred<Any>, val expiresAt: TimeMark)

    private val mutex = Mutex()
    private val entries = HashMap<Key, Entry>()

    override suspend fun currentWeather(point: GeoPoint): CurrentWeather =
        cached(Key("conditions", point, 0), CONDITIONS_TTL) { delegate.currentWeather(point) }

    override suspend fun hourlyForecast(point: GeoPoint, hours: Int): List<HourlyForecast> =
        cached(Key("hourly", point, hours), HOURLY_TTL) { delegate.hourlyForecast(point, hours) }

    override suspend fun dailyForecast(point: GeoPoint, days: Int): List<DailyForecast> =
        cached(Key("daily", point, days), DAILY_TTL) { delegate.dailyForecast(point, days) }

    override suspend fun searchPlaces(query: String): List<Place> = delegate.searchPlaces(query)

    override suspend fun placeAt(point: GeoPoint): Place = delegate.placeAt(point)

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T : Any> cached(key: Key, ttl: Duration, load: suspend () -> T): T {
        val entry = mutex.withLock {
            entries[key]?.takeIf { it.expiresAt.hasNotPassedNow() }
                ?: Entry(scope.async { load() }, timeSource.markNow() + ttl).also { entries[key] = it }
        }
        return try {
            entry.result.await() as T
        } catch (e: WeatherError) {
            mutex.withLock { if (entries[key] === entry) entries.remove(key) }
            throw e
        }
    }

    companion object {
        val CONDITIONS_TTL = 10.minutes
        val HOURLY_TTL = 30.minutes
        val DAILY_TTL = 1.hours
    }
}
