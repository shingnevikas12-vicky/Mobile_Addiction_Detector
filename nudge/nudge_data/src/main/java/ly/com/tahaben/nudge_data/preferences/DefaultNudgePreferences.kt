package ly.com.tahaben.nudge_data.preferences

import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.booleanPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import ly.com.tahaben.core.util.GlobalKey
import ly.com.tahaben.nudge_domain.preferences.NudgePreferences

class DefaultNudgePreferences(
    private val sharedPref: SharedPreferences,
    private val dataStore: DataStore<androidx.datastore.preferences.core.Preferences>
) : NudgePreferences {

    private val mainSwitchKey = booleanPreferencesKey(GlobalKey.Pref_KEY_APP_MAIN_SWITCH)

    // Mirrors notification_filter DataStore gating: toggles are honored only when the
    // user has flipped the global app main switch on.
    override suspend fun isNudgeEnabled(): Boolean =
        sharedPref.getBoolean(NudgePreferences.KEY_NUDGE_ENABLED, false) &&
            dataStore.data.map { it[mainSwitchKey] ?: true }.first()

    override fun setIsNudgeEnabled(isEnabled: Boolean) {
        sharedPref.edit()
            .putBoolean(NudgePreferences.KEY_NUDGE_ENABLED, isEnabled)
            .apply()
    }

    override fun getThresholdMultiplier(): Float =
        sharedPref.getFloat(NudgePreferences.KEY_THRESHOLD_MULTIPLIER, DEFAULT_THRESHOLD_MULTIPLIER)

    override fun setThresholdMultiplier(multiplier: Float) {
        sharedPref.edit()
            .putFloat(NudgePreferences.KEY_THRESHOLD_MULTIPLIER, multiplier)
            .apply()
    }

    override fun getAllowlistedPackages(): Set<String> =
        sharedPref.getStringSet(NudgePreferences.KEY_ALLOWLIST, emptySet()) ?: emptySet()

    override fun setAllowlistedPackages(packages: Set<String>) {
        sharedPref.edit()
            .putStringSet(NudgePreferences.KEY_ALLOWLIST, packages)
            .apply()
    }

    // Per-package float baselines stored under a prefixed key (SharedPreferences has no
    // Map<String,Float>), so we reconstruct the map by scanning keys with the prefix.
    override fun getBaselineMap(): Map<String, Float> {
        val result = mutableMapOf<String, Float>()
        val prefix = NudgePreferences.KEY_BASELINE_PREFIX
        for ((key, value) in sharedPref.all) {
            if (key.startsWith(prefix) && value is Float) {
                result[key.removePrefix(prefix)] = value
            }
        }
        return result
    }

    override fun setBaselineMap(map: Map<String, Float>) {
        val prefix = NudgePreferences.KEY_BASELINE_PREFIX
        val editor = sharedPref.edit()
        // drop any stale entries before rewriting
        getBaselineMap().keys.forEach { editor.remove(prefix + it) }
        map.forEach { (pkg, rate) -> editor.putFloat(prefix + pkg, rate) }
        editor.apply()
    }

    override fun getBaselineCacheDate(): String? =
        sharedPref.getString(NudgePreferences.KEY_BASELINE_CACHE_DATE, null)

    override fun setBaselineCacheDate(date: String?) {
        sharedPref.edit()
            .putString(NudgePreferences.KEY_BASELINE_CACHE_DATE, date)
            .apply()
    }

    companion object {
        const val DEFAULT_THRESHOLD_MULTIPLIER = 3.0f
    }
}
