package dev.rogerbertan.weatherfish.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.rogerbertan.weatherfish.domain.ActiveLocation
import dev.rogerbertan.weatherfish.domain.GeoPoint
import dev.rogerbertan.weatherfish.domain.MapCredentials
import dev.rogerbertan.weatherfish.domain.MapCredentialsProvider
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class MapUiState(
    val credentials: MapCredentials?,
    val center: GeoPoint?,
    val showWind: Boolean,
)

@HiltViewModel
class MapViewModel @Inject constructor(
    activeLocation: ActiveLocation,
    credentialsProvider: MapCredentialsProvider,
) : ViewModel() {

    private val credentials = credentialsProvider.mapCredentials
    private val showWind = MutableStateFlow(false)

    val uiState: StateFlow<MapUiState> = combine(activeLocation.place, showWind) { place, wind ->
        MapUiState(credentials, place.point, wind)
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        MapUiState(credentials, center = null, showWind = showWind.value),
    )

    fun toggleWind() {
        showWind.update { !it }
    }
}
