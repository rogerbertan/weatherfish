package dev.rogerbertan.weatherfish.data

import dev.rogerbertan.weatherfish.domain.WeatherError
import dev.rogerbertan.weatherfish.domain.WeatherRepository
import dev.rogerbertan.weatherfish.ui.otherPlace
import dev.rogerbertan.weatherfish.ui.testCurrent
import dev.rogerbertan.weatherfish.ui.testPlace
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TestTimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CachingWeatherRepositoryTest {

    private val delegate = mockk<WeatherRepository> {
        coEvery { currentWeather(any()) } returns testCurrent
        coEvery { dailyForecast(any(), any()) } returns emptyList()
        coEvery { searchPlaces(any()) } returns listOf(testPlace)
    }
    private val time = TestTimeSource()

    private fun TestScope.repository() =
        CachingWeatherRepository(delegate, time, CoroutineScope(coroutineContext + SupervisorJob()))

    @Test
    fun `should reuse conditions within ttl`() = runTest {
        // Arrange
        val repository = repository()
        repository.currentWeather(testPlace.point)
        time += 9.minutes

        // Act
        val current = repository.currentWeather(testPlace.point)

        // Assert
        assertEquals(testCurrent, current)
        coVerify(exactly = 1) { delegate.currentWeather(testPlace.point) }
    }

    @Test
    fun `should fetch conditions again after ttl`() = runTest {
        // Arrange
        val repository = repository()
        repository.currentWeather(testPlace.point)
        time += 10.minutes

        // Act
        repository.currentWeather(testPlace.point)

        // Assert
        coVerify(exactly = 2) { delegate.currentWeather(testPlace.point) }
    }

    @Test
    fun `should reuse daily forecast for almost an hour`() = runTest {
        // Arrange
        val repository = repository()
        repository.dailyForecast(testPlace.point, days = 7)
        time += 59.minutes

        // Act
        repository.dailyForecast(testPlace.point, days = 7)

        // Assert
        coVerify(exactly = 1) { delegate.dailyForecast(testPlace.point, 7) }
    }

    @Test
    fun `should cache each point separately`() = runTest {
        // Arrange
        val repository = repository()
        repository.currentWeather(testPlace.point)

        // Act
        repository.currentWeather(otherPlace.point)

        // Assert
        coVerify(exactly = 1) { delegate.currentWeather(otherPlace.point) }
    }

    @Test
    fun `should share request in flight between concurrent callers`() = runTest {
        // Arrange
        coEvery { delegate.currentWeather(testPlace.point) } coAnswers {
            delay(1_000)
            testCurrent
        }
        val repository = repository()

        // Act
        val results = List(2) { async { repository.currentWeather(testPlace.point) } }.awaitAll()

        // Assert
        assertEquals(listOf(testCurrent, testCurrent), results)
        coVerify(exactly = 1) { delegate.currentWeather(testPlace.point) }
    }

    @Test
    fun `should not cache failures`() = runTest {
        // Arrange
        coEvery { delegate.currentWeather(testPlace.point) } throws WeatherError.Network andThen testCurrent
        val repository = repository()
        assertThrows<WeatherError> { repository.currentWeather(testPlace.point) }

        // Act
        val current = repository.currentWeather(testPlace.point)

        // Assert
        assertEquals(testCurrent, current)
    }

    @Test
    fun `should not cache place search`() = runTest {
        // Arrange
        val repository = repository()
        repository.searchPlaces("Porto")

        // Act
        repository.searchPlaces("Porto")

        // Assert
        coVerify(exactly = 2) { delegate.searchPlaces("Porto") }
    }
}
