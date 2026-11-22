package dev.rogerbertan.weatherfish.ui.share

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode

@Composable
fun QrCameraView(
    isAnalyzing: Boolean,
    onCodesDetected: (List<String>) -> Unit,
    onCameraUnavailable: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnCodesDetected by rememberUpdatedState(onCodesDetected)
    val currentOnCameraUnavailable by rememberUpdatedState(onCameraUnavailable)
    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
        }
    }
    val barcodeScanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build(),
        )
    }
    DisposableEffect(barcodeScanner) {
        onDispose { barcodeScanner.close() }
    }
    DisposableEffect(lifecycleOwner) {
        var active = true
        controller.initializationFuture.addListener(
            {
                if (!active) return@addListener
                val selector = controller.availableSelector()
                if (selector == null) {
                    currentOnCameraUnavailable()
                } else {
                    controller.cameraSelector = selector
                    controller.bindToLifecycle(lifecycleOwner)
                }
            },
            context.mainExecutor,
        )
        onDispose {
            active = false
            controller.unbind()
        }
    }
    DisposableEffect(isAnalyzing) {
        if (isAnalyzing) {
            val executor = context.mainExecutor
            val analyzer = MlKitAnalyzer(
                listOf(barcodeScanner),
                ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL,
                executor,
            ) { result ->
                val codes = result.getValue(barcodeScanner).orEmpty().mapNotNull { it.rawValue }
                currentOnCodesDetected(codes)
            }
            controller.setImageAnalysisAnalyzer(executor, analyzer)
        }
        onDispose { controller.clearImageAnalysisAnalyzer() }
    }
    AndroidView(
        factory = { viewContext ->
            PreviewView(viewContext).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                this.controller = controller
            }
        },
        modifier = modifier,
    )
}

private fun CameraController.availableSelector(): CameraSelector? = try {
    listOf(CameraSelector.DEFAULT_BACK_CAMERA, CameraSelector.DEFAULT_FRONT_CAMERA).firstOrNull(::hasCamera)
} catch (e: IllegalStateException) {
    null
}
