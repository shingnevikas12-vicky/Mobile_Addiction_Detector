# Digital Wellness Feature Map — implementability without an external service

A grounded read of the feature map against **Android-only, on-device-only** implementation. Where a feature needs a server, bank API, or cloud identity, that's called out explicitly. Statuses reference this repo's current state where relevant.

## Legend (implementability)

- **DONE** — already implemented in Vikas (on-device).
- **ON-DEVICE** — feasible on Android with no server, given the user grants the listed permission(s). May need a foreground service or a Settings-level grant (not a runtime dialog).
- **ON-DEVICE \*caveat** — feasible, but with a noted platform/policy or battery caveat.
- **NEEDS EXTERNAL** — cannot be honestly done without a server / third-party / cloud identity.
- **APPLE-ONLY-LIMITED** — possible in spirit on Android, but explicitly NOT possible as a 3rd-party iOS app (Apple locks Screen Time usage data to Family Controls / the system setting).

---

## Summary matrix

| # | Feature | Status | Notes |
|---|---------|--------|-------|
| 1.1 | Screen time per app/category | DONE | `UsageStatsManager.queryEvents` + Room cache + `CacheWorker`. Category from `ApplicationInfo.category`. `PACKAGE_USAGE_STATS` (Settings grant). |
| 1.2 | Unlock/pickup count | ON-DEVICE *caveat | True pickup needs `SensorManager` accelerometer (battery-sensitive). Vikas uses `ACTION_SCREEN_ON`/`USER_PRESENT` as a cheap unlock proxy (DONE). |
| 1.3 | Session length distribution | DONE-ish | Derived from foreground/background usage events + `AppLaunchDetector.awayTimeMillis`. Not surfaced in UI yet. |
| 1.4 | Time-of-day heatmap | ON-DEVICE | From usage-event timestamps. Trivial once 1.1 data is cached. |
| 1.5 | App-switching frequency | DONE | `AppLaunchDetector` on `TYPE_WINDOW_STATE_CHANGED`. Fires `AccessibilityEvent` per switch. |
| 1.6 | Notification reaction time | ON-DEVICE *caveat | Correlate `onNotificationPosted` (key) → app foreground (`UsageStatsManager`/`AccessibilityService`). Noisy (in-app notifs); linkable on-device. |
| 1.7 | Phantom checking | ON-DEVICE *caveat | Accelerometer pickup + short session + return-to-sleep. Same sensor caveat as 1.2. |
| 1.8 | Scroll velocity/distance | ON-DEVICE | `TYPE_VIEW_SCROLLED` carries `itemCount`/maxScroll; velocity = delta/time. Vikas inspects scrolls for infinite-scroll (DONE). Velocity is a small extension. |
| 2.1 | Personal baseline modeling | ON-DEVICE | Statistics / on-device ML (TFLite) over own history. No server. |
| 2.2 | Trigger detection | ON-DEVICE | Correlate with time, device location (`LocationManager`), calendar (`CalendarContract`). All on-device. |
| 2.3 | Craving vs. action logging | ON-DEVICE | Quick urge entry + "did I open it within X min?" self-log. |
| 2.4 | Trend reports | DONE | `CacheWorker` persists per-day aggregates; UI reads cached `UsageDao`. |
| 3.1 | Soft friction (pause/breath) | DONE | Delayed-launch overlay via `AccessibilityService`. |
| 3.2 | Hard limits + real friction | DONE | `TimeLimitService` foreground service polls foreground app; overlay requires re-entry of friction. |
| 3.3 | Notification batching/digest | ON-DEVICE | `NotificationListenerService` cancels + batches into a digest channel. Vikas cancels+logs (DONE base). |
| 3.4 | Contextual in-the-moment nudges | ON-DEVICE | Count opens per hour (1.1 data) → overlay/Toast. |
| 3.5 | Replacement prompts | ON-DEVICE | Overlay with a concrete 2-min alt + deep link to a calm activity. |
| 3.6 | Location/context rules | ON-DEVICE *caveat | `LocationManager` geofence on-device; battery-heavy if polled. No Google Play services required. |
| 3.7 | Emergency bypass | ON-DEVICE | Allowlist (maps/calls/banking); skip blocking for allowlisted packages. Must be explicit, non-bypassable-by-design. |
| 4.1 | Time-reclaimed visualization | ON-DEVICE | Compute minutes not spent because blocks fired. |
| 4.2 | Behavior milestones | ON-DEVICE | Reduce-by-X% streak vs. own baseline; see streak-opinion note below. |
| 4.3 | Commitment devices ($$$) | NEEDS EXTERNAL | Real monetary stake requires a payment provider + escrow/charity integration. Local "non-financial pledge" is implementable on-device. |
| 5.1 | Accountability partners | ON-DEVICE *caveat | Local peer sync (encrypted export + airdrop-style import) is feasible; real-time shared dashboard needs a backend. |
| 5.2 | Parental dashboards | ON-DEVICE *caveat | Locally-managed child profile or local-network (mDNS/Nearby) sync; cloud-based family dashboards need a backend. |
| 5.3 | Peer support communities | NEEDS EXTERNAL | Forum/community needs a server. |
| 5.4 | Exportable therapist reports | DONE | Room DB → JSON/CSV, user-controlled share. |
| 6.1 | Per-behavior concern selection | ON-DEVICE | Blocklist/allowlist per category; map apps to concerns. |
| 6.2 | Harm-reduction vs abstinence framing | ON-DEVICE | Goal-model toggle (reduction vs cutoff). |
| 7.1 | Gambling spend-pattern (bank link) | NEEDS EXTERNAL | Open Banking / bank API — heavily regulated; no honest on-device path. |
| 7.2 | Crisis resources for distress | ON-DEVICE | Static, localized helpline `tel:`/`sms:` links + deep links. Do not infer distress algorithmically beyond the user's own opt-in flags. |
| 8.1 | On-device processing by default | DONE (design) | All caching + analysis is local; no telemetry pipeline. |
| 8.2 | Never monetize attention data | DONE (policy) | No analytics/ad SDK collecting usage data. |
| 8.3 | No covert monitoring of adults | ON-DEVICE *caveat | Must be an explicit, consent-gated toggle; the app must be visibly present (persistent notification when active) so the monitored person always knows. |
| 8.4 | User owns data (export/delete) | DONE | Full DB export + wipe. |
| 9.1 | iOS Screen Time / Android Digital Wellbeing | APPLE-ONLY-LIMITED | iOS: a 3rd-party app cannot read per-app Screen Time usage at all (locked to Family Setup / system). Android: this is `UsageStatsManager` (DONE). |
| 9.2 | Cross-device sync | NEEDS EXTERNAL (real-time) | Phone+tablet local sync feasible (Nearby/Wi-Fi Direct, on-device); browser-extension sync needs cloud. |
| 9.3 | Wearable HR integration | ON-DEVICE *cavebet | Read a paired watch's HR via Bluetooth `Sensor.TYPE_HEART_RATE` (or GATT). Continuous HR is battery-heavy; coarse correlation is fine. |

> \*caveat items are implementable but come with a documented constraint (policy, battery, or permission grant path).

---

## Detailed notes per category

### 1. Core tracking (the raw signal)
Vikas's spine is `UsageRepositoryImpl` (`usage_overview` module): it requests `PACKAGE_USAGE_STATS`, walks `UsageEvents` from `queryEvents(from, to)`, maps each event to a `UsageDataItem` (with app label + category), and persists it through `UsageDao` via a `CacheWorker`. Event type `1` = `MOVE_TO_FOREGROUND` (app open), `2` = `MOVE_TO_BACKGROUND` (app close) — so session length and app-switch frequency fall straight out of the stream. `AppLaunchDetector` (`app/src/.../service/AppLaunchDetector.kt`) re-derives switches from `TYPE_WINDOW_STATE_CHANGED` in the `AccessibilityService` and already exposes `awayTimeMillis` (how long the app sat out of the foreground) — i.e., the raw material for session-length distribution.

- **Pickup count** is the one genuine gap. Android exposes **no public API** for "device picked up off a surface"; Pixels report it inside Digital Wellbeing but never to apps. Two honest paths:
  1. Keep the current cheap proxy (`ACTION_SCREEN_ON`/`USER_PRESENT`) — good enough for an **unlock** count and zero battery cost.
  2. Add `Sensor.TYPE_PICK_UP_GESTURE` or the accelerometer via `SensorManager` for a true pickup count. This must run in a foreground service or `JobScheduler` (background sensor access is throttled on Android 10+), and it is battery-sensitive. Mark it opt-in.
- **Notification reaction time** is linkable without a server: stash the `sbn.key` + post time in the `NotificationService` DB row, and match it against the next foreground event for the same package within a window (e.g., 10 min). Caveat: a notification for an in-app action will never correlate, so treat it as a signal, not a fact.
- **Scroll velocity** is a trivial generalization of the existing `listenToScrollEvent`: timestamp `TYPE_VIEW_SCROLLED` deltas and the per-event `itemCount` growth give velocity; `maxY`/`scrollY` give distance. It only sees apps that emit scroll events to accessibility (most do), so treat absence as "unknown," not "zero."

Design posture (per the feature map's warning): keep language to **pattern awareness**. This codebase already does the right thing — it surfaces timing/durations, not diagnoses. Don't bolt a "withdrawal" label onto a timestamp.

### 2. Pattern analysis / AI layer
Everything here is a transformation of the cached `UsageDao` rows, so all ON-DEVICE.
- **Personal baseline**: per-app daily usage, session count, and bedtime usage → mean/stddev over a rolling 14-day window; flag deviations (e.g., >1.5σ above your own baseline for that hour). A TFLite model is optional polish; simple stats catch most signal.
- **Trigger detection**: join usage with (a) hour-of-day, (b) `LocationManager` coarse location / geofence, (c) `CalendarContract` events (requires calendar permission). All local.
- **Trend reports**: already structurally present via `CacheWorker` + `isDayDataFullyUpdated`; wire UI to read `getUpdatedDays()` into a weekly/monthly view.
- **Craving vs. action**: a one-tap "I feel like opening X" entry; the next foreground event for X within a configurable window logs a "converted." Pure on-device correlation of two timestamps.

Honest opinion echoed: **no clinical framing**. "Withdrawal detection," "addiction severity," or "interventions on behalf of a user who hasn't opted in" are all out. The app owns *patterns*, the user owns *meaning*.

### 3. Intervention / friction tools
This is Vikas's sweet spot and the highest-leverage area.
- **Soft/hard friction** and **time limits** are DONE: `TimeLimitService` (foreground, wake-locked) polls the foreground package via `queryUsageStats(INTERVAL_BEST, …)` every second and shows an overlay dialog with friction to dismiss. Delayed launch (`showDelayedLaunchOverlay`) and delayed unlock (`showDelayedUnlockOverlay`) route through the `AccessibilityService`.
- **Contextual nudges** ("Instagram 14× this hour") are a count of foreground events per package per hour over the cached stream — feed it to a `Toast`/overlay in the moment.
- **Notification batching** is the next step past the current cancel-and-log: keep a rolling digest (e.g., "5 notifications batched from 3 apps") in a low-priority channel, delivered on a schedule the user picks.
- **Location/context rules** use `LocationManager` (GPS/network) — no Google Play services required. Caveat: don't spin a tight polling loop; use `PendingIntent` geofence (`addProximityAlert` / `Geofencing` via a foreground service) to wake only at boundaries.
- **Emergency bypass** is a safety-critical allowlist: before any block/overlay fires, check the package against a hardcoded + user-selected allowlist (Maps, Phone, SMS, Camera in emergencies, banking). The user's "emergency bypass, no matter what's blocked" is non-negotiable design, not a setting you can leave unenforced.

### 4. Motivation & reinforcement
- **Time-reclaimed** and **milestones** are arithmetic over the block/fire events already logged.
- **Commitment devices**: a real-money stake is the one place a local-only app is dishonest — escrow and charity payout need a payment provider, so either (a) make it non-financial ("public pledge the partner can see") or (b) own that it's a future, externally-backed feature. Don't fake it with a local counterfeit "points."
- **Streaks** (per the map's caution): fine as lightweight pattern feedback, but breaking a streak must stay **low-stakes** — no account-wide lockout, no "shame" messaging that re-rigs a loop. Tie milestones to *behavior change relative to the user's own baseline*, not to "did you open the lockout screen N days."

### 5. Social & accountability
- **Accountability partners**: true real-time shared stats need a backend, but a local, end-to-end-local path exists — export an **encrypted, time-boxed summary** (e.g., "X unlocks, Y hours on Z") as a QR/text that a partner scans/imports on their own device. That honors "share high-level stats, not raw logs" with zero server. Flag clearly which flows need a future backend.
- **Peer communities** are a hard NEEDS_EXTERNAL — no honest community feature ships without servers and moderation.
- **Therapist export** is DONE-adjacent: Room → JSON/CSV with a user-initiated share intent.

### 6. Personalization
Both are trivial ON-DEVICE toggles: a concern-category picker maps packages to goals, and a goal-mode toggle (`harm-reduction: "reduce Instagram by 40%" vs `abstinence: "0s Instagram`) selects the rule engine. Neither needs a server; both should drive the *detection thresholds*, not the other way around.

### 7. High-risk / crisis handling
- **Bank-linked gambling spend detection** is NEEDS_EXTERNAL and regulated — flag it as future + link to helplines in the meantime.
- **Crisis escalation** is ON-DEVICE and *must* be human-routed: a curated, localized list of helpline `tel:`/`sms:` URIs + deep links to official apps, triggered by the user's own explicit disclosures — never by inferring "distress" from scroll velocity. The app's job is to *point* to humans, not to play therapist.

### 8. Privacy & ethics (lead with this)
Vikas already embodies most of this: `PACKAGE_USAGE_STATS` + `NotificationListenerService` + `AccessibilityService` are all **opt-in, Settings-level grants**, the DB is local Room, export is a manual share intent, and uninstall wipes state. What to make explicit and *enforced*:
- **On-device by default**: no network calls from `AccessibilityService`/`NotificationService`/`UsageRepositoryImpl` for analysis. (Telemetry/analytics must be a separate, default-off module — and one this repo does not have.)
- **Never monetize**: no ad SDK, no attention-data egress. This is a competitive advantage for a wellness product, not a technical footnote.
- **No covert monitoring of adults**: the monitored adult must always know they're visible. Concretely: (1) a persistent, non-dismissable notification when any service is active; (2) a "monitored" indicator the target can tap to reveal who set it up; (3) a one-tap "pause everything for 24h" that requires no permission from the set-up partner. This is a hard constraint on parental/partner features, not a ToS line.

### 9. Ecosystem / technical
- **Android foundation**: `UsageStatsManager` (tracking) + `NotificationManager` (filtering) + `AccessibilityService` (friction/overlays/grayscale) + `LocationManager` (context) + `SensorManager` (pickup). This is the full on-device stack. iOS is APPLE-ONLY-LIMITED — a 3rd-party iOS wellness app cannot read per-app usage or intercept app opens at all.
- **Cross-device**: phone↔tablet **local sync** (EncryptedSharedPreferences / Room export via Nearby or a local-network sync channel) is ON-DEVICE; real-time unified dashboard + browser extension is NEEDS_EXTERNAL (cloud identity/sync).
- **Wearables**: Bluetooth HR from a paired watch is ON-DEVICE; it's a correlation signal (stress-driven vs boredom-driven usage), not a diagnostic, and should be coarse to protect battery.

Platform reality checks worth shipping in the README, since they explain distribution choices:
- `AccessibilityService` used for app-blocking / delayed-launch / grayscale **violates Play Store policy** for non-accessibility use cases — this is why Vikas is distributed via **IzzyOnDroid / F-Droid / direct APK**, not the Play Store. Be honest about this up front.
- `WRITE_SECURE_SETTINGS` (grayscale daltonizer) is **not a runtime permission** — it requires ADB (`adb shell cmd accessibility persist-scalar ...` / `settings put secure …`) or the app being a device owner / system app. A Play-distributed APK cannot self-grant it.
- `PACKAGE_USAGE_STATS` and notification-access **cannot** be requested via a runtime permission dialog — the app must send the user to `ACTION_USAGE_ACCESS_SETTINGS` / `ACTION_NOTIFICATION_ACCESS_SETTINGS` and trust them back. This isn't a bug; it's the API.

---

## What to build next, given Vikas's base
1. (DONE base exists) Wire `CacheWorker` rows into a **weekly heatmap** + **personal-baseline** deviation nudge. Highest signal/effort ratio.
2. (DONE base exists) Turn `TimeLimitService` + `AppLaunchDetector` into **contextual nudge counts** ("Instagram 12× this hour").
3. (Partial) Extend `NotificationService` to **batch** into a digest channel on a schedule.
4. (New, opt-in) Add a **pickup/phantom-checking** mode backed by `Sensor.TYPE_PICK_UP_GESTURE` in a foreground service, gated behind a clear battery/monitor warning.
5. (New) **Emergency bypass allowlist UI** — make the hardcoded list user-extensible and always-on.

Everything in 1–5 is ON-DEVICE with no server. Items 4.3, 5.1–5.3, 7.1, and 9.2 (cloud sync) are the honest NEEDS_EXTERNAL ones — keep them out of v1 scope rather than faking them locally.
