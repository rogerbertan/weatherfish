package dev.rogerbertan.weatherfish.data.rest

import dev.rogerbertan.weatherfish.data.parsingOrThrow
import dev.rogerbertan.weatherfish.domain.WeatherError
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import java.io.IOException

data class TextResponse(val status: HttpStatusCode, val headers: Headers, val body: String)

suspend fun HttpClient.getText(url: String, block: HttpRequestBuilder.() -> Unit = {}): TextResponse =
    try {
        val response = get(url, block)
        TextResponse(response.status, response.headers, response.bodyAsText())
    } catch (e: IOException) {
        throw WeatherError.Network
    }

inline fun <T> TextResponse.parseOrThrow(
    errorInBody: (String) -> WeatherError? = { null },
    parse: (String) -> T,
): T {
    if (!status.isSuccess()) throw errorInBody(body) ?: errorForStatus(status)
    return parsingOrThrow { parse(body) }
}

fun errorForStatus(status: HttpStatusCode): WeatherError = when (status) {
    HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> WeatherError.Unauthorized
    HttpStatusCode.TooManyRequests -> WeatherError.QuotaExceeded
    HttpStatusCode.NotFound -> WeatherError.NotFound
    else -> WeatherError.Unknown("http ${status.value}")
}