package dev.rogerbertan.weatherfish.data.xweather

import com.aerisweather.aeris.communication.AerisEngine
import com.aerisweather.aeris.logging.LogLevel
import dev.rogerbertan.weatherfish.BuildConfig
import dev.rogerbertan.weatherfish.domain.MapCredentials
import dev.rogerbertan.weatherfish.domain.MapCredentialsProvider

object XweatherSdk : MapCredentialsProvider {
    override val mapCredentials = credentialsOf(BuildConfig.XWEATHER_CLIENT_ID, BuildConfig.XWEATHER_CLIENT_SECRET)

    val isConfigured = mapCredentials != null

    fun init() {
        LogLevel.setLoggingLevel(LogLevel.NONE)
        AerisEngine.initWithKeys(
            BuildConfig.XWEATHER_CLIENT_ID,
            BuildConfig.XWEATHER_CLIENT_SECRET,
            BuildConfig.APPLICATION_ID,
        )
    }
}

internal fun credentialsOf(clientId: String, clientSecret: String): MapCredentials? =
    if (clientId.isNotBlank() && clientSecret.isNotBlank()) MapCredentials(clientId, clientSecret) else null
