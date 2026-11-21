package dev.rogerbertan.weatherfish.ui.details

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.Opacity
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.rogerbertan.weatherfish.R
import dev.rogerbertan.weatherfish.domain.CurrentWeather
import dev.rogerbertan.weatherfish.domain.TemperatureUnit
import dev.rogerbertan.weatherfish.ui.common.LoadStateContent
import dev.rogerbertan.weatherfish.ui.common.formatTemperature
import kotlin.math.roundToInt

private enum class DetailProperty(@StringRes val label: Int, val icon: ImageVector) {
    FeelsLike(R.string.details_feels_like, Icons.Outlined.Thermostat),
    Humidity(R.string.details_humidity, Icons.Outlined.WaterDrop),
    DewPoint(R.string.details_dew_point, Icons.Outlined.Opacity),
    Wind(R.string.details_wind, Icons.Outlined.Air),
    Pressure(R.string.details_pressure, Icons.Outlined.Compress),
    Visibility(R.string.details_visibility, Icons.Outlined.Visibility),
    Uv(R.string.details_uv, Icons.Outlined.WbSunny),
}

private fun DetailProperty.format(current: CurrentWeather, unit: TemperatureUnit): String = when (this) {
    DetailProperty.FeelsLike -> formatTemperature(current.feelsLikeC, unit)
    DetailProperty.Humidity -> "${current.humidityPercent}%"
    DetailProperty.DewPoint -> formatTemperature(current.dewPointC, unit)
    DetailProperty.Wind -> "${current.windSpeedKph.roundToInt()} km/h ${current.windDirection}"
    DetailProperty.Pressure -> "${current.pressureMb.roundToInt()} hPa"
    DetailProperty.Visibility -> "${current.visibilityKm.roundToInt()} km"
    DetailProperty.Uv -> current.uvIndex.toString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DetailsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.details_title)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }
            },
            windowInsets = WindowInsets(0),
        )
        LoadStateContent(state = uiState, onRetry = viewModel::retry) { details ->
            PropertyGrid(current = details.current, unit = details.unit)
        }
    }
}

@Composable
private fun PropertyGrid(current: CurrentWeather, unit: TemperatureUnit, modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(DetailProperty.entries, key = { it.name }) { property ->
            PropertyCard(property = property, value = property.format(current, unit))
        }
    }
}

@Composable
private fun PropertyCard(property: DetailProperty, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(property.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                text = stringResource(property.label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = value, style = MaterialTheme.typography.titleLarge)
        }
    }
}