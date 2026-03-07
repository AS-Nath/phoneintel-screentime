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
- Battery level, charging status, and notification count
- Phone health score with letter grade — a single number that reflects your overall habits

### Phone Health
- Full health score breakdown across screen time, night usage, unlock frequency, and notification load
- Attention stats: session count, average and longest session, short session ratio
- Fragmentation index: how scattered your attention is across the day

### Focus Mode
- Start a focus session with a stated intent (Work, Reading, Rest, etc.)
- Blocks chosen apps for the duration — the one exception to the no-force rule, and only when *you* choose it
- Mindful unlock notification at every unlock: a gentle nudge, not a wall

### Notification Tracking
- Per-app notification counts for today, 7 days, and 30 days
- See which apps are most aggressively competing for your attention
- Notification content is never stored — only counts and timestamps

### Network Usage
- Per-app Wi-Fi vs mobile data breakdown
- Upload and download split
- Range filters: today, 7 days, 30 days

### Bluetooth
- Known devices with full connection history
- Connect/disconnect timeline and total duration per device

### Battery Analytics
- Current level and charging status
- 24-hour bar chart color-coded by level
- 7-day average battery level

### Year-End Recap
- Total screen time, notifications, and data used for the year
- Top 5 apps of the year, daily averages, longest streak
- Most productive month and fun stats — all derived locally from your own data

---

## Architecture
```
MVVM + Room + Jetpack Compose + Hilt

app/
├── data/
│   ├── db/
│   │   ├── entities/       Room @Entity classes (7 tables)
│   │   ├── dao/            Type-safe DAOs with Flow queries
│   │   └── AppDatabase.kt  Room database config
│   └── repository/         Data sources + system API bridges
├── domain/
│   └── model/              Pure Kotlin data models
├── service/
│   ├── DataCollectionService.kt    Foreground service + screen/unlock tracking
│   ├── NotificationListenerService.kt
│   └── BootReceiver.kt
├── ui/
│   ├── dashboard/          Home screen + ViewModel
│   ├── health/             Phone health score screen
│   ├── notifications/      Notification stats
│   ├── network/            Per-app network usage
│   ├── bluetooth/          Bluetooth device tracking
│   ├── battery/            Battery analytics
│   ├── recap/              Year-end recap
│   ├── components/         Shared composables
│   ├── theme/              Material3 theme + colors
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

---

## Roadmap

- [ ] Permission onboarding flow (step-by-step grant screens)
- [ ] Daily summary WorkManager job (pre-aggregates `daily_summary`)
- [ ] App usage goals with self-set soft limits
- [ ] Notification quiet hours suggestions
- [ ] Weekly habit review screen — progress over time, not just today
- [ ] Export data as CSV (local only)
- [ ] Home screen widget: today's screen time
- [ ] Dark/light theme toggle in settings
- [ ] App icon categories (Social, Productivity, Entertainment)
- [ ] Year recap shareable card (generated locally, no upload)