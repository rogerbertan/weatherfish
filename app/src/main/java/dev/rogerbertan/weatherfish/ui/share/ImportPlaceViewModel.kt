package dev.rogerbertan.weatherfish.ui.share

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.rogerbertan.weatherfish.domain.PlaceQrCodec
import dev.rogerbertan.weatherfish.domain.SharedPlace
import dev.rogerbertan.weatherfish.domain.UserPreferences
import dev.rogerbertan.weatherfish.domain.WeatherRepository
import dev.rogerbertan.weatherfish.ui.ImportPlaceRoute
import dev.rogerbertan.weatherfish.ui.common.LoadState
import dev.rogerbertan.weatherfish.ui.common.loadState
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface ImportState {
    data class Confirm(val name: String) : ImportState
    data class Saving(val name: String) : ImportState
    data class Saved(val name: String, val location: String) : ImportState
    data class AlreadySaved(val name: String) : ImportState
    data object InvalidLink : ImportState
    data object Failed : ImportState
}

@HiltViewModel
class ImportPlaceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val preferences: UserPreferences,
    private val repository: WeatherRepository,
) : ViewModel() {

    private val shared = PlaceQrCodec.fromParams(
        name = savedStateHandle.get<String>(ImportPlaceRoute::name.name).orEmpty(),
        latitude = savedStateHandle.get<String>(ImportPlaceRoute::lat.name).orEmpty(),
        longitude = savedStateHandle.get<String>(ImportPlaceRoute::lon.name).orEmpty(),
    )
    private val state = MutableStateFlow(shared?.let { ImportState.Confirm(it.name) } ?: ImportState.InvalidLink)

    val uiState: StateFlow<ImportState> = state.asStateFlow()

    fun save() {
        val place = shared ?: return
        if (state.value is ImportState.Saving) return
        state.value = ImportState.Saving(place.name)
        viewModelScope.launch {
            state.value = importPlace(place)
        }
    }

    private suspend fun importPlace(shared: SharedPlace): ImportState {
        val existing = preferences.favorites.first().find { it.point == shared.point }
        if (existing != null) return ImportState.AlreadySaved(existing.name)
        val place = when (val result = loadState { repository.placeAt(shared.point) }) {
            is LoadState.Loaded -> result.value
            else -> return ImportState.Failed
        }
        preferences.addFavorite(place.copy(name = shared.name, point = shared.point))
        return ImportState.Saved(shared.name, listOfNotNull(place.region, place.country).joinToString(", "))
    }
}