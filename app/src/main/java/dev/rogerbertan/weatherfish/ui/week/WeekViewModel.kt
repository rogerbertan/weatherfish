package dev.rogerbertan.weatherfish.ui.week

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.rogerbertan.weatherfish.domain.ActiveLocation
import dev.rogerbertan.weatherfish.domain.DailyForecast
import dev.rogerbertan.weatherfish.domain.WeatherRepository
import dev.rogerbertan.weatherfish.ui.common.LoadState
import dev.rogerbertan.weatherfish.ui.common.loadState
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel
class WeekViewModel @Inject constructor(
    private val repository: WeatherRepository,
    private val activeLocation: ActiveLocation,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoadState<List<DailyForecast>>>(LoadState.Loading)
    val uiState: StateFlow<LoadState<List<DailyForecast>>> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        viewModelScope.launch {
            _uiState.value = LoadState.Loading
            _uiState.value = loadState { repository.dailyForecast(activeLocation.place.first().point, days = 7) }
        }
    }
}