package dev.rogerbertan.weatherfish.data.location

import dev.rogerbertan.weatherfish.domain.GeoPoint

interface LocationProvider {
    suspend fun currentLocation(): GeoPoint?
}
