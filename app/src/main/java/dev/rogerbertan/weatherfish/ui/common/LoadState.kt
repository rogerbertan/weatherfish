package dev.rogerbertan.weatherfish.ui.common

import dev.rogerbertan.weatherfish.domain.WeatherError

sealed interface LoadState<out T> {
    data object Loading : LoadState<Nothing>
    data class Failed(val error: WeatherError) : LoadState<Nothing>
    data class Loaded<T>(val value: T) : LoadState<T>
}

fun <T, R> LoadState<T>.map(transform: (T) -> R): LoadState<R> = when (this) {
    LoadState.Loading -> LoadState.Loading
    is LoadState.Failed -> this
    is LoadState.Loaded -> LoadState.Loaded(transform(value))
}

suspend fun <T> loadState(block: suspend () -> T): LoadState<T> =
    try {
        LoadState.Loaded(block())
    } catch (e: WeatherError) {
        LoadState.Failed(e)
    }