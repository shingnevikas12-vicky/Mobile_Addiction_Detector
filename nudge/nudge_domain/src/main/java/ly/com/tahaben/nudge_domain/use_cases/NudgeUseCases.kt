package ly.com.tahaben.nudge_domain.use_cases

data class NudgeUseCases(
    val isNudgeEnabled: IsNudgeEnabled,
    val setIsNudgeEnabled: SetIsNudgeEnabled,
    val getThresholdMultiplier: GetThresholdMultiplier,
    val setThresholdMultiplier: SetThresholdMultiplier,
    val getAllowlistedPackages: GetAllowlistedPackages,
    val setAllowlistedPackages: SetAllowlistedPackages,
    val getHourlyOpenBaseline: GetHourlyOpenBaseline,
    val getLiveHourlyOpens: GetLiveHourlyOpens,
    val shouldNudge: ShouldNudge
)
