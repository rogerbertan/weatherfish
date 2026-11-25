package dev.rogerbertan.weatherfish.data.openmeteo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenMeteoForecastResponse(
    val timezone: String,
    val current: Current? = null,
    val hourly: Hourly? = null,
    val daily: Daily? = null,
)

@Serializable
data class Current(
    val time: Long,
    @SerialName("temperature_2m") val temperature: Double,
    @SerialName("apparent_temperature") val apparentTemperature: Double,
    @SerialName("relative_humidity_2m") val humidity: Int,
    @SerialName("dew_point_2m") val dewPoint: Double,
    @SerialName("wind_speed_10m") val windSpeed: Double,
    @SerialName("wind_direction_10m") val windDirection: Double,
    @SerialName("pressure_msl") val pressure: Double,
    val visibility: Double,
    @SerialName("uv_index") val uvIndex: Double,
    @SerialName("weather_code") val weatherCode: Int,
)

@Serializable
data class Hourly(
    val time: List<Long>,
    @SerialName("temperature_2m") val temperature: List<Double>,
    @SerialName("precipitation_probability") val precipitationProbability: List<Int?>,
    @SerialName("weather_code") val weatherCode: List<Int>,
)

@Serializable
data class Daily(
    val time: List<String>,
    @SerialName("temperature_2m_max") val maxTemperature: List<Double>,
    @SerialName("temperature_2m_min") val minTemperature: List<Double>,
    @SerialName("precipitation_probability_max") val precipitationProbability: List<Int?>,
    @SerialName("weather_code") val weatherCode: List<Int>,
)

@Serializable
data class GeocodingResponse(val results: List<GeocodingResult> = emptyList())

@Serializable
data class GeocodingResult(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val country: String = "",
    val admin1: String? = null,
)
