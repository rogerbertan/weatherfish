package dev.rogerbertan.weatherfish.ui.share

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
fun ScanAreaOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
        val side = size.minDimension * SCAN_AREA_FRACTION
        val topLeft = Offset((size.width - side) / 2, (size.height - side) / 2)
        val areaSize = Size(side, side)
        val cornerRadius = CornerRadius(16.dp.toPx())
        drawRect(color = Color.Black.copy(alpha = SCRIM_ALPHA))
        drawRoundRect(
            color = Color.Transparent,
            topLeft = topLeft,
            size = areaSize,
            cornerRadius = cornerRadius,
            blendMode = BlendMode.Clear,
        )
        drawRoundRect(
            color = Color.White,
            topLeft = topLeft,
            size = areaSize,
            cornerRadius = cornerRadius,
            style = Stroke(width = 3.dp.toPx()),
        )
    }
}

private const val SCAN_AREA_FRACTION = 0.6f
private const val SCRIM_ALPHA = 0.55f
