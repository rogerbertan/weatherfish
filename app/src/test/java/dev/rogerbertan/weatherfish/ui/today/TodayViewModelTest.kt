package dev.rogerbertan.weatherfish.ui.today

import dev.rogerbertan.weatherfish.domain.ActiveLocation
import dev.rogerbertan.weatherfish.domain.TemperatureUnit
import dev.rogerbertan.weatherfish.domain.UserPreferences
import dev.rogerbertan.weatherfish.domain.WeatherError
import dev.rogerbertan.weatherfish.domain.WeatherRepository
import dev.rogerbertan.weatherfish.ui.MainDispatcherExtension
import dev.rogerbertan.weatherfish.ui.common.LoadState
import dev.rogerbertan.weatherfish.ui.otherPlace
import dev.rogerbertan.weatherfish.ui.testCurrent
import dev.rogerbertan.weatherfish.ui.testHourly
import dev.rogerbertan.weatherfish.ui.testPlace
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val activeLocation = mockk<ActiveLocation>(relaxUnitFun = true) {
        every { place } returns flowOf(testPlace)
        every { isLocating } returns flowOf(false)
    }
    private val preferences = mockk<UserPreferences>(relaxUnitFun = true) {
        every { temperatureUnit } returns flowOf(TemperatureUnit.Celsius)
        every { favorites } returns flowOf(emptyList())
    }

    private fun viewModel() = TodayViewModel(repository, activeLocation, preferences)

    private val loaded = LoadState.Loaded(
        TodayContent(TodayWeather(testPlace, testCurrent, testHourly), TemperatureUnit.Celsius, isFavorite = false),
    )

    @Test
    fun `should start loading and then show weather for active location`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        val viewModel = viewModel()

        // Act
        val initial = viewModel.uiState.value.content
        advanceUntilIdle()

        // Assert
        assertEquals(LoadState.Loading, initial)
        assertEquals(loaded, viewModel.uiState.value.content)
    }

    @Test
    fun `should show error when hourly forecast fails`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        coEvery { repository.hourlyForecast(testPlace.point, 24) } throws WeatherError.Network

        // Act
        val viewModel = viewModel()
        advanceUntilIdle()

        // Assert
        assertEquals(LoadState.Failed(WeatherError.Network), viewModel.uiState.value.content)
    }

    @Test
    fun `should show weather after retry when previous load failed`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        coEvery { repository.currentWeather(testPlace.point) } throws WeatherError.Network andThen testCurrent
        val viewModel = viewModel()
        advanceUntilIdle()

        // Act
        viewModel.retry()
        advanceUntilIdle()

        // Assert
        assertEquals(loaded, viewModel.uiState.value.content)
    }

    @Test
    fun `should reload weather when active location changes`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        val place = MutableStateFlow(testPlace)
        every { activeLocation.place } returns place
        coEvery { repository.currentWeather(otherPlace.point) } returns testCurrent
        coEvery { repository.hourlyForecast(otherPlace.point, 24) } returns testHourly
        val viewModel = viewModel()
        advanceUntilIdle()

        // Act
        place.value = otherPlace
        advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value.content as LoadState.Loaded
        assertEquals(otherPlace, state.value.weather.place)
    }

    @Test
    fun `should show locating indicator while active location is locating`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        every { activeLocation.isLocating } returns flowOf(true)

        // Act
        val viewModel = viewModel()
        advanceUntilIdle()

        // Assert
        assertEquals(true, viewModel.uiState.value.isLocating)
        assertEquals(loaded, viewModel.uiState.value.content)
    }

    @Test
    fun `should show temperature unit and favorite flag from preferences`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        every { preferences.temperatureUnit } returns flowOf(TemperatureUnit.Fahrenheit)
        every { preferences.favorites } returns flowOf(listOf(testPlace))

        // Act
        val viewModel = viewModel()
        advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value.content as LoadState.Loaded
        assertEquals(TemperatureUnit.Fahrenheit, state.value.unit)
        assertEquals(true, state.value.isFavorite)
    }

    @Test
    fun `should add active place to favorites when toggled`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        val viewModel = viewModel()
        advanceUntilIdle()

        // Act
        viewModel.toggleFavorite()
        advanceUntilIdle()

        // Assert
        coVerify { preferences.addFavorite(testPlace) }
    }

    @Test
    fun `should remove active place from favorites when toggled again`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        every { preferences.favorites } returns flowOf(listOf(testPlace))
        val viewModel = viewModel()
        advanceUntilIdle()

        // Act
        viewModel.toggleFavorite()
        advanceUntilIdle()

        // Assert
        coVerify { preferences.removeFavorite(testPlace) }
    }

    @Test
    fun `should use current location when permission is granted`() = runTest(mainDispatcher.dispatcher) {
        // Arrange
        val viewModel = viewModel()

        // Act
        viewModel.onLocationPermissionGranted()

        // Assert
        verify { activeLocation.useCurrentLocation() }
    }
}
