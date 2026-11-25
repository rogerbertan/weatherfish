package dev.rogerbertan.weatherfish.data.rest

import dev.rogerbertan.weatherfish.domain.WeatherError
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class HttpTextTest {

    @ParameterizedTest(name = "should map status {0} to {1}")
    @CsvSource(
        "401, Unauthorized",
        "403, Unauthorized",
        "429, QuotaExceeded",
        "404, NotFound",
    )
    fun `should map http status to weather error`(status: Int, expected: String) {
        // Act
        val error = errorForStatus(HttpStatusCode.fromValue(status))

        // Assert
        assertEquals(expected, error::class.simpleName)
    }

    @Test
    fun `should map other http status to unknown error with the code`() {
        // Act
        val error = errorForStatus(HttpStatusCode.InternalServerError)

        // Assert
        assertEquals(WeatherError.Unknown("http 500"), error)
    }

    @Test
    fun `should throw status error without parsing when status is an error`() {
        // Arrange
        val response = TextResponse(HttpStatusCode.TooManyRequests, headersOf(), """{"results":[]}""")

        // Act
        val error = assertThrows<WeatherError> { response.parseOrThrow { emptyList<String>() } }

        // Assert
        assertEquals(WeatherError.QuotaExceeded, error)
    }

    @Test
    fun `should prefer the error found in the body when status is an error`() {
        // Arrange
        val response = TextResponse(HttpStatusCode.BadRequest, headersOf(), "body")

        // Act
        val error = assertThrows<WeatherError> {
            response.parseOrThrow(errorInBody = { WeatherError.Unknown("from $it") }) { it }
        }

        // Assert
        assertEquals(WeatherError.Unknown("from body"), error)
    }

    @Test
    fun `should keep parsing error when status is successful`() {
        // Arrange
        val response = TextResponse(HttpStatusCode.OK, headersOf(), "<html>ok</html>")

        // Act
        val error = assertThrows<WeatherError> { response.parseOrThrow { it.toInt() } }

        // Assert
        assertInstanceOf(WeatherError.Unknown::class.java, error)
        assertNotEquals(WeatherError.Unknown("http 200"), error)
    }
}
