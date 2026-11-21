package dev.rogerbertan.weatherfish.ui.common

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.FilterDrama
import androidx.compose.material.icons.outlined.Grain
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Thunderstorm
import androidx.compose.material.icons.outlined.Umbrella
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.Dehaze
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.rogerbertan.weatherfish.R
import dev.rogerbertan.weatherfish.domain.WeatherCondition

private data class ConditionStyle(val icon: ImageVector, val tint: Color, val label: Int)

private fun WeatherCondition.style(): ConditionStyle = when (this) {
    WeatherCondition.Clear -> ConditionStyle(Icons.Outlined.WbSunny, Color(0xFFF6B100), R.string.condition_clear)
    WeatherCondition.PartlyCloudy -> ConditionStyle(Icons.Outlined.FilterDrama, Color(0xFF8AA4B8), R.string.condition_partly_cloudy)
    WeatherCondition.Cloudy -> ConditionStyle(Icons.Outlined.Cloud, Color(0xFF7D8A96), R.string.condition_cloudy)
    WeatherCondition.Fog -> ConditionStyle(Icons.Outlined.Dehaze, Color(0xFF9AA5AE), R.string.condition_fog)
    WeatherCondition.Drizzle -> ConditionStyle(Icons.Outlined.Grain, Color(0xFF4F8FC0), R.string.condition_drizzle)
    WeatherCondition.Rain -> ConditionStyle(Icons.Outlined.Umbrella, Color(0xFF2F6FB0), R.string.condition_rain)
    WeatherCondition.Thunderstorm -> ConditionStyle(Icons.Outlined.Thunderstorm, Color(0xFF6A4FB0), R.string.condition_thunderstorm)
    WeatherCondition.Snow -> ConditionStyle(Icons.Outlined.AcUnit, Color(0xFF6EC6E8), R.string.condition_snow)
    WeatherCondition.Wind -> ConditionStyle(Icons.Outlined.Air, Color(0xFF5E9E8F), R.string.condition_wind)
    WeatherCondition.Unknown -> ConditionStyle(Icons.AutoMirrored.Outlined.HelpOutline, Color(0xFF8A8A8A), R.string.condition_unknown)
}

@Composable
fun ConditionIcon(condition: WeatherCondition, modifier: Modifier = Modifier, size: Dp = 32.dp) {
    val style = condition.style()
    Icon(
        imageVector = style.icon,
        contentDescription = stringResource(style.label),
        tint = style.tint,
        modifier = modifier.size(size),
    )
}