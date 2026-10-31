package ly.com.tahaben.nudge_domain.use_cases

import ly.com.tahaben.nudge_domain.preferences.NudgePreferences

class SetIsNudgeEnabled(private val preferences: NudgePreferences) {

    operator fun invoke(isEnabled: Boolean) {
        preferences.setIsNudgeEnabled(isEnabled)
    }
}
