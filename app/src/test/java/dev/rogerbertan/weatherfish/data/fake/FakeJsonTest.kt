package dev.rogerbertan.weatherfish.data.fake

import dev.rogerbertan.weatherfish.domain.WeatherCondition
import dev.rogerbertan.weatherfish.domain.WeatherError
import java.io.File
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class FakeJsonTest {

    private fun asset(endpoint: FakeEndpoint) = File("src/main/assets/${endpoint.file}").readText()

    @Test
    fun `should parse conditions asset into current weather`() {
        // Arrange
        val json = asset(FakeEndpoint.Conditions)

        // Act
        val current = parseConditions(json)

        // Assert
        assertEquals(27.4, current.temperatureC)
        assertEquals(WeatherCondition.PartlyCloudy, current.condition)
    }

    @Test
    fun `should parse twenty four consecutive hours from hourly asset`() {
        // Arrange
        val json = asset(FakeEndpoint.HourlyForecast)

        // Act
        val hourly = parseHourly(json, hours = 24)

        // Assert
        assertEquals(24, hourly.size)
        assertEquals(List(23) { 3600L }, hourly.zipWithNext { a, b -> b.time.epochSecond - a.time.epochSecond })
    }

    @Test
    fun `should parse seven consecutive days from daily asset`() {
        // Arrange
        val json = asset(FakeEndpoint.DailyForecast)
        val expectedDates = (0L..6L).map { LocalDate.of(2026, 11, 21).plusDays(it) }

        // Act
        val daily = parseDaily(json, days = 7)

        // Assert
        assertEquals(expectedDates, daily.map { it.date })
    }

    @Test
    fun `should match places ignoring case and accents`() {
        // Arrange
        val json = asset(FakeEndpoint.PlacesSearch)

        // Act
        val places = parsePlaces(json, query = "SAO pau")

        // Assert
        assertEquals(listOf("São Paulo"), places.map { it.name })
    }

    @Test
    fun `should throw domain error when xweather responds with error`() {
        // Arrange
        val json = """{"success":false,"error":{"code":"invalid_location","description":"x"},"response":[]}"""

        // Act
        val error = assertThrows<WeatherError> { parseConditions(json) }

        // Assert
        assertEquals(WeatherError.NotFound, error)
    }
}