package dev.rogerbertan.weatherfish.data.location

import dev.rogerbertan.weatherfish.domain.GeoPoint

data class GeocodedName(val name: String, val region: String?, val country: String)

interface ReverseGeocoder {
    suspend fun nameAt(point: GeoPoint): GeocodedName?
}
