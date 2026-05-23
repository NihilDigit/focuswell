# Repository Guidelines

## Project Structure & Module Organization

This repository now contains the Android implementation under the nested Gradle project in `app/`.

- `README.md`: short project introduction and entry point.
- `docs/focuswell-product-spec.md`: canonical product, UX, architecture, and data-model specification.
- `DESIGN.md`: canonical UI design system and durable Material 3 Expressive constraints.
- `app/app/src/main/java/dev/nihildigit/focuswell/`: Android Kotlin and Jetpack Compose source.
- `app/app/src/test/`: JVM tests for domain behavior.
- Future instrumentation tests should use standard Android conventions, for example `src/androidTest/`.

Keep product decisions in `docs/focuswell-product-spec.md` and durable visual rules in `DESIGN.md`. Do not scatter product or design rules across comments or issue notes.

## Build, Test, and Development Commands

Useful repository commands:

```powershell
git status --short
git log --oneline
```

Use the Gradle wrapper from the Android project:

```powershell
cd app
.\gradlew.bat build
.\gradlew.bat test
.\gradlew.bat connectedAndroidTest
.\gradlew.bat installDebug
```

Use the local Android CLI and connected real device workflow when available.

## Coding Style & Naming Conventions

Use Kotlin and Jetpack Compose for the Android app. Prefer clear domain names from the spec:

- `FocusSession`
- `LeisureSession`
- `LedgerEntry`
- `DailyTracker`
- `ReminderPlan`

Use UTC for stored timestamps, `Asia/Shanghai` for daily-window calculation, and `dailyDate` for business-day records. Keep ledger/accounting calculations in a dedicated domain layer instead of UI code.

Markdown documents should use concise headings, short paragraphs, and fenced code blocks for rules or examples.

## Testing Guidelines

Add or update focused tests for:

- daily boundary calculation at 04:00 Asia/Shanghai
- focus earning formula
- leisure cost splitting across 01:00
- ledger adjustment behavior after edits/deletes
- reminder staleness checks using `sessionId + revision`

Name tests after behavior, not implementation details.

## Commit & Pull Request Guidelines

Current history uses concise imperative commits, for example:

```text
Initial product spec
```

Continue with short, descriptive commit messages. Pull requests should include:

- summary of product or code changes
- affected screens or data models
- test results, or a note explaining why tests were not run
- screenshots for UI changes once the Android app exists

## Agent-Specific Instructions

Treat `docs/focuswell-product-spec.md` as the source of truth. If implementation choices conflict with the spec, update the spec intentionally in the same change.

Treat `DESIGN.md` as the source of truth for reusable UI rules. When changing Compose surfaces, color semantics, component hierarchy, or responsive layout behavior, update `DESIGN.md` in the same change and verify on a connected device or emulator when practical.
