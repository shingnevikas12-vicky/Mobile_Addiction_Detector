package ly.com.tahaben.nudge_domain.use_cases

import ly.com.tahaben.nudge_domain.util.SAFETY_ALLOWLIST

/**
 * Decides whether to fire an in-the-moment nudge for [packageName], given the live
 * [currentHourlyOpenCount] for the current hour.
 *
 * Rules (all on-device):
 *  - nudge feature + global main switch must be enabled
 *  - package must NOT be in the hardcoded safety allowlist (maps/phone/sms/camera/...)
 *  - package must NOT be in the user's allowlist
 *  - count must reach the personal baseline multiplied by the threshold multiplier
 *
 * Returns false for packages with no history (baseline 0f): without a baseline we can't
 * judge "deviation from your own normal," so we don't nudge — avoiding generic-threshold
 * false positives on new apps.
 */
class ShouldNudge(
    private val isNudgeEnabled: IsNudgeEnabled,
    private val getThresholdMultiplier: GetThresholdMultiplier,
    private val getHourlyOpenBaseline: GetHourlyOpenBaseline,
    private val getAllowlistedPackages: GetAllowlistedPackages
) {

    suspend operator fun invoke(packageName: String, currentHourlyOpenCount: Int): Boolean {
        if (!isNudgeEnabled()) return false
        if (packageName in SAFETY_ALLOWLIST) return false
        if (packageName in getAllowlistedPackages()) return false
        val baseline = getHourlyOpenBaseline(packageName)
        if (baseline <= 0f) return false
        val threshold = getThresholdMultiplier()
        return currentHourlyOpenCount.toFloat() >= baseline * threshold
    }
}
