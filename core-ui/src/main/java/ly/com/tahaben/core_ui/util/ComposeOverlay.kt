package ly.com.tahaben.core_ui.util

import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner

/**
 * Wires a ComposeView inflated outside an Activity with a real lifecycle / ViewModelStore
 * and attaches it as an overlay window.
 *
 * This is the behavior-preserving extraction of the "Trick The ComposeView into thinking
 * we are tracking lifecycle" block that was duplicated in AccessibilityService.showDialog
 * and TimeLimitService.showTimeUpDialog. New overlay call sites (e.g. the contextual nudge)
 * should use [attachOverlay] / [dismissOverlay] instead of repeating the handshake.
 */
fun ComposeView.attachOverlay(
    windowManager: WindowManager,
    params: WindowManager.LayoutParams
): ComposeView {
    val viewModelStore = ViewModelStore()
    val viewModelStoreOwner = object : ViewModelStoreOwner {
        override val viewModelStore: ViewModelStore
            get() = viewModelStore
    }
    val lifecycleOwner = ComposeOverlayLifecycleOwner()
    lifecycleOwner.performRestore(null)
    lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    setViewTreeLifecycleOwner(lifecycleOwner)
    setViewTreeViewModelStoreOwner(viewModelStoreOwner)
    setViewTreeSavedStateRegistryOwner(lifecycleOwner)
    // This is required or otherwise the UI will not recompose.
    lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
    lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    windowManager.addView(this, params)
    return this
}

fun WindowManager.dismissOverlay(view: View) {
    removeView(view)
}
