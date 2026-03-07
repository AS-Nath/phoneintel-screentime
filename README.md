# PhoneIntel 📱

> Your phone's intelligence — 100% local, zero telemetry, zero ads.

A native Android system intelligence app built with Kotlin and Jetpack Compose. Every byte of data stays on your device.

---

## Architecture

```
MVVM + Room + Jetpack Compose + Hilt

app/
├── data/
│   ├── db/
│   │   ├── entities/       Room @Entity classes (6 tables)
│   │   ├── dao/            Type-safe DAOs with Flow queries
│   │   └── AppDatabase.kt  Room database config
│   └── repository/         Data sources + system API bridges
├── domain/
│   └── model/              Pure Kotlin data models
├── service/
│   ├── DataCollectionService.kt    Foreground service (15-min sync)
│   ├── NotificationListenerService.kt
│   └── BootReceiver.kt
├── ui/
│   ├── dashboard/          Home screen + ViewModel
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
| `daily_summary` | Pre-aggregated daily rollup for fast reads |

---

## Features

### Dashboard
- Hero card: today's total screen time + battery level ring
- Quick stats: notifications, Wi-Fi, mobile data
- 7-day screen time trend bar chart
- Top 5 apps used today with usage bars
- Navigation shortcuts to all modules

### Notification Tracking
- Count per app: today / 7 days / 30 days
- Ranked list of top notifiers
- **Privacy**: notification titles/content are never stored

### Network Usage
- Per-app breakdown: Wi-Fi vs mobile data
- Upload and download split
- Wi-Fi/Mobile percentage visual
- Range filters: today / 7 days / 30 days

### Bluetooth
- Known devices list with connection history
- Connect/disconnect event timeline
- Total connected duration per device

### Battery Analytics
- Current level + charging status hero card
- 24-hour bar chart timeline (color-coded by level)
- 7-day average level

### Year-End Recap
- Total screen time, notifications, data used
- Top 5 apps of the year
- Daily average screen time
- Longest usage streak
- Most productive month (lowest avg screen time)
- Fun facts derived from your data

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

> ⚠️ `PACKAGE_USAGE_STATS` and `BIND_NOTIFICATION_LISTENER_SERVICE` are **special permissions** that require the user to manually grant them in Android Settings. The app must direct users to the Settings screen.

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

## Privacy Design Decisions

1. **No internet permission for data** — `INTERNET` is listed only for potential future local web server (debug). No outbound calls.
2. **Notification content never stored** — `NotificationListenerService` captures only package name, category, and timestamp.
3. **Bluetooth MAC hashing** — Device MAC addresses are hashed before storage.
4. **Backup explicitly disabled** — `backup_rules.xml` and `data_extraction_rules.xml` prevent data leaving via Google Backup or device transfer.
5. **No analytics SDK** — No Firebase, no Crashlytics, no third-party SDKs that phone home.
6. **Local-only Room database** — `phoneintel.db` lives in the app's private data directory.

---

## Roadmap / Next Steps

- [ ] Permission onboarding flow (step-by-step grant screens)
- [ ] Daily summary WorkManager job (pre-aggregates `daily_summary`)
- [ ] App usage goals + screen time limits
- [ ] Notification quiet hours suggestions
- [ ] Export data as CSV (local only)
- [ ] Widget: today's screen time on home screen
- [ ] Dark/light theme toggle in settings
- [ ] App icon categories (Social, Productivity, Entertainment)
- [ ] Year recap shareable card (generated locally, no upload)
