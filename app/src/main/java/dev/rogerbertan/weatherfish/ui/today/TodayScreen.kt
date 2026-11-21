package dev.rogerbertan.weatherfish.ui.today

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.rogerbertan.weatherfish.R
import dev.rogerbertan.weatherfish.domain.CurrentWeather
import dev.rogerbertan.weatherfish.domain.HourlyForecast
import dev.rogerbertan.weatherfish.domain.Place
import dev.rogerbertan.weatherfish.domain.TemperatureUnit
import dev.rogerbertan.weatherfish.ui.common.ConditionIcon
import dev.rogerbertan.weatherfish.ui.common.FavoriteButton
import dev.rogerbertan.weatherfish.ui.common.LOCATION_PERMISSION
import dev.rogerbertan.weatherfish.ui.common.hasLocationPermission
import dev.rogerbertan.weatherfish.ui.common.LoadStateContent
import dev.rogerbertan.weatherfish.ui.common.formatHour
import dev.rogerbertan.weatherfish.ui.common.formatTemperature
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    onOpenFavorites: () -> Unit,
    onOpenDetails: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TodayViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LocationPermissionRequest(onGranted = viewModel::onLocationPermissionGranted)
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { if (uiState.isLocating) LocatingIndicator() },
            actions = {
                IconButton(onClick = onOpenFavorites) {
                    Icon(
                        Icons.AutoMirrored.Outlined.List,
                        contentDescription = stringResource(R.string.favorites_title),
                    )
                }
            },
            windowInsets = WindowInsets(0),
        )
        LoadStateContent(state = uiState.content, onRetry = viewModel::retry) { state ->
            TodayContent(
                state = state,
                onToggleFavorite = viewModel::toggleFavorite,
                onOpenDetails = onOpenDetails,
            )
        }
    }
}

@Composable
private fun LocatingIndicator(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        Text(
            text = stringResource(R.string.today_locating),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LocationPermissionRequest(onGranted: () -> Unit) {
    val context = LocalContext.current
    var asked by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) onGranted()
    }
    LaunchedEffect(Unit) {
        if (!asked && !context.hasLocationPermission()) {
            asked = true
            launcher.launch(LOCATION_PERMISSION)
        }
    }
}

@Composable
private fun TodayContent(
    state: TodayContent,
    onToggleFavorite: () -> Unit,
    onOpenDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val weather = state.weather
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        CurrentSummary(
            place = weather.place,
            current = weather.current,
            unit = state.unit,
            isFavorite = state.isFavorite,
            onToggleFavorite = onToggleFavorite,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Text(
            text = stringResource(R.string.today_next_24h),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        HourlyRow(hourly = weather.hourly, zone = weather.place.timeZone, unit = state.unit)
        FilledTonalButton(
            onClick = onOpenDetails,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        ) {
            Text(stringResource(R.string.today_see_details))
        }
    }
}

@Composable
private fun CurrentSummary(
    place: Place,
    current: CurrentWeather,
    unit: TemperatureUnit,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = place.name, style = MaterialTheme.typography.headlineMedium)
            FavoriteButton(isFavorite = isFavorite, onClick = onToggleFavorite)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ConditionIcon(condition = current.condition, size = 64.dp)
            Text(text = formatTemperature(current.temperatureC, unit), style = MaterialTheme.typography.displayLarge)
        }
        Text(text = current.description, style = MaterialTheme.typography.titleMedium)
        Text(
            text = stringResource(R.string.today_feels_like, formatTemperature(current.feelsLikeC, unit)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HourlyRow(hourly: List<HourlyForecast>, zone: ZoneId, unit: TemperatureUnit, modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(hourly, key = { it.time.epochSecond }) { hour ->
            HourCard(hour = hour, zone = zone, unit = unit)
        }
    }
}

@Composable
private fun HourCard(hour: HourlyForecast, zone: ZoneId, unit: TemperatureUnit, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = formatHour(hour.time, zone), style = MaterialTheme.typography.labelLarge)
            ConditionIcon(condition = hour.condition)
            Text(text = formatTemperature(hour.temperatureC, unit), style = MaterialTheme.typography.titleMedium)
            Text(
                text = "${hour.precipitationChancePercent}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}