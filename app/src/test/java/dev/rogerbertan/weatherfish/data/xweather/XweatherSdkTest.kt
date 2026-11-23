package dev.rogerbertan.weatherfish.data.xweather

import dev.rogerbertan.weatherfish.domain.MapCredentials
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class XweatherSdkTest {

    @ParameterizedTest
    @CsvSource(
        "'', secret",
        "id, ''",
        "' ', secret",
        "id, ' '",
    )
    fun `should have no credentials when a key is blank`(clientId: String, clientSecret: String) {
        // Act
        val credentials = credentialsOf(clientId, clientSecret)

        // Assert
        assertNull(credentials)
    }

    @Test
    fun `should have credentials when both keys are filled`() {
        // Arrange
        val clientId = "id"
        val clientSecret = "secret"

        // Act
        val credentials = credentialsOf(clientId, clientSecret)

        // Assert
        assertEquals(MapCredentials(clientId, clientSecret), credentials)
    }
}
