package dev.rogerbertan.weatherfish.ui.common

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager

const val LOCATION_PERMISSION = Manifest.permission.ACCESS_COARSE_LOCATION

fun Context.hasLocationPermission(): Boolean =
    checkSelfPermission(LOCATION_PERMISSION) == PackageManager.PERMISSION_GRANTED
