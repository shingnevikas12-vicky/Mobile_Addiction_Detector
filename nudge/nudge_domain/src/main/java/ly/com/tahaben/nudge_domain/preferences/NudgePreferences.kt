package ly.com.tahaben.nudge_domain.preferences

interface NudgePreferences {

    suspend fun isNudgeEnabled(): Boolean

    fun setIsNudgeEnabled(isEnabled: Boolean)

    fun getThresholdMultiplier(): Float

    fun setThresholdMultiplier(multiplier: Float)

    fun getAllowlistedPackages(): Set<String>

    fun setAllowlistedPackages(packages: Set<String>)

    fun getBaselineMap(): Map<String, Float>

    fun setBaselineMap(map: Map<String, Float>)

    fun getBaselineCacheDate(): String?

    fun setBaselineCacheDate(date: String?)

    companion object {
        const val KEY_NUDGE_ENABLED = "nudge_enabled"
        const val KEY_THRESHOLD_MULTIPLIER = "nudge_threshold_multiplier"
        const val KEY_ALLOWLIST = "nudge_allowlist"
        const val KEY_BASELINE_PREFIX = "nudge_baseline_pkg_"
        const val KEY_BASELINE_CACHE_DATE = "nudge_baseline_cache_date"
    }
}
