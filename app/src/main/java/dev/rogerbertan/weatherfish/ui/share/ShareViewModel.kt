package dev.rogerbertan.weatherfish.ui.share

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.rogerbertan.weatherfish.domain.ActiveLocation
import dev.rogerbertan.weatherfish.domain.PlaceQrCodec
import dev.rogerbertan.weatherfish.domain.QrScanException
import dev.rogerbertan.weatherfish.domain.QrScanner
import dev.rogerbertan.weatherfish.ui.DeepLinks
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SharedQr(val placeName: String, val content: String)

sealed interface ShareMessage {
    data object InvalidCode : ShareMessage
    data object ScanFailed : ShareMessage
}

data class ShareUiState(
    val qr: SharedQr?,
    val isScanning: Boolean,
    val message: ShareMessage?,
    val scannedLink: String?,
)

private data class ScanState(
    val isScanning: Boolean = false,
    val message: ShareMessage? = null,
    val scannedLink: String? = null,
)

@HiltViewModel
class ShareViewModel @Inject constructor(
    activeLocation: ActiveLocation,
    private val scanner: QrScanner,
) : ViewModel() {

    private val scanState = MutableStateFlow(ScanState())

    private val sharedQr = activeLocation.place.map { place ->
        SharedQr(place.name, DeepLinks.place(place.name, place.point))
    }

    val uiState: StateFlow<ShareUiState> = combine(sharedQr, scanState) { qr, scan ->
        ShareUiState(qr, scan.isScanning, scan.message, scan.scannedLink)
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        ShareUiState(qr = null, isScanning = false, message = null, scannedLink = null),
    )

    fun scan() {
        if (scanState.value.isScanning) return
        scanState.value = ScanState(isScanning = true)
        viewModelScope.launch {
            scanState.value = scanForLink()
        }
    }

    fun messageShown() {
        scanState.update { it.copy(message = null) }
    }

    fun linkOpened() {
        scanState.update { it.copy(scannedLink = null) }
    }

    private suspend fun scanForLink(): ScanState {
        val content = try {
            scanner.scan()
        } catch (e: QrScanException) {
            return ScanState(message = ShareMessage.ScanFailed)
        } ?: return ScanState()
        if (!PlaceQrCodec.isPlaceLink(content)) return ScanState(message = ShareMessage.InvalidCode)
        return ScanState(scannedLink = content)
    }
}