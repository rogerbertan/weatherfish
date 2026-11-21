package dev.rogerbertan.weatherfish.domain

import kotlinx.coroutines.flow.Flow

interface ActiveLocation {
    val place: Flow<Place>
    val isLocating: Flow<Boolean>
    fun select(place: Place)
    fun useCurrentLocation()
}
