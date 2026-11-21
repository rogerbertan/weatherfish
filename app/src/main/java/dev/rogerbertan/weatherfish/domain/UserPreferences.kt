package dev.rogerbertan.weatherfish.domain

import kotlinx.coroutines.flow.Flow

interface UserPreferences {
    val favorites: Flow<List<Place>>
    val temperatureUnit: Flow<TemperatureUnit>
    suspend fun addFavorite(place: Place)
    suspend fun removeFavorite(place: Place)
    suspend fun setTemperatureUnit(unit: TemperatureUnit)
}
