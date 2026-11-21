package dev.rogerbertan.weatherfish

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import dev.rogerbertan.weatherfish.ui.WeatherFishApp
import dev.rogerbertan.weatherfish.ui.theme.WeatherFishTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WeatherFishTheme {
                WeatherFishApp()
            }
        }
    }
}