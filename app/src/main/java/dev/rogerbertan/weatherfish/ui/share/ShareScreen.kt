package dev.rogerbertan.weatherfish.ui.share

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.rogerbertan.weatherfish.R
import dev.rogerbertan.weatherfish.ui.common.Placeholder

@Composable
fun ShareScreen(modifier: Modifier = Modifier) {
    Placeholder(icon = Icons.Outlined.Share, text = stringResource(R.string.share_placeholder), modifier = modifier)
}