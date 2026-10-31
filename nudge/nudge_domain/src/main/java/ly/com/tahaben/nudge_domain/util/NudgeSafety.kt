package ly.com.tahaben.nudge_domain.util

/**
 * Packages that must never be nudged — emergency/baseline-critical apps.
 * Mirrors the user-specified "maps, calls, banking always work" rule.
 * This is a sensible default set; users can extend the allowlist per app.
 */
val SAFETY_ALLOWLIST: Set<String> = setOf(
    // dialer / phone
    "com.android.dialer",
    "com.google.android.dialer",
    // sms / messaging
    "com.android.mms",
    "com.google.android.apps.messaging",
    // maps / navigation
    "com.google.android.apps.maps",
    "com.google.android.Maps",
    "com.sygic.brainstorm",
    // camera
    "com.android.camera",
    "com.google.android.camera",
    // browser
    "com.android.chrome",
    "com.google.android.apps.chrome"
)
