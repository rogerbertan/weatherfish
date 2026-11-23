package dev.rogerbertan.weatherfish.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.DisposableMapEffect
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.style.MapStyle
import com.mapbox.maps.extension.compose.style.projection.generated.Projection
import com.xweather.mapsgl.config.weather.account.XweatherAccount
import com.xweather.mapsgl.map.mapbox.MapboxMapController
import com.xweather.mapsgl.weather.LayerCode
import dev.rogerbertan.weatherfish.domain.GeoPoint
import dev.rogerbertan.weatherfish.domain.MapCredentials

private const val PLACE_ZOOM = 6.0

@Composable
fun WeatherMapView(
    credentials: MapCredentials,
    center: GeoPoint?,
    showWind: Boolean,
    modifier: Modifier = Modifier,
) {
    val viewportState = rememberMapViewportState()
    var centeredOn by rememberSaveable { mutableStateOf<String?>(null) }
    var loadedController by remember { mutableStateOf<MapboxMapController?>(null) }

    MapboxMap(
        modifier = modifier,
        mapViewportState = viewportState,
        style = { MapStyle(style = Style.LIGHT, projection = Projection.MERCATOR) },
    ) {
        DisposableMapEffect(credentials) { mapView ->
            val controller = MapboxMapController(
                mapView,
                XweatherAccount(credentials.clientId, credentials.clientSecret),
            )
            val subscription = controller.mapboxMap?.subscribeMapLoaded {
                controller.addWeatherLayer(LayerCode.TEMPERATURES, null, controller.labelsLayerId())
                loadedController = controller
            }
            onDispose {
                subscription?.cancel()
                loadedController = null
                controller.shutdown()
            }
        }
        MapEffect(center) {
            if (center == null) return@MapEffect
            val key = "${center.latitude},${center.longitude}"
            if (key == centeredOn) return@MapEffect
            val camera = CameraOptions.Builder()
                .center(Point.fromLngLat(center.longitude, center.latitude))
                .zoom(PLACE_ZOOM)
                .build()
            if (centeredOn == null) viewportState.setCameraOptions(camera) else viewportState.flyTo(camera)
            centeredOn = key
        }
        MapEffect(loadedController, showWind) {
            val controller = loadedController ?: return@MapEffect
            val hasWind = controller.hasWeatherLayer(LayerCode.WIND_PARTICLES)
            if (showWind && !hasWind) controller.addWeatherLayer(LayerCode.WIND_PARTICLES, null, controller.labelsLayerId())
            if (!showWind && hasWind) controller.removeWeatherLayer(LayerCode.WIND_PARTICLES)
        }
    }
}

private fun MapboxMapController.labelsLayerId(): String? =
    mapboxMap?.style?.styleLayers?.firstOrNull { it.type == "symbol" }?.id
