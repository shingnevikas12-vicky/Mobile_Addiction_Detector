package ly.com.tahaben.nudge_presentation.whitelist

import ly.com.tahaben.core.model.AppItem

data class NudgeWhitelistState(
    val appsList: List<AppItem> = emptyList(),
    val searchResults: List<AppItem> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = true,
    val isHintVisible: Boolean = true,
    val showSystemApps: Boolean = false,
    val showAllowlistOnly: Boolean = false
)
