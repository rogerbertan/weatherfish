package dev.rogerbertan.weatherfish.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.rogerbertan.weatherfish.data.ApplicationScope
import dev.rogerbertan.weatherfish.data.CachingWeatherRepository
import dev.rogerbertan.weatherfish.data.DeviceActiveLocation
import dev.rogerbertan.weatherfish.data.fake.FakeConfig
import dev.rogerbertan.weatherfish.data.fake.FakeWeatherRepository
import dev.rogerbertan.weatherfish.data.location.FusedLocationProvider
import dev.rogerbertan.weatherfish.data.location.LocationProvider
import dev.rogerbertan.weatherfish.data.settings.DataStoreUserPreferences
import dev.rogerbertan.weatherfish.data.settings.SettingsSerializer
import dev.rogerbertan.weatherfish.data.settings.StoredSettings
import dev.rogerbertan.weatherfish.data.xweather.XweatherWeatherRepository
import dev.rogerbertan.weatherfish.domain.ActiveLocation
import dev.rogerbertan.weatherfish.domain.UserPreferences
import dev.rogerbertan.weatherfish.domain.WeatherRepository
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

data class DataSourceConfig(val useFake: Boolean)

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    abstract fun activeLocation(impl: DeviceActiveLocation): ActiveLocation

    @Binds
    abstract fun userPreferences(impl: DataStoreUserPreferences): UserPreferences

    @Binds
    abstract fun locationProvider(impl: FusedLocationProvider): LocationProvider

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
            @ApplicationScope scope: CoroutineScope,
        ): WeatherRepository = CachingWeatherRepository(
            delegate = if (config.useFake) fake.get() else xweather.get(),
            timeSource = TimeSource.Monotonic,
            scope = scope,
        )

        @Provides
        @Singleton
        @ApplicationScope
        fun applicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        @Provides
        @Singleton
        fun settingsDataStore(@ApplicationContext context: Context): DataStore<StoredSettings> =
            DataStoreFactory.create(
                serializer = SettingsSerializer,
                corruptionHandler = ReplaceFileCorruptionHandler { StoredSettings() },
                produceFile = { context.dataStoreFile("settings.json") },
            )
    }
}