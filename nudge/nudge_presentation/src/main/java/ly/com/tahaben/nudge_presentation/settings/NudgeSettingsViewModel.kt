package ly.com.tahaben.nudge_presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ly.com.tahaben.nudge_domain.preferences.NudgePreferences
import ly.com.tahaben.nudge_domain.use_cases.NudgeUseCases
import javax.inject.Inject

@HiltViewModel
class NudgeSettingsViewModel @Inject constructor(
    private val nudgeUseCases: NudgeUseCases,
    private val preferences: NudgePreferences
) : ViewModel() {

    private val _state = MutableStateFlow(NudgeSettingsState())
    val state = _state.asStateFlow()

    init {
        checkNudgeEnabled()
        loadThreshold()
        loadAllowlistedCount()
    }

    private fun checkNudgeEnabled() {
        viewModelScope.launch {
            _state.update { it.copy(isNudgeEnabled = nudgeUseCases.isNudgeEnabled()) }
        }
    }

    private fun loadThreshold() {
        _state.update { it.copy(thresholdMultiplier = preferences.getThresholdMultiplier()) }
    }

    private fun loadAllowlistedCount() {
        _state.update { it.copy(allowlistedCount = nudgeUseCases.getAllowlistedPackages().size) }
    }

    fun setIsNudgeEnabled(isEnabled: Boolean) {
        nudgeUseCases.setIsNudgeEnabled(isEnabled)
        _state.update { it.copy(isNudgeEnabled = isEnabled) }
    }

    fun showThresholdPicker() {
        _state.update { it.copy(isThresholdPickerVisible = true) }
    }

    fun dismissThresholdPicker() {
        _state.update { it.copy(isThresholdPickerVisible = false) }
    }

    fun setThresholdMultiplier(value: Int) {
        val asFloat = value.toFloat()
        nudgeUseCases.setThresholdMultiplier(asFloat)
        _state.update { it.copy(thresholdMultiplier = asFloat, isThresholdPickerVisible = false) }
    }
}
