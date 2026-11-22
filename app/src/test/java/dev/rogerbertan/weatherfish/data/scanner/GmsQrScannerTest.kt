package dev.rogerbertan.weatherfish.data.scanner

import com.google.android.gms.tasks.OnCanceledListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import dev.rogerbertan.weatherfish.domain.QrScanException
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class GmsQrScannerTest {

    private val onSuccess = slot<OnSuccessListener<in Barcode>>()
    private val onCanceled = slot<OnCanceledListener>()
    private val onFailure = slot<OnFailureListener>()

    private val task = mockk<Task<Barcode>> {
        every { addOnSuccessListener(capture(onSuccess)) } returns this
        every { addOnCanceledListener(capture(onCanceled)) } returns this
        every { addOnFailureListener(capture(onFailure)) } returns this
    }
    private val client = mockk<GmsBarcodeScanner> {
        every { startScan() } returns task
    }

    private val scanner = GmsQrScanner(client)

    @Test
    fun `should return raw value when scan succeeds`() = runTest {
        // Arrange
        val barcode = mockk<Barcode> { every { rawValue } returns "weatherfish://place?name=A&lat=0&lon=0" }
        val result = async { scanner.scan() }
        runCurrent()

        // Act
        onSuccess.captured.onSuccess(barcode)

        // Assert
        assertEquals("weatherfish://place?name=A&lat=0&lon=0", result.await())
    }

    @Test
    fun `should return empty text when barcode has no raw value`() = runTest {
        // Arrange
        val barcode = mockk<Barcode> { every { rawValue } returns null }
        val result = async { scanner.scan() }
        runCurrent()

        // Act
        onSuccess.captured.onSuccess(barcode)

        // Assert
        assertEquals("", result.await())
    }

    @Test
    fun `should return null when scan is cancelled`() = runTest {
        // Arrange
        val result = async { scanner.scan() }
        runCurrent()

        // Act
        onCanceled.captured.onCanceled()

        // Assert
        assertNull(result.await())
    }

    @ParameterizedTest(name = "should return null for ml kit error code {0}")
    @ValueSource(ints = [MlKitException.CODE_SCANNER_CANCELLED, MlKitException.INTERNAL])
    fun `should return null when user closes the scanner`(code: Int) = runTest {
        // Arrange
        val closed = mockk<MlKitException> { every { errorCode } returns code }
        val result = async { scanner.scan() }
        runCurrent()

        // Act
        onFailure.captured.onFailure(closed)

        // Assert
        assertNull(result.await())
    }

    @Test
    fun `should throw scan exception when scanner module is unavailable`() = runTest {
        // Arrange
        val unavailable = mockk<MlKitException> { every { errorCode } returns MlKitException.CODE_SCANNER_UNAVAILABLE }
        val result = async { runCatching { scanner.scan() } }
        runCurrent()

        // Act
        onFailure.captured.onFailure(unavailable)

        // Assert
        assertThrows<QrScanException> { result.await().getOrThrow() }
    }

    @Test
    fun `should throw scan exception when scan fails`() = runTest {
        // Arrange
        val error = IllegalStateException("module unavailable")
        val result = async { runCatching { scanner.scan() } }
        runCurrent()

        // Act
        onFailure.captured.onFailure(error)

        // Assert
        val thrown = assertThrows<QrScanException> { result.await().getOrThrow() }
        assertSame(error, generateSequence<Throwable>(thrown) { it.cause }.last())
    }
}