package dev.rogerbertan.weatherfish.ui

import dev.rogerbertan.weatherfish.domain.GeoPoint
import dev.rogerbertan.weatherfish.domain.PlaceQrCodec

object DeepLinks {
    private const val BASE = "weatherfish://"

    const val TODAY = "${BASE}today"
    const val DETAILS = "$TODAY/details"
    const val FAVORITES = "$TODAY/favorites"
    const val WEEK = "${BASE}week"
    const val SEARCH = "${BASE}search"
    const val MAP = "${BASE}map"
    const val SHARE = "${BASE}share"
    const val CAMERA_SCAN = "$SHARE/scan"
    const val PLACE = PlaceQrCodec.PLACE_URI

    fun place(name: String, point: GeoPoint): String = PlaceQrCodec.encode(name, point)
}