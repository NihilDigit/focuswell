# Repository Guidelines

This repository contains the Android app, reminder backend, product concept, and design system for FocusWell. `CLAUDE.md` is a symbolic link to this file and must stay equivalent to `AGENTS.md`.

## Project Layout

- `README.md`: project entry point, build commands, and release summary.
- `docs/focuswell-product-spec.md`: product concept, accounting boundaries, vocabulary, and current non-goals.
- `DESIGN.md`: durable UI rules for Material 3 Expressive usage.
- `app/`: nested Android Gradle project.
- `app/app/src/main/java/dev/nihildigit/focuswell/`: Kotlin, Jetpack Compose, reminders, data, and domain code.
- `app/app/src/test/`: JVM tests for accounting and domain behavior.
- `backend/`: TypeScript reminder backend for QStash callbacks and FCM delivery.

Do not scatter product or design decisions across comments. Update the concept document or design document when a change alters durable product rules, accounting semantics, navigation model, visual language, or explicit non-goals.

## Local Commands

Android:

```powershell
cd app
.\gradlew.bat testDebugUnitTest assembleDebug
.\gradlew.bat connectedAndroidTest
.\gradlew.bat installDebug
```

Backend:

```powershell
cd backend
bun run check
bun test
```

Use a connected real Android device for reminder, lifecycle, notification, and background-behavior smoke tests whenever practical. Emulator-only evidence is not enough for push/background claims.

## Code Rules

Use Kotlin and Jetpack Compose for Android UI. Keep accounting logic out of Compose and in the domain/data layer.

Use domain names from the product model:

- `FocusSession`
- `LeisureSession`
- `LedgerEntry`
- `DailyTracker`
- `ReminderPlan`

Store timestamps in UTC. Use the device's current system time zone for business-day calculation. Use `dailyDate` for day records.

Large Compose files should be split by screen or component group. `MainScreen.kt` should stay as the entry and shared shell, not the home for every UI component.

## Tests

Add focused tests for changes that affect:

- 04:00 system-time-zone daily boundary behavior.
- Focus earning formula.
- Leisure cost splitting across 01:00.
- Ledger adjustments after edits or deletes.
- Reminder staleness checks using `sessionId + revision`.
- Backend skip/send behavior for cancelled or stale reminders.

Name tests after behavior, not implementation details.

## Release Workflow

Release builds are tag-driven. CI creates the GitHub Release and attaches APKs; the coding agent edits the final Release text after CI succeeds.

1. Finish code, docs, version metadata, signing configuration, and CI changes in ordinary commits.
2. Manually create and push a time-based release tag such as `26.5.4`.
3. Wait for CI to build the release APKs and create the GitHub Release.
4. After CI succeeds, use `gh release edit` to update the generated Release title, description, and notes.

CI must not generate the final user-facing release copy. CI may create a draft or generated release and attach artifacts. The agent who pushed the tag is responsible for the post-CI release-note edit unless the user explicitly asks to do it themselves.

The release CI should build signed APKs for:

- `arm64-v8a`
- `armeabi-v7a`
- `x86_64`

Signing material belongs in GitHub Secrets. Never commit keystores, passwords, service account JSON, or token files.

## Agent Notes

Treat `docs/focuswell-product-spec.md` as the durable product concept and accounting-boundary document. It should stay short. Do not turn it back into a line-by-line implementation spec; exact current behavior belongs in code, tests, and release evidence.

Treat `DESIGN.md` as the reusable UI source of truth. When changing Compose surfaces, shape, elevation, icon usage, typography, motion, color semantics, or responsive behavior, update `DESIGN.md` in the same change.

Before claiming the app is basically usable, verify with current evidence:

- Android JVM tests pass.
- Backend typecheck and tests pass.
- Debug or release APK installs on a real connected device.
- Timing, ledger, reminder scheduling, FCM delivery, notification display, lifecycle, and background behavior have been smoke-tested on the real device.
