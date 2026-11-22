package dev.rogerbertan.weatherfish.ui.share

import dev.rogerbertan.weatherfish.ui.MainDispatcherExtension
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class CameraScanViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val viewModel = CameraScanViewModel()

    private fun CameraScanViewModel.detect(vararg codes: String, frames: Int = 1) {
        repeat(frames) { onCodesDetected(codes.toList()) }
    }

    @Test
    fun `should open place link after confirmation frames`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        val frames = CameraScanViewModel.CONFIRMATION_FRAMES

        // Act
        viewModel.detect(PLACE_LINK, frames = frames)

        // Assert
        assertEquals(PLACE_LINK, viewModel.uiState.value.scannedLink)
    }

    @Test
    fun `should not open place link before confirmation frames`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        val frames = CameraScanViewModel.CONFIRMATION_FRAMES - 1

        // Act
        viewModel.detect(PLACE_LINK, frames = frames)

        // Assert
        assertNull(viewModel.uiState.value.scannedLink)
    }

    @Test
    fun `should restart confirmation when a frame has no code`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        viewModel.detect(PLACE_LINK, frames = CameraScanViewModel.CONFIRMATION_FRAMES - 1)

        // Act
        viewModel.detect()
        viewModel.detect(PLACE_LINK)

        // Assert
        assertNull(viewModel.uiState.value.scannedLink)
    }

    @Test
    fun `should restart confirmation when code changes`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        viewModel.detect(PLACE_LINK, frames = CameraScanViewModel.CONFIRMATION_FRAMES - 1)

        // Act
        viewModel.detect(OTHER_PLACE_LINK)

        // Assert
        assertNull(viewModel.uiState.value.scannedLink)
    }

    @Test
    fun `should stop analyzing after reading place link`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        val frames = CameraScanViewModel.CONFIRMATION_FRAMES

        // Act
        viewModel.detect(PLACE_LINK, frames = frames)

        // Assert
        assertFalse(viewModel.uiState.value.isAnalyzing)
    }

    @Test
    fun `should ignore frames after reading place link`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        viewModel.detect(PLACE_LINK, frames = CameraScanViewModel.CONFIRMATION_FRAMES)

        // Act
        viewModel.detect(OTHER_PLACE_LINK, frames = CameraScanViewModel.CONFIRMATION_FRAMES)

        // Assert
        assertEquals(PLACE_LINK, viewModel.uiState.value.scannedLink)
    }

    @Test
    fun `should prefer place link when frame has several codes`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        val frames = CameraScanViewModel.CONFIRMATION_FRAMES

        // Act
        viewModel.detect(OTHER_CODE, PLACE_LINK, frames = frames)

        // Assert
        assertEquals(PLACE_LINK, viewModel.uiState.value.scannedLink)
    }

    @Test
    fun `should show invalid code when confirmed code is not a place link`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        val frames = CameraScanViewModel.CONFIRMATION_FRAMES

        // Act
        viewModel.detect(OTHER_CODE, frames = frames)

        // Assert
        assertEquals(CameraScanUiState(isAnalyzing = true, message = ShareMessage.InvalidCode), viewModel.uiState.value)
    }

    @Test
    fun `should show invalid code once while the same code stays in view`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        viewModel.detect(OTHER_CODE, frames = CameraScanViewModel.CONFIRMATION_FRAMES)
        viewModel.messageShown()

        // Act
        viewModel.detect(OTHER_CODE, frames = CameraScanViewModel.CONFIRMATION_FRAMES)

        // Assert
        assertNull(viewModel.uiState.value.message)
    }

    @Test
    fun `should show invalid code again when the code comes back into view`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        viewModel.detect(OTHER_CODE, frames = CameraScanViewModel.CONFIRMATION_FRAMES)
        viewModel.messageShown()
        viewModel.detect()

        // Act
        viewModel.detect(OTHER_CODE, frames = CameraScanViewModel.CONFIRMATION_FRAMES)

        // Assert
        assertEquals(ShareMessage.InvalidCode, viewModel.uiState.value.message)
    }

    @Test
    fun `should clear scanned link when link is opened`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        viewModel.detect(PLACE_LINK, frames = CameraScanViewModel.CONFIRMATION_FRAMES)

        // Act
        viewModel.linkOpened()

        // Assert
        assertEquals(CameraScanUiState(isAnalyzing = false), viewModel.uiState.value)
    }

    @Test
    fun `should stop analyzing when camera is unavailable`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        val expected = CameraScanUiState(isAnalyzing = false, isCameraUnavailable = true)

        // Act
        viewModel.onCameraUnavailable()

        // Assert
        assertEquals(expected, viewModel.uiState.value)
    }

    @Test
    fun `should ignore frames when camera is unavailable`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        viewModel.onCameraUnavailable()

        // Act
        viewModel.detect(PLACE_LINK, frames = CameraScanViewModel.CONFIRMATION_FRAMES)

        // Assert
        assertNull(viewModel.uiState.value.scannedLink)
    }

    private companion object {
        const val PLACE_LINK = "weatherfish://place?name=Curitiba%20Centro&lat=-25.4284&lon=-49.2733"
        const val OTHER_PLACE_LINK = "weatherfish://place?name=Porto%20Alegre&lat=-30.0&lon=-51.2"
        const val OTHER_CODE = "https://example.com"
    }
}
