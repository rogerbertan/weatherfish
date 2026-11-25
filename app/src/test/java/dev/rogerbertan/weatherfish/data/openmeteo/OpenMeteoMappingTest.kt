package dev.rogerbertan.weatherfish.data.openmeteo

import dev.rogerbertan.weatherfish.domain.GeoPoint
import dev.rogerbertan.weatherfish.domain.WeatherCondition
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class OpenMeteoMappingTest {

    private fun fixture(name: String) = File("src/test/resources/openmeteo/$name.json").readText()

    @ParameterizedTest(name = "should map wmo {0} to {1}")
    @CsvSource(
        "0, Clear, Céu limpo",
        "1, Clear, Predominantemente limpo",
        "2, PartlyCloudy, Parcialmente nublado",
        "3, Cloudy, Nublado",
        "45, Fog, Nevoeiro",
        "53, Drizzle, Garoa",
        "57, Drizzle, Garoa congelante",
        "63, Rain, Chuva",
        "67, Rain, Chuva congelante",
        "75, Snow, Neve",
        "81, Rain, Pancadas de chuva",
        "86, Snow, Pancadas de neve",
        "95, Thunderstorm, Trovoada",
        "99, Thunderstorm, Trovoada com granizo",
        "42, Unknown, Condição desconhecida",
    )
    fun `should map wmo code to condition and description`(code: Int, condition: WeatherCondition, description: String) {
        // Act
        val result = wmoCode(code)

        // Assert
        assertEquals(WmoCode(condition, description), result)
    }

    @ParameterizedTest(name = "should map {0} degrees to {1}")
    @CsvSource(
        "0, N",
        "11.2, N",
        "11.3, NNE",
        "90, E",
        "180, S",
        "247.5, WSW",
        "348.7, NNW",
        "348.8, N",
        "360, N",
        "-11.3, NNW",
        "-90, W",
    )
    fun `should map wind degrees to compass point`(degrees: Double, expected: String) {
        // Act
        val result = compassDirection(degrees)

        // Assert
        assertEquals(expected, result)
    }

    @Test
    fun `should parse current fixture into current weather`() {
        // Arrange
        val json = fixture("current")

        // Act
        val current = parseOpenMeteoCurrent(json)

        // Assert
        assertEquals(Instant.ofEpochSecond(1795569300), current.observedAt)
        assertEquals(WeatherCondition.Drizzle, current.condition)
        assertEquals("Garoa", current.description)
        assertEquals(22.9, current.temperatureC)
        assertEquals(26.6, current.feelsLikeC)
        assertEquals(90, current.humidityPercent)
        assertEquals(21.3, current.dewPointC)
        assertEquals(5.7, current.windSpeedKph)
        assertEquals(1013.7, current.pressureMb)
        assertEquals("N", current.windDirection)
        assertEquals(20.38, current.visibilityKm)
        assertEquals(0, current.uvIndex)
    }

    @Test
    fun `should parse twenty four consecutive hours from hourly fixture`() {
        // Arrange
        val json = fixture("hourly")

        // Act
        val hourly = parseOpenMeteoHourly(json)

        // Assert
        assertEquals(24, hourly.size)
        assertEquals(List(23) { 3600L }, hourly.zipWithNext { a, b -> b.time.epochSecond - a.time.epochSecond })
        assertEquals(25, hourly.first().precipitationChancePercent)
    }

    @Test
    fun `should parse seven local days from daily fixture`() {
        // Arrange
        val json = fixture("daily")

        // Act
        val daily = parseOpenMeteoDaily(json)

        // Assert
        assertEquals((0L..6L).map { LocalDate.of(2026, 11, 24).plusDays(it) }, daily.map { it.date })
        assertEquals(20.4, daily.first().minTemperatureC)
        assertEquals(30.9, daily.first().maxTemperatureC)
        assertEquals("Trovoada com granizo", daily.first().description)
    }

    @Test
    fun `should parse places fixture keeping accents region and time zone`() {
        // Arrange
        val json = fixture("places")

        // Act
        val place = parseOpenMeteoPlaces(json).first()

        // Assert
        assertEquals("São Paulo", place.name)
        assertEquals("São Paulo", place.region)
        assertEquals("Brasil", place.country)
        assertEquals(GeoPoint(-23.5475, -46.63611), place.point)
        assertEquals(ZoneId.of("America/Sao_Paulo"), place.timeZone)
    }

    @Test
    fun `should parse empty geocoding response as no places`() {
        // Arrange
        val json = """{"generationtime_ms":0.1}"""

        // Act
        val places = parseOpenMeteoPlaces(json)

        // Assert
        assertEquals(emptyList<Nothing>(), places)
    }

    @Test
    fun `should parse time zone from forecast without variables`() {
        // Arrange
        val json = fixture("place")

        // Act
        val timeZone = parseOpenMeteoTimeZone(json)

        // Assert
        assertEquals(ZoneId.of("America/Sao_Paulo"), timeZone)
    }
}