package dev.rogerbertan.weatherfish.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.rogerbertan.weatherfish.R

@Composable
fun FavoriteButton(isFavorite: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) {
        if (isFavorite) {
            Icon(
                Icons.Filled.Star,
                contentDescription = stringResource(R.string.favorite_remove),
                tint = MaterialTheme.colorScheme.primary,
            )
        } else {
            Icon(Icons.Outlined.StarBorder, contentDescription = stringResource(R.string.favorite_add))
        }
    }
}