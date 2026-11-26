package dev.rogerbertan.weatherfish.ui.share

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.rogerbertan.weatherfish.domain.PlaceQrCodec
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CameraScanUiState(
    val isAnalyzing: Boolean = true,
    val message: ShareMessage? = null,
    val scannedLink: String? = null,
    val isCameraUnavailable: Boolean = false,
)

@HiltViewModel
class CameraScanViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(CameraScanUiState())
    val uiState: StateFlow<CameraScanUiState> = _uiState.asStateFlow()

    private var candidate: String? = null
    private var streak = 0

    fun onCodesDetected(codes: List<String>) {
        if (!_uiState.value.isAnalyzing) return
        val placeLink = codes.firstOrNull(PlaceQrCodec::isPlaceLink)
        val code = placeLink ?: codes.firstOrNull()
        streak = if (code != null && code == candidate) streak + 1 else 1
        candidate = code
        if (code == null || streak != CONFIRMATION_FRAMES) return
        if (placeLink != null) {
            _uiState.value = CameraScanUiState(isAnalyzing = false, scannedLink = code)
        } else {
            _uiState.update { it.copy(message = ShareMessage.InvalidCode) }
        }
    }

    fun onCameraUnavailable() {
        _uiState.update { it.copy(isAnalyzing = false, isCameraUnavailable = true) }
    }

    fun messageShown() {
        _uiState.update { it.copy(message = null) }
    }

    fun linkOpened() {
        _uiState.update { it.copy(scannedLink = null) }
    }

    companion object {
        const val CONFIRMATION_FRAMES = 3
    }
}
