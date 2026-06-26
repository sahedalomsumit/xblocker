# XBlocker

XBlocker is an Android app that blocks adult content in browsers using an **Accessibility Service** — no root, no VPN, no cloud. The blocker is on by default, and turning it off or removing any site from your custom blocklist requires a 3-hour wait, so the protection actually holds.

## 1. Overview

XBlocker is a fully offline, on-device Android application that blocks adult/explicit web content in real-time by monitoring browser address bars via Android's Accessibility Service API. When a blocked domain is detected, XBlocker navigates the user away and shows a "Site Blocked" overlay. It also lets users manually add custom URLs to a blocklist. The app enforces friction-based delays — users must wait 3 hours before disabling the blocker or removing any blocked URL — preventing impulsive bypassing.

**No VPN. No cloud database. No account required. No data leaves the device.**

## 2. Core Features

### 2.1 Browser Content Blocking (Accessibility Service)

- Monitors the URL bar of all major browsers in real-time
- No VPN, no root — only Accessibility permission required
- Default blocklist of 500+ known adult domains (bundled as asset)
- Blocker is ON by default after first setup
- When a blocked domain is detected:
  - Presses **Back** to navigate away from the page
  - Shows a full-screen **"Site Blocked"** overlay with the domain name
- Supported browsers: Chrome, Brave, Samsung Internet, Firefox, Edge, DuckDuckGo, Opera, Vivaldi, Kiwi

### 2.2 Custom Blocklist

- Dedicated tab: user can manually add any domain
- Supports wildcard patterns: `*.example.com`
- Shows list of all manually added entries with status
- Removing an entry triggers the 3-hour delay

### 2.3 3-Hour Delay System

- Disabling the blocker: starts a 3-hour countdown timer
- Removing a manually added URL: same 3-hour countdown
- Timer persists across app restarts (stored in Room DB via WorkManager)
- User can cancel a pending disable request during the wait
- Clear visual countdown shown in UI (HH:MM:SS)

### 2.4 Permission Onboarding

- Full permission request flow on first launch
- **Accessibility permission** (mandatory — for URL bar monitoring)
- Notification permission (for countdown alerts)
- App auto-detects when Accessibility Service is enabled and advances onboarding
- Cannot complete onboarding without Accessibility permission

### 2.5 UI

- Dark theme only — matches brand colors
- Bottom nav: Dashboard / Blocklist / Settings
- Live stats: domains blocked today, blocker status, active timer

## 3. Tech Stack

| Layer              | Tool                         | Purpose                                |
| :----------------- | :--------------------------- | :------------------------------------- |
| **Language**       | Kotlin 2.x                   | Primary language                       |
| **Min SDK**        | API 26 (Android 8.0)         | Accessibility API + modern features    |
| **Target SDK**     | API 35 (Android 15)          | Latest Play Store requirement          |
| **Architecture**   | MVVM + Clean Architecture    | Separation of concerns                 |
| **UI**             | Jetpack Compose              | Modern declarative UI                  |
| **Navigation**     | Navigation Compose           | Bottom nav + screen routing            |
| **DI**             | Hilt (Dagger)                | Dependency injection                   |
| **Local DB**       | Room + SQLite                | Blocklist + timer state                |
| **Async**          | Kotlin Coroutines + Flow     | Background service + DB ops            |
| **Blocking**       | Android AccessibilityService | Monitor browser URL bars               |
| **Domain Match**   | Custom `DnsBlockEngine`      | O(1) HashSet + wildcard suffix lookup  |
| **Notifications**  | NotificationManager          | Countdown alerts                       |
| **Background**     | WorkManager                  | 3-hour timer persistence               |
| **Preferences**    | DataStore (Preferences)      | App settings                           |
| **Testing**        | JUnit + MockK + Espresso     | Unit + UI tests                        |

## 4. Project Structure

```
app/
├── src/main/
│   ├── java/com/sahed/xblocker/
│   │   ├── data/
│   │   │   ├── db/              # Room DB, DAOs, entities
│   │   │   ├── repository/      # BlocklistRepo, TimerRepo
│   │   │   └── datastore/       # App settings (DataStore)
│   │   ├── domain/
│   │   │   ├── model/           # BlockedDomain, TimerState
│   │   │   └── usecase/         # AddDomain, RequestDisable, etc.
│   │   ├── service/
│   │   │   ├── XBlockerAccessibilityService.kt  # Core blocking service
│   │   │   ├── DnsBlockEngine.kt                # Domain matching engine
│   │   │   ├── TimerWorker.kt                   # WorkManager 3h timer
│   │   │   └── BootReceiver.kt                  # State sync on boot
│   │   ├── ui/
│   │   │   ├── MainActivity.kt
│   │   │   ├── BlockedActivity.kt   # "Site Blocked" overlay
│   │   │   ├── dashboard/           # Home screen
│   │   │   ├── blocklist/           # Custom URL tab
│   │   │   ├── settings/            # Settings screen
│   │   │   ├── onboarding/          # Permission flow
│   │   │   └── theme/               # Colors, typography
│   │   └── di/                      # Hilt modules
│   ├── assets/
│   │   └── blocklist.txt            # Default adult domain list
│   └── res/
│       └── xml/
│           └── accessibility_service_config.xml
└── build.gradle.kts
```

## 5. Android Permissions

| Permission                         | Required      | Use                                        |
| :--------------------------------- | :------------ | :----------------------------------------- |
| **BIND_ACCESSIBILITY_SERVICE**     | YES — mandatory | Monitor browser URL bars                 |
| **POST_NOTIFICATIONS**             | YES (API 33+) | Countdown notifications                    |
| **RECEIVE_BOOT_COMPLETED**         | YES           | Sync blocker state after device reboot     |
| **SCHEDULE_EXACT_ALARM**           | YES           | Precise 3-hour timer delivery              |
| **USE_EXACT_ALARM**                | YES (API 33+) | Exact alarm permission                     |
| **INTERNET**                       | YES           | Asset loading, future updates              |

### Manifest Declaration

```xml
<service
    android:name=".service.XBlockerAccessibilityService"
    android:exported="true"
    android:label="@string/accessibility_service_label"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
    <intent-filter>
        <action android:name="android.accessibilityservice.AccessibilityService" />
    </intent-filter>
    <meta-data
        android:name="android.accessibilityservice"
        android:resource="@xml/accessibility_service_config" />
</service>
```

## 6. Accessibility Service Architecture

### How It Works

```
User opens browser → navigates to a URL
        ↓
XBlockerAccessibilityService.onAccessibilityEvent()
    [TYPE_WINDOW_CONTENT_CHANGED | TYPE_WINDOW_STATE_CHANGED]
        ↓
extractUrlFromEvent()
    1. Try known view IDs (e.g. "com.android.chrome:id/url_bar")
    2. Fallback: BFS traversal for EditText nodes that look like URLs
        ↓
parseDomain(url)
    URI parsing → extract host → strip "www." prefix
        ↓
DnsBlockEngine.shouldBlock(domain)
    ├── BLOCKED → performGlobalAction(BACK) + launch BlockedActivity
    └── ALLOWED → do nothing
```

### Accessibility Service Config

```xml
<!-- res/xml/accessibility_service_config.xml -->
<accessibility-service
    android:accessibilityEventTypes="typeWindowContentChanged|typeWindowStateChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagDefault|flagRetrieveInteractiveWindows|flagReportViewIds"
    android:canRetrieveWindowContent="true"
    android:notificationTimeout="100" />
```

### Supported Browsers

| Browser             | Package Name                        |
| :------------------ | :---------------------------------- |
| Chrome              | `com.android.chrome`                |
| Brave               | `com.brave.browser`                 |
| Samsung Internet    | `com.sec.android.app.sbrowser`      |
| Firefox             | `org.mozilla.firefox`               |
| Firefox Fenix       | `org.mozilla.fenix`                 |
| Microsoft Edge      | `com.microsoft.emmx`                |
| DuckDuckGo          | `com.duckduckgo.mobile.android`     |
| Opera               | `com.opera.browser`                 |
| Vivaldi             | `com.vivaldi.browser`               |
| Kiwi Browser        | `com.kiwibrowser.browser`           |

### Domain Matching Engine (`DnsBlockEngine`)

```kotlin
fun shouldBlock(domain: String): Boolean {
    val normalized = domain.lowercase().trimEnd('.')
    // O(1) exact match
    if (blocklist.contains(normalized)) return true
    // Wildcard suffix match (*.example.com)
    return wildcards.any { normalized == it || normalized.endsWith(".$it") }
}
```

### Default Blocklist Asset

- Bundled at `assets/blocklist.txt` — one domain per line
- Loaded into memory (`HashSet<String>`) on service connect
- User custom entries loaded from Room DB and merged at startup
- Use open-source lists: Steven Black hosts, oisd.nl, hagezi

## 7. Database Schema (Room)

### Table: `blocked_domains`

| Column             | Type           | Nullable | Description             |
| :----------------- | :------------- | :------- | :---------------------- |
| **id**             | INTEGER PK     | No       | Auto-increment          |
| **domain**         | TEXT           | No       | e.g. `example.com`      |
| **is_custom**      | INTEGER (bool) | No       | 1 = user-added          |
| **added_at**       | INTEGER        | No       | Unix timestamp          |
| **pending_delete** | INTEGER (bool) | No       | Awaiting 3h timer       |
| **delete_at**      | INTEGER        | Yes      | Epoch for deletion time |
| **blocked_count**  | INTEGER        | No       | Times blocked (stats)   |

### Table: `timer_events`

| Column           | Type           | Nullable | Description                         |
| :--------------- | :------------- | :------- | :---------------------------------- |
| **id**           | INTEGER PK     | No       | Auto-increment                      |
| **event_type**   | TEXT           | No       | `DISABLE_BLOCKER` / `REMOVE_DOMAIN` |
| **triggered_at** | INTEGER        | No       | When request was made               |
| **execute_at**   | INTEGER        | No       | `triggered_at` + 10800000ms         |
| **domain_id**    | INTEGER FK     | Yes      | Ref `blocked_domains(id)`           |
| **is_complete**  | INTEGER (bool) | No       | 0 = pending                         |
| **cancelled**    | INTEGER (bool) | No       | User cancelled                      |

## 8. 3-Hour Delay Logic

### Disable Blocker Flow

- User taps "Disable (3hr delay)"
- App creates a `timer_event` row (`event_type = DISABLE_BLOCKER`)
- Schedules a `WorkManager` one-time task with 3h initial delay
- UI shows live countdown: "Blocker disables in 02:47:31"
- User can tap "Cancel" to abort — cancels the WorkManager task and marks `cancelled = 1`
- After 3 hours, `TimerWorker` sets `isBlockerActive = false` in DataStore
- The Accessibility Service reads this flag and skips all events

### Remove Domain Flow

- User taps delete icon on a custom domain
- App creates `timer_event` (`event_type = REMOVE_DOMAIN`, `domain_id = X`)
- Domain shows "Pending removal in 02:47:31" state in list
- After 3 hours, domain row deleted from DB and engine reloads

## 9. UI Screens

### 9.1 Onboarding (First Launch)

1. **Welcome** — intro to XBlocker and how it works
2. **Accessibility Permission** — opens system Accessibility Settings; auto-advances when XBlocker is enabled (detected via `LifecycleEventObserver` on `ON_RESUME`)
3. **Notifications** — optional `POST_NOTIFICATIONS` request
4. **All Set** — blocker is now active

### 9.2 Dashboard Tab

- Blocker status chip: Active (emerald pulse) / Inactive (muted)
- "Enable in Settings" button → opens Accessibility Settings
- "Disable (3hr delay)" button → triggers countdown
- Domains blocked today counter
- Active countdown card (HH:MM:SS) with Cancel button
- Info cards: Custom Rules count, Default Blocklist size, "No VPN Required"

### 9.3 Blocklist Tab

- Add domain input + button at top
- Filter: All / Custom / Pending
- List of custom entries with domain, date added, status badge
- Pending delete entries show countdown + Cancel button
- Swipe to delete triggers 3h timer

### 9.4 Settings Tab

- Default blocklist toggle (disable/enable bundled list)
- Auto-start on boot toggle
- App version, privacy notice

## 10. Brand & Theme (Jetpack Compose)

```kotlin
// Colors
val Accent    = Color(0xFF8B5CF6)  // Purple
val Emerald   = Color(0xFF10B981)  // Active / success
val Rose      = Color(0xFFE05252)  // Blocked / danger
val Amber     = Color(0xFFF59E0B)  // Timer / warning
val BgDark    = Color(0xFF050505)  // Near black background
val CardBg    = Color(0xFF0D0D0D)  // Card surface
val Border    = Color(0x14FFFFFF)  // rgba white 8%
val TextMain  = Color(0xFFFFFFFF)
val TextMuted = Color(0xFF94A3B8)
```

## 11. Gradle Dependencies

`build.gradle.kts (app)`:

```kotlin
// Compose
implementation(platform("androidx.compose:compose-bom:2024.x"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.navigation:navigation-compose:2.8.x")

// Lifecycle
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.x")
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.x")

// Hilt DI
implementation("com.google.dagger:hilt-android:2.x")
ksp("com.google.dagger:hilt-compiler:2.x")
implementation("androidx.hilt:hilt-navigation-compose:1.2.x")
implementation("androidx.hilt:hilt-work:1.2.x")

// Room
implementation("androidx.room:room-runtime:2.6.x")
implementation("androidx.room:room-ktx:2.6.x")
ksp("androidx.room:room-compiler:2.6.x")

// DataStore
implementation("androidx.datastore:datastore-preferences:1.1.x")

// WorkManager (for 3h timer persistence)
implementation("androidx.work:work-runtime-ktx:2.9.x")
```

## 12. Known Challenges & Solutions

| Challenge                                 | Solution                                                                                        |
| :---------------------------------------- | :---------------------------------------------------------------------------------------------- |
| **OEM battery killers (Xiaomi, Samsung)** | Guide user to disable battery optimization. Accessibility Services typically survive better.    |
| **Browser updates changing view IDs**     | Use a priority list of known IDs + BFS traversal fallback for unknown/updated browsers.         |
| **Browser with no URL bar exposed**       | Service silently ignores packages not in the supported browser list.                            |
| **Large blocklist memory usage**          | `HashSet<String>` — O(1) lookup. 200k domains ≈ 30MB RAM.                                     |
| **Play Store Accessibility policy**       | Service description clearly states it only reads URL text, not keystrokes or personal data.     |
| **Timer accuracy after doze mode**        | `WorkManager` with exact alarms where available (`SCHEDULE_EXACT_ALARM`).                       |
| **Disable timer bypass**                  | Service checks `isBlockerActive` flag on every event — can't be bypassed by clearing app data. |

## 13. Open Source Blocklists (Default Bundle)

- Steven Black hosts: [github.com/StevenBlack/hosts](https://github.com/StevenBlack/hosts)
- OISD Full: [oisd.nl](https://oisd.nl) (nsfw category)
- HaGeZi Ultimate: [github.com/hagezi/dns-blocklists](https://github.com/hagezi/dns-blocklists)
- Combine, deduplicate, strip comments → save as `assets/blocklist.txt`

## 14. Play Store Submission Notes

- Category: Tools or Parental Controls
- Content rating: Everyone (it blocks adult content, not shows it)
- Accessibility declaration: Must provide justification for `BIND_ACCESSIBILITY_SERVICE` — describe that the service only reads the URL bar to filter adult content
- Privacy Policy required — mention: no data collected, no network calls, no keystrokes read
- Short description: Block adult sites in any browser with time-delay safeguards
- Target API: Must be API 34+ as of August 2024 requirement

## 15. Dev Environment Setup

| Tool                | Version / Notes                                               |
| :------------------ | :------------------------------------------------------------ |
| **Android Studio**  | Ladybug (2024.2.x) or newer                                   |
| **JDK**             | 17 (bundled with Android Studio)                              |
| **Kotlin**          | 2.x                                                           |
| **Gradle**          | 8.x with Kotlin DSL (.kts)                                    |
| **AGP**             | 8.x (Android Gradle Plugin)                                   |
| **Min test device** | Android 8.0 physical device (Accessibility Service testing)   |
| **Emulator**        | Works for UI and accessibility service testing                |

---

**XBlocker** — Built for focus. Enforced by design.

_Developed by Sahed Alom Sumit_
