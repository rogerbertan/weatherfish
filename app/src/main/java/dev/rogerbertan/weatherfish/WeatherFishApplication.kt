package dev.rogerbertan.weatherfish

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.rogerbertan.weatherfish.data.xweather.XweatherSdk

@HiltAndroidApp
class WeatherFishApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        XweatherSdk.init()
    }
}
