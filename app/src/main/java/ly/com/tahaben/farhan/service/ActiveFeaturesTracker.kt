package ly.com.tahaben.farhan.service

import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import ly.com.tahaben.core.service.ActiveFeature
import ly.com.tahaben.core.service.RunningServicesNotifier
import ly.com.tahaben.infinite_scroll_blocker_domain.use_cases.InfiniteScrollUseCases
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import ly.com.tahaben.infinite_scroll_blocker_domain.preferences.Preferences as InfiniteScrollPreferences

/**
 * Watches every feature toggle that the ongoing "Vikas is active" notification reports on and
 * pushes the current set of enabled features to [RunningServicesNotifier], so the notification
 * only ever lists features that are really on (each toggle check also implies the app main
 * switch is on).
 */
@Singleton
class ActiveFeaturesTracker @Inject constructor(
    private val sharedPref: SharedPreferences,
    private val dataStore: DataStore<Preferences>,
    private val infiniteScrollUseCases: InfiniteScrollUseCases,
    private val runningServicesNotifier: RunningServicesNotifier
) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun start() {
        scope.launch {
            merge(sharedPrefToggleChanges(), dataStore.data.map { })
                .conflate()
                .collect {
                    val features = enabledFeatures()
                    Timber.d("enabled features: $features")
                    runningServicesNotifier.setEnabledFeatures(features)
                }
        }
    }

    private fun sharedPrefToggleChanges(): Flow<Unit> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == null || key in TOGGLE_KEYS) {
                trySend(Unit)
            }
        }
        sharedPref.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { sharedPref.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    private suspend fun enabledFeatures(): Set<ActiveFeature> = buildSet {
        if (infiniteScrollUseCases.isServiceEnabled()) {
            add(ActiveFeature.INFINITE_SCROLL_BLOCKER)
        }
    }

    companion object {
        private val TOGGLE_KEYS = setOf(
            InfiniteScrollPreferences.KEY_INFINITE_SCROLL_SERVICE_STATS
        )
    }
}
