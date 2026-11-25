package dev.rogerbertan.weatherfish.data.openmeteo

import dev.rogerbertan.weatherfish.data.location.ReverseGeocoder
import dev.rogerbertan.weatherfish.data.rest.getText
import dev.rogerbertan.weatherfish.data.rest.parseOrThrow
import dev.rogerbertan.weatherfish.domain.CurrentWeather
import dev.rogerbertan.weatherfish.domain.DailyForecast
import dev.rogerbertan.weatherfish.domain.GeoPoint
import dev.rogerbertan.weatherfish.domain.HourlyForecast
import dev.rogerbertan.weatherfish.domain.Place
import dev.rogerbertan.weatherfish.domain.WeatherError
import dev.rogerbertan.weatherfish.domain.WeatherRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.parameter
import javax.inject.Inject

class OpenMeteoWeatherRepository @Inject constructor(
    private val client: HttpClient,
    private val geocoder: ReverseGeocoder,
) : WeatherRepository {

    override suspend fun currentWeather(point: GeoPoint): CurrentWeather =
        forecast(point, mapOf("current" to CURRENT_FIELDS, "timeformat" to "unixtime"), ::parseOpenMeteoCurrent)

    override suspend fun hourlyForecast(point: GeoPoint, hours: Int): List<HourlyForecast> =
        forecast(
            point,
            mapOf("hourly" to HOURLY_FIELDS, "forecast_hours" to hours, "timeformat" to "unixtime"),
            ::parseOpenMeteoHourly,
        )

    override suspend fun dailyForecast(point: GeoPoint, days: Int): List<DailyForecast> =
        forecast(point, mapOf("daily" to DAILY_FIELDS, "forecast_days" to days), ::parseOpenMeteoDaily)

    override suspend fun searchPlaces(query: String): List<Place> {
        val term = query.trim()
        if (term.isBlank()) return emptyList()
        val response = client.getText(GEOCODING_URL) {
            parameter("name", term)
            parameter("count", 10)
            parameter("language", "pt")
        }
        return response.parseOrThrow(parse = ::parseOpenMeteoPlaces)
    }

    override suspend fun placeAt(point: GeoPoint): Place {
        val name = geocoder.nameAt(point) ?: throw WeatherError.NotFound
        val timeZone = forecast(point, emptyMap(), ::parseOpenMeteoTimeZone)
        return Place(name.name, name.region, name.country, point, timeZone)
    }

    private suspend fun <T> forecast(point: GeoPoint, params: Map<String, Any>, parse: (String) -> T): T {
        val response = client.getText(FORECAST_URL) {
            parameter("latitude", point.latitude)
            parameter("longitude", point.longitude)
            parameter("timezone", "auto")
            params.forEach { (name, value) -> parameter(name, value) }
        }
        return response.parseOrThrow(parse = parse)
    }

    private companion object {
        const val FORECAST_URL = "https://api.open-meteo.com/v1/forecast"
        const val GEOCODING_URL = "https://geocoding-api.open-meteo.com/v1/search"
        const val CURRENT_FIELDS = "temperature_2m,apparent_temperature,relative_humidity_2m,dew_point_2m," +
            "wind_speed_10m,wind_direction_10m,pressure_msl,visibility,uv_index,weather_code"
        const val HOURLY_FIELDS = "temperature_2m,precipitation_probability,weather_code"
        const val DAILY_FIELDS = "temperature_2m_max,temperature_2m_min,precipitation_probability_max,weather_code"
    }
}
