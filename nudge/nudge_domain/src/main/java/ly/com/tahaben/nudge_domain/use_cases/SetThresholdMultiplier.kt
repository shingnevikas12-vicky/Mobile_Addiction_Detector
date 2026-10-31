package ly.com.tahaben.nudge_domain.use_cases

import ly.com.tahaben.nudge_domain.preferences.NudgePreferences

class SetThresholdMultiplier(private val preferences: NudgePreferences) {

    operator fun invoke(multiplier: Float) {
        preferences.setThresholdMultiplier(multiplier)
    }
}
