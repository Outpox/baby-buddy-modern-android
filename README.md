# Baby Buddy Companion (Android)

An unofficial Android client for a self-hosted [Baby Buddy](https://github.com/babybuddy/babybuddy) server — track feeding, sleep, diapers, pumping, tummy time and more, with a live nursing timer that syncs between parents' phones in real time, home-screen widgets, lock-screen notifications and offline-first sync.

> **Requires a Baby Buddy server.** This repo is *just the Android app*: it contains no backend and no database of its own. All of your data lives on (and is read from) a self-hosted [Baby Buddy](https://github.com/babybuddy/babybuddy) instance over its REST API. Without one you can still look around in the built-in **demo mode** (sample data, everything works except syncing). Go set up the excellent open-source [babybuddy/babybuddy](https://github.com/babybuddy/babybuddy) server first — huge props to that project; everything here builds on its API.

Built with Kotlin, Jetpack Compose and Material 3. Originally generated with Google AI Studio.

<table>
  <tr>
    <td align="center"><b>Today Dashboard (Day)</b></td>
    <td align="center"><b>Today Dashboard (Night)</b></td>
    <td align="center"><b>Quick Log</b></td>
  </tr>
  <tr>
    <td><img src="fastlane/metadata/android/en-US/images/phoneScreenshots/01-today-dashboard.png" alt="Today dashboard (Day)" width="250"></td>
    <td><img src="fastlane/metadata/android/en-US/images/phoneScreenshots/06-today-dashboard-night.png" alt="Today dashboard (Night)" width="250"></td>
    <td><img src="fastlane/metadata/android/en-US/images/phoneScreenshots/02-quick-log.png" alt="Log screen" width="250"></td>
  </tr>
  <tr>
    <td align="center"><b>24h Trends</b></td>
    <td align="center"><b>History Timeline</b></td>
    <td align="center"><b>Settings</b></td>
  </tr>
  <tr>
    <td><img src="fastlane/metadata/android/en-US/images/phoneScreenshots/03-trends.png" alt="Trends" width="250"></td>
    <td><img src="fastlane/metadata/android/en-US/images/phoneScreenshots/04-history.png" alt="History" width="250"></td>
    <td><img src="fastlane/metadata/android/en-US/images/phoneScreenshots/05-settings.png" alt="Settings" width="250"></td>
  </tr>
  <tr>
    <td align="center"><b>Rhythm models</b></td>
    <td align="center"><b>Model evaluation</b></td>
    <td align="center"><b>Predicted next times</b></td>
  </tr>
  <tr>
    <td><img src="fastlane/metadata/android/en-US/images/phoneScreenshots/10-models.png" alt="Trends Models tab: the estimate in use for each rhythm, with Reset" width="250"></td>
    <td><img src="fastlane/metadata/android/en-US/images/phoneScreenshots/11-model-card.png" alt="A rhythm's models compared with the default estimate, with Save this version" width="250"></td>
    <td><img src="fastlane/metadata/android/en-US/images/phoneScreenshots/12-today-predictions.png" alt="Today showing the next feed, diaper and sleep predicted by the models" width="250"></td>
  </tr>
</table>

<sub>Screenshots use the built-in demo data.</sub>

## Features

- **Today dashboard** — at-a-glance status for the last feed, sleep, diaper, bath, tummy time and milk stash, plus optional cards (tummy time goal, temperature, notes, growth). Each tracked item has its own color across the app.
- **Nursing timer** — one-tap Left/Right side switching, per-side time correction, pause/resume, auto-end after 30 minutes idle, and a lock-screen notification with side controls. No save step: every tap is recorded through the sync queue.
- **Sleep timer** — running timer kept on the phone (survives app restarts) or manual start/end entry for past sleeps.
- **Log screen** — one list to log breastfeeding, bottle, diaper, sleep, pumping, bath and growth; tap an entry to open its form. Quick notes for your server's tags live on Today.
- **Home-screen widgets** — three widgets: **Quick log** (one-tap feed/diaper/pump/tummy-time buttons), **Timer** (live self-ticking clock with side, pause and end controls) and **Summary** (today's counts at a glance), all driven straight from the app's state.
- **Lock-screen notifications** — while a feed runs, an ongoing notification shows the live clock and Left/Right/End controls; a running sleep gets its own lock-screen timer notification. Both can be turned off in Settings. Optionally, a silent "what's next" notification counts down to the next feed, diaper change or nap while no timer runs.
- **Quick Settings tiles & shortcuts** — Breastfeed (start/pause), Sleep (start/woke up) and Diaper tiles for the pull-down shade, and long-press launcher shortcuts for Feed left, Feed right, Diaper and Sleep now.
- **While you were away** — coming back to the app, Today lists the feeds, diapers and sleeps the other caregivers logged in the meantime.
- **Reminders & notifications** — optional "feed due", "diaper change due", "awake for" and "asleep for" alarms with an optional heads-up 5–60 minutes before, which refresh from the server before posting, plus sync-status and undo toasts.
- **Rhythm models (optional)** — fits a model per rhythm on the phone (AutoML over linear regression, boosted trees, a small neural net and nearest neighbours), shows how each compares with the default estimate, and can time the reminders, Today's "next" times and the summary widget. See [Rhythm models](#rhythm-models).
- **Growth** — weight, length and head circumference, with each measurement's percentile and charts against the [WHO Child Growth Standards](https://www.who.int/tools/child-growth-standards) (birth to 2 years).
- **Demo mode** — "Try Demo Mode with Sample Data" on the login screen: 90 days of a simulated baby's feeds, diapers, sleep, pumping, baths and growth, with reminders and rhythm models already set up.
- **History & Trends** — week-by-week history (feeds, diapers, sleep, pumping, tummy time, baths and notes, all editable) and 24-hour rhythm charts for feeding, sleep and diaper patterns.
- **Shared timers — live sync between parents.** A running breastfeed or sleep started on one phone is mirrored through the Baby Buddy timers API and shows up on every other parent's phone within seconds: the other parent sees the live clock and current side, can switch sides or end the feed, and if a timer is stopped on one device it ends everywhere. Both phones stay in sync of who started what and when (newest change wins).
- **Offline-first** — writes are queued locally and flushed in order with retry/backoff; today's entries are cached on the device.
- **Night mode** — automatic dim warm palette (by time of day or system setting).
- **12/24-hour clock**, scroll-wheel (one continuous date and time: minutes roll into hours, hours into days) or typed time entry, per-phone tracked-item hiding, configurable quick actions.
- **About** — Settings → About shows the version, build type, git tag/commit and build date, for bug reports.

### Home-screen widgets

<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/07-widgets.png" alt="Quick log, nursing timer and today summary widgets on an Android home screen" width="360" align="right">

Three home-screen widgets that update straight from the app's state, even when the app is closed:

- **Quick log** — one-tap Feed, Diaper and Tummy time buttons, with the baby's name, age and when they were last fed.
- **Nursing timer** — the running feed's live clock, current side and start time, with Switch side and Pause/Resume buttons.
- **Today summary** — today's feed count and wet/dirty diapers, plus the next feed/diaper/nap when [rhythm models](#rhythm-models) are in use; tap to open the app.

Add them from the launcher's widget list or from **Customize Today → Phone Home Screen** in the app.

<br clear="right">

### Lock-screen controls

<table>
  <tr>
    <td><img src="fastlane/metadata/android/en-US/images/phoneScreenshots/08-feed-notification.png" alt="Feeding notification with Left, Pause right and End buttons" width="400"></td>
    <td><img src="fastlane/metadata/android/en-US/images/phoneScreenshots/09-sleep-notification.png" alt="Sleeping notification with a Woke up button" width="400"></td>
  </tr>
</table>

- **Feeding** — while a feed runs: the active side, each side's time and the start time, with Left / Right (pause or switch) and End buttons, usable from the lock screen.
- **Sleeping** — while a sleep is timed: when the baby fell asleep and a **Woke up** button that saves the sleep.

Both can be turned off in Settings → Notifications. There you can also turn on **What's next on lock screen**: a silent notification that counts down to the next feed, then lists the next diaper change and nap (from the reminder intervals or a saved [rhythm model](#rhythm-models)), shown while no feed or sleep is timed.

### Quick Settings tiles and shortcuts

Edit your Quick Settings panel to add **Breastfeed** (tap to start on the side opposite the last feed, tap again to pause or resume), **Sleep** (tap when asleep, again when awake; the sleep is saved) and **Diaper** (opens the diaper log). Long-press a tile to open its screen. Long-press the app icon for **Feed left**, **Feed right**, **Diaper** and **Sleep now**.

### Reminders

<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/13-reminders.png" alt="Notification settings: feed, awake, sleep and diaper reminders and the heads-up choice" width="250" align="right">

Settings → Notifications:

- **Feed reminder**: when a set time (e.g. 3 hours) has passed since the last feed started.
- **Awake reminder**: when the baby has been awake for a set time, to catch the nap window.
- **Sleep reminder**: when a timed sleep reaches a set length.
- **Diaper reminder**: when a set time has passed since the last change.
- **Heads-up before**: also notify 5, 10, 15, 20, 30, 45 or 60 minutes before any of these is due ("Feed due in 15m"); the due notification replaces it.
- With a [rhythm model](#rhythm-models) saved for that rhythm, the reminder fires when the model expects it instead of after the fixed time (which stays as the fallback).
- *Multi-device awareness*: reminders check the Baby Buddy server before alerting, so a feed or change logged on another phone cancels them.

<br clear="right">

### Rhythm models

An optional tab in Trends that learns your baby's rhythm, on the phone, and can use it to estimate what's next.

**Turn it on:** Settings → Appearance → **Models tab in Trends**. Then open **Trends → Models**.

<table>
  <tr>
    <td><img src="fastlane/metadata/android/en-US/images/phoneScreenshots/14-enable-models.png" alt="Settings, Appearance: Models tab in Trends switch" width="220"></td>
    <td><img src="fastlane/metadata/android/en-US/images/phoneScreenshots/10-models.png" alt="Models tab" width="220"></td>
    <td><img src="fastlane/metadata/android/en-US/images/phoneScreenshots/11-model-card.png" alt="A rhythm's evaluation" width="220"></td>
  </tr>
</table>

1. **Pick the history to learn from**: the last 30, 60, 90 or 180 days, or **Custom…** for any date range (e.g. to leave out a week of illness). Tap **Fit models**; it takes a few seconds to a minute.
2. **Four rhythms**, each with its own model: time to the **next feed**, the **next diaper change**, how long a **sleep** lasts (the expected wake-up) and how long the baby stays **awake** between sleeps (the next nap). Each model's inputs include the time of day, the baby's age, the previous intervals, the time since the last feed/change/wake-up, the feed's length and amount, and the last 24 hours' totals.
3. **AutoML** tries linear regression, gradient-boosted trees, a small neural network and nearest neighbours, each with several settings, and scores them by how well they predicted your most recent entries when trained only on older ones. Each card shows every family's best typical error **next to the default estimate's** (the reminder's fixed interval, or the average interval when none is set), which inputs matter most, and a chart of actual vs predicted.
4. **Use it**: pick a model in the list (the best is selected) and tap **Use …**. After fitting again later, **Save this version** replaces the saved model for that rhythm; the card shows the saved and new errors side by side.
5. **Revert any time**: **Reset to default** on a card, or **Reset** / **Reset all** under *Estimates in use* at the top of the tab, goes back to the default estimate.

A saved model then sets the time for that rhythm's **reminder**, Today's **Next feed / Next diaper / Next sleep / Wake-up** card and the **summary widget**'s "Next:" line, recalculated whenever a feed, change or sleep is logged on any phone. Everything (training and the saved models) stays on the phone; the server only provides your entries. The demo mode starts with models fitted and in use.

Predictions are estimates from past patterns, not advice; babies change quickly, so fit again every week or two.

## Requirements

- **A running [Baby Buddy](https://github.com/babybuddy/babybuddy) server** — this is a hard requirement, not optional. This app is a pure client; the server stores every feeding, sleep, diaper change, etc. and exposes them over its REST API. Install it from [github.com/babybuddy/babybuddy](https://github.com/babybuddy/babybuddy) (Docker or bare metal) before using this app.
- Android 7.0+ (minSdk 24).
- To build: [Android Studio](https://developer.android.com/studio) (latest, AGP 9.4 / compileSdk 37), or JDK 17+ (21 recommended) with the Gradle wrapper.

## Getting started

### Install

Download the APK from the [latest release](https://github.com/liorbass/baby-buddy-modern-android/releases/latest) and open it on your phone (allow installing apps from your browser when Android asks). New releases install over the old one.

To get updates automatically, install it with [Obtainium](https://github.com/ImranR98/Obtainium): tap the badge on your phone, or add this repository's URL in Obtainium. The app config is in [`docs/obtainium/`](docs/obtainium/dev.liorbass.babybuddy.json).

<a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22dev.liorbass.babybuddy%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2Fliorbass%2Fbaby-buddy-modern-android%22%2C%22author%22%3A%22liorbass%22%2C%22name%22%3A%22Baby%20Buddy%20Companion%22%7D"><img src="docs/images/badge_obtainium.png" alt="Get it on Obtainium" height="54"></a>

From 1.1.0 the app ID is `dev.liorbass.babybuddy`; 1.0.x was a different app ID, so uninstall it after moving over (log in again once).

### Build

```bash
git clone https://github.com/liorbass/baby-buddy-modern-android.git
cd baby-buddy-modern-android
./gradlew assembleDebug        # APK at app/build/outputs/apk/debug/app-debug.apk
```

or open the project in Android Studio and run the `app` configuration.

Notes:

- **Signing:** debug builds use the standard Gradle debug key (or a gitignored `debug.keystore` in the repo root, if present). An APK you build yourself can't update one installed from the releases (different signing key), and vice versa.

### Contributing setup

Checks run through [pre-commit](https://pre-commit.com) (`.pre-commit-config.yaml`): file hygiene, gitleaks secret scanning, ktlint formatting (Java 11+ on `PATH`), actionlint, and blocks on keystores, `.env`, APKs and data exports. Install once per clone, from the same environment you commit from (on Windows: a Windows terminal, so Android Studio's commits use it):

```bash
pipx install pre-commit          # or: uv tool install pre-commit / pip install pre-commit
pre-commit install
cp tools/hooks/blocklist.example tools/hooks/blocklist.local   # names/addresses to keep out of git (gitignored)
```

`pre-commit run --all-files` checks everything; `pre-commit autoupdate` bumps hook versions. The same checks run in CI (`.github/workflows/checks.yml`).

### Connect to your server

1. Install and launch the app.
2. Enter your Baby Buddy server URL (e.g. `https://baby.example.com`), your API token (from the Baby Buddy web UI under your user profile) and the child's name.
3. Optionally set a Cloudflare Access service token if your server sits behind Cloudflare Access.
4. Plain `http://` addresses work too (e.g. a server on your home network), but the token is then sent unencrypted; prefer HTTPS or a VPN away from home.
5. If your server uses a self-signed certificate, turn on **Allow self-signed certificates**. This switches off certificate checking for the connection, so only use it on a network you trust (a VPN or your home network).

The API token and Cloudflare secret are stored encrypted with an Android Keystore key.

No server yet? Tap **No server yet? Try the demo** at the top of the login screen (or **Try Demo Mode with Sample Data** under the form): the whole app, Settings included, runs on 90 days of simulated sample data, with growth charts, reminders and fitted rhythm models. Log in to your own server from Settings → Server connection when you're ready; the demo's data and models are left behind.

### Run tests

```bash
./gradlew testDebugUnitTest        # JVM/Robolectric unit tests
./gradlew connectedDebugAndroidTest # instrumented tests (needs a device/emulator)
```

## Project layout

```
app/src/main/java/com/example/
├── BabyBuddyApplication.kt   # app entry; attaches repository, syncs widgets/notifications
├── MainActivity.kt           # navigation switch, theming, screen host
├── data/
│   ├── api/                  # Retrofit service + DTOs for the Baby Buddy REST API
│   ├── ml/                   # rhythm models: features, regressors, AutoML (pure Kotlin)
│   ├── model/                # UI models, time formatting
│   └── repository/           # BabyBuddyRepository (single source of truth), offline
│                             # store, write queue, shared timers, secret cipher
├── ui/
│   ├── MainViewModel.kt      # UI state, toasts, screen navigation
│   ├── components/           # shared composables (forms, dialogs, pickers, widget cards)
│   ├── screens/              # Today, Log, History, Trends, Sleep, Settings, ...
│   └── theme/                # colors (incl. night mode), type, theme
└── widget/                   # home-screen widgets, notifications, reminders
```

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for a detailed architecture guide (state flow, sync/write queue, shared timers, server field mappings, navigation) and [docs/ICD.md](docs/ICD.md) for the interface control document (how the app talks to the Baby Buddy backend).

## How data maps to the server

The app uses only stock Baby Buddy API endpoints (`/api/children`, `/api/feedings`, `/api/sleep`, `/api/changes`, `/api/tummy-times`, `/api/pumping`, `/api/temperature`, `/api/weight`, `/api/height`, `/api/head-circumference`, `/api/notes`, `/api/tags`, `/api/timers`). Features the server has no fields for are stored in `notes` with a parseable format:

- caregiver name (`Caregiver: …`), breastfeed side split (`Left: … | Right: … | Sides: …`)
- diaper rash/cream/consistency (`Rash | Cream: … | Consistency: …`)
- baths (a note tagged `bath`), milk stash (a note tagged `milk-stash`)
- length is stored in `/api/height` (the WHO uses lying-down length under 2 years)

Settings kept only on the phone: your caregiver name, theme, clock format, reminders, tracked items, Today cards, saved rhythm models, and the boy/girl choice used to pick the WHO charts (Baby Buddy has no such field).

## Privacy

- The app talks only to your own Baby Buddy server (and Cloudflare Access, if you configure it). No analytics, ads or tracking.
- Your API token and Cloudflare secret are encrypted on the phone with an Android Keystore key and excluded from backups, as are the offline queue and cache.

## Permissions

| Permission | Why |
|---|---|
| Internet | Talk to your Baby Buddy server |
| Notifications (Android 13+, asked when lock-screen timers or reminders are on) | Lock-screen feed/sleep timers and reminders |
| Run at startup | Set reminders again after the phone restarts (alarms don't survive a restart) |
| Run in the background (foreground service) | Only while a feed or sleep is being timed: keeps the lock-screen timer in step with the other parent's phone, so a feed ended there also ends here |

No location, contacts, camera, microphone or storage access.

## Medical disclaimer

Growth percentiles and charts are for information only. They are not medical advice and not a diagnosis; talk to your pediatrician or health visitor about your child's growth, feeding or sleep.

## Credits

- [Baby Buddy](https://github.com/babybuddy/babybuddy) — the server, data model and REST API this app is built on.
- [WHO Child Growth Standards](https://www.who.int/tools/child-growth-standards) — growth reference data (WHO Multicentre Growth Reference Study Group, 2006), from the WHO's published expanded tables; see `WhoGrowthStandards.kt`.

## Disclaimer

This is an independent, unofficial client. It is not affiliated with or endorsed by the Baby Buddy project. All credit for the Baby Buddy data model, REST API and the original web app goes to [babybuddy/babybuddy](https://github.com/babybuddy/babybuddy) and its contributors — this repo is only a mobile front end for it.
This repo was vibe coded for my own personal use, if you don't like something/want a feature, submit an issue, if you don't like the response, go and fork yourself.

## License

[Apache License 2.0](LICENSE)
