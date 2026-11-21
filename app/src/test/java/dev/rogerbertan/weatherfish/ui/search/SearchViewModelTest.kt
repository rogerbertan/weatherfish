package dev.rogerbertan.weatherfish.ui.search

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshots.Snapshot
import dev.rogerbertan.weatherfish.domain.WeatherRepository
import dev.rogerbertan.weatherfish.ui.MainDispatcherExtension
import dev.rogerbertan.weatherfish.ui.common.LoadState
import dev.rogerbertan.weatherfish.ui.testPlace
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class SearchViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val repository = mockk<WeatherRepository> {
        coEvery { searchPlaces(any()) } returns listOf(testPlace)
    }

    private fun SearchViewModel.type(text: String) {
        query.setTextAndPlaceCursorAtEnd(text)
        Snapshot.sendApplyNotifications()
    }

    @Test
    fun `should show empty results without loading when screen opens`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        val viewModel = SearchViewModel(repository)

        // Act
        advanceUntilIdle()

        // Assert
        assertEquals(LoadState.Loaded(emptyList<Nothing>()), viewModel.uiState.value)
        coVerify(exactly = 0) { repository.searchPlaces(any()) }
    }

    @Test
    fun `should show loading immediately while user types`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        val viewModel = SearchViewModel(repository)
        runCurrent()

        // Act
        viewModel.type("Porto")
        runCurrent()

        // Assert
        assertEquals(LoadState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `should search only the last query typed within debounce window`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        val viewModel = SearchViewModel(repository)
        runCurrent()

        // Act
        viewModel.type("Po")
        runCurrent()
        advanceTimeBy(100)
        viewModel.type("Porto")
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { repository.searchPlaces("Po") }
        coVerify(exactly = 1) { repository.searchPlaces("Porto") }
        assertEquals(LoadState.Loaded(listOf(testPlace)), viewModel.uiState.value)
    }

    @Test
    fun `should not search again when user only adds spaces`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        val viewModel = SearchViewModel(repository)
        viewModel.type("Porto")
        advanceUntilIdle()

        // Act
        viewModel.type("Porto ")
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { repository.searchPlaces(any()) }
        assertEquals(LoadState.Loaded(listOf(testPlace)), viewModel.uiState.value)
    }

    @Test
    fun `should clear results without loading when query becomes blank`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        val viewModel = SearchViewModel(repository)
        viewModel.type("Porto")
        advanceUntilIdle()

        // Act
        viewModel.type("   ")
        runCurrent()

        // Assert
        assertEquals(LoadState.Loaded(emptyList<Nothing>()), viewModel.uiState.value)
    }
}