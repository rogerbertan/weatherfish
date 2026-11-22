package dev.rogerbertan.weatherfish.domain

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class SharedPlace(val name: String, val point: GeoPoint)

object PlaceQrCodec {

    const val MAX_NAME_LENGTH = 100
    const val MAX_CONTENT_LENGTH = 512

    const val PLACE_URI = "weatherfish://place"
    const val NAME_PARAM = "name"
    const val LATITUDE_PARAM = "lat"
    const val LONGITUDE_PARAM = "lon"

    private val forbiddenCharTypes = setOf(Character.CONTROL.toInt(), Character.FORMAT.toInt())

    fun encode(place: Place): String = encode(place.name, place.point)

    fun encode(name: String, point: GeoPoint): String {
        var truncated = name.take(MAX_NAME_LENGTH)
        var content = contentOf(truncated, point)
        while (content.length > MAX_CONTENT_LENGTH || truncated.lastOrNull()?.isHighSurrogate() == true) {
            truncated = truncated.dropLast(1)
            content = contentOf(truncated, point)
        }
        return content
    }

    private fun contentOf(name: String, point: GeoPoint): String {
        val encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20")
        return "$PLACE_URI?$NAME_PARAM=$encodedName&$LATITUDE_PARAM=${point.latitude}&$LONGITUDE_PARAM=${point.longitude}"
    }

    fun isPlaceLink(content: String): Boolean =
        content.length <= MAX_CONTENT_LENGTH && content.startsWith("$PLACE_URI?")

    fun fromParams(name: String, latitude: String, longitude: String): SharedPlace? {
        val validName = name.takeIf { it.isValidName() } ?: return null
        val lat = latitude.toCoordinate(90.0) ?: return null
        val lon = longitude.toCoordinate(180.0) ?: return null
        return SharedPlace(validName, GeoPoint(lat, lon))
    }

    private fun String.isValidName(): Boolean =
        isNotBlank() && length <= MAX_NAME_LENGTH && none { Character.getType(it) in forbiddenCharTypes }

    private fun String.toCoordinate(limit: Double): Double? =
        toDoubleOrNull()?.takeIf { it.isFinite() && it in -limit..limit }
}