package dev.rogerbertan.weatherfish.ui.share

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.rogerbertan.weatherfish.R

@Composable
fun ImportPlaceDialog(onDismiss: () -> Unit, viewModel: ImportPlaceViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ImportPlaceContent(state = state, onSave = viewModel::save, onDismiss = onDismiss)
}

@Composable
private fun ImportPlaceContent(state: ImportState, onSave: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = AlertDialogDefaults.shape,
        color = AlertDialogDefaults.containerColor,
        tonalElevation = AlertDialogDefaults.TonalElevation,
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(text = stringResource(R.string.import_title), style = MaterialTheme.typography.headlineSmall)
            ImportMessage(state = state)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                when (state) {
                    is ImportState.Confirm -> {
                        TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                        TextButton(onClick = onSave) { Text(stringResource(R.string.import_save)) }
                    }
                    ImportState.Failed -> {
                        TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
                        TextButton(onClick = onSave) { Text(stringResource(R.string.retry)) }
                    }
                    is ImportState.Saving -> TextButton(onClick = {}, enabled = false) { Text(stringResource(R.string.ok)) }
                    else -> TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) }
                }
            }
        }
    }
}

@Composable
private fun ImportMessage(state: ImportState, modifier: Modifier = Modifier) {
    when (state) {
        is ImportState.Confirm -> Text(stringResource(R.string.import_confirm, state.name), modifier = modifier)
        is ImportState.Saving -> Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            Text(stringResource(R.string.import_saving, state.name))
        }
        is ImportState.Saved -> Text(stringResource(R.string.import_saved, state.name, state.location), modifier = modifier)
        is ImportState.AlreadySaved -> Text(stringResource(R.string.import_already_saved, state.name), modifier = modifier)
        ImportState.InvalidLink -> Text(stringResource(R.string.import_invalid_link), modifier = modifier)
        ImportState.Failed -> Text(stringResource(R.string.import_failed), modifier = modifier)
    }
}