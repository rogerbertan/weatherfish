package dev.rogerbertan.weatherfish.ui.map

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Map
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.rogerbertan.weatherfish.R
import dev.rogerbertan.weatherfish.ui.common.Placeholder

@Composable
fun MapScreen(modifier: Modifier = Modifier) {
    Placeholder(icon = Icons.Outlined.Map, text = stringResource(R.string.map_placeholder), modifier = modifier)
}