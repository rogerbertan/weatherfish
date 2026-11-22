package dev.rogerbertan.weatherfish.ui

import dev.rogerbertan.weatherfish.domain.PlaceQrCodec
import kotlinx.serialization.descriptors.elementNames
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ImportPlaceRouteTest {

    @Test
    fun `should name route fields after place link params`() {
        // Act
        val fields = ImportPlaceRoute.serializer().descriptor.elementNames.toList()

        // Assert
        assertEquals(listOf(PlaceQrCodec.NAME_PARAM, PlaceQrCodec.LATITUDE_PARAM, PlaceQrCodec.LONGITUDE_PARAM), fields)
    }
}