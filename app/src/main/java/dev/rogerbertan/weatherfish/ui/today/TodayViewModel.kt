package dev.rogerbertan.weatherfish.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.rogerbertan.weatherfish.domain.ActiveLocation
import dev.rogerbertan.weatherfish.domain.CurrentWeather
import dev.rogerbertan.weatherfish.domain.HourlyForecast
import dev.rogerbertan.weatherfish.domain.Place
import dev.rogerbertan.weatherfish.domain.WeatherRepository
import dev.rogerbertan.weatherfish.ui.common.LoadState
import dev.rogerbertan.weatherfish.ui.common.loadState
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class TodayWeather(
    val place: Place,
    val current: CurrentWeather,
    val hourly: List<HourlyForecast>,
)

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val repository: WeatherRepository,
    private val activeLocation: ActiveLocation,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoadState<TodayWeather>>(LoadState.Loading)
    val uiState: StateFlow<LoadState<TodayWeather>> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        viewModelScope.launch {
            _uiState.value = LoadState.Loading
            _uiState.value = loadState { fetch(activeLocation.place.first()) }
        }
    }

    private suspend fun fetch(place: Place): TodayWeather = coroutineScope {
        val current = async { repository.currentWeather(place.point) }
        val hourly = async { repository.hourlyForecast(place.point, hours = 24) }
        TodayWeather(place, current.await(), hourly.await())
    }
}