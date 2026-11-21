package dev.rogerbertan.weatherfish.data.xweather

import kotlinx.serialization.Serializable

@Serializable
data class XweatherResponse<T>(
    val success: Boolean,
    val error: XweatherError? = null,
    val response: List<T> = emptyList(),
)

@Serializable
data class XweatherError(val code: String, val description: String = "")

@Serializable
data class Loc(val lat: Double, val long: Double)

@Serializable
data class PlaceName(
    val name: String,
    val stateFull: String = "",
    val country: String = "",
    val countryFull: String = "",
)

@Serializable
data class Profile(val tz: String)

@Serializable
data class PlaceResponse(val loc: Loc, val place: PlaceName, val profile: Profile)

@Serializable
data class ConditionsResponse(val periods: List<ConditionsPeriod>)

@Serializable
data class ConditionsPeriod(
    val timestamp: Long,
    val tempC: Double,
    val feelslikeC: Double,
    val humidity: Int,
    val dewpointC: Double,
    val windSpeedKPH: Double,
    val windDir: String,
    val pressureMB: Double,
    val visibilityKM: Double,
    val uvi: Int,
    val weather: String,
    val weatherPrimaryCoded: String,
)

@Serializable
data class ForecastResponse(val periods: List<ForecastPeriod>)

@Serializable
data class ForecastPeriod(
    val timestamp: Long,
    val dateTimeISO: String,
    val tempC: Double? = null,
    val maxTempC: Double? = null,
    val minTempC: Double? = null,
    val pop: Int,
    val weather: String,
    val weatherPrimaryCoded: String,
)