package dev.rogerbertan.weatherfish.data

import dev.rogerbertan.weatherfish.data.location.LocationProvider
import dev.rogerbertan.weatherfish.data.settings.DataStoreUserPreferences
import dev.rogerbertan.weatherfish.domain.ActiveLocation
import dev.rogerbertan.weatherfish.domain.GeoPoint
import dev.rogerbertan.weatherfish.domain.Place
import dev.rogerbertan.weatherfish.domain.WeatherError
import dev.rogerbertan.weatherfish.domain.WeatherRepository
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

val defaultPlace = Place(
    name = "São Paulo",
    region = "São Paulo",
    country = "Brasil",
    point = GeoPoint(-23.5475, -46.63611),
    timeZone = ZoneId.of("America/Sao_Paulo"),
)

@Singleton
class DeviceActiveLocation @Inject constructor(
    private val locationProvider: LocationProvider,
    private val repository: WeatherRepository,
    private val preferences: DataStoreUserPreferences,
    @ApplicationScope private val scope: CoroutineScope,
) : ActiveLocation {

    private val active = MutableStateFlow<Place?>(null)
    private val locatingNow = MutableStateFlow(false)

    @Volatile
    private var locating: Job? = null

    override val place: Flow<Place> = flow {
        resolveOnce()
        emitAll(active.filterNotNull())
    }

    override val isLocating: Flow<Boolean> = locatingNow.asStateFlow()

    override fun select(place: Place) {
        locating?.cancel()
        activate(place)
    }

    override fun useCurrentLocation() {
        locating?.cancel()
        val job = scope.launch(start = CoroutineStart.LAZY) { currentPlace()?.let(::activate) }
        locating = job
        locatingNow.value = true
        job.invokeOnCompletion { if (locating === job) locatingNow.value = false }
        job.start()
    }

    private suspend fun resolveOnce() {
        if (active.value != null) return
        val fallback = preferences.lastPlace() ?: defaultPlace
        if (active.compareAndSet(null, fallback)) useCurrentLocation()
    }

    private suspend fun currentPlace(): Place? {
        val point = locationProvider.currentLocation() ?: return null
        return try {
            repository.placeAt(point)
        } catch (e: WeatherError) {
            null
        }
    }

    private fun activate(place: Place) {
        active.value = place
        persist(place)
    }

    private fun persist(place: Place) {
        scope.launch { preferences.saveLastPlace(place) }
    }
}
