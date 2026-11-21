package dev.rogerbertan.weatherfish.ui.favorites

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.rogerbertan.weatherfish.R
import dev.rogerbertan.weatherfish.domain.Place
import dev.rogerbertan.weatherfish.domain.TemperatureUnit
import dev.rogerbertan.weatherfish.ui.common.LOCATION_PERMISSION
import dev.rogerbertan.weatherfish.ui.common.LoadStateContent
import dev.rogerbertan.weatherfish.ui.common.hasLocationPermission

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val useCurrentLocation = {
        viewModel.useCurrentLocation()
        onBack()
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) useCurrentLocation()
    }
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.favorites_title)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }
            },
            windowInsets = WindowInsets(0),
        )
        LoadStateContent(state = uiState, onRetry = {}) { state ->
            FavoritesContent(
                state = state,
                onUnitChange = viewModel::setUnit,
                onUseCurrentLocation = {
                    if (context.hasLocationPermission()) useCurrentLocation()
                    else permissionLauncher.launch(LOCATION_PERMISSION)
                },
                onSelect = { place ->
                    viewModel.select(place)
                    onBack()
                },
                onRemove = viewModel::remove,
            )
        }
    }
}

@Composable
private fun FavoritesContent(
    state: FavoritesUiState,
    onUnitChange: (TemperatureUnit) -> Unit,
    onUseCurrentLocation: () -> Unit,
    onSelect: (Place) -> Unit,
    onRemove: (Place) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        item(key = "unit") {
            UnitSelector(
                selected = state.unit,
                onSelect = onUnitChange,
                modifier = Modifier.padding(16.dp),
            )
        }
        item(key = "current") {
            ListItem(
                headlineContent = { Text(stringResource(R.string.favorites_current_location)) },
                leadingContent = { Icon(Icons.Outlined.MyLocation, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onUseCurrentLocation),
            )
            HorizontalDivider()
        }
        if (state.favorites.isEmpty()) {
            item(key = "empty") {
                Text(
                    text = stringResource(R.string.favorites_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(32.dp),
                )
            }
        }
        items(state.favorites, key = { "${it.point.latitude},${it.point.longitude}" }) { place ->
            ListItem(
                headlineContent = { Text(place.name) },
                supportingContent = { Text(listOfNotNull(place.region, place.country).joinToString(", ")) },
                leadingContent = { Icon(Icons.Outlined.Star, contentDescription = null) },
                trailingContent = {
                    IconButton(onClick = { onRemove(place) }) {
                        Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.favorite_remove))
                    }
                },
                modifier = Modifier.clickable { onSelect(place) },
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun UnitSelector(
    selected: TemperatureUnit,
    onSelect: (TemperatureUnit) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = stringResource(R.string.favorites_unit), style = MaterialTheme.typography.titleSmall)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            TemperatureUnit.entries.forEachIndexed { index, unit ->
                SegmentedButton(
                    selected = unit == selected,
                    onClick = { onSelect(unit) },
                    shape = SegmentedButtonDefaults.itemShape(index, TemperatureUnit.entries.size),
                ) {
                    Text(stringResource(unit.label))
                }
            }
        }
    }
}

private val TemperatureUnit.label: Int
    get() = when (this) {
        TemperatureUnit.Celsius -> R.string.unit_celsius
        TemperatureUnit.Fahrenheit -> R.string.unit_fahrenheit
    }