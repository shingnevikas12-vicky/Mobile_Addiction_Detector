package ly.com.tahaben.nudge_domain.use_cases

import java.time.LocalDate
import ly.com.tahaben.nudge_domain.preferences.NudgePreferences
import ly.com.tahaben.usage_overview_domain.model.UsageDataItem
import ly.com.tahaben.usage_overview_domain.repository.UsageRepository

/**
 * Test double: returns pre-built per-day usage from [eventsByDay], and never touches Android.
 */
class FakeUsageRepository(
    private val eventsByDay: Map<LocalDate, List<UsageDataItem>> = emptyMap()
) : UsageRepository {
    override suspend fun cacheUsageEvents(date: LocalDate) = Unit
    override suspend fun getUsageEvents(date: LocalDate): List<UsageDataItem> = eventsByDay[date].orEmpty()
    override suspend fun getUsageEventsFromDb(date: LocalDate): List<UsageDataItem> = eventsByDay[date].orEmpty()
    override suspend fun isDayDataFullyUpdated(date: LocalDate): Boolean = true
    override fun checkUsagePermission(): Boolean = true
    override suspend fun getCachedDays(): List<LocalDate> = eventsByDay.keys.toList()
    override suspend fun deleteCacheForDay(date: LocalDate) = Unit
}

/**
 * Test double: in-memory NudgePreferences.
 */
class FakeNudgePreferences(
    enabled: Boolean = true,
    threshold: Float = 3.0f
) : NudgePreferences {
    private var enabled = enabled
    private var threshold = threshold
    private var allowlist: Set<String> = emptySet()
    private var baselineMap: Map<String, Float> = emptyMap()
    private var baselineCacheDate: String? = null

    override suspend fun isNudgeEnabled(): Boolean = enabled
    override fun setIsNudgeEnabled(isEnabled: Boolean) { enabled = isEnabled }
    override fun getThresholdMultiplier(): Float = threshold
    override fun setThresholdMultiplier(multiplier: Float) { threshold = multiplier }
    override fun getAllowlistedPackages(): Set<String> = allowlist
    override fun setAllowlistedPackages(packages: Set<String>) { allowlist = packages }
    override fun getBaselineMap(): Map<String, Float> = baselineMap
    override fun setBaselineMap(map: Map<String, Float>) { baselineMap = map }
    override fun getBaselineCacheDate(): String? = baselineCacheDate
    override fun setBaselineCacheDate(date: String?) { baselineCacheDate = date }
}

object TestFixtures {
    fun usageEvent(
        packageName: String,
        type: UsageDataItem.EventType,
        timestamp: Long
    ) = UsageDataItem(
        appName = packageName,
        packageName = packageName,
        usageTimestamp = timestamp,
        usageType = type,
        appCategory = UsageDataItem.Category.SOCIAL
    )
}
