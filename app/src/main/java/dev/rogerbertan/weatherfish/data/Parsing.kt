package dev.rogerbertan.weatherfish.data

import dev.rogerbertan.weatherfish.domain.WeatherError
import java.time.DateTimeException

inline fun <T> parsingOrThrow(block: () -> T): T =
    try {
        block()
    } catch (e: IllegalArgumentException) {
        throw WeatherError.Unknown(e.message ?: "invalid response")
    } catch (e: DateTimeException) {
        throw WeatherError.Unknown(e.message ?: "invalid date")
    } catch (e: NullPointerException) {
        throw WeatherError.Unknown(e.message ?: "missing field")
    }
