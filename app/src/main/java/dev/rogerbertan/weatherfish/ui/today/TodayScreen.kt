package dev.rogerbertan.weatherfish.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.rogerbertan.weatherfish.R
import dev.rogerbertan.weatherfish.domain.CurrentWeather
import dev.rogerbertan.weatherfish.domain.HourlyForecast
import dev.rogerbertan.weatherfish.domain.Place
import dev.rogerbertan.weatherfish.ui.common.ConditionIcon
import dev.rogerbertan.weatherfish.ui.common.LoadStateContent
import dev.rogerbertan.weatherfish.ui.common.formatHour
import dev.rogerbertan.weatherfish.ui.common.formatTemperature
import java.time.ZoneId

@Composable
fun TodayScreen(
    onOpenDetails: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TodayViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LoadStateContent(state = uiState, onRetry = viewModel::retry, modifier = modifier) { weather ->
        TodayContent(weather = weather, onOpenDetails = onOpenDetails)
    }
}

@Composable
private fun TodayContent(weather: TodayWeather, onOpenDetails: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        CurrentSummary(
            place = weather.place,
            current = weather.current,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Text(
            text = stringResource(R.string.today_next_24h),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        HourlyRow(hourly = weather.hourly, zone = weather.place.timeZone)
        FilledTonalButton(
            onClick = onOpenDetails,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        ) {
            Text(stringResource(R.string.today_see_details))
        }
    }
}

@Composable
private fun CurrentSummary(place: Place, current: CurrentWeather, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = place.name, style = MaterialTheme.typography.headlineMedium)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ConditionIcon(condition = current.condition, size = 64.dp)
            Text(text = formatTemperature(current.temperatureC), style = MaterialTheme.typography.displayLarge)
        }
        Text(text = current.description, style = MaterialTheme.typography.titleMedium)
        Text(
            text = stringResource(R.string.today_feels_like, formatTemperature(current.feelsLikeC)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HourlyRow(hourly: List<HourlyForecast>, zone: ZoneId, modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(hourly, key = { it.time.epochSecond }) { hour ->
            HourCard(hour = hour, zone = zone)
        }
    }
}

@Composable
private fun HourCard(hour: HourlyForecast, zone: ZoneId, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = formatHour(hour.time, zone), style = MaterialTheme.typography.labelLarge)
            ConditionIcon(condition = hour.condition)
            Text(text = formatTemperature(hour.temperatureC), style = MaterialTheme.typography.titleMedium)
            Text(
                text = "${hour.precipitationChancePercent}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}