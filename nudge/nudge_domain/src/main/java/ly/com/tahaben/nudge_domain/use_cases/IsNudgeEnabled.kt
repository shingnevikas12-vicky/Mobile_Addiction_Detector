package ly.com.tahaben.nudge_domain.use_cases

import ly.com.tahaben.nudge_domain.preferences.NudgePreferences

class IsNudgeEnabled(private val preferences: NudgePreferences) {

    suspend operator fun invoke(): Boolean = preferences.isNudgeEnabled()
}
