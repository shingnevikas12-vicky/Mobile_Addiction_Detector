package ly.com.tahaben.nudge_presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ly.com.tahaben.core.R
import ly.com.tahaben.core_ui.LocalSpacing
import ly.com.tahaben.core_ui.components.NumberPickerDialog
import ly.com.tahaben.core_ui.components.SwitchRow
import ly.com.tahaben.core_ui.mirror

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NudgeSettingsScreen(
    onNavigateUp: () -> Unit,
    onNavigateToWhitelist: () -> Unit,
    viewModel: NudgeSettingsViewModel = hiltViewModel()
) {
    val spacing = LocalSpacing.current
    val state = viewModel.state.collectAsStateWithLifecycle().value

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nudge_settings_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            modifier = Modifier.mirror(),
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { padding: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(spacing.spaceMedium)
        ) {
            SwitchRow(
                modifier = Modifier.fillMaxWidth(),
                string = stringResource(R.string.nudge_enabled),
                selected = state.isNudgeEnabled,
                supportingText = stringResource(R.string.nudge_enabled_summary),
                onCheckedChange = { viewModel.setIsNudgeEnabled(it) }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = state.isNudgeEnabled) { viewModel.showThresholdPicker() }
                    .padding(vertical = spacing.spaceSmall),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.nudge_threshold_label), fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.nudge_threshold_hint, state.thresholdMultiplier.toInt()))
                }
                Text("×${state.thresholdMultiplier.toInt()}")
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = state.isNudgeEnabled) { onNavigateToWhitelist() }
                    .padding(vertical = spacing.spaceSmall),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.nudge_allowlist_label), fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.nudge_allowlist_count, state.allowlistedCount))
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Text(
                stringResource(R.string.nudge_baseline_explanation),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (state.isThresholdPickerVisible) {
        NumberPickerDialog(
            onDismissDialog = { viewModel.dismissThresholdPicker() },
            initialValue = state.thresholdMultiplier.toInt().coerceIn(1, 10),
            minValue = 1,
            maxValue = 10,
            unit = "×",
            onValueChangedListener = { _, _ -> },
            onConfirmValue = { viewModel.setThresholdMultiplier(it) }
        )
    }
}
