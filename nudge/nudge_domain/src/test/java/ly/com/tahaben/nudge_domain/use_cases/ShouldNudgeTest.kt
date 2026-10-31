package ly.com.tahaben.nudge_domain.use_cases

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import ly.com.tahaben.nudge_domain.util.SAFETY_ALLOWLIST
import ly.com.tahaben.usage_overview_domain.model.UsageDataItem
import org.junit.Test

class ShouldNudgeTest {

    private val today = LocalDate.now()
    private val yesterday = today.minusDays(1L)
    private val twoDaysAgo = today.minusDays(2L)
    private val threeDaysAgo = today.minusDays(3L)

    /** 3 days × 8 opens/day of "social" → hourly average = 24 / (3*24) = 0.333… */
    private fun repoWith(opensPerDay: Int, days: List<LocalDate>): FakeUsageRepository {
        val byDay = days.associateWith { day ->
            List(opensPerDay) { i ->
                TestFixtures.usageEvent(
                    packageName = "com.example.social",
                    type = UsageDataItem.EventType.Start,
                    timestamp = day.atStartOfDay().plusSeconds(i.toLong() * 3600)
                        .toInstant(java.time.ZoneOffset.UTC).toEpochMilli()
                )
            }
        }
        return FakeUsageRepository(byDay)
    }

    private fun nudgeUseCases(
        repo: FakeUsageRepository,
        prefs: FakeNudgePreferences = FakeNudgePreferences()
    ): NudgeUseCases {
        val enabled = IsNudgeEnabled(prefs)
        val getThreshold = GetThresholdMultiplier(prefs)
        val getBaseline = GetHourlyOpenBaseline(repo, prefs)
        val getAllowlisted = GetAllowlistedPackages(prefs)
        return NudgeUseCases(
            isNudgeEnabled = enabled,
            setIsNudgeEnabled = SetIsNudgeEnabled(prefs),
            getThresholdMultiplier = getThreshold,
            setThresholdMultiplier = SetThresholdMultiplier(prefs),
            getAllowlistedPackages = getAllowlisted,
            setAllowlistedPackages = SetAllowlistedPackages(prefs),
            getHourlyOpenBaseline = getBaseline,
            getLiveHourlyOpens = GetLiveHourlyOpens(repo),
            shouldNudge = ShouldNudge(enabled, getThreshold, getBaseline, getAllowlisted)
        )
    }

    @Test
    fun `baseline is hourly average over cached days`() = runTest {
        val repo = repoWith(opensPerDay = 8, days = listOf(yesterday, twoDaysAgo, threeDaysAgo))
        val useCases = nudgeUseCases(repo)
        val baseline = useCases.getHourlyOpenBaseline("com.example.social")
        // 24 opens / (3 days * 24 hours) = 0.333…
        assertThat(baseline).isWithin(0.001f).of(24f / (3 * 24))
    }

    @Test
    fun `nudges when opens cross the personal baseline times multiplier`() = runTest {
        val repo = repoWith(opensPerDay = 8, days = listOf(yesterday, twoDaysAgo, threeDaysAgo))
        val prefs = FakeNudgePreferences(threshold = 3.0f)
        val useCases = nudgeUseCases(repo, prefs)
        // baseline ≈ 0.333 → threshold ≈ 1.0 → 1 open this hour should trigger
        assertThat(useCases.shouldNudge("com.example.social", currentHourlyOpenCount = 2)).isTrue()
        // 0 opens this hour should not
        assertThat(useCases.shouldNudge("com.example.social", currentHourlyOpenCount = 0)).isFalse()
    }

    @Test
    fun `does not nudge when toggle is off`() = runTest {
        val repo = repoWith(opensPerDay = 8, days = listOf(yesterday, twoDaysAgo, threeDaysAgo))
        val useCases = nudgeUseCases(repo, FakeNudgePreferences(enabled = false))
        assertThat(useCases.shouldNudge("com.example.social", currentHourlyOpenCount = 50)).isFalse()
    }

    @Test
    fun `does not nudge allowlisted packages`() = runTest {
        val repo = repoWith(opensPerDay = 8, days = listOf(yesterday, twoDaysAgo, threeDaysAgo))
        val prefs = FakeNudgePreferences()
        prefs.setAllowlistedPackages(setOf("com.example.social"))
        val useCases = nudgeUseCases(repo, prefs)
        assertThat(useCases.shouldNudge("com.example.social", currentHourlyOpenCount = 99)).isFalse()
    }

    @Test
    fun `never nudges the hardcoded safety allowlist`() = runTest {
        assertThat(SAFETY_ALLOWLIST).contains("com.google.android.apps.maps")
        val repo = FakeUsageRepository()
        val useCases = nudgeUseCases(repo)
        assertThat(useCases.shouldNudge("com.google.android.apps.maps", currentHourlyOpenCount = 500))
            .isFalse()
    }

    @Test
    fun `does not nudge apps with no history`() = runTest {
        val repo = FakeUsageRepository()
        val useCases = nudgeUseCases(repo)
        assertThat(useCases.shouldNudge("com.example.brandnew", currentHourlyOpenCount = 5)).isFalse()
    }

    @Test
    fun `baseline is cached and reused within a day`() = runTest {
        val repo = repoWith(opensPerDay = 8, days = listOf(yesterday, twoDaysAgo, threeDaysAgo))
        val prefs = FakeNudgePreferences()
        val useCases = nudgeUseCases(repo, prefs)
        val first = useCases.getHourlyOpenBaseline("com.example.social")
        val second = useCases.getHourlyOpenBaseline("com.example.social")
        assertThat(first).isEqualTo(second)
        assertThat(prefs.getBaselineCacheDate()).isEqualTo(today.toString())
    }
}
