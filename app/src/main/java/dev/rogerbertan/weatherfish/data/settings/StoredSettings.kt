package dev.rogerbertan.weatherfish.data.settings

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import dev.rogerbertan.weatherfish.domain.GeoPoint
import dev.rogerbertan.weatherfish.domain.Place
import dev.rogerbertan.weatherfish.domain.TemperatureUnit
import java.io.InputStream
import java.io.OutputStream
import java.time.ZoneId
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

@Serializable
data class StoredSettings(
    val favorites: List<StoredPlace> = emptyList(),
    val lastPlace: StoredPlace? = null,
    val temperatureUnit: TemperatureUnit = TemperatureUnit.Celsius,
)

@Serializable
data class StoredPlace(
    val name: String,
    val region: String?,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val timeZone: String,
)

fun Place.toStored() = StoredPlace(
    name = name,
    region = region,
    country = country,
    latitude = point.latitude,
    longitude = point.longitude,
    timeZone = timeZone.id,
)

fun StoredPlace.toDomain() = Place(
    name = name,
    region = region,
    country = country,
    point = GeoPoint(latitude, longitude),
    timeZone = ZoneId.of(timeZone),
)

private val settingsJson = Json { ignoreUnknownKeys = true }

object SettingsSerializer : Serializer<StoredSettings> {

    override val defaultValue = StoredSettings()

    override suspend fun readFrom(input: InputStream): StoredSettings =
        try {
            settingsJson.decodeFromString(StoredSettings.serializer(), input.readBytes().decodeToString())
        } catch (e: SerializationException) {
            throw CorruptionException("Unable to read settings", e)
        }

    override suspend fun writeTo(t: StoredSettings, output: OutputStream) {
        output.write(settingsJson.encodeToString(StoredSettings.serializer(), t).encodeToByteArray())
    }
}
