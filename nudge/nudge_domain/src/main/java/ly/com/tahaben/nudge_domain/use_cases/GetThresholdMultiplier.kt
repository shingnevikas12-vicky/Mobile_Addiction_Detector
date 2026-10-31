package ly.com.tahaben.nudge_domain.use_cases

import ly.com.tahaben.nudge_domain.preferences.NudgePreferences

class GetThresholdMultiplier(private val preferences: NudgePreferences) {

    operator fun invoke(): Float = preferences.getThresholdMultiplier()
}
