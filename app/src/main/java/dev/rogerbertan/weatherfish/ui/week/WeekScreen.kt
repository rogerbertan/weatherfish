package dev.rogerbertan.weatherfish.ui.week

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
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
import dev.rogerbertan.weatherfish.domain.DailyForecast
import dev.rogerbertan.weatherfish.ui.common.ConditionIcon
import dev.rogerbertan.weatherfish.ui.common.LoadStateContent
import dev.rogerbertan.weatherfish.ui.common.formatDayMonth
import dev.rogerbertan.weatherfish.ui.common.formatTemperature
import dev.rogerbertan.weatherfish.ui.common.formatWeekday

@Composable
fun WeekScreen(modifier: Modifier = Modifier, viewModel: WeekViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LoadStateContent(state = uiState, onRetry = viewModel::retry, modifier = modifier) { days ->
        DayList(days = days)
    }
}

@Composable
private fun DayList(days: List<DailyForecast>, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "title") {
            Text(
                text = stringResource(R.string.week_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        items(days, key = { it.date.toEpochDay() }) { day ->
            DayRow(day = day)
        }
    }
}

@Composable
private fun DayRow(day: DailyForecast, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ConditionIcon(condition = day.condition, size = 40.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = formatWeekday(day.date), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${formatDayMonth(day.date)} · ${day.description}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${formatTemperature(day.maxTemperatureC)} / ${formatTemperature(day.minTemperatureC)}",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "${day.precipitationChancePercent}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}