package dev.rogerbertan.weatherfish.ui.today

import dev.rogerbertan.weatherfish.domain.ActiveLocation
import dev.rogerbertan.weatherfish.domain.WeatherError
import dev.rogerbertan.weatherfish.domain.WeatherRepository
import dev.rogerbertan.weatherfish.ui.MainDispatcherExtension
import dev.rogerbertan.weatherfish.ui.common.LoadState
import dev.rogerbertan.weatherfish.ui.testCurrent
import dev.rogerbertan.weatherfish.ui.testHourly
import dev.rogerbertan.weatherfish.ui.testPlace
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class TodayViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val repository = mockk<WeatherRepository> {
        coEvery { currentWeather(testPlace.point) } returns testCurrent
        coEvery { hourlyForecast(testPlace.point, 24) } returns testHourly
    }
    private val activeLocation = mockk<ActiveLocation> {
        every { place } returns flowOf(testPlace)
    }

    private val loaded = LoadState.Loaded(TodayWeather(testPlace, testCurrent, testHourly))

    @Test
    fun `should start loading and then show weather for active location`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        val viewModel = TodayViewModel(repository, activeLocation)

        // Act
        val initial = viewModel.uiState.value
        advanceUntilIdle()

        // Assert
        assertEquals(LoadState.Loading, initial)
        assertEquals(loaded, viewModel.uiState.value)
    }

    @Test
    fun `should show error when hourly forecast fails`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        coEvery { repository.hourlyForecast(testPlace.point, 24) } throws WeatherError.Network

        // Act
        val viewModel = TodayViewModel(repository, activeLocation)
        advanceUntilIdle()

        // Assert
        assertEquals(LoadState.Failed(WeatherError.Network), viewModel.uiState.value)
    }

    @Test
    fun `should show weather after retry when previous load failed`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        coEvery { repository.currentWeather(testPlace.point) } throws WeatherError.Network andThen testCurrent
        val viewModel = TodayViewModel(repository, activeLocation)
        advanceUntilIdle()

        // Act
        viewModel.retry()
        advanceUntilIdle()

        // Assert
        assertEquals(loaded, viewModel.uiState.value)
    }
}