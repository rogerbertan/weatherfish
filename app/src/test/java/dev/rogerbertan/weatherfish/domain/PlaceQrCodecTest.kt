package dev.rogerbertan.weatherfish.domain

import dev.rogerbertan.weatherfish.ui.testPlace
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class PlaceQrCodecTest {

    private fun readLikeNavigation(content: String): SharedPlace? {
        val params = content.substringAfter('?').split('&').associate { pair ->
            pair.substringBefore('=') to URLDecoder.decode(pair.substringAfter('=').replace("+", "%2B"), StandardCharsets.UTF_8)
        }
        return PlaceQrCodec.fromParams(
            params[PlaceQrCodec.NAME_PARAM].orEmpty(),
            params[PlaceQrCodec.LATITUDE_PARAM].orEmpty(),
            params[PlaceQrCodec.LONGITUDE_PARAM].orEmpty(),
        )
    }

    @Test
    fun `should encode place as weatherfish uri`() {
        // Act
        val content = PlaceQrCodec.encode(testPlace)

        // Assert
        assertEquals("weatherfish://place?name=Porto%20Alegre&lat=-30.0&lon=-51.2", content)
    }

    @Test
    fun `should read encoded place with name and point`() {
        // Arrange
        val content = PlaceQrCodec.encode(testPlace)

        // Act
        val shared = readLikeNavigation(content)

        // Assert
        assertEquals(SharedPlace("Porto Alegre", testPlace.point), shared)
    }

    @Test
    fun `should keep accents and reserved characters in name`() {
        // Arrange
        val place = testPlace.copy(name = "São João d'Aliança & Cia = 1 + 2", point = GeoPoint(-14.7048333, -47.5235))

        // Act
        val shared = readLikeNavigation(PlaceQrCodec.encode(place))

        // Assert
        assertEquals(SharedPlace(place.name, place.point), shared)
    }

    @Test
    fun `should truncate giant name when encoding`() {
        // Arrange
        val place = testPlace.copy(name = "a".repeat(PlaceQrCodec.MAX_NAME_LENGTH + 50))

        // Act
        val shared = readLikeNavigation(PlaceQrCodec.encode(place))

        // Assert
        assertEquals("a".repeat(PlaceQrCodec.MAX_NAME_LENGTH), shared?.name)
    }

    @ParameterizedTest(name = "should round trip 100 times \"{0}\" within the content limit")
    @CsvSource("ã", "東", "😀")
    fun `should keep encoded content readable when name has multibyte characters`(character: String) {
        // Arrange
        val place = testPlace.copy(name = character.repeat(PlaceQrCodec.MAX_NAME_LENGTH))

        // Act
        val content = PlaceQrCodec.encode(place)

        // Assert
        val shared = readLikeNavigation(content)
        assertNotNull(shared)
        assertTrue(PlaceQrCodec.isPlaceLink(content))
        assertTrue(shared.name.isNotEmpty() && place.name.startsWith(shared.name))
    }

    @Test
    fun `should build shared place from valid params`() {
        // Act
        val shared = PlaceQrCodec.fromParams("Porto Alegre", "-30.0", "-51.2")

        // Assert
        assertEquals(SharedPlace("Porto Alegre", testPlace.point), shared)
    }

    @ParameterizedTest(name = "should accept lat {0} and lon {1}")
    @CsvSource(
        "90.0, 180.0",
        "-90.0, -180.0",
        "0.0, 0.0",
    )
    fun `should accept coordinates at the limits`(latitude: String, longitude: String) {
        // Act
        val shared = PlaceQrCodec.fromParams("Polo", latitude, longitude)

        // Assert
        assertEquals(SharedPlace("Polo", GeoPoint(latitude.toDouble(), longitude.toDouble())), shared)
    }

    @ParameterizedTest(name = "should reject params name=\"{0}\" lat=\"{1}\" lon=\"{2}\"")
    @CsvSource(
        "'', 0, 0",
        "' ', 0, 0",
        "Porto, 90.1, 0",
        "Porto, -90.1, 0",
        "Porto, 0, 180.1",
        "Porto, 0, -180.1",
        "Porto, NaN, 0",
        "Porto, Infinity, 0",
        "Porto, abc, 0",
        "Porto, 1.2.3, 0",
        "Porto, '', 0",
    )
    fun `should reject invalid params`(name: String, latitude: String, longitude: String) {
        // Act
        val shared = PlaceQrCodec.fromParams(name, latitude, longitude)

        // Assert
        assertNull(shared)
    }

    @Test
    fun `should reject name longer than limit`() {
        // Act
        val shared = PlaceQrCodec.fromParams("a".repeat(PlaceQrCodec.MAX_NAME_LENGTH + 1), "0", "0")

        // Assert
        assertNull(shared)
    }

    @ParameterizedTest(name = "should reject name with control or format character U+{0}")
    @ValueSource(strings = ["000A", "0000", "202E", "200B"])
    fun `should reject name with control or format characters`(codePoint: String) {
        // Arrange
        val name = "Promoção" + Char(codePoint.toInt(16)) + "acesse golpe.com"

        // Act
        val shared = PlaceQrCodec.fromParams(name, "0", "0")

        // Assert
        assertNull(shared)
    }

    @ParameterizedTest(name = "should recognize \"{0}\" as place link")
    @ValueSource(
        strings = [
            "weatherfish://place?name=Porto&lat=0&lon=0",
            "weatherfish://place?lat=999",
        ],
    )
    fun `should recognize place links`(content: String) {
        // Act
        val result = PlaceQrCodec.isPlaceLink(content)

        // Assert
        assertTrue(result)
    }

    @ParameterizedTest(name = "should not recognize \"{0}\" as place link")
    @ValueSource(
        strings = [
            "",
            "Porto Alegre",
            "https://example.com/?name=Porto&lat=0&lon=0",
            "weatherfish://week",
            "weatherfish://place",
            "weatherfish://placebo?name=Porto&lat=0&lon=0",
        ],
    )
    fun `should not recognize other content as place link`(content: String) {
        // Act
        val result = PlaceQrCodec.isPlaceLink(content)

        // Assert
        assertFalse(result)
    }

    @Test
    fun `should not recognize place link longer than limit`() {
        // Arrange
        val content = "weatherfish://place?name=${"%C3%A3".repeat(PlaceQrCodec.MAX_NAME_LENGTH)}&lat=0&lon=0"

        // Act
        val result = PlaceQrCodec.isPlaceLink(content)

        // Assert
        assertFalse(result)
    }
}