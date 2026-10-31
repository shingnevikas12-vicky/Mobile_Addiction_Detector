package ly.com.tahaben.nudge_domain.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ly.com.tahaben.nudge_domain.preferences.NudgePreferences
import ly.com.tahaben.nudge_domain.use_cases.GetAllowlistedPackages
import ly.com.tahaben.nudge_domain.use_cases.GetHourlyOpenBaseline
import ly.com.tahaben.nudge_domain.use_cases.GetLiveHourlyOpens
import ly.com.tahaben.nudge_domain.use_cases.GetThresholdMultiplier
import ly.com.tahaben.nudge_domain.use_cases.IsNudgeEnabled
import ly.com.tahaben.nudge_domain.use_cases.NudgeUseCases
import ly.com.tahaben.nudge_domain.use_cases.SetAllowlistedPackages
import ly.com.tahaben.nudge_domain.use_cases.SetIsNudgeEnabled
import ly.com.tahaben.nudge_domain.use_cases.SetThresholdMultiplier
import ly.com.tahaben.nudge_domain.use_cases.ShouldNudge
import ly.com.tahaben.usage_overview_domain.repository.UsageRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NudgeDomainModule {

    @Provides
    @Singleton
    fun provideNudgeUseCases(
        preferences: NudgePreferences,
        usageRepository: UsageRepository
    ): NudgeUseCases {
        val isNudgeEnabled = IsNudgeEnabled(preferences)
        val getThreshold = GetThresholdMultiplier(preferences)
        val getAllowlisted = GetAllowlistedPackages(preferences)
        val getBaseline = GetHourlyOpenBaseline(usageRepository, preferences)
        return NudgeUseCases(
            isNudgeEnabled = isNudgeEnabled,
            setIsNudgeEnabled = SetIsNudgeEnabled(preferences),
            getThresholdMultiplier = getThreshold,
            setThresholdMultiplier = SetThresholdMultiplier(preferences),
            getAllowlistedPackages = getAllowlisted,
            setAllowlistedPackages = SetAllowlistedPackages(preferences),
            getHourlyOpenBaseline = getBaseline,
            getLiveHourlyOpens = GetLiveHourlyOpens(usageRepository),
            shouldNudge = ShouldNudge(isNudgeEnabled, getThreshold, getBaseline, getAllowlisted)
        )
    }
}
