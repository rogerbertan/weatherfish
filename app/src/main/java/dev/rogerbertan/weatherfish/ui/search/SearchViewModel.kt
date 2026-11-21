package dev.rogerbertan.weatherfish.ui.search

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.rogerbertan.weatherfish.domain.Place
import dev.rogerbertan.weatherfish.domain.WeatherRepository
import dev.rogerbertan.weatherfish.ui.common.LoadState
import dev.rogerbertan.weatherfish.ui.common.loadState
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: WeatherRepository,
) : ViewModel() {

    val query = TextFieldState()

    private val _uiState = MutableStateFlow<LoadState<List<Place>>>(LoadState.Loaded(emptyList()))
    val uiState: StateFlow<LoadState<List<Place>>> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            snapshotFlow { query.text.trim().toString() }.collect(::search)
        }
    }

    fun retry() = search(query.text.trim().toString())

    private fun search(term: String) {
        searchJob?.cancel()
        if (term.isEmpty()) {
            _uiState.value = LoadState.Loaded(emptyList())
            return
        }
        _uiState.value = LoadState.Loading
        searchJob = viewModelScope.launch {
            delay(300)
            _uiState.value = loadState { repository.searchPlaces(term) }
        }
    }
}