# xblocker

XBlocker is an Android app that blocks adult content system-wide using on-device DNS filtering — no root, no cloud. The blocker is on by default, and turning it off or removing any site from your custom blocklist requires a 3-hour wait, so the protection actually holds.

## 1. Overview

XBlocker is a fully offline, on-device Android application that blocks adult/explicit web content at the device level using a VPN-based DNS filtering approach. It also lets users manually add custom URLs to a blocklist. The app enforces friction-based delays — users must wait 3 hours before disabling the blocker or removing any blocked URL — preventing impulsive bypassing.

No cloud database, no account required, no data leaves the device.

## 2. Core Features

### 2.1 Adult Content Blocking
* VPN-based local DNS filtering (no root required)
* Default blocklist of 50,000+ known adult domains (bundled as asset)
* Blocker is ON by default at first launch
* DNS queries resolved locally — blocked domains return `0.0.0.0`
* Works system-wide across all browsers and apps

### 2.2 Custom Blocklist
* Dedicated tab: user can manually add any domain/URL
* Supports wildcard patterns: `*.example.com`
* Shows list of all manually added entries with status
* Removing an entry triggers the 3-hour delay

### 2.3 3-Hour Delay System
* Disabling the blocker: starts a 3-hour countdown timer
* Removing a manually added URL: same 3-hour countdown
* Timer persists across app restarts (stored in Room DB)
* User can cancel pending disable request during the wait
* Clear visual countdown shown in UI (HH:MM:SS)

### 2.4 Permission Onboarding
* Full permission request flow on first launch
* VPN permission (mandatory for DNS filtering)
* Notification permission (for countdown alerts)
* Draw over other apps — optional, for overlay warnings
* Cannot proceed past onboarding without VPN permission

### 2.5 UI
* Dark theme only — matches brand colors
* Bottom nav: Dashboard / Blocklist / Settings
* Live stats: domains blocked today, VPN status, active timer

## 3. Tech Stack

| Layer | Tool | Purpose |
| :--- | :--- | :--- |
| **Language** | Kotlin 1.9+ | Primary language |
| **Min SDK** | API 26 (Android 8.0) | VPN API + modern features |
| **Target SDK** | API 35 (Android 15) | Latest Play Store requirement |
| **Architecture** | MVVM + Clean Architecture | Separation of concerns |
| **UI** | Jetpack Compose | Modern declarative UI |
| **Navigation** | Navigation Compose | Bottom nav + screen routing |
| **DI** | Hilt (Dagger) | Dependency injection |
| **Local DB** | Room + SQLite | Blocklist + timer state |
| **Async** | Kotlin Coroutines + Flow | Background VPN + DB ops |
| **VPN/DNS** | Android VpnService API | Intercept + filter DNS |
| **DNS Parsing** | `pcap4j` or custom DNS parser | Parse DNS UDP packets |
| **Notifications** | NotificationManager | Countdown alerts |
| **Foreground Svc** | ForegroundService | Keep VPN alive |
| **Preferences** | DataStore (Proto) | App settings |
| **Testing** | JUnit5 + MockK + Espresso | Unit + UI tests |

## 4. Project Structure

```
app/
├── src/main/
│   ├── java/com/xblocker/
│   │   ├── data/
│   │   │   ├── db/              # Room DB, DAOs, entities
│   │   │   ├── repository/      # BlocklistRepo, TimerRepo
│   │   │   └── datastore/       # App settings (DataStore)
│   │   ├── domain/
│   │   │   ├── model/           # BlockedDomain, TimerState
│   │   │   └── usecase/         # AddDomain, RequestDisable, etc.
│   │   ├── service/
│   │   │   ├── XBlockerVpnService.kt # Core VPN service
│   │   │   └── TimerService.kt  # 3h countdown
│   │   ├── ui/
│   │   │   ├── dashboard/       # Home screen
│   │   │   ├── blocklist/       # Custom URL tab
│   │   │   ├── settings/        # Settings screen
│   │   │   ├── onboarding/      # Permission flow
│   │   │   └── theme/           # Colors, typography
│   │   └── di/                  # Hilt modules
│   ├── assets/
│   │   └── blocklist.txt        # Default adult domain list
│   └── res/
└── build.gradle.kts
```

## 5. Android Permissions

Add these to `AndroidManifest.xml`:

| Permission | Required | Use |
| :--- | :--- | :--- |
| **BIND_VPN_SERVICE** | YES — mandatory | Run VPN tunnel |
| **FOREGROUND_SERVICE** | YES | Keep service alive |
| **FOREGROUND_SERVICE_SPECIAL_USE** | YES (API 34+) | VPN foreground type |
| **POST_NOTIFICATIONS** | YES (API 33+) | Countdown notifications |
| **RECEIVE_BOOT_COMPLETED** | YES | Auto-restart VPN on boot |
| **SYSTEM_ALERT_WINDOW** | Optional | Overlay block warning |

Also add in manifest:

```xml
<uses-permission android:name="android.permission.INTERNET" />

<service android:name=".service.XBlockerVpnService"
         android:permission="android.permission.BIND_VPN_SERVICE">
    <intent-filter>
        <action android:name="android.net.VpnService" />
    </intent-filter>
</service>
```

## 6. VPN / DNS Filtering Architecture

### How it works
* App creates a local VPN tunnel using `VpnService`
* All device traffic is routed through this tunnel
* DNS queries (UDP port 53) are intercepted and parsed
* If queried domain matches blocklist → return `0.0.0.0` (NXDOMAIN)
* All other traffic is forwarded to real upstream DNS (e.g. `1.1.1.1`)
* No actual VPN server — everything stays on-device

### VPN Configuration
```kotlin
val builder = Builder()
    .setSession("XBlocker")
    .addAddress("10.0.0.2", 32)
    .addDnsServer("10.0.0.1") // virtual DNS
    .addRoute("0.0.0.0", 0)   // route all traffic
    .setMtu(1500)
    .establish()
```

### DNS Matching Logic
```kotlin
fun shouldBlock(domain: String): Boolean {
    val normalized = domain.lowercase().trimEnd('.')
    return blocklist.contains(normalized)
        || blocklist.any { it.startsWith("_.") && normalized.endsWith(it.removePrefix("_.")) }
}
```

### Default Blocklist Asset
* Bundled at `assets/blocklist.txt` — one domain per line
* Loaded into memory (`HashSet<String>`) on VPN service start
* Use open-source lists: Steven Black hosts, oisd.nl, hagezi
* ~50k–200k domains, loads in < 2 seconds on modern devices
* User custom entries loaded from Room DB and merged at runtime

## 7. Database Schema (Room)

### Table: `blocked_domains`
| Column | Type | Nullable | Description |
| :--- | :--- | :--- | :--- |
| **id** | INTEGER PK | No | Auto-increment |
| **domain** | TEXT | No | e.g. example.com |
| **is_custom** | INTEGER (bool) | No | 1 = user-added |
| **added_at** | INTEGER | No | Unix timestamp |
| **pending_delete** | INTEGER (bool) | No | Awaiting 3h timer |
| **delete_at** | INTEGER | Yes | Epoch for deletion time |

### Table: `timer_events`
| Column | Type | Nullable | Description |
| :--- | :--- | :--- | :--- |
| **id** | INTEGER PK | No | Auto-increment |
| **event_type** | TEXT | No | `DISABLE_BLOCKER` / `REMOVE_DOMAIN` |
| **triggered_at** | INTEGER | No | When request was made |
| **execute_at** | INTEGER | No | `triggered_at` + 10800000ms |
| **domain_id** | INTEGER FK | Yes | Ref `blocked_domains(id)` |
| **is_complete** | INTEGER (bool) | No | 0 = pending |
| **cancelled** | INTEGER (bool) | No | User cancelled |

## 8. 3-Hour Delay Logic

### Disable Blocker Flow
* User taps "Disable Blocker"
* App creates a `timer_event` row (`event_type = DISABLE_BLOCKER`)
* Starts a `WorkManager` `PeriodicWork` or coroutine countdown
* UI shows live countdown: "Blocker disables in 02:47:31"
* User can tap "Cancel" to abort — marks `cancelled = 1` in DB
* After 3 hours, VPN service stops and `is_active = false`

### Remove Domain Flow
* User taps delete icon on a custom domain
* App creates `timer_event` (`event_type = REMOVE_DOMAIN`, `domain_id = X`)
* Domain shows "Pending removal in 02:47:31" state in list
* After 3 hours, domain row deleted from DB
* VPN in-memory `HashSet` updated immediately

### Timer Persistence
```kotlin
// On app open: check DB for incomplete, non-cancelled events
val pending = timerRepo.getPendingEvents()
pending.forEach { event ->
    val remaining = event.execute_at - System.currentTimeMillis()
    if (remaining > 0) resumeCountdown(event, remaining)
    else executeTimerAction(event)
}
```

## 9. UI Screens

### 9.1 Onboarding (First Launch)
* Screen 1: App intro + what permissions are needed and why
* Screen 2: Request VPN permission (`VpnService.prepare()`)
* Screen 3: Request `POST_NOTIFICATIONS` (Android 13+)
* Screen 4: Optional `SYSTEM_ALERT_WINDOW` explanation
* Cannot skip VPN permission — show blocking message if denied

### 9.2 Dashboard Tab
* VPN status chip: Active (emerald) / Inactive (muted)
* Domains blocked today counter (persisted in DataStore)
* Disable Blocker button (triggers 3h timer)
* Active countdown card if timer running
* Quick stats: total custom blocked, default list size

### 9.3 Blocklist Tab
* Add URL input + button at top
* Filter: All / Custom / Pending
* List of custom entries with domain, date added, status badge
* Pending delete entries show countdown + Cancel button
* Swipe to delete triggers 3h timer

### 9.4 Settings Tab
* Default blocklist toggle (disable/enable bundled list)
* Upstream DNS selector: Cloudflare, Google, Custom
* Auto-start on boot toggle
* App version, open source licenses

## 10. Brand & Theme (Jetpack Compose)

```kotlin
// Colors.kt
val Accent = Color(0xFF8B5CF6)  // Purple
val Emerald = Color(0xFF10B981) // Green
val BgDark = Color(0xFF050505)  // Near black
val CardBg = Color(0xFF0D0D0D)  // Card surface
val Border = Color(0x14FFFFFF)  // rgba white 8%
val TextMain = Color(0xFFFFFFFF)
val TextMuted = Color(0xFF94A3B8)

// Typography.kt — use downloadable font
val AppFonts = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semi, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)
```

## 11. Gradle Dependencies

`build.gradle.kts (app)`:
```kotlin
// Compose
implementation("androidx.compose.ui:ui:1.7.x")
implementation("androidx.compose.material3:material3:1.3.x")
implementation("androidx.navigation:navigation-compose:2.8.x")
implementation("androidx.activity:activity-compose:1.9.x")

// Architecture
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.x")
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.x")

// Hilt DI
implementation("com.google.dagger:hilt-android:2.52")
kapt("com.google.dagger:hilt-compiler:2.52")
implementation("androidx.hilt:hilt-navigation-compose:1.2.x")

// Room
implementation("androidx.room:room-runtime:2.6.x")
implementation("androidx.room:room-ktx:2.6.x")
kapt("androidx.room:room-compiler:2.6.x")

// DataStore
implementation("androidx.datastore:datastore-preferences:1.1.x")

// WorkManager (for 3h timer persistence)
implementation("androidx.work:work-runtime-ktx:2.9.x")

// DNS parsing (choose one)
implementation("org.pcap4j:pcap4j-core:1.8.2")
// OR write lightweight custom DNS parser (UDP byte parsing)

// Testing
testImplementation("io.mockk:mockk:1.13.x")
testImplementation("org.junit.jupiter:junit-jupiter:5.10.x")
androidTestImplementation("androidx.compose.ui:ui-test-junit4")
```

## 12. Development Phases

| Phase | Focus | Deliverables |
| :--- | :--- | :--- |
| **1** | Foundation | Project setup, Hilt, Room, nav skeleton |
| **2** | VPN Core | VpnService running, DNS intercept working |
| **3** | Blocklist Engine | Load asset list, HashSet matching, Room sync |
| **4** | 3h Timer System | WorkManager timers, DB persistence, UI countdown |
| **5** | Onboarding UI | Permission flow, first-launch detection |
| **6** | Full UI | Dashboard, Blocklist tab, Settings tab, theme |
| **7** | Polish & Test | Edge cases, battery optimization, unit tests |
| **8** | Play Store | Release build, signing, store listing |

## 13. Known Challenges & Solutions

| Challenge | Solution |
| :--- | :--- |
| **OEM battery killers (Xiaomi, Samsung)** | Guide user to disable battery optimization for XBlocker. Show prompt on first run. |
| **VPN gets disconnected in background** | Use `FOREGROUND_SERVICE`. Restart on `BOOT_COMPLETED` broadcast. |
| **Large blocklist memory usage** | Use `HashSet<String>` — O(1) lookup. 200k domains ≈ 30MB RAM. |
| **DNS over HTTPS (DoH) bypass** | Also block known DoH endpoints in blocklist (cloudflare, google, etc.) at DNS level. |
| **Play Store adult content policy** | Position as parental control / self-discipline tool. Avoid explicit keywords in listing. |
| **Timer accuracy after doze mode** | Use `WorkManager` with `setRequiresBatteryNotLow` + exact alarms where available. |

## 14. Open Source Blocklists (Default Bundle)

* Steven Black hosts: [github.com/StevenBlack/hosts](https://github.com/StevenBlack/hosts)
* OISD Full: [oisd.nl](https://oisd.nl) (nsfw category)
* HaGeZi Ultimate: [github.com/hagezi/dns-blocklists](https://github.com/hagezi/dns-blocklists)
* Combine, deduplicate, strip comments → save as `assets/blocklist.txt`
* Update script: Node.js/Python script to regenerate list on each release

## 15. Play Store Submission Notes

* Category: Tools or Parental Controls
* Content rating: Everyone (it blocks adult content, not shows it)
* VPN declaration: Must complete Play Console VPN declaration form
* Privacy Policy required — mention: no data collected, no network calls
* Short description: Block adult sites system-wide with time-delay safeguards
* Target API: Must be API 34+ as of August 2024 requirement
* Sensitive permissions declaration: `BIND_VPN_SERVICE` requires justification

## 16. Dev Environment Setup

| Tool | Version / Notes |
| :--- | :--- |
| **Android Studio** | Ladybug (2024.2.x) or newer |
| **JDK** | 17 (bundled with Android Studio) |
| **Kotlin** | 1.9.x |
| **Gradle** | 8.x with Kotlin DSL (.kts) |
| **AGP** | 8.x (Android Gradle Plugin) |
| **Min test device** | Android 8.0 physical device (VPN testing needs real device) |
| **Emulator** | Use for UI only — VPN service needs physical device |

---

**XBlocker** — Built for focus. Enforced by design.

*Developed by Sahed Alom Sumit*
