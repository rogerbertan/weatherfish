package dev.rogerbertan.weatherfish.data.settings

import androidx.datastore.core.DataStore
import dev.rogerbertan.weatherfish.domain.Place
import dev.rogerbertan.weatherfish.domain.TemperatureUnit
import dev.rogerbertan.weatherfish.domain.UserPreferences
import java.io.IOException
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen

class DataStoreUserPreferences @Inject constructor(
    private val dataStore: DataStore<StoredSettings>,
) : UserPreferences {

    private val settings: Flow<StoredSettings> = dataStore.data.retryWhen { cause, _ ->
        if (cause !is IOException) return@retryWhen false
        emit(StoredSettings())
        delay(READ_RETRY_DELAY)
        true
    }

    override val favorites: Flow<List<Place>> =
        settings.map { it.favorites }.distinctUntilChanged().map { stored -> stored.map { it.toDomain() } }

    override val temperatureUnit: Flow<TemperatureUnit> =
        settings.map { it.temperatureUnit }.distinctUntilChanged()

    override suspend fun addFavorite(place: Place) = update { settings ->
        if (settings.favorites.any { it.matches(place) }) settings
        else settings.copy(favorites = settings.favorites + place.toStored())
    }

    override suspend fun removeFavorite(place: Place) = update { settings ->
        settings.copy(favorites = settings.favorites.filterNot { it.matches(place) })
    }

    override suspend fun setTemperatureUnit(unit: TemperatureUnit) = update { it.copy(temperatureUnit = unit) }

    suspend fun lastPlace(): Place? = settings.first().lastPlace?.toDomain()

    suspend fun saveLastPlace(place: Place) = update { it.copy(lastPlace = place.toStored()) }

    private suspend fun update(transform: (StoredSettings) -> StoredSettings) {
        try {
            dataStore.updateData(transform)
        } catch (e: IOException) {
            return
        }
    }

    private fun StoredPlace.matches(place: Place) =
        latitude == place.point.latitude && longitude == place.point.longitude

    private companion object {
        val READ_RETRY_DELAY = 1.seconds
    }
}
