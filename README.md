# PhoneIntel 📱

> Take back your time — on your own terms.

Most screen time apps treat you like a child. They block apps, set hard limits, and lock you out of your own phone. PhoneIntel takes the opposite approach: give you complete visibility into your habits, and trust you to make better decisions yourself.

No cold turkey. No frustration. Just honest data, and a path you control.

Everything runs 100% locally. No accounts, no cloud, no telemetry.

---

## Philosophy

Phone addiction isn't beaten by brute force — it's beaten by awareness. Blocking apps feels good for a day, then you uninstall the blocker in frustration and you're back to square one.

PhoneIntel shows you the truth about your habits: how many times you unlock your phone, how long each session actually lasts, how fragmented your attention is, how much of your night you spend on your screen. Then it gets out of the way and lets you decide what to do about it.

Progress happens in steps, not overnight. PhoneIntel is built for the long game.

---

## Features

### Dashboard
- Total screen time today with a 7-day trend chart
- Top 5 apps by usage
- Unlock count, average session length, and attention fragmentation index
- Phone health score with grade and contextual label — a single number that reflects your overall habits
- XP level card showing your current level, title, and progress toward the next level
- Insight strip — a one-line preview of your most important behavioural pattern this week

### Phone Health
- Full health score breakdown across screen time, night usage, unlock frequency, and longest session
- Attention stats: session count, average and longest session, short session ratio
- Fragmentation index: how scattered your attention is across the day

### Focus Mode
- Start a focus session with a stated intent (Work, Study, Family, Sleep, Custom)
- Search and select apps to block individually
- Blocks chosen apps for the duration — the one exception to the no-force rule, and only when *you* choose it
- Mindful unlock notification at every unlock: a gentle nudge, not a wall
- Full-screen block overlay when a blocked app is opened, showing the app's real name
- "Stay Focused" returns you to PhoneIntel; "End Focus Session" drops you into the app with an XP penalty
- Cancelling a session from within the app carries no penalty — only breaking it from the block screen does

### Insights
- Weekly behavioural analysis derived entirely from your local data
- Six insight types: Night Habit, Single App Sink, Compulsive Checker, Fragmentation Spike, Notification Driver, Improving
- Digital Personality card — a two-word label derived from your dominant pattern this week
- Biggest Attention Leak card — the app most responsible for pulling you in
- Demo cards shown until enough data has accumulated

### XP + Levelling
- Earn XP passively each hour based on your phone health score
- Score ≥ 80 → +20 XP · Score 60–79 → +10 XP · Score 40–59 → +5 XP · Score < 40 → no XP
- Breaking a focus session from the block popup costs 50 XP
- Levels carry titles: Newcomer → Aware → Mindful → Focused → Disciplined → Intentional → Balanced → Clear-headed → Present → Enlightened → Master
- Full XP ledger stored locally; every event is append-only and auditable

### Notification Tracking
- Per-app notification counts for today, 7 days, and 30 days
- See which apps are most aggressively competing for your attention
- Notification content is never stored — only counts and timestamps

### Battery Analytics
- Current level and charging status
- 24-hour bar chart colour-coded by level
- 7-day average battery level

---

## Architecture
```
MVVM + Room + Jetpack Compose + Hilt

app/
├── data/
│   ├── db/
│   │   ├── entities/       Room @Entity classes (8 tables)
│   │   ├── dao/            Type-safe DAOs with Flow queries
│   │   └── AppDatabase.kt  Room database config
│   └── repository/         Data sources + system API bridges (all in Repositories.kt)
├── domain/
│   └── model/              Pure Kotlin data models + InsightEngine
├── service/
│   ├── DataCollectionService.kt      Foreground service, screen/unlock tracking, XP ticks
│   ├── FocusEnforcementService.kt    Persistent foreground-app polling, block overlay
│   ├── NotificationListenerService.kt
│   └── BootReceiver.kt
├── ui/
│   ├── dashboard/          Home screen + ViewModel
│   ├── health/             Phone health score screen
│   ├── focus/              Focus setup, active session, block overlay activity
│   ├── insights/           Weekly insight cards + personality card
│   ├── notifications/      Notification stats
│   ├── network/            Per-app network usage
│   ├── bluetooth/          Bluetooth device tracking
│   ├── battery/            Battery analytics
│   ├── recap/              Year-end recap
│   ├── components/         Shared composables
│   ├── theme/              Material3 dark forest green theme
│   └── NavGraph.kt         Navigation setup
├── di/
│   └── DatabaseModule.kt   Hilt DI bindings
└── util/
    └── DateUtil.kt         Date formatting helpers
```

---

## Database Schema

| Table | Purpose |
|---|---|
| `app_usage` | Daily per-app foreground time + launch count |
| `notification_events` | Notification received events (no content stored) |
| `network_usage` | Daily per-app Wi-Fi + mobile bytes |
| `bluetooth_events` | Connect/disconnect events per device |
| `battery_snapshots` | Periodic level, charging state, temperature |
| `unlock_sessions` | Per-session unlock time, lock time, and duration |
| `daily_summary` | Pre-aggregated daily rollup for fast reads |
| `xp_ledger` | Append-only XP event log (gains + penalties) |

---

## Design

PhoneIntel uses a custom dark forest green theme throughout:

| Token | Hex | Usage |
|---|---|---|
| `BgBase` | `#0A1A12` | Screen backgrounds |
| `BgCard` | `#0F2218` | Card surfaces |
| `Mint` | `#3DEBA8` | Primary accent — scores, CTAs, highlights |
| `TextPrimary` | `#EEF2EE` | Body text |
| `TextSecondary` | `#7A9A84` | Labels, secondary text |
| `CoralAccent` | `#FF6B6B` | Alerts, warnings, block overlay |
| `AmberAccent` | `#FFB547` | Medium-severity warnings |

---

## Permissions Required

| Permission | Why |
|---|---|
| `PACKAGE_USAGE_STATS` | Read per-app screen time from UsageStatsManager |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Count notification events per app |
| `READ_PHONE_STATE` | Network stats access |
| `ACCESS_NETWORK_STATE` / `ACCESS_WIFI_STATE` | Wi-Fi state detection |
| `BLUETOOTH_CONNECT` (API 31+) | Detect connected BT devices |
| `RECEIVE_BOOT_COMPLETED` | Restart service on reboot |
| `FOREGROUND_SERVICE` | Keep collection running in background |

> ⚠️ `PACKAGE_USAGE_STATS` and `BIND_NOTIFICATION_LISTENER_SERVICE` are special permissions that must be granted manually in Android Settings. The app directs users to the correct settings screens.

---

## Setup & Build
```bash
# Clone and open in Android Studio Ladybug or later
git clone <repo>

# Build debug APK
./gradlew assembleDebug

# Run tests
./gradlew test
```

**Requirements:**
- Android Studio Ladybug (2024.2.1+)
- JDK 17
- Min SDK: 26 (Android 8.0)
- Target SDK: 35

---

## Privacy

1. **No internet permission for data** — no outbound calls, ever.
2. **Notification content never stored** — only package name, category, and timestamp are captured.
3. **Bluetooth MAC hashing** — device addresses are hashed before storage.
4. **Backup explicitly disabled** — blocks Google Backup and device transfer.
5. **No analytics SDK** — no Firebase, no Crashlytics, no third-party SDKs that phone home.
6. **Local-only Room database** — `phoneintel.db` lives in the app's private data directory and never leaves it.

---

## Known Platform Behaviour

**Samsung One UI (Android 12+):** `ACTION_USER_PRESENT` is not broadcast after biometric unlock. PhoneIntel uses `ACTION_SCREEN_ON` as the unlock signal instead, which works reliably across all OEMs and lock screen types.

**Focus blocking reliability:** Some apps (news readers, video players) fire `MOVE_TO_FOREGROUND` once on launch and go quiet. PhoneIntel's enforcement service maintains a persistent foreground-app state updated on every poll cycle rather than querying a short event window, which ensures these apps are caught reliably.

---
