package dev.rogerbertan.weatherfish.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.SystemClock
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.rogerbertan.weatherfish.domain.GeoPoint
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

class FusedLocationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : LocationProvider {

    private val client = LocationServices.getFusedLocationProviderClient(context)

    override suspend fun currentLocation(): GeoPoint? {
        val permission = context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (permission != PackageManager.PERMISSION_GRANTED) return null
        val location = try {
            val last = client.lastLocation.await()
            if (last != null && last.age() <= MAX_LOCATION_AGE) {
                last
            } else {
                withTimeoutOrNull(FRESH_LOCATION_TIMEOUT) {
                    val cancellation = CancellationTokenSource()
                    client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellation.token)
                        .await(cancellation)
                } ?: last
            }
        } catch (e: ApiException) {
            null
        } catch (e: SecurityException) {
            null
        }
        return location?.let { GeoPoint(it.latitude, it.longitude) }
    }

    private fun Location.age(): Duration = (SystemClock.elapsedRealtimeNanos() - elapsedRealtimeNanos).nanoseconds

    private companion object {
        val MAX_LOCATION_AGE = 10.minutes
        val FRESH_LOCATION_TIMEOUT = 10.seconds
    }
}
