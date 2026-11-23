package dev.rogerbertan.weatherfish.domain

data class MapCredentials(val clientId: String, val clientSecret: String)

interface MapCredentialsProvider {
    val mapCredentials: MapCredentials?
}
