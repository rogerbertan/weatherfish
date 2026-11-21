package dev.rogerbertan.weatherfish.data.xweather

import com.aerisweather.aeris.model.AerisDataJSON
import dev.rogerbertan.weatherfish.data.fake.FakeEndpoint
import dev.rogerbertan.weatherfish.data.fake.parseConditions
import dev.rogerbertan.weatherfish.data.fake.parseDaily
import dev.rogerbertan.weatherfish.data.fake.parseHourly
import dev.rogerbertan.weatherfish.data.fake.parsePlaces
import dev.rogerbertan.weatherfish.domain.WeatherError
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AerisMappingTest {

    private fun asset(endpoint: FakeEndpoint) = File("src/main/assets/${endpoint.file}").readText()

    private fun aerisData(json: String): List<AerisDataJSON> =
        Json.parseToJsonElement(json).jsonObject.getValue("response").jsonArray
            .map { AerisDataJSON.fromJSON(it.toString()) }

    @Test
    fun `should map conditions like the fake parser`() {
        // Arrange
        val json = asset(FakeEndpoint.Conditions)

        // Act
        val current = parseAerisConditions(aerisData(json))

        // Assert
        assertEquals(parseConditions(json), current)
    }

    @Test
    fun `should map hourly forecast like the fake parser`() {
        // Arrange
        val json = asset(FakeEndpoint.HourlyForecast)

        // Act
        val hourly = parseAerisHourly(aerisData(json), hours = 24)

        // Assert
        assertEquals(parseHourly(json, hours = 24), hourly)
    }

    @Test
    fun `should map daily forecast like the fake parser`() {
        // Arrange
        val json = asset(FakeEndpoint.DailyForecast)

        // Act
        val daily = parseAerisDaily(aerisData(json), days = 7)

        // Assert
        assertEquals(parseDaily(json, days = 7), daily)
    }

    @Test
    fun `should map places like the fake parser`() {
        // Arrange
        val json = asset(FakeEndpoint.PlacesSearch)

        // Act
        val places = parseAerisPlaces(aerisData(json))

        // Assert
        assertEquals(parsePlaces(json, query = ""), places)
    }

    @Test
    fun `should throw not found when conditions response is empty`() {
        // Arrange
        val data = emptyList<AerisDataJSON>()

        // Act
        val error = assertThrows<WeatherError> { parseAerisConditions(data) }

        // Assert
        assertEquals(WeatherError.NotFound, error)
    }

    @Test
    fun `should throw unknown error when sdk item has no periods`() {
        // Arrange
        val data = listOf(AerisDataJSON.fromJSON("""{"loc":{"lat":0,"long":0}}"""))

        // Act
        val error = assertThrows<WeatherError> { parsingOrThrow { parseAerisConditions(data) } }

        // Assert
        assertInstanceOf(WeatherError.Unknown::class.java, error)
    }
}