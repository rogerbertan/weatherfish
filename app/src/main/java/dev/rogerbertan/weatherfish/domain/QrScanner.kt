package dev.rogerbertan.weatherfish.domain

interface QrScanner {
    suspend fun scan(): String?
}

class QrScanException(cause: Throwable) : Exception(cause)