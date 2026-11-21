package dev.rogerbertan.weatherfish.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.rogerbertan.weatherfish.domain.ActiveLocation
import dev.rogerbertan.weatherfish.domain.CurrentWeather
import dev.rogerbertan.weatherfish.domain.TemperatureUnit
import dev.rogerbertan.weatherfish.domain.UserPreferences
import dev.rogerbertan.weatherfish.domain.WeatherRepository
import dev.rogerbertan.weatherfish.ui.common.LoadState
import dev.rogerbertan.weatherfish.ui.common.loadState
import dev.rogerbertan.weatherfish.ui.common.map
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailsUiState(val current: CurrentWeather, val unit: TemperatureUnit)

@HiltViewModel
class DetailsViewModel @Inject constructor(
    private val repository: WeatherRepository,
    private val activeLocation: ActiveLocation,
    preferences: UserPreferences,
) : ViewModel() {

    private val attempts = MutableStateFlow(0)
    private val current = MutableStateFlow<LoadState<CurrentWeather>>(LoadState.Loading)

    val uiState: StateFlow<LoadState<DetailsUiState>> =
        combine(current, preferences.temperatureUnit) { current, unit -> current.map { DetailsUiState(it, unit) } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, LoadState.Loading)

    init {
        viewModelScope.launch {
            combine(activeLocation.place, attempts) { place, _ -> place }.collectLatest { place ->
                current.value = LoadState.Loading
                current.value = loadState { repository.currentWeather(place.point) }
            }
        }
    }

    fun retry() = attempts.update { it + 1 }
}
