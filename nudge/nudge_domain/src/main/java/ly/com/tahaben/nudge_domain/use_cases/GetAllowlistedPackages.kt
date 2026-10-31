package ly.com.tahaben.nudge_domain.use_cases

import ly.com.tahaben.nudge_domain.preferences.NudgePreferences

class GetAllowlistedPackages(private val preferences: NudgePreferences) {

    operator fun invoke(): Set<String> = preferences.getAllowlistedPackages()
}
