package dev.rogerbertan.weatherfish.data.xweather

import dev.rogerbertan.weatherfish.domain.WeatherCondition
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class XweatherMappingTest {

    @ParameterizedTest(name = "should title case \"{0}\" as \"{1}\"")
    @CsvSource(
        "são paulo, São Paulo",
        "rio de janeiro, Rio de Janeiro",
        "buenos aires, Buenos Aires",
        "rio grande do sul, Rio Grande do Sul",
    )
    fun `should title case place names keeping portuguese particles lowercase`(raw: String, expected: String) {
        // Act
        val result = titleCasePlaceName(raw)

        // Assert
        assertEquals(expected, result)
    }

    @ParameterizedTest(name = "should map \"{0}\" to {1}")
    @CsvSource(
        "::CL, Clear",
        "::FW, Clear",
        "::SC, PartlyCloudy",
        "::BK, Cloudy",
        "::OV, Cloudy",
        ":L:L, Drizzle",
        ":L:RW, Rain",
        "::T, Thunderstorm",
        "::S, Snow",
        "::BR, Fog",
        "::BD, Wind",
        "::XX, Unknown",
    )
    fun `should map coded weather to condition`(coded: String, expected: WeatherCondition) {
        // Act
        val result = conditionFromCoded(coded)

        // Assert
        assertEquals(expected, result)
    }
}