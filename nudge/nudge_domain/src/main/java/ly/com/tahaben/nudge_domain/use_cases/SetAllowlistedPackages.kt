package ly.com.tahaben.nudge_domain.use_cases

import ly.com.tahaben.nudge_domain.preferences.NudgePreferences

class SetAllowlistedPackages(private val preferences: NudgePreferences) {

    operator fun invoke(packages: Set<String>) {
        preferences.setAllowlistedPackages(packages)
    }
}
