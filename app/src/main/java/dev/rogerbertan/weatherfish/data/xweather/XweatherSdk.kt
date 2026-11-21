package dev.rogerbertan.weatherfish.data.xweather

import com.aerisweather.aeris.communication.AerisEngine
import com.aerisweather.aeris.logging.LogLevel
import dev.rogerbertan.weatherfish.BuildConfig

object XweatherSdk {
    val isConfigured = BuildConfig.XWEATHER_CLIENT_ID.isNotBlank() && BuildConfig.XWEATHER_CLIENT_SECRET.isNotBlank()

    fun init() {
        LogLevel.setLoggingLevel(LogLevel.NONE)
        AerisEngine.initWithKeys(
            BuildConfig.XWEATHER_CLIENT_ID,
            BuildConfig.XWEATHER_CLIENT_SECRET,
            BuildConfig.APPLICATION_ID,
        )
    }
}