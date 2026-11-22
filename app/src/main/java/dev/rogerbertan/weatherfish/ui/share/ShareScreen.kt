package dev.rogerbertan.weatherfish.ui.share

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.rogerbertan.weatherfish.R

@Composable
fun ShareScreen(
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ShareViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scannedLink = uiState.scannedLink
    LaunchedEffect(scannedLink) {
        if (scannedLink != null) {
            onOpenLink(scannedLink)
            viewModel.linkOpened()
        }
    }
    val message = uiState.message
    val messageText = message?.text()
    LaunchedEffect(message) {
        if (messageText != null) {
            snackbarHostState.showSnackbar(messageText)
            viewModel.messageShown()
        }
    }
    Box(modifier = modifier.fillMaxSize()) {
        uiState.qr?.let { qr ->
            ShareContent(qr = qr, isScanning = uiState.isScanning, onScan = viewModel::scan)
        }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun ShareContent(qr: SharedQr, isScanning: Boolean, onScan: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = qr.placeName, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        QrCode(
            content = qr.content,
            contentDescription = stringResource(R.string.share_qr_description, qr.placeName),
            modifier = Modifier.widthIn(max = 280.dp).fillMaxWidth(),
        )
        Text(
            text = stringResource(R.string.share_caption),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onScan, enabled = !isScanning) {
            if (isScanning) {
                CircularProgressIndicator(modifier = Modifier.size(ButtonDefaults.IconSize), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Outlined.QrCodeScanner, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            }
            Text(text = stringResource(R.string.share_scan), modifier = Modifier.padding(start = ButtonDefaults.IconSpacing))
        }
    }
}

@Composable
private fun ShareMessage.text(): String = when (this) {
    ShareMessage.InvalidCode -> stringResource(R.string.share_invalid_code)
    ShareMessage.ScanFailed -> stringResource(R.string.share_scan_failed)
}