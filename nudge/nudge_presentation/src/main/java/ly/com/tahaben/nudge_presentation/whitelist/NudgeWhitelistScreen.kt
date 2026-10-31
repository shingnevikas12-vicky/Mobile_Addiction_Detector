package ly.com.tahaben.nudge_presentation.whitelist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ly.com.tahaben.core.R
import ly.com.tahaben.core_ui.LocalSpacing
import ly.com.tahaben.core_ui.components.AppExceptionListItem
import ly.com.tahaben.core_ui.components.SearchTextField
import ly.com.tahaben.core_ui.mirror
import ly.com.tahaben.core.util.SearchEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NudgeWhitelistScreen(
    onNavigateUp: () -> Unit,
    viewModel: NudgeWhitelistViewModel = hiltViewModel(),
) {
    val spacing = LocalSpacing.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current
    var displaySearchField by rememberSaveable { mutableStateOf(false) }
    var mDisplayMenu by rememberSaveable { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.nudge_whitelist_title)) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            ),
            navigationIcon = {
                IconButton(onClick = onNavigateUp) {
                    Icon(
                        modifier = Modifier.mirror(),
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back)
                    )
                }
            },
            actions = {
                IconButton(onClick = {
                    displaySearchField = !displaySearchField
                    if (!displaySearchField) viewModel.onEvent(SearchEvent.HideSearch)
                }) {
                    if (displaySearchField) {
                        Icon(Icons.Default.Close, stringResource(R.string.close_search))
                    } else {
                        Icon(Icons.Default.Search, stringResource(R.string.open_search_field))
                    }
                }
                AnimatedVisibility(visible = displaySearchField) {
                    SearchTextField(
                        text = state.query,
                        onValueChange = { viewModel.onEvent(SearchEvent.OnQueryChange(it)) },
                        shouldShowHint = state.isHintVisible,
                        onSearch = {
                            keyboardController?.hide()
                            viewModel.onEvent(SearchEvent.OnSearch)
                        },
                        onFocusChanged = { viewModel.onEvent(SearchEvent.OnSearchFocusChange(it.isFocused)) }
                    )
                }
                IconButton(onClick = { mDisplayMenu = !mDisplayMenu }) {
                    Icon(Icons.Default.MoreVert, stringResource(R.string.drop_down_menu))
                }
                DropdownMenu(
                    expanded = mDisplayMenu,
                    onDismissRequest = { mDisplayMenu = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(R.string.show_system_apps),
                                textAlign = TextAlign.Center
                            )
                        },
                        onClick = {
                            viewModel.onEvent(
                                SearchEvent.OnSystemAppsVisibilityChange(!state.showSystemApps)
                            )
                        },
                        trailingIcon = {
                            Checkbox(
                                checked = state.showSystemApps,
                                onCheckedChange = {
                                    viewModel.onEvent(
                                        SearchEvent.OnSystemAppsVisibilityChange(it)
                                    )
                                }
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(R.string.show_whitelist_only),
                                textAlign = TextAlign.Center
                            )
                        },
                        onClick = {
                            viewModel.onEvent(
                                SearchEvent.OnExceptionsOnlyChange(!state.showAllowlistOnly)
                            )
                        },
                        trailingIcon = {
                            Checkbox(
                                checked = state.showAllowlistOnly,
                                onCheckedChange = {
                                    viewModel.onEvent(SearchEvent.OnExceptionsOnlyChange(it))
                                }
                            )
                        }
                    )
                }
            }
        )
        Crossfade(
            targetState = state.isLoading,
            animationSpec = tween(durationMillis = 200),
            label = "whitelist loading"
        ) {
            if (it) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = spacing.spaceMedium, vertical = spacing.spaceSmall)
                ) {
                    items(state.searchResults) { app ->
                        AppExceptionListItem(
                            app = app,
                            onClick = { isChecked ->
                                if (isChecked) {
                                    viewModel.addAppToAllowlist(app.packageName)
                                } else {
                                    viewModel.removeAppFromAllowlist(app.packageName)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
