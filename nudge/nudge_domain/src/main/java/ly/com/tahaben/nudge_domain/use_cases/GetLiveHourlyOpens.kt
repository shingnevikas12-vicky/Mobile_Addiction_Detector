package ly.com.tahaben.nudge_domain.use_cases

import ly.com.tahaben.usage_overview_domain.model.UsageDataItem
import ly.com.tahaben.usage_overview_domain.repository.UsageRepository
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Opens of each package in the *current hour so far today*, read from the cached usage DB.
 *
 * Used to seed the in-memory counter in the AccessibilityService when it (re)connects, so a
 * service restart doesn't lose the opens that already happened this hour. Start events are
 * stored with a negated timestamp (see `UsageEventMapper`), so their absolute value is the
 * real event epoch-millis.
 */
class GetLiveHourlyOpens(private val usageRepository: UsageRepository) {

    suspend operator fun invoke(): Map<String, Int> {
        val today = LocalDate.now()
        val now = LocalDateTime.now()
        val zone = ZoneId.systemDefault()
        val hourStart = now.withMinute(0).withSecond(0).withNano(0).atZone(zone).toInstant().toEpochMilli()
        val hourEnd = hourStart + HOUR_MS
        val counts = mutableMapOf<String, Int>()
        usageRepository.getUsageEventsFromDb(today).forEach { item ->
            if (item.usageType == UsageDataItem.EventType.Start) {
                val epoch = kotlin.math.abs(item.usageTimestamp)
                if (epoch >= hourStart && epoch < hourEnd) {
                    counts[item.packageName] = (counts[item.packageName] ?: 0) + 1
                }
            }
        }
        return counts
    }

    companion object {
        private const val HOUR_MS = 3_600_000L
    }
}
