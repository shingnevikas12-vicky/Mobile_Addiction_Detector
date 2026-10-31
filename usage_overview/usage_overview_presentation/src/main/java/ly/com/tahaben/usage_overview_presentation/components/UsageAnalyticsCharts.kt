package ly.com.tahaben.usage_overview_presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ly.com.tahaben.core.R
import ly.com.tahaben.core_ui.LocalSpacing
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * A daily-total bar chart for a week or month. Values are minutes per day.
 * Pure Canvas — no charting dependency.
 */
@Composable
fun UsageBarChart(
    values: List<Pair<LocalDate, Long>>,
    modifier: Modifier = Modifier,
    onDayClick: ((LocalDate) -> Unit)? = null
) {
    val spacing = LocalSpacing.current
    val barColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val maxValue = values.maxOfOrNull { it.second }?.coerceAtLeast(1L) ?: 1L

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier.padding(horizontal = spacing.spaceMedium),
            text = stringResource(id = R.string.usage_bar_chart_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(spacing.spaceSmall))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .padding(horizontal = spacing.spaceMedium),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(spacing.spaceExtraSmall)
        ) {
            values.forEach { (date, minutes) ->
                val fraction = (minutes.toFloat() / maxValue).coerceIn(0f, 1f)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(110.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    val barHeight = (96 * fraction).dp.coerceAtLeast(if (minutes > 0) 2.dp else 0.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(barHeight)
                            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            .then(
                                if (minutes > 0) {
                                    Modifier.clickable(enabled = onDayClick != null) { onDayClick?.invoke(date) }
                                } else {
                                    Modifier
                                }
                            )
                            .background(if (minutes > 0) barColor else trackColor)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

/**
 * A 24-hour × 7-day heatmap of usage, colored by total minutes per hour-cell.
 * Tap a cell to jump to that day.
 */
@Composable
fun UsageHeatmap(
    hourlyTotals: Map<LocalDate, Map<Int, Long>>,
    modifier: Modifier = Modifier,
    onDayClick: ((LocalDate) -> Unit)? = null
) {
    val spacing = LocalSpacing.current
    val days = hourlyTotals.keys.sorted()
    val allMinutes = hourlyTotals.values.flatMap { it.values }
    val maxMinutes = (allMinutes.maxOrNull() ?: 0L).coerceAtLeast(1L)

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier.padding(horizontal = spacing.spaceMedium),
            text = stringResource(id = R.string.usage_heatmap_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(spacing.spaceSmall))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.spaceMedium)
        ) {
            // Hour axis labels (0, 6, 12, 18)
            Column(
                modifier = Modifier
                    .width(20.dp)
                    .height(264.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(0, 6, 12, 18).forEach { hour ->
                    Text(
                        text = hour.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            Spacer(modifier = Modifier.width(spacing.spaceSmall))
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(spacing.spaceExtraSmall)
            ) {
                days.forEach { date ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val dayTotals = hourlyTotals[date] ?: emptyMap()
                        (0..23).forEach { hour ->
                            val minutes = dayTotals[hour] ?: 0L
                            val alpha = if (minutes > 0) {
                                (0.2f + 0.8f * (minutes.toFloat() / maxMinutes)).coerceIn(0f, 1f)
                            } else {
                                0.06f
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(11.dp)
                                    .padding(vertical = 1.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .then(
                                        if (onDayClick != null) {
                                            Modifier.clickable { onDayClick(date) }
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .background(
                                        if (minutes > 0) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = alpha)
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    )
                            )
                        }
                        Text(
                            text = date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}
