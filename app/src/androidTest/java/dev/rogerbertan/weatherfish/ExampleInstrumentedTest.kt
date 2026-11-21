package dev.rogerbertan.weatherfish

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ExampleInstrumentedTest {

    @Test
    fun `should run with the app package as target context`() {
        // Arrange
        val instrumentation = InstrumentationRegistry.getInstrumentation()

        // Act
        val packageName = instrumentation.targetContext.packageName

        // Assert
        assertEquals("dev.rogerbertan.weatherfish", packageName)
    }
}