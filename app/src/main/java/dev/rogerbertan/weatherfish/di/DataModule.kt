package dev.rogerbertan.weatherfish.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.rogerbertan.weatherfish.data.FixedActiveLocation
import dev.rogerbertan.weatherfish.data.fake.FakeConfig
import dev.rogerbertan.weatherfish.data.fake.FakeWeatherRepository
import dev.rogerbertan.weatherfish.data.xweather.XweatherWeatherRepository
import dev.rogerbertan.weatherfish.domain.ActiveLocation
import dev.rogerbertan.weatherfish.domain.WeatherRepository
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

data class DataSourceConfig(val useFake: Boolean)

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun activeLocation(impl: FixedActiveLocation): ActiveLocation

    companion object {
        @Provides
        fun dataSourceConfig() = DataSourceConfig(useFake = false)

        @Provides
        fun fakeConfig() = FakeConfig(latency = 800.milliseconds, simulatedError = null)

        @Provides
        @Singleton
        fun weatherRepository(
            config: DataSourceConfig,
            fake: Provider<FakeWeatherRepository>,
            xweather: Provider<XweatherWeatherRepository>,
        ): WeatherRepository = if (config.useFake) fake.get() else xweather.get()
    }
}