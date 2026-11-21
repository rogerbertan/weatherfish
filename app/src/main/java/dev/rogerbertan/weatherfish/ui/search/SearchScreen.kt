package dev.rogerbertan.weatherfish.ui.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import dev.rogerbertan.weatherfish.domain.Place
import dev.rogerbertan.weatherfish.ui.common.LoadState
import dev.rogerbertan.weatherfish.ui.common.LoadStateContent

@Composable
fun SearchScreen(modifier: Modifier = Modifier, viewModel: SearchViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SearchContent(
        query = viewModel.query,
        uiState = uiState,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

@Composable
private fun SearchContent(
    query: TextFieldState,
    uiState: LoadState<List<Place>>,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            state = query,
            placeholder = { Text(stringResource(R.string.search_placeholder)) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            lineLimits = TextFieldLineLimits.SingleLine,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )
        LoadStateContent(state = uiState, onRetry = onRetry) { places ->
            when {
                query.text.isBlank() -> CenteredHint(stringResource(R.string.search_hint))
                places.isEmpty() -> CenteredHint(stringResource(R.string.search_empty))
                else -> PlaceList(places = places)
            }
        }
    }
}

@Composable
private fun PlaceList(places: List<Place>, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(places, key = { "${it.point.latitude},${it.point.longitude}" }) { place ->
            ListItem(
                headlineContent = { Text(place.name) },
                supportingContent = { Text(listOfNotNull(place.region, place.country).joinToString(", ")) },
                leadingContent = { Icon(Icons.Outlined.LocationOn, contentDescription = null) },
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun CenteredHint(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}