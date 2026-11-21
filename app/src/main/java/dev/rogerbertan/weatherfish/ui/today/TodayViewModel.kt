package dev.rogerbertan.weatherfish.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.rogerbertan.weatherfish.domain.ActiveLocation
import dev.rogerbertan.weatherfish.domain.CurrentWeather
import dev.rogerbertan.weatherfish.domain.HourlyForecast
import dev.rogerbertan.weatherfish.domain.Place
import dev.rogerbertan.weatherfish.domain.TemperatureUnit
import dev.rogerbertan.weatherfish.domain.UserPreferences
import dev.rogerbertan.weatherfish.domain.WeatherRepository
import dev.rogerbertan.weatherfish.ui.common.LoadState
import dev.rogerbertan.weatherfish.ui.common.loadState
import dev.rogerbertan.weatherfish.ui.common.map
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TodayWeather(
    val place: Place,
    val current: CurrentWeather,
    val hourly: List<HourlyForecast>,
)

data class TodayContent(
    val weather: TodayWeather,
    val unit: TemperatureUnit,
    val isFavorite: Boolean,
)

data class TodayUiState(
    val content: LoadState<TodayContent>,
    val isLocating: Boolean,
)

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val repository: WeatherRepository,
    private val activeLocation: ActiveLocation,
    private val preferences: UserPreferences,
) : ViewModel() {

    private val attempts = MutableStateFlow(0)
    private val weather = MutableStateFlow<LoadState<TodayWeather>>(LoadState.Loading)

    val uiState: StateFlow<TodayUiState> = combine(
        weather,
        preferences.temperatureUnit,
        preferences.favorites,
        activeLocation.isLocating,
    ) { weather, unit, favorites, isLocating ->
        val content = weather.map { loaded ->
            TodayContent(loaded, unit, isFavorite = favorites.any { it.point == loaded.place.point })
        }
        TodayUiState(content, isLocating)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, TodayUiState(LoadState.Loading, isLocating = false))

    init {
        viewModelScope.launch {
            combine(activeLocation.place, attempts) { place, _ -> place }.collectLatest { place ->
                weather.value = LoadState.Loading
                weather.value = loadState { fetch(place) }
            }
        }
    }

    fun retry() = attempts.update { it + 1 }

    fun toggleFavorite() {
        val state = (uiState.value.content as? LoadState.Loaded)?.value ?: return
        viewModelScope.launch {
            if (state.isFavorite) preferences.removeFavorite(state.weather.place)
            else preferences.addFavorite(state.weather.place)
        }
    }

    fun onLocationPermissionGranted() = activeLocation.useCurrentLocation()

    private suspend fun fetch(place: Place): TodayWeather = coroutineScope {
        val current = async { repository.currentWeather(place.point) }
        val hourly = async { repository.hourlyForecast(place.point, hours = 24) }
        TodayWeather(place, current.await(), hourly.await())
    }
}
