package dev.rogerbertan.weatherfish.data.xweather

import android.util.Log
import dev.rogerbertan.weatherfish.data.fake.FakeEndpoint
import dev.rogerbertan.weatherfish.domain.GeoPoint
import dev.rogerbertan.weatherfish.domain.MapCredentials
import dev.rogerbertan.weatherfish.domain.MapCredentialsProvider
import dev.rogerbertan.weatherfish.domain.WeatherError
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class XweatherRestRepositoryTest {

    private val point = GeoPoint(-23.5475, -46.63611)
    private val credentials = mockk<MapCredentialsProvider> {
        every { mapCredentials } returns MapCredentials("the-id", "the-secret")
    }

    private fun asset(endpoint: FakeEndpoint) = File("src/main/assets/${endpoint.file}").readText()

    private fun MockRequestHandleScope.respondJson(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
        vararg extraHeaders: Pair<String, String>,
    ): HttpResponseData = respond(
        content = body,
        status = status,
        headers = headersOf(
            HttpHeaders.ContentType to listOf("application/json;  charset=utf-8; x-cost-v1=error||0"),
            *extraHeaders.map { (name, value) -> name to listOf(value) }.toTypedArray(),
        ),
    )

    private fun repository(engine: MockEngine, provider: MapCredentialsProvider = credentials) =
        XweatherRestRepository(HttpClient(engine) { expectSuccess = false }, provider)

    @BeforeEach
    fun mockLog() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
    }

    @AfterEach
    fun unmockLog() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `should request conditions with credentials and map the response`() = runTest {
        // Arrange
        val json = asset(FakeEndpoint.Conditions)
        val engine = MockEngine { respondJson(json) }

        // Act
        val current = repository(engine).currentWeather(point)

        // Assert
        val request = engine.requestHistory.single()
        assertEquals("/conditions/-23.54750,-46.63611", request.url.encodedPath)
        assertEquals("the-id", request.url.parameters["client_id"])
        assertEquals("the-secret", request.url.parameters["client_secret"])
        assertEquals(parseConditions(json), current)
    }

    @Test
    fun `should request hourly forecast with filter and limit`() = runTest {
        // Arrange
        val json = asset(FakeEndpoint.HourlyForecast)
        val engine = MockEngine { respondJson(json) }

        // Act
        val hourly = repository(engine).hourlyForecast(point, hours = 24)

        // Assert
        val request = engine.requestHistory.single()
        assertEquals("/forecasts/-23.54750,-46.63611", request.url.encodedPath)
        assertEquals("1hr", request.url.parameters["filter"])
        assertEquals("24", request.url.parameters["limit"])
        assertEquals(parseHourly(json, hours = 24), hourly)
    }

    @Test
    fun `should request daily forecast with filter and limit`() = runTest {
        // Arrange
        val json = asset(FakeEndpoint.DailyForecast)
        val engine = MockEngine { respondJson(json) }

        // Act
        val daily = repository(engine).dailyForecast(point, days = 7)

        // Assert
        val request = engine.requestHistory.single()
        assertEquals("day", request.url.parameters["filter"])
        assertEquals("7", request.url.parameters["limit"])
        assertEquals(parseDaily(json, days = 7), daily)
    }

    @Test
    fun `should search places with folded term sorted by population`() = runTest {
        // Arrange
        val engine = MockEngine { respondJson(asset(FakeEndpoint.PlacesSearch)) }

        // Act
        val places = repository(engine).searchPlaces("  São Paulo!? ")

        // Assert
        val request = engine.requestHistory.single()
        assertEquals("/places/search", request.url.encodedPath)
        assertEquals("name:^sao paulo", request.url.parameters["query"])
        assertEquals("pop:-1", request.url.parameters["sort"])
        assertEquals("10", request.url.parameters["limit"])
        assertTrue(places.isNotEmpty())
    }

    @Test
    fun `should return no places without request when search term is blank`() = runTest {
        // Arrange
        val engine = MockEngine { respondJson(asset(FakeEndpoint.PlacesSearch)) }

        // Act
        val places = repository(engine).searchPlaces(" ?! ")

        // Assert
        assertEquals(emptyList<Nothing>(), places)
        assertEquals(0, engine.requestHistory.size)
    }

    @Test
    fun `should request place at point and map the closest place`() = runTest {
        // Arrange
        val json = asset(FakeEndpoint.PlacesClosest)
        val engine = MockEngine { respondJson(json) }

        // Act
        val place = repository(engine).placeAt(point)

        // Assert
        assertEquals("/places/-23.54750,-46.63611", engine.requestHistory.single().url.encodedPath)
        assertEquals(parseClosestPlace(json), place)
    }

    @Test
    fun `should throw unauthorized when xweather rejects the client`() = runTest {
        // Arrange
        val body = """{"success":false,"error":{"code":"invalid_client","description":"x"},"response":[]}"""
        val engine = MockEngine { respondJson(body, HttpStatusCode.Unauthorized) }

        // Act
        val error = assertThrows<WeatherError> { repository(engine).currentWeather(point) }

        // Assert
        assertEquals(WeatherError.Unauthorized, error)
    }

    @Test
    fun `should throw quota exceeded when xweather reports max hits`() = runTest {
        // Arrange
        val body = """{"success":false,"error":{"code":"maxhits","description":"x"},"response":[]}"""
        val engine = MockEngine { respondJson(body, HttpStatusCode.TooManyRequests) }

        // Act
        val error = assertThrows<WeatherError> { repository(engine).currentWeather(point) }

        // Assert
        assertEquals(WeatherError.QuotaExceeded, error)
    }

    @Test
    fun `should keep the api error code when status is an error`() = runTest {
        // Arrange
        val body = """{"success":false,"error":{"code":"invalid_request","description":"x"},"response":[]}"""
        val engine = MockEngine { respondJson(body, HttpStatusCode.BadRequest) }

        // Act
        val error = assertThrows<WeatherError> { repository(engine).currentWeather(point) }

        // Assert
        assertEquals(WeatherError.Unknown("invalid_request"), error)
    }

    @Test
    fun `should throw error from http status when body is not json`() = runTest {
        // Arrange
        val engine = MockEngine { respondJson("<html>down</html>", HttpStatusCode.ServiceUnavailable) }

        // Act
        val error = assertThrows<WeatherError> { repository(engine).currentWeather(point) }

        // Assert
        assertEquals(WeatherError.Unknown("http 503"), error)
    }

    @Test
    fun `should throw network error when the connection fails`() = runTest {
        // Arrange
        val engine = MockEngine { throw IOException("https://data.api.xweather.com/?client_secret=the-secret") }

        // Act
        val error = assertThrows<WeatherError> { repository(engine).currentWeather(point) }

        // Assert
        assertEquals(WeatherError.Network, error)
    }

    @Test
    fun `should throw unauthorized without request when credentials are missing`() = runTest {
        // Arrange
        val engine = MockEngine { respondJson(asset(FakeEndpoint.Conditions)) }
        val missing = mockk<MapCredentialsProvider> { every { mapCredentials } returns null }

        // Act
        val error = assertThrows<WeatherError> { repository(engine, missing).currentWeather(point) }

        // Assert
        assertEquals(WeatherError.Unauthorized, error)
        assertEquals(0, engine.requestHistory.size)
    }

    @Test
    fun `should log rate limit headers without credentials`() = runTest {
        // Arrange
        val engine = MockEngine {
            respondJson(
                asset(FakeEndpoint.Conditions),
                HttpStatusCode.OK,
                "x-ratelimit-limit-minute" to "100",
                "x-ratelimit-remaining-minute" to "99",
                "x-ratelimit-reset-minute" to "Wed, 25 Nov 2026 00:54:00 GMT",
            )
        }

        // Act
        repository(engine).currentWeather(point)

        // Assert
        verify {
            Log.d(any(), match { it.contains("99/100") && it.contains("00:54:00") && !it.contains("the-secret") })
        }
    }

    @Test
    fun `should cancel the http request when the caller is cancelled`() = runTest {
        // Arrange
        val started = CompletableDeferred<Unit>()
        val requestCancelled = CompletableDeferred<Unit>()
        val engine = MockEngine {
            started.complete(Unit)
            try {
                awaitCancellation()
            } catch (e: CancellationException) {
                requestCancelled.complete(Unit)
                throw e
            }
        }
        val job = launch { repository(engine).currentWeather(point) }
        started.await()

        // Act
        job.cancel()
        requestCancelled.await()

        // Assert
        assertTrue(job.isCancelled)
    }
}
