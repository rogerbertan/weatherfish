package dev.rogerbertan.weatherfish.ui.search

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.rogerbertan.weatherfish.domain.ActiveLocation
import dev.rogerbertan.weatherfish.domain.Place
import dev.rogerbertan.weatherfish.domain.UserPreferences
import dev.rogerbertan.weatherfish.domain.WeatherRepository
import dev.rogerbertan.weatherfish.ui.common.LoadState
import dev.rogerbertan.weatherfish.ui.common.loadState
import dev.rogerbertan.weatherfish.ui.common.map
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchResult(val place: Place, val isFavorite: Boolean)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: WeatherRepository,
    private val activeLocation: ActiveLocation,
    private val preferences: UserPreferences,
) : ViewModel() {

    val query = TextFieldState()

    private val attempts = MutableStateFlow(0)
    private val results = MutableStateFlow<LoadState<List<Place>>>(LoadState.Loaded(emptyList()))

    val uiState: StateFlow<LoadState<List<SearchResult>>> =
        combine(results, preferences.favorites) { results, favorites ->
            val favoritePoints = favorites.mapTo(HashSet()) { it.point }
            results.map { places ->
                places.map { place -> SearchResult(place, isFavorite = place.point in favoritePoints) }
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, LoadState.Loaded(emptyList()))

    init {
        viewModelScope.launch {
            combine(snapshotFlow { currentTerm() }, attempts) { term, _ -> term }
                .onEach { term -> results.value = if (term.isEmpty()) LoadState.Loaded(emptyList()) else LoadState.Loading }
                .debounce { term -> if (term.isEmpty()) Duration.ZERO else 300.milliseconds }
                .mapLatest { term -> term to search(term) }
                .filter { (term, _) -> term == currentTerm() }
                .collect { (_, state) -> results.value = state }
        }
    }

    fun retry() = attempts.update { it + 1 }

    fun select(place: Place) = activeLocation.select(place)

    fun toggleFavorite(result: SearchResult) {
        viewModelScope.launch {
            if (result.isFavorite) preferences.removeFavorite(result.place) else preferences.addFavorite(result.place)
        }
    }

    private fun currentTerm() = query.text.trim().toString()

    private suspend fun search(term: String): LoadState<List<Place>> =
        if (term.isEmpty()) LoadState.Loaded(emptyList()) else loadState { repository.searchPlaces(term) }
}
