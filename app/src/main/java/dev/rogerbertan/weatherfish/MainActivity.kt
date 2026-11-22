package dev.rogerbertan.weatherfish

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import dev.rogerbertan.weatherfish.ui.WeatherFishApp
import dev.rogerbertan.weatherfish.ui.theme.WeatherFishTheme
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val newIntents = Channel<Intent>(Channel.UNLIMITED)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) prepareLaunchLink()
        enableEdgeToEdge()
        setContent {
            WeatherFishTheme {
                WeatherFishApp(newIntents = newIntents.receiveAsFlow())
            }
        }
    }

    private fun prepareLaunchLink() {
        if (intent.data == null) return
        if (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0) {
            intent = Intent(intent).setData(null)
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        newIntents.trySend(intent)
    }
}