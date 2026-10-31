package ly.com.tahaben.nudge_data.di

import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ly.com.tahaben.nudge_data.preferences.DefaultNudgePreferences
import ly.com.tahaben.nudge_domain.preferences.NudgePreferences
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NudgeDataModule {

    @Provides
    @Singleton
    fun provideNudgePreferences(
        sharedPref: SharedPreferences,
        dataStore: DataStore<Preferences>
    ): NudgePreferences = DefaultNudgePreferences(sharedPref, dataStore)
}
