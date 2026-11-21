package dev.rogerbertan.weatherfish.domain

import java.time.ZoneId

data class GeoPoint(val latitude: Double, val longitude: Double)

data class Place(
    val name: String,
    val region: String?,
    val country: String,
    val point: GeoPoint,
    val timeZone: ZoneId,
)