package dev.rogerbertan.weatherfish.data.xweather

import android.content.Context
import com.aerisweather.aeris.communication.Action
import com.aerisweather.aeris.communication.AerisCommunicationTask
import com.aerisweather.aeris.communication.AerisRequest
import com.aerisweather.aeris.communication.Endpoint
import com.aerisweather.aeris.communication.EndpointType
import com.aerisweather.aeris.communication.parameter.ParameterBuilder
import com.aerisweather.aeris.model.AerisDataJSON
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.rogerbertan.weatherfish.domain.CurrentWeather
import dev.rogerbertan.weatherfish.domain.DailyForecast
import dev.rogerbertan.weatherfish.domain.GeoPoint
import dev.rogerbertan.weatherfish.domain.HourlyForecast
import dev.rogerbertan.weatherfish.domain.Place
import dev.rogerbertan.weatherfish.domain.WeatherError
import dev.rogerbertan.weatherfish.domain.WeatherRepository
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class XweatherWeatherRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : WeatherRepository {

    override suspend fun currentWeather(point: GeoPoint): CurrentWeather =
        fetch(AerisRequest(Endpoint(EndpointType.CONDITIONS), point.id), ::parseAerisConditions)

    override suspend fun hourlyForecast(point: GeoPoint, hours: Int): List<HourlyForecast> =
        fetch(forecasts(point, filter = "1hr", limit = hours)) { parseAerisHourly(it, hours) }

    override suspend fun dailyForecast(point: GeoPoint, days: Int): List<DailyForecast> =
        fetch(forecasts(point, filter = "day", limit = days)) { parseAerisDaily(it, days) }

    override suspend fun searchPlaces(query: String): List<Place> {
        val term = query.foldForSearch().filter { it.isLetterOrDigit() || it in " -'" }
        if (term.isBlank()) return emptyList()
        val params = ParameterBuilder()
            .withQuery("name:^$term")
            .withSort("pop:-1")
            .withLimit(10)
            .build()
        return fetch(AerisRequest(Endpoint(EndpointType.PLACES), Action.SEARCH, *params), ::parseAerisPlaces)
    }

    override suspend fun placeAt(point: GeoPoint): Place =
        fetch(AerisRequest(Endpoint(EndpointType.PLACES), point.id)) {
            parseAerisPlaces(it).firstOrNull() ?: throw WeatherError.NotFound
        }

    private val GeoPoint.id get() = String.format(Locale.ROOT, "%.5f,%.5f", latitude, longitude)

    private fun forecasts(point: GeoPoint, filter: String, limit: Int) = AerisRequest(
        Endpoint(EndpointType.FORECASTS),
        point.id,
        *ParameterBuilder().withFilter(filter).withLimit(limit).build(),
    )

    private suspend fun <T> fetch(request: AerisRequest, parse: (List<AerisDataJSON>) -> T): T {
        if (!XweatherSdk.isConfigured) throw WeatherError.Unauthorized
        val response = withContext(Dispatchers.IO) {
            AerisCommunicationTask(context, request).executeSyncTask()
        } ?: throw WeatherError.Unknown("empty response")
        val data = response.orThrow()
        return parsingOrThrow { parse(data) }
    }
}