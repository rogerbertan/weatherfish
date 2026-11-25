package dev.rogerbertan.weatherfish.data.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.rogerbertan.weatherfish.domain.GeoPoint
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class AndroidReverseGeocoder @Inject constructor(
    @ApplicationContext private val context: Context,
) : ReverseGeocoder {

    override suspend fun nameAt(point: GeoPoint): GeocodedName? {
        if (!Geocoder.isPresent()) return null
        val address = suspendCancellableCoroutine<Address?> { continuation ->
            Geocoder(context, LOCALE).getFromLocation(
                point.latitude,
                point.longitude,
                1,
                object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        continuation.resume(addresses.firstOrNull())
                    }

                    override fun onError(errorMessage: String?) {
                        continuation.resume(null)
                    }
                },
            )
        } ?: return null
        val name = address.locality ?: address.subAdminArea ?: return null
        return GeocodedName(name, address.adminArea, address.countryName.orEmpty())
    }

    private companion object {
        val LOCALE: Locale = Locale.forLanguageTag("pt-BR")
    }
}
