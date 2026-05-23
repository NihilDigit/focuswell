# FocusWell

FocusWell is an Android app for turning focused work into leisure time you can spend later.

It is built for people who need structure without streak pressure. A focus session earns reserve. A leisure session spends reserve. The app keeps a ledger so the balance can be audited and corrected instead of treated as a vague habit score.

## Product

FocusWell uses a few fixed rules:

- Each day starts at 04:00 in `Asia/Shanghai`.
- A new FocusWell day grants 60 minutes of leisure reserve.
- Focus earns reserve by elapsed active time, session type, and tag multiplier.
- Leisure spends reserve by elapsed time.
- Leisure between 01:00 and 04:00 spends at 2x speed.
- Edits and deletes create ledger adjustments instead of hiding history.
- Reminders use `sessionId + revision` so stale callbacks do not notify old sessions.

The main screen is for the current day: reserve, active mode, and daily trackers. Balance is the account ledger. History is where records can be reviewed or edited. Settings holds rules, tags, trackers, backup, and reset.

The product spec lives in [docs/focuswell-product-spec.md](docs/focuswell-product-spec.md). UI rules live in [DESIGN.md](DESIGN.md).

## Android App

The app is written in Kotlin and Jetpack Compose. It includes:

- Focus, leisure, depleted, and wind-down modes.
- Editable focus and leisure history.
- Daily trackers and rule-based tracker progress.
- JSON export/import.
- Local notifications and remote FCM reminders.
- Material 3 Expressive-inspired shape, motion, typography, and icon treatment.

## Build

```powershell
cd app
.\gradlew.bat test assembleDebug
```

Install a debug build:

```powershell
cd app
.\gradlew.bat installDebug
```

Build a signed release when signing environment variables are available:

```powershell
cd app
.\gradlew.bat test assembleRelease "-PfocuswellVersionName=26.5.1"
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

Releases are manual-tag driven. Use a time-based tag such as `26.5.1`, wait for CI to create the GitHub Release and attach APKs, then edit the Release title and notes by hand.
