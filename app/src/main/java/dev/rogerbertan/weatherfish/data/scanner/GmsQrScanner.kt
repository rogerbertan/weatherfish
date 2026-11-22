package dev.rogerbertan.weatherfish.data.scanner

import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import dev.rogerbertan.weatherfish.domain.QrScanException
import dev.rogerbertan.weatherfish.domain.QrScanner
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class GmsQrScanner @Inject constructor(
    private val client: GmsBarcodeScanner,
) : QrScanner {

    override suspend fun scan(): String? = suspendCancellableCoroutine { continuation ->
        client.startScan()
            .addOnSuccessListener { barcode -> continuation.resume(barcode.rawValue.orEmpty()) }
            .addOnCanceledListener { continuation.resume(null) }
            .addOnFailureListener { error ->
                if (error.isClosedByUser()) continuation.resume(null)
                else continuation.resumeWithException(QrScanException(error))
            }
    }

    private fun Exception.isClosedByUser() =
        this is MlKitException && errorCode in CLOSED_BY_USER_CODES

    private companion object {
        val CLOSED_BY_USER_CODES = setOf(MlKitException.CODE_SCANNER_CANCELLED, MlKitException.INTERNAL)
    }
}