package ly.com.tahaben.farhan

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ly.com.tahaben.core.R
import ly.com.tahaben.core.model.UIModeAppearance
import ly.com.tahaben.core.navigation.Routes
import ly.com.tahaben.core.util.UiEvent
import ly.com.tahaben.core_ui.theme.VikasTheme
import ly.com.tahaben.domain.preferences.Preferences
import ly.com.tahaben.farhan.db.DatabaseCombineHelper
import ly.com.tahaben.infinite_scroll_blocker_domain.use_cases.InfiniteScrollUseCases
import ly.com.tahaben.infinite_scroll_blocker_presentation.InfiniteScrollingBlockerScreen
import ly.com.tahaben.infinite_scroll_blocker_presentation.exceptions.InfiniteScrollExceptionsScreen
import ly.com.tahaben.infinite_scroll_blocker_presentation.onboarding.InfiniteScrollOnBoardingScreen
import ly.com.tahaben.onboarding_presentaion.OnBoardingScreen
import ly.com.tahaben.onboarding_presentaion.SelectAppearanceScreen
import ly.com.tahaben.onboarding_presentaion.main.MainScreen
import ly.com.tahaben.onboarding_presentaion.main.MainScreenEvent
import ly.com.tahaben.onboarding_presentaion.main.MainScreenViewModel
import javax.inject.Inject
import kotlin.random.Random
import androidx.compose.runtime.collectAsState

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var infiniteScrollUseCases: InfiniteScrollUseCases

    @Inject
    lateinit var onBoardingPref: Preferences

    @Inject
    lateinit var databaseCombineHelper: DatabaseCombineHelper

    val mainScreenViewModel by viewModels<MainScreenViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val shouldShowOnBoarding = onBoardingPref.loadShouldShowOnBoarding()
        var shouldShowSelectThemeScreen: Boolean
        runBlocking {
            shouldShowSelectThemeScreen = (onBoardingPref.loadThemeColors() == "Unknown")
        }
        val tip = getTip()
        val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        setContent {

            val mainState = mainScreenViewModel.mainScreenState.collectAsState().value
            val isDarkMode = when (mainState.uiMode) {
                UIModeAppearance.DARK_MODE -> true
                UIModeAppearance.LIGHT_MODE -> false
                UIModeAppearance.FOLLOW_SYSTEM -> isSystemInDarkTheme()
            }
            val view = LocalView.current
            if (!view.isInEditMode) {
                LaunchedEffect(isDarkMode) {
                    val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
                    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                        !isDarkMode
                }
            }
            VikasTheme(
                darkMode = isDarkMode,
                colorStyle = mainState.themeColors
            ) {
                val navController = rememberNavController()
                val snackbarHostState = remember {
                    SnackbarHostState()
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) {
                    it.calculateBottomPadding()
                    NavHost(
                        navController = navController,
                        startDestination = if (shouldShowOnBoarding) {
                            Routes.WELCOME
                        } else if (shouldShowSelectThemeScreen) {
                            Routes.SELECT_THEME
                        } else {
                            Routes.MAIN
                        },
                    ) {
                        composable(Routes.WELCOME) {
                            OnBoardingScreen(
                                onFinishOnBoarding = {
                                    navController.navigate(Routes.SELECT_THEME) {
                                        popUpTo(Routes.WELCOME) {
                                            inclusive = true
                                        }
                                    }
                                }
                            )
                        }
                        composable(Routes.SELECT_THEME) {
                            SelectAppearanceScreen(
                                state = mainState,
                                onEvent = mainScreenViewModel::onEvent,
                                onOkClick = {
                                    navController.navigate(Routes.MAIN) {
                                        popUpTo(Routes.SELECT_THEME) {
                                            inclusive = true
                                        }
                                    }
                                }
                            )
                        }
                        composable(Routes.MAIN) {
                            val mainScreenUiEvent =
                                mainScreenViewModel.uiEvent.collectAsState(
                                    initial = UiEvent.HideSnackBar
                                ).value
                            
                            val infiniteScrollBlockEnabled = runBlocking { infiniteScrollUseCases.isServiceEnabled() }

                            MainScreen(
                                tip = tip,
                                isGrayscaleEnabled = false,
                                isInfiniteScrollBlockerEnabled =  infiniteScrollBlockEnabled &&
                                        infiniteScrollUseCases.isAccessibilityPermissionGranted(),
                                isNotificationFilterEnabled = false,
                                isLauncherEnabled = false,
                                isDelayedLaunchEnabled = false,
                                isNudgeEnabled = false,
                                navController = navController,
                                onEvent = { event ->
                                    when (event) {
                                        MainScreenEvent.OnCombineDbAgreeClick -> {
                                            mainScreenViewModel.onEvent(event)
                                            coroutineScope.launch {
                                                val result =
                                                    databaseCombineHelper.combineDatabases()
                                                mainScreenViewModel.onEvent(MainScreenEvent.OnDismissCombineDbDialog)
                                                if (result) {
                                                    snackbarHostState.showSnackbar(getString(R.string.db_migration_sucsessful))
                                                } else {
                                                    snackbarHostState.showSnackbar(getString(R.string.db_migration_fail))
                                                    finish()
                                                }
                                            }
                                        }

                                        MainScreenEvent.OnExitApp -> finish()
                                        else -> mainScreenViewModel.onEvent(event)
                                    }
                                },
                                state = mainState,
                                snackbarHostState = snackbarHostState,
                                uiEvent = mainScreenUiEvent
                            )
                        }
                        composable(Routes.INFINITE_SCROLLING) {
                            if (infiniteScrollUseCases.loadShouldShowOnBoarding()) {
                                InfiniteScrollOnBoardingScreen(
                                    onNextClick = {
                                        navController.navigate(Routes.INFINITE_SCROLLING) {
                                            popUpTo(Routes.MAIN)
                                        }
                                    }
                                )
                            } else {
                                InfiniteScrollingBlockerScreen(
                                    onNavigateUp = { navController.navigateUp() },
                                    onNavigateToExceptions = { navController.navigate(Routes.INFINITE_SCROLLING_EXCEPTIONS) }
                                )
                            }
                        }
                        composable(Routes.INFINITE_SCROLLING_EXCEPTIONS) {
                            InfiniteScrollExceptionsScreen(
                                scaffoldState = snackbarHostState,
                                onNavigateUp = { navController.navigateUp() },
                            )
                        }
                    }
                }
            }
        }
    }

    private fun getTip(): String {
        val array: Array<String> = this.resources.getStringArray(R.array.tips)
        return array[Random.nextInt(array.size)]
    }

    private fun shouldShowRational(permission: String): Boolean {
        return shouldShowRequestPermissionRationale(
            permission
        )
    }
}
