package dev.rogerbertan.weatherfish.ui.search

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshots.Snapshot
import dev.rogerbertan.weatherfish.domain.ActiveLocation
import dev.rogerbertan.weatherfish.domain.UserPreferences
import dev.rogerbertan.weatherfish.domain.WeatherError
import dev.rogerbertan.weatherfish.domain.WeatherRepository
import dev.rogerbertan.weatherfish.ui.MainDispatcherExtension
import dev.rogerbertan.weatherfish.ui.common.LoadState
import dev.rogerbertan.weatherfish.ui.testPlace
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
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
    private val activeLocation = mockk<ActiveLocation>(relaxUnitFun = true)
    private val preferences = mockk<UserPreferences>(relaxUnitFun = true) {
        every { favorites } returns flowOf(emptyList())
    }

    private fun viewModel() = SearchViewModel(repository, activeLocation, preferences)

    private fun SearchViewModel.type(text: String) {
        query.setTextAndPlaceCursorAtEnd(text)
        Snapshot.sendApplyNotifications()
    }

    private val found = LoadState.Loaded(listOf(SearchResult(testPlace, isFavorite = false)))

    @Test
    fun `should show empty results without loading when screen opens`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        val viewModel = viewModel()

        // Act
        advanceUntilIdle()

        // Assert
        assertEquals(LoadState.Loaded(emptyList<Nothing>()), viewModel.uiState.value)
        coVerify(exactly = 0) { repository.searchPlaces(any()) }
    }

    @Test
    fun `should show loading immediately while user types`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        val viewModel = viewModel()
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
        val viewModel = viewModel()
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
        assertEquals(found, viewModel.uiState.value)
    }

    @Test
    fun `should not search again when user only adds spaces`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        val viewModel = viewModel()
        viewModel.type("Porto")
        advanceUntilIdle()

        // Act
        viewModel.type("Porto ")
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { repository.searchPlaces(any()) }
        assertEquals(found, viewModel.uiState.value)
    }

    @Test
    fun `should clear results without loading when query becomes blank`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        val viewModel = viewModel()
        viewModel.type("Porto")
        advanceUntilIdle()

        // Act
        viewModel.type("   ")
        runCurrent()

        // Assert
        assertEquals(LoadState.Loaded(emptyList<Nothing>()), viewModel.uiState.value)
    }

    @Test
    fun `should show results after retry when previous search failed`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        coEvery { repository.searchPlaces("Porto") } throws WeatherError.Network andThen listOf(testPlace)
        val viewModel = viewModel()
        viewModel.type("Porto")
        advanceUntilIdle()

        // Act
        viewModel.retry()
        advanceUntilIdle()

        // Assert
        assertEquals(found, viewModel.uiState.value)
    }

    @Test
    fun `should mark favorite results`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        every { preferences.favorites } returns flowOf(listOf(testPlace))
        val viewModel = viewModel()

        // Act
        viewModel.type("Porto")
        advanceUntilIdle()

        // Assert
        assertEquals(LoadState.Loaded(listOf(SearchResult(testPlace, isFavorite = true))), viewModel.uiState.value)
    }

    @Test
    fun `should select place as active location`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        val viewModel = viewModel()

        // Act
        viewModel.select(testPlace)

        // Assert
        verify { activeLocation.select(testPlace) }
    }

    @Test
    fun `should add result to favorites when toggled`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        val viewModel = viewModel()

        // Act
        viewModel.toggleFavorite(SearchResult(testPlace, isFavorite = false))
        advanceUntilIdle()

        // Assert
        coVerify { preferences.addFavorite(testPlace) }
    }
}
