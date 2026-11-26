package dev.rogerbertan.weatherfish.ui.share

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

@Composable
fun QrCode(content: String, contentDescription: String, modifier: Modifier = Modifier) {
    val matrix = remember(content) { encodeQr(content) }
    Spacer(
        modifier = modifier
            .aspectRatio(1f)
            .background(Color.White)
            .semantics { this.contentDescription = contentDescription }
            .drawWithCache {
                val module = size.width / matrix.width
                val path = Path()
                for (y in 0 until matrix.height) {
                    var x = 0
                    while (x < matrix.width) {
                        if (!matrix[x, y]) {
                            x++
                            continue
                        }
                        val start = x
                        while (x < matrix.width && matrix[x, y]) x++
                        path.addRect(Rect(start * module, y * module, x * module, (y + 1) * module))
                    }
                }
                onDrawBehind { drawPath(path, Color.Black) }
            },
    )
}

private fun encodeQr(content: String): BitMatrix = QRCodeWriter().encode(
    content,
    BarcodeFormat.QR_CODE,
    0,
    0,
    mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M, EncodeHintType.MARGIN to 4),
)