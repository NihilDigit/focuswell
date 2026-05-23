# FocusWell

FocusWell is a local-first Android app for turning focused work into leisure time you can spend later.

It is built for people who need structure without streak pressure. A focus session earns reserve. A leisure session spends reserve. The app keeps a ledger so the balance can be audited and corrected instead of treated as a vague habit score.

## Product

FocusWell uses configurable accounting rules with these defaults:

- Each day starts at 04:00 in the device's current system time zone.
- A new FocusWell day grants 60 minutes of leisure reserve.
- Focus earns reserve by elapsed active time, session type, and tag multiplier.
- Leisure spends reserve by elapsed time.
- Leisure between 01:00 and 04:00 spends at 2x speed.
- Edits and deletes create ledger adjustments instead of hiding history.
- Reminders use `sessionId + revision` so stale callbacks do not notify old sessions.

Today is for the current reserve, active mode, and daily trackers. Balance combines the ledger, records, and record editing. Plan manages tags and trackers. Settings holds appearance, rules, backup, and reset.

Rules are user-adjustable after install. Tags and trackers are part of the user's plan, not general app settings.

The product concept and accounting boundaries live in [docs/focuswell-product-spec.md](docs/focuswell-product-spec.md). Exact behavior should be checked against the current code and tests. UI rules live in [DESIGN.md](DESIGN.md).

## Android App

The app is written in Kotlin and Jetpack Compose. It includes:

- Focus, leisure, and depleted modes.
- Room-backed local persistence for timers, records, trackers, tags, and ledger entries.
- Editable focus and leisure records in Balance.
- Daily trackers and rule-based tracker progress.
- Adjustable accounting rules and configurable tracker rewards.
- JSON export/import.
- In-app update checks against GitHub Releases, with ABI-specific APK download and checksum verification.
- Local notifications and remote FCM reminders.
- Minified release builds with scoped Room/R8 keep rules.
- Material 3 Expressive-inspired shape, motion, typography, and icon treatment.

## Build

```powershell
cd app
.\gradlew.bat testDebugUnitTest assembleDebug
```

Install a debug build:

```powershell
cd app
.\gradlew.bat installDebug
```

Build a signed release when signing environment variables are available:

```powershell
cd app
.\gradlew.bat testDebugUnitTest assembleRelease "-PfocuswellVersionName=26.5.5"
```

Release APKs are split by ABI: `arm64-v8a`, `armeabi-v7a`, and `x86_64`.

## Backend

The backend schedules reminder callbacks and sends FCM messages.

```powershell
cd backend
bun run check
bun test
```

Production backend secrets live in Vercel. Android release signing secrets live in GitHub Secrets.

## Release

Releases are manual-tag driven. Use a time-based tag such as `26.5.5`, wait for CI to create the GitHub Release and attach APKs, then edit the Release title and notes by hand.

The current public release is [26.5.6](https://github.com/NihilDigit/focuswell/releases/tag/26.5.6). It adds in-app update checks, fixes late-night Leisure remaining-time display, and keeps release APKs split by ABI.
