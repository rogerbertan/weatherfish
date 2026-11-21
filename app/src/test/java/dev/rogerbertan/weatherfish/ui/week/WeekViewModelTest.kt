package dev.rogerbertan.weatherfish.ui.week

import dev.rogerbertan.weatherfish.domain.ActiveLocation
import dev.rogerbertan.weatherfish.domain.TemperatureUnit
import dev.rogerbertan.weatherfish.domain.UserPreferences
import dev.rogerbertan.weatherfish.domain.WeatherRepository
import dev.rogerbertan.weatherfish.ui.MainDispatcherExtension
import dev.rogerbertan.weatherfish.ui.common.LoadState
import dev.rogerbertan.weatherfish.ui.otherPlace
import dev.rogerbertan.weatherfish.ui.testPlace
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class WeekViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val repository = mockk<WeatherRepository> {
        coEvery { dailyForecast(any(), 7) } returns emptyList()
    }
    private val place = MutableStateFlow(testPlace)
    private val activeLocation = mockk<ActiveLocation> {
        every { place } returns this@WeekViewModelTest.place
    }
    private val preferences = mockk<UserPreferences> {
        every { temperatureUnit } returns flowOf(TemperatureUnit.Fahrenheit)
    }

    @Test
    fun `should show seven day forecast with temperature unit from preferences`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        val viewModel = WeekViewModel(repository, activeLocation, preferences)

        // Act
        advanceUntilIdle()

        // Assert
        assertEquals(LoadState.Loaded(WeekUiState(emptyList(), TemperatureUnit.Fahrenheit)), viewModel.uiState.value)
    }

    @Test
    fun `should reload forecast when active location changes`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        WeekViewModel(repository, activeLocation, preferences)
        advanceUntilIdle()

        // Act
        place.value = otherPlace
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { repository.dailyForecast(otherPlace.point, 7) }
    }
}
