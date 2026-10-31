package ly.com.tahaben.nudge_domain.use_cases

import ly.com.tahaben.nudge_domain.preferences.NudgePreferences
import ly.com.tahaben.usage_overview_domain.model.UsageDataItem
import ly.com.tahaben.usage_overview_domain.repository.UsageRepository
import java.time.LocalDate

/**
 * Per-app average opens-per-hour over the user's own recent history (default 7 days).
 *
 * "Opens" = cached `UsageDataItem` events whose type is `EventType.Start`
 * (i.e. `UsageEvents.Event.MOVE_TO_FOREGROUND`). The result is cached in prefs keyed
 * by the current date so it is recomputed at most once per day; the caller compares it
 * against the live per-hour open count to detect *deviation from the user's own norm*,
 * not an arbitrary threshold.
 */
class GetHourlyOpenBaseline(
    private val usageRepository: UsageRepository,
    private val preferences: NudgePreferences
) {

    suspend operator fun invoke(packageName: String): Float {
        val today = LocalDate.now()
        val cacheDate = preferences.getBaselineCacheDate()
        val map = if (cacheDate == today.toString()) {
            preferences.getBaselineMap()
        } else {
            computeAndCache(today)
        }
        return map[packageName] ?: 0f
    }

    private suspend fun computeAndCache(today: LocalDate): Map<String, Float> {
        val counts = mutableMapOf<String, Int>()
        var daysWithData = 0
        var d = today.minusDays(DAYS_WINDOW)
        while (d < today) {
            val events = usageRepository.getUsageEventsFromDb(d)
            if (events.isNotEmpty()) daysWithData++
            events.forEach { item ->
                if (item.usageType == UsageDataItem.EventType.Start) {
                    counts[item.packageName] = (counts[item.packageName] ?: 0) + 1
                }
            }
            d = d.plusDays(1L)
        }
        // Guard against division by zero for a fresh install with no history.
        val hours = (maxOf(daysWithData, 1) * 24).toFloat()
        val result = counts.mapValues { it.value / hours }
        preferences.setBaselineMap(result)
        preferences.setBaselineCacheDate(today.toString())
        return result
    }

    companion object {
        private const val DAYS_WINDOW: Long = 7
    }
}
