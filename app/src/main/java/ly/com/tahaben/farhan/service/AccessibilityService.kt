package ly.com.tahaben.farhan.service

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.CountDownTimer
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ly.com.tahaben.core.R
import ly.com.tahaben.core.service.RunningService
import ly.com.tahaben.core.service.RunningServicesNotifier
import ly.com.tahaben.core_ui.LocalSpacing
import ly.com.tahaben.core_ui.theme.VikasTheme
import ly.com.tahaben.core_ui.use_cases.UiUseCases
import ly.com.tahaben.core_ui.util.attachOverlay
import ly.com.tahaben.core_ui.util.isCurrentlyDark
import ly.com.tahaben.infinite_scroll_blocker_domain.model.ScrollViewInfo
import ly.com.tahaben.infinite_scroll_blocker_domain.use_cases.InfiniteScrollUseCases
import ly.com.tahaben.usage_overview_domain.repository.UnlockRepository
import timber.log.Timber
import java.time.LocalDate
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@AndroidEntryPoint
class AccessibilityService : AccessibilityService() {

    private var recentScrollViews: HashMap<Int, ScrollViewInfo> = hashMapOf()
    private val softInputPackages = mutableListOf<String>()

    @Inject
    lateinit var infiniteScrollUseCases: InfiniteScrollUseCases

    @Inject
    lateinit var uiUseCases: UiUseCases

    @Inject
    lateinit var runningServicesNotifier: RunningServicesNotifier

    @Inject
    lateinit var unlockRepository: UnlockRepository

    private var lastUnlockOverlayShownAt = 0L

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_ON) {
                val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
                if (keyguardManager.isKeyguardLocked) return
            }
            val now = System.currentTimeMillis()
            if (now - lastUnlockOverlayShownAt < 5000L) return
            lastUnlockOverlayShownAt = now
            scope.launch {
                unlockRepository.incrementUnlock(LocalDate.now())
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).enabledInputMethodList.forEach {
            softInputPackages.add(it.packageName)
        }
        registerReceiver(unlockReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_ON)
        })
    }

    private val activityClassCache = HashMap<String, Boolean>()
    private val appLaunchDetector = AppLaunchDetector(
        isActivityClass = { pkg, cls ->
            activityClassCache.getOrPut("$pkg/$cls") {
                try {
                    packageManager.getActivityInfo(ComponentName(pkg, cls), 0)
                    true
                } catch (e: PackageManager.NameNotFoundException) {
                    false
                }
            }
        },
        ignoredClassNames = emptySet()
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        val foregroundPackage = rootInActiveWindow?.packageName?.toString()
        appLaunchDetector.seed(foregroundPackage)
        runningServicesNotifier.serviceStarted(RunningService.ACCESSIBILITY)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return

        val infiniteScrollBlockEnabled = runBlocking { infiniteScrollUseCases.isServiceEnabled() }
        if (infiniteScrollBlockEnabled && packageName != this.packageName) {
            if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
                if (!infiniteScrollUseCases.isPackageInInfiniteScrollExceptions(packageName)) {
                    listenToScrollEvent(event)
                }
            }
        }

        // YouTube Shorts Blocker
        if (packageName == "com.google.android.youtube" || packageName == "com.google.android.apps.youtube.mango") {
            val isShortsBlockerEnabled = runBlocking { infiniteScrollUseCases.isShortsBlockerEnabled() }
            if (isShortsBlockerEnabled) {
                checkAndBlockYouTubeShorts(event)
            }
        }

        // Instagram Reels Blocker
        if (packageName == "com.instagram.android" || packageName == "com.instagram.lite") {
            val isReelsBlockerEnabled = runBlocking { infiniteScrollUseCases.isReelsBlockerEnabled() }
            if (isReelsBlockerEnabled) {
                checkAndBlockInstagramReels(event)
            }
        }
    }

    private var lastShortsSeenTime = 0L
    private var shortsSessionStartTime = 0L
    private var lastShortsBlockTime = 0L

    private var lastReelsSeenTime = 0L
    private var reelsSessionStartTime = 0L
    private var lastReelsBlockTime = 0L

    private fun checkAndBlockYouTubeShorts(event: AccessibilityEvent) {
        val now = System.currentTimeMillis()
        if (now - lastShortsBlockTime < 1200L) return

        val rootNode = rootInActiveWindow ?: return
        try {
            val shortsViewIds = listOf(
                "com.google.android.youtube:id/reel_recycler",
                "com.google.android.youtube:id/reel_watch_fragment_container",
                "com.google.android.youtube:id/shorts_container",
                "com.google.android.youtube:id/reel_player_page_v2",
                "com.google.android.youtube:id/reel_player_page",
                "com.google.android.youtube:id/reel_player_view",
                "com.google.android.youtube:id/shorts_shelf_item",
                "com.google.android.youtube:id/reel_player_header_layout"
            )

            var isShortsActive = false
            for (viewId in shortsViewIds) {
                val nodes = rootNode.findAccessibilityNodeInfosByViewId(viewId)
                if (nodes.isNotEmpty()) {
                    isShortsActive = true
                    nodes.forEach { it.recycle() }
                    break
                }
            }

            if (!isShortsActive) {
                val className = event.className?.toString() ?: ""
                if (className.contains("Reel", ignoreCase = true) || className.contains("Shorts", ignoreCase = true)) {
                    isShortsActive = true
                }
            }

            if (isShortsActive) {
                val blockedUntil = infiniteScrollUseCases.getShortsBlockedUntil()
                val allowedDurationMinutes = infiniteScrollUseCases.getShortsAllowedDuration()
                val blockDurationMinutes = infiniteScrollUseCases.getShortsBlockDuration()

                if (now < blockedUntil) {
                    lastShortsBlockTime = now
                    val remainingMin = ((blockedUntil - now) / 60_000L).toInt() + 1
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    scope.launch(Dispatchers.Main) {
                        Toast.makeText(this@AccessibilityService, "Shorts on cooldown ($remainingMin min)", Toast.LENGTH_SHORT).show()
                    }
                    return
                }

                if (allowedDurationMinutes == 0) {
                    lastShortsBlockTime = now
                    val newBlockedUntil = now + blockDurationMinutes * 60_000L
                    infiniteScrollUseCases.setShortsBlockedUntil(newBlockedUntil)
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    return
                }

                if (now - lastShortsSeenTime > 30_000L || shortsSessionStartTime == 0L) {
                    shortsSessionStartTime = now
                }
                lastShortsSeenTime = now

                val sessionDurationMillis = now - shortsSessionStartTime
                val allowedMillis = allowedDurationMinutes * 60_000L

                if (sessionDurationMillis >= allowedMillis) {
                    lastShortsBlockTime = now
                    shortsSessionStartTime = 0L
                    val newBlockedUntil = now + blockDurationMinutes * 60_000L
                    infiniteScrollUseCases.setShortsBlockedUntil(newBlockedUntil)
                    performGlobalAction(GLOBAL_ACTION_BACK)
                }
            } else {
                if (now - lastShortsSeenTime > 15_000L) {
                    shortsSessionStartTime = 0L
                }
            }
        } finally {
            rootNode.recycle()
        }
    }

    private fun checkAndBlockInstagramReels(event: AccessibilityEvent) {
        val now = System.currentTimeMillis()
        if (now - lastReelsBlockTime < 1200L) return

        val rootNode = rootInActiveWindow ?: return
        try {
            val reelsViewIds = listOf(
                "com.instagram.android:id/clips_video_container",
                "com.instagram.android:id/clips_viewer_view_pager",
                "com.instagram.android:id/reel_viewer_container",
                "com.instagram.android:id/clips_action_bar_layout",
                "com.instagram.android:id/reel_viewer_framelayout",
                "com.instagram.android:id/clips_swipe_refresh_layout",
                "com.instagram.android:id/clips_media_item"
            )

            var isReelsActive = false
            for (viewId in reelsViewIds) {
                val nodes = rootNode.findAccessibilityNodeInfosByViewId(viewId)
                if (nodes.isNotEmpty()) {
                    isReelsActive = true
                    nodes.forEach { it.recycle() }
                    break
                }
            }

            if (!isReelsActive) {
                val className = event.className?.toString() ?: ""
                if (className.contains("Clips", ignoreCase = true) || className.contains("Reel", ignoreCase = true)) {
                    isReelsActive = true
                }
            }

            if (isReelsActive) {
                val blockedUntil = infiniteScrollUseCases.getReelsBlockedUntil()
                val allowedDurationMinutes = infiniteScrollUseCases.getShortsAllowedDuration()
                val blockDurationMinutes = infiniteScrollUseCases.getShortsBlockDuration()

                if (now < blockedUntil) {
                    lastReelsBlockTime = now
                    val remainingMin = ((blockedUntil - now) / 60_000L).toInt() + 1
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    scope.launch(Dispatchers.Main) {
                        Toast.makeText(this@AccessibilityService, "Reels on cooldown ($remainingMin min)", Toast.LENGTH_SHORT).show()
                    }
                    return
                }

                if (allowedDurationMinutes == 0) {
                    lastReelsBlockTime = now
                    val newBlockedUntil = now + blockDurationMinutes * 60_000L
                    infiniteScrollUseCases.setReelsBlockedUntil(newBlockedUntil)
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    return
                }

                if (now - lastReelsSeenTime > 30_000L || reelsSessionStartTime == 0L) {
                    reelsSessionStartTime = now
                }
                lastReelsSeenTime = now

                val sessionDurationMillis = now - reelsSessionStartTime
                val allowedMillis = allowedDurationMinutes * 60_000L

                if (sessionDurationMillis >= allowedMillis) {
                    lastReelsBlockTime = now
                    reelsSessionStartTime = 0L
                    val newBlockedUntil = now + blockDurationMinutes * 60_000L
                    infiniteScrollUseCases.setReelsBlockedUntil(newBlockedUntil)
                    performGlobalAction(GLOBAL_ACTION_BACK)
                }
            } else {
                if (now - lastReelsSeenTime > 15_000L) {
                    reelsSessionStartTime = 0L
                }
            }
        } finally {
            rootNode.recycle()
        }
    }

    private fun listenToScrollEvent(event: AccessibilityEvent) {
        val maxY = if (event.itemCount > 0) event.itemCount else -1
        if (event.source == null || event.className == null || maxY == -1) return

        val scrollViewId = (
                event.className.hashCode() +
                        event.packageName.hashCode() +
                        (event.source!!.viewIdResourceName?.hashCode() ?: 1) +
                        event.source!!.getBoundsInScreen(Rect()).hashCode()
                )
        if (!recentScrollViews.containsKey(scrollViewId)) {
            recentScrollViews[scrollViewId] = ScrollViewInfo(maxY)
        } else {
            val scrollViewInfo = recentScrollViews[scrollViewId]!!
            val timeSinceLastUpdate = (System.currentTimeMillis() - scrollViewInfo.updatedAt).milliseconds.inWholeMinutes
            if (timeSinceLastUpdate > 3) {
                recentScrollViews.clear()
                return
            }
            var isInfinite = false
            if (maxY > scrollViewInfo.maxY) {
                scrollViewInfo.timesGrown++
                scrollViewInfo.maxY = maxY
                scrollViewInfo.updatedAt = System.currentTimeMillis()
                if (scrollViewInfo.timesGrown >= 3) {
                    isInfinite = true
                }
            }

            val scrollingTime = (System.currentTimeMillis() - scrollViewInfo.addedAt).milliseconds.inWholeMinutes

            if (isInfinite && scrollingTime >= infiniteScrollUseCases.getTimeOutDuration()) {
                showDialog(event.packageName.toString())
                recentScrollViews.clear()
            }
        }
    }

    private val windowManager get() = getSystemService(WINDOW_SERVICE) as WindowManager

    @OptIn(ExperimentalMaterial3Api::class)
    fun showDialog(packageName: String) {
        val countDownSeconds = infiniteScrollUseCases.getCountDown()
        val layoutFlag: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val isDarkMode = uiUseCases.isDarkModeEnabled()
        val themeColors = uiUseCases.getCurrentThemeColors()
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        val dialogMsg = infiniteScrollUseCases.getDialogMessage()
        val composeView = ComposeView(this)
        composeView.setContent {
            var currentCountDown by remember { mutableIntStateOf(countDownSeconds) }
            val dismissEnabled by remember { derivedStateOf { currentCountDown <= 0 } }
            val timer = object : CountDownTimer(countDownSeconds.seconds.inWholeMilliseconds, 1.seconds.inWholeMilliseconds) {
                override fun onTick(p0: Long) { currentCountDown -= 1 }
                override fun onFinish() { currentCountDown = 0 }
            }
            DisposableEffect(key1 = Unit) {
                timer.start()
                onDispose { timer.cancel() }
            }
            VikasTheme(darkMode = isDarkMode.isCurrentlyDark(), colorStyle = themeColors) {
                val scope = rememberCoroutineScope()
                val bottomSheetState = rememberBottomSheetScaffoldState(
                    bottomSheetState = rememberStandardBottomSheetState(
                        initialValue = SheetValue.Expanded,
                        skipHiddenState = false,
                    )
                )
                val spacing = LocalSpacing.current
                fun dismissOverlay() {
                    scope.launch {
                        bottomSheetState.bottomSheetState.hide()
                    }.invokeOnCompletion {
                        windowManager.removeView(composeView)
                    }
                }

                BottomSheetScaffold(
                    modifier = Modifier.fillMaxSize(),
                    scaffoldState = bottomSheetState,
                    sheetDragHandle = { },
                    sheetContent = {
                        Column(
                            modifier = Modifier.padding(spacing.spaceExtraLarge),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(spacing.spaceSmall)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Hey!",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                            Row(Modifier.fillMaxWidth()) {
                                Text(modifier = Modifier.weight(0.8f), text = dialogMsg)
                                Spacer(modifier = Modifier.width(spacing.spaceSmall))
                                Crossfade(
                                    modifier = Modifier.weight(0.2f),
                                    targetState = dismissEnabled
                                ) { isEnabled ->
                                    if (!isEnabled) {
                                        AnimatedContent(targetState = currentCountDown) {
                                            Text(text = it.toString(), fontStyle = FontStyle.Italic)
                                        }
                                    }
                                }
                            }

                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    dismissOverlay()
                                    this@AccessibilityService.startActivity(
                                        Intent(Intent.ACTION_MAIN)
                                            .addCategory(Intent.CATEGORY_HOME)
                                            .apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                                    )
                                }) {
                                Text(text = "Take me out of here")
                            }
                            Text(
                                modifier = Modifier.clickable { if (dismissEnabled) dismissOverlay() else Unit },
                                text = "Continue scrolling",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    },
                    sheetSwipeEnabled = false,
                    containerColor = BottomSheetDefaults.ScrimColor
                ) {

                }
            }
        }
        composeView.attachOverlay(windowManager, params)
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        recentScrollViews.clear()
        softInputPackages.clear()
        appLaunchDetector.reset()
        runningServicesNotifier.serviceStopped(RunningService.ACCESSIBILITY)
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        recentScrollViews.clear()
        softInputPackages.clear()
        appLaunchDetector.reset()
        runningServicesNotifier.serviceStopped(RunningService.ACCESSIBILITY)
        unregisterReceiver(unlockReceiver)
        scope.cancel()
        super.onDestroy()
    }
}
