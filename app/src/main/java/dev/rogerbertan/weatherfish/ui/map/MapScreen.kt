package dev.rogerbertan.weatherfish.ui.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.rogerbertan.weatherfish.R
import dev.rogerbertan.weatherfish.ui.common.Placeholder

@Composable
fun MapScreen(modifier: Modifier = Modifier, viewModel: MapViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MapContent(uiState = uiState, onToggleWind = viewModel::toggleWind, modifier = modifier)
}

@Composable
private fun MapContent(uiState: MapUiState, onToggleWind: () -> Unit, modifier: Modifier = Modifier) {
    val credentials = uiState.credentials
    if (credentials == null) {
        Placeholder(icon = Icons.Outlined.Map, text = stringResource(R.string.map_unavailable), modifier = modifier)
        return
    }
    Box(modifier = modifier.fillMaxSize()) {
        WeatherMapView(
            credentials = credentials,
            center = uiState.center,
            showWind = uiState.showWind,
            modifier = Modifier.fillMaxSize(),
        )
        FilterChip(
            selected = uiState.showWind,
            onClick = onToggleWind,
            label = { Text(stringResource(R.string.map_wind)) },
            leadingIcon = { Icon(Icons.Outlined.Air, contentDescription = null) },
            colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
        )
    }
}
