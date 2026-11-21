package dev.rogerbertan.weatherfish.domain

interface WeatherRepository {
    suspend fun currentWeather(point: GeoPoint): CurrentWeather
    suspend fun hourlyForecast(point: GeoPoint, hours: Int): List<HourlyForecast>
    suspend fun dailyForecast(point: GeoPoint, days: Int): List<DailyForecast>
    suspend fun searchPlaces(query: String): List<Place>
}