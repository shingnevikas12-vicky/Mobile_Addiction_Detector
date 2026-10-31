package ly.com.tahaben.nudge_presentation.whitelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ly.com.tahaben.core.data.repository.InstalledAppsRepository
import ly.com.tahaben.core.model.AppItem
import ly.com.tahaben.core.util.SearchEvent
import ly.com.tahaben.nudge_domain.use_cases.NudgeUseCases
import javax.inject.Inject

@HiltViewModel
class NudgeWhitelistViewModel @Inject constructor(
    private val nudgeUseCases: NudgeUseCases,
    private val installedAppsRepo: InstalledAppsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(NudgeWhitelistState())
    val state = _state.asStateFlow()

    init {
        getInstalledApps()
    }

    private fun getInstalledApps() {
        viewModelScope.launch {
            val allowlist = nudgeUseCases.getAllowlistedPackages()
            withContext(Dispatchers.IO) {
                val apps = installedAppsRepo.getInstalledApps().sortedBy { it.name }
                apps.forEach {
                    it.isException = it.packageName in allowlist
                }
                val notInstalledApps = allowlist
                    .filterNot { packageName ->
                        packageName in apps.map { app -> app.packageName }
                    }
                    .map { AppItem(name = it, packageName = it, isException = true) }
                _state.update {
                    it.copy(
                        isLoading = false,
                        appsList = apps + notInstalledApps,
                        searchResults = apps + notInstalledApps
                    )
                }
            }
            filterSystemApps()
        }
    }

    fun onEvent(event: SearchEvent) {
        when (event) {
            is SearchEvent.OnQueryChange -> {
                _state.update { it.copy(query = event.query) }
                executeSearch()
            }

            SearchEvent.OnSearch -> executeSearch()

            is SearchEvent.OnSearchFocusChange -> {
                _state.update { it.copy(isHintVisible = !event.isFocused && it.query.isBlank()) }
            }

            is SearchEvent.OnSystemAppsVisibilityChange -> {
                _state.update { it.copy(showSystemApps = event.showSystemApps) }
                filterSystemApps()
            }

            SearchEvent.HideSearch -> {
                _state.update { it.copy(query = "") }
                filterSystemApps()
            }

            is SearchEvent.OnExceptionsOnlyChange -> {
                _state.update { it.copy(showAllowlistOnly = event.showExceptionsOnly) }
                filterAllowlistOnly()
            }
        }
    }

    private fun executeSearch() {
        val results = state.value.appsList.filter {
            it.name?.contains(state.value.query, true) == true ||
                it.packageName.contains(state.value.query, true)
        }
        _state.update { it.copy(searchResults = results) }
    }

    private fun filterSystemApps() {
        _state.update { currentState ->
            val filtered = if (currentState.showSystemApps) {
                currentState.appsList
            } else {
                currentState.appsList.filter { !it.isSystemApp }
            }
            currentState.copy(searchResults = filtered)
        }
        if (state.value.showAllowlistOnly) filterAllowlistOnly()
    }

    private fun filterAllowlistOnly() {
        _state.update { currentState ->
            if (currentState.showAllowlistOnly) {
                currentState.copy(searchResults = currentState.searchResults.filter { it.isException })
            } else {
                currentState.copy()
            }
        }
    }

    fun addAppToAllowlist(packageName: String) {
        val set = nudgeUseCases.getAllowlistedPackages().toMutableSet()
        set.add(packageName)
        nudgeUseCases.setAllowlistedPackages(set)
    }

    fun removeAppFromAllowlist(packageName: String) {
        val set = nudgeUseCases.getAllowlistedPackages().toMutableSet()
        set.remove(packageName)
        nudgeUseCases.setAllowlistedPackages(set)
    }
}
