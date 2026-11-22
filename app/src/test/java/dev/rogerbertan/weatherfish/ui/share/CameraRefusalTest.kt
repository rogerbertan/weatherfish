package dev.rogerbertan.weatherfish.ui.share

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class CameraRefusalTest {

    @ParameterizedTest
    @CsvSource(
        nullValues = ["null"],
        value = [
            "null, true, Denied",
            "null, false, Denied",
            "Denied, true, Denied",
            "Denied, false, Blocked",
            "Blocked, false, Blocked",
            "Blocked, true, Denied",
        ],
    )
    fun `should classify a refused camera request`(
        previous: CameraRefusal?,
        showRationale: Boolean,
        expected: CameraRefusal,
    ) {
        // Arrange
        val rationale = showRationale

        // Act
        val refusal = nextCameraRefusal(previous, rationale)

        // Assert
        assertEquals(expected, refusal)
    }
}
