package dev.rogerbertan.weatherfish.ui.share

import dev.rogerbertan.weatherfish.domain.ActiveLocation
import dev.rogerbertan.weatherfish.domain.QrScanException
import dev.rogerbertan.weatherfish.domain.QrScanner
import dev.rogerbertan.weatherfish.ui.MainDispatcherExtension
import dev.rogerbertan.weatherfish.ui.testPlace
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class ShareViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val activeLocation = mockk<ActiveLocation> {
        every { place } returns flowOf(testPlace)
    }
    private val scanner = mockk<QrScanner>()

    private fun viewModel() = ShareViewModel(activeLocation, scanner)

    @Test
    fun `should show place link of active place as qr`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        val viewModel = viewModel()

        // Act
        advanceUntilIdle()

        // Assert
        assertEquals(
            SharedQr("Porto Alegre", "weatherfish://place?name=Porto%20Alegre&lat=-30.0&lon=-51.2"),
            viewModel.uiState.value.qr,
        )
    }

    @Test
    fun `should open scanned content when qr is a place link`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        coEvery { scanner.scan() } returns "weatherfish://place?name=Curitiba%20Centro&lat=-25.4284&lon=-49.2733"
        val viewModel = viewModel()

        // Act
        viewModel.scan()
        advanceUntilIdle()

        // Assert
        assertEquals(
            "weatherfish://place?name=Curitiba%20Centro&lat=-25.4284&lon=-49.2733",
            viewModel.uiState.value.scannedLink,
        )
    }

    @Test
    fun `should show scanning while scanner is open`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        coEvery { scanner.scan() } coAnswers { CompletableDeferred<String?>().await() }
        val viewModel = viewModel()

        // Act
        viewModel.scan()
        runCurrent()

        // Assert
        assertTrue(viewModel.uiState.value.isScanning)
    }

    @Test
    fun `should stop scanning when scanner returns`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        val result = CompletableDeferred<String?>()
        coEvery { scanner.scan() } coAnswers { result.await() }
        val viewModel = viewModel()
        viewModel.scan()
        runCurrent()

        // Act
        result.complete(null)
        advanceUntilIdle()

        // Assert
        assertFalse(viewModel.uiState.value.isScanning)
    }

    @Test
    fun `should do nothing when scan is cancelled`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        coEvery { scanner.scan() } returns null
        val viewModel = viewModel()

        // Act
        viewModel.scan()
        advanceUntilIdle()

        // Assert
        assertNull(viewModel.uiState.value.scannedLink)
        assertNull(viewModel.uiState.value.message)
    }

    @Test
    fun `should show invalid code message when qr is not a weatherfish place`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        coEvery { scanner.scan() } returns "https://example.com"
        val viewModel = viewModel()

        // Act
        viewModel.scan()
        advanceUntilIdle()

        // Assert
        assertEquals(ShareMessage.InvalidCode, viewModel.uiState.value.message)
    }

    @Test
    fun `should show scan failed message when scanner fails`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        coEvery { scanner.scan() } throws QrScanException(IllegalStateException("module unavailable"))
        val viewModel = viewModel()

        // Act
        viewModel.scan()
        advanceUntilIdle()

        // Assert
        assertEquals(ShareMessage.ScanFailed, viewModel.uiState.value.message)
    }

    @Test
    fun `should clear message when shown`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        coEvery { scanner.scan() } returns "https://example.com"
        val viewModel = viewModel()
        viewModel.scan()
        advanceUntilIdle()

        // Act
        viewModel.messageShown()
        advanceUntilIdle()

        // Assert
        assertNull(viewModel.uiState.value.message)
    }

    @Test
    fun `should clear scanned link when opened`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        coEvery { scanner.scan() } returns "weatherfish://place?name=Curitiba&lat=-25.4&lon=-49.3"
        val viewModel = viewModel()
        viewModel.scan()
        advanceUntilIdle()

        // Act
        viewModel.linkOpened()
        advanceUntilIdle()

        // Assert
        assertNull(viewModel.uiState.value.scannedLink)
    }
}