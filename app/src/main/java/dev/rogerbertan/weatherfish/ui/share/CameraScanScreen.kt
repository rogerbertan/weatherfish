package dev.rogerbertan.weatherfish.ui.share

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.NoPhotography
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.rogerbertan.weatherfish.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScanScreen(
    onLinkScanned: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CameraScanViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scannedLink = uiState.scannedLink
    LaunchedEffect(scannedLink) {
        if (scannedLink != null) {
            onLinkScanned(scannedLink)
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
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.camera_scan_title)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }
            },
            windowInsets = WindowInsets(0),
        )
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            CameraPermissionGate(modifier = Modifier.fillMaxSize()) {
                if (uiState.isCameraUnavailable) {
                    CameraMessage(
                        text = stringResource(R.string.camera_unavailable),
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    CameraScanContent(
                        isAnalyzing = uiState.isAnalyzing,
                        onCodesDetected = viewModel::onCodesDetected,
                        onCameraUnavailable = viewModel::onCameraUnavailable,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun CameraScanContent(
    isAnalyzing: Boolean,
    onCodesDetected: (List<String>) -> Unit,
    onCameraUnavailable: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.clipToBounds().background(Color.Black)) {
        QrCameraView(
            isAnalyzing = isAnalyzing,
            onCodesDetected = onCodesDetected,
            onCameraUnavailable = onCameraUnavailable,
            modifier = Modifier.fillMaxSize(),
        )
        ScanAreaOverlay(modifier = Modifier.fillMaxSize())
        Text(
            text = stringResource(R.string.camera_scan_hint),
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 24.dp, vertical = 16.dp),
        )
    }
}

enum class CameraRefusal { Denied, Blocked }

fun nextCameraRefusal(previous: CameraRefusal?, showRationale: Boolean): CameraRefusal =
    if (!showRationale && previous != null) CameraRefusal.Blocked else CameraRefusal.Denied

@Composable
private fun CameraPermissionGate(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    var granted by remember { mutableStateOf(context.hasCameraPermission()) }
    var refusal by rememberSaveable { mutableStateOf<CameraRefusal?>(null) }
    var requesting by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
        requesting = false
        granted = result
        refusal = if (result) {
            null
        } else {
            nextCameraRefusal(refusal, activity?.shouldShowRequestPermissionRationale(CAMERA_PERMISSION) == true)
        }
    }
    val request = {
        requesting = true
        launcher.launch(CAMERA_PERMISSION)
    }
    LaunchedEffect(granted, refusal, requesting) {
        if (!granted && refusal == null && !requesting) request()
    }
    LifecycleResumeEffect(Unit) {
        granted = context.hasCameraPermission()
        if (granted) refusal = null
        onPauseOrDispose { }
    }
    when {
        granted -> Box(modifier = modifier) { content() }
        refusal == null -> Box(modifier = modifier)
        refusal == CameraRefusal.Denied -> CameraMessage(
            text = stringResource(R.string.camera_permission_denied),
            action = stringResource(R.string.camera_permission_allow),
            onAction = request,
            modifier = modifier,
        )
        else -> CameraMessage(
            text = stringResource(R.string.camera_permission_blocked),
            action = stringResource(R.string.camera_permission_settings),
            onAction = { context.openAppSettings() },
            modifier = modifier,
        )
    }
}

@Composable
private fun CameraMessage(
    text: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Outlined.NoPhotography,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        if (action != null) {
            Button(onClick = onAction) {
                Text(text = action)
            }
        }
    }
}

private const val CAMERA_PERMISSION = Manifest.permission.CAMERA

private fun Context.hasCameraPermission(): Boolean =
    checkSelfPermission(CAMERA_PERMISSION) == PackageManager.PERMISSION_GRANTED

private fun Context.openAppSettings() {
    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
}
