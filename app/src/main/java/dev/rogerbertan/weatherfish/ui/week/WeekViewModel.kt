package dev.rogerbertan.weatherfish.ui.week

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.rogerbertan.weatherfish.domain.ActiveLocation
import dev.rogerbertan.weatherfish.domain.DailyForecast
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

data class WeekUiState(val days: List<DailyForecast>, val unit: TemperatureUnit)

@HiltViewModel
class WeekViewModel @Inject constructor(
    private val repository: WeatherRepository,
    private val activeLocation: ActiveLocation,
    preferences: UserPreferences,
) : ViewModel() {

    private val attempts = MutableStateFlow(0)
    private val days = MutableStateFlow<LoadState<List<DailyForecast>>>(LoadState.Loading)

    val uiState: StateFlow<LoadState<WeekUiState>> =
        combine(days, preferences.temperatureUnit) { days, unit -> days.map { WeekUiState(it, unit) } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, LoadState.Loading)

    init {
        viewModelScope.launch {
            combine(activeLocation.place, attempts) { place, _ -> place }.collectLatest { place ->
                days.value = LoadState.Loading
                days.value = loadState { repository.dailyForecast(place.point, days = 7) }
            }
        }
    }

    fun retry() = attempts.update { it + 1 }
}
