package dev.rogerbertan.weatherfish.domain

sealed class WeatherError : Exception() {
    data object Network : WeatherError()
    data object Unauthorized : WeatherError()
    data object NotFound : WeatherError()
    data class Unknown(override val message: String) : WeatherError()
}