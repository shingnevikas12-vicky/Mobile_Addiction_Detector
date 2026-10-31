package ly.com.tahaben.nudge_presentation.overlay

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ly.com.tahaben.core.R
import ly.com.tahaben.core_ui.LocalSpacing

/**
 * Lightweight in-the-moment "pattern awareness" card. Deliberately non-blocking and
 * always-dismissible: this nudges, it does not trap. Maps to the overlay shown by
 * the AccessibilityService when an app crosses the user's personal baseline.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NudgeOverlayContent(
    appName: String,
    openCount: Int,
    onTakeFive: () -> Unit,
    onContinue: () -> Unit,
    onDontShowAgainToday: () -> Unit
) {
    val spacing = LocalSpacing.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(spacing.spaceMedium),
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = AlertDialogDefaults.TonalElevation,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(spacing.spaceMedium),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.spaceMedium)
        ) {
            Text(
                text = stringResource(R.string.nudge_overlay_message, appName, openCount),
                style = MaterialTheme.typography.titleMedium
            )
            Button(onClick = onTakeFive) {
                Text(stringResource(R.string.nudge_take_five))
            }
            Button(onClick = onContinue) {
                Text(stringResource(R.string.nudge_continue))
            }
            Text(
                text = stringResource(R.string.nudge_not_again_today),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.clickable { onDontShowAgainToday() }
            )
        }
    }
}
