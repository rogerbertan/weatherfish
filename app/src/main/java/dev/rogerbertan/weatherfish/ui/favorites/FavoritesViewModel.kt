package dev.rogerbertan.weatherfish.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.rogerbertan.weatherfish.domain.ActiveLocation
import dev.rogerbertan.weatherfish.domain.Place
import dev.rogerbertan.weatherfish.domain.TemperatureUnit
import dev.rogerbertan.weatherfish.domain.UserPreferences
import dev.rogerbertan.weatherfish.ui.common.LoadState
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FavoritesUiState(val favorites: List<Place>, val unit: TemperatureUnit)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val preferences: UserPreferences,
    private val activeLocation: ActiveLocation,
) : ViewModel() {

    val uiState: StateFlow<LoadState<FavoritesUiState>> =
        combine(preferences.favorites, preferences.temperatureUnit) { favorites, unit ->
            LoadState.Loaded(FavoritesUiState(favorites, unit))
        }.stateIn(viewModelScope, SharingStarted.Eagerly, LoadState.Loading)

    fun select(place: Place) = activeLocation.select(place)

    fun useCurrentLocation() = activeLocation.useCurrentLocation()

    fun remove(place: Place) {
        viewModelScope.launch { preferences.removeFavorite(place) }
    }

    fun setUnit(unit: TemperatureUnit) {
        viewModelScope.launch { preferences.setTemperatureUnit(unit) }
    }
}