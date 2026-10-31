package ly.com.tahaben.nudge_presentation.settings

data class NudgeSettingsState(
    val isNudgeEnabled: Boolean = false,
    val thresholdMultiplier: Float = 3.0f,
    val allowlistedCount: Int = 0,
    val isThresholdPickerVisible: Boolean = false
)
