package dev.rogerbertan.weatherfish.data.openmeteo

import dev.rogerbertan.weatherfish.data.location.GeocodedName
import dev.rogerbertan.weatherfish.data.location.ReverseGeocoder
import dev.rogerbertan.weatherfish.domain.GeoPoint
import dev.rogerbertan.weatherfish.domain.WeatherError
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.mockk.coEvery
import io.mockk.mockk
import java.io.File
import java.io.IOException
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class OpenMeteoWeatherRepositoryTest {

    private val point = GeoPoint(-23.5475, -46.63611)
    private val geocoder = mockk<ReverseGeocoder>()
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json; charset=utf-8")

    private fun fixture(name: String) = File("src/test/resources/openmeteo/$name.json").readText()

    private fun engineWith(body: String, status: HttpStatusCode = HttpStatusCode.OK) =
        MockEngine { respond(body, status, jsonHeaders) }

    private fun repository(engine: MockEngine) =
        OpenMeteoWeatherRepository(HttpClient(engine) { expectSuccess = false }, geocoder)

    @Test
    fun `should request current fields in unix time for the point`() = runTest {
        // Arrange
        val engine = engineWith(fixture("current"))

        // Act
        val current = repository(engine).currentWeather(point)

        // Assert
        val url = engine.requestHistory.single().url
        assertEquals("api.open-meteo.com", url.host)
        assertEquals("/v1/forecast", url.encodedPath)
        assertEquals("-23.5475", url.parameters["latitude"])
        assertEquals("-46.63611", url.parameters["longitude"])
        assertEquals("auto", url.parameters["timezone"])
        assertEquals("unixtime", url.parameters["timeformat"])
        assertEquals(parseOpenMeteoCurrent(fixture("current")), current)
    }

    @Test
    fun `should request the given number of forecast hours`() = runTest {
        // Arrange
        val engine = engineWith(fixture("hourly"))

        // Act
        val hourly = repository(engine).hourlyForecast(point, hours = 24)

        // Assert
        val url = engine.requestHistory.single().url
        assertEquals("24", url.parameters["forecast_hours"])
        assertEquals("temperature_2m,precipitation_probability,weather_code", url.parameters["hourly"])
        assertEquals(24, hourly.size)
    }

    @Test
    fun `should request the given number of forecast days in local dates`() = runTest {
        // Arrange
        val engine = engineWith(fixture("daily"))

        // Act
        val daily = repository(engine).dailyForecast(point, days = 7)

        // Assert
        val url = engine.requestHistory.single().url
        assertEquals("7", url.parameters["forecast_days"])
        assertEquals(null, url.parameters["timeformat"])
        assertEquals(7, daily.size)
    }

    @Test
    fun `should search places in portuguese keeping accents`() = runTest {
        // Arrange
        val engine = engineWith(fixture("places"))

        // Act
        val places = repository(engine).searchPlaces("  são paulo ")

        // Assert
        val url = engine.requestHistory.single().url
        assertEquals("geocoding-api.open-meteo.com", url.host)
        assertEquals("são paulo", url.parameters["name"])
        assertEquals("pt", url.parameters["language"])
        assertEquals("10", url.parameters["count"])
        assertEquals("São Paulo", places.first().name)
    }

    @Test
    fun `should return no places without request when search term is blank`() = runTest {
        // Arrange
        val engine = engineWith(fixture("places"))

        // Act
        val places = repository(engine).searchPlaces("   ")

        // Assert
        assertEquals(emptyList<Nothing>(), places)
        assertEquals(0, engine.requestHistory.size)
    }

    @Test
    fun `should build place from geocoded name and forecast time zone`() = runTest {
        // Arrange
        coEvery { geocoder.nameAt(point) } returns GeocodedName("São Paulo", "São Paulo", "Brasil")
        val engine = engineWith(fixture("place"))

        // Act
        val place = repository(engine).placeAt(point)

        // Assert
        val url = engine.requestHistory.single().url
        assertEquals(setOf("latitude", "longitude", "timezone"), url.parameters.names())
        assertEquals("São Paulo", place.name)
        assertEquals("São Paulo", place.region)
        assertEquals("Brasil", place.country)
        assertEquals(point, place.point)
        assertEquals(ZoneId.of("America/Sao_Paulo"), place.timeZone)
    }

    @Test
    fun `should throw not found without request when the point has no geocoded name`() = runTest {
        // Arrange
        coEvery { geocoder.nameAt(point) } returns null
        val engine = engineWith(fixture("place"))

        // Act
        val error = assertThrows<WeatherError> { repository(engine).placeAt(point) }

        // Assert
        assertEquals(WeatherError.NotFound, error)
        assertEquals(0, engine.requestHistory.size)
    }

    @Test
    fun `should throw quota exceeded instead of empty search when open meteo rate limits`() = runTest {
        // Arrange
        val engine = engineWith("""{"error":true,"reason":"Too many requests"}""", HttpStatusCode.TooManyRequests)

        // Act
        val error = assertThrows<WeatherError> { repository(engine).searchPlaces("são paulo") }

        // Assert
        assertEquals(WeatherError.QuotaExceeded, error)
    }

    @Test
    fun `should throw unknown error with status when open meteo rejects the request`() = runTest {
        // Arrange
        val engine = engineWith(fixture("error"), HttpStatusCode.BadRequest)

        // Act
        val error = assertThrows<WeatherError> { repository(engine).dailyForecast(point, days = 7) }

        // Assert
        assertEquals(WeatherError.Unknown("http 400"), error)
    }

    @Test
    fun `should throw quota exceeded when open meteo rate limits`() = runTest {
        // Arrange
        val engine = engineWith("""{"error":true,"reason":"Too many requests"}""", HttpStatusCode.TooManyRequests)

        // Act
        val error = assertThrows<WeatherError> { repository(engine).currentWeather(point) }

        // Assert
        assertEquals(WeatherError.QuotaExceeded, error)
    }

    @Test
    fun `should throw network error when the connection fails`() = runTest {
        // Arrange
        val engine = MockEngine { throw IOException("offline") }

        // Act
        val error = assertThrows<WeatherError> { repository(engine).currentWeather(point) }

        // Assert
        assertEquals(WeatherError.Network, error)
    }
}
