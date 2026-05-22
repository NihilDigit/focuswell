# Repository Guidelines

## Project Structure & Module Organization

This repository is currently in the product and architecture design phase.

- `README.md`: short project introduction and entry point.
- `docs/focuswell-product-spec.md`: canonical product, UX, architecture, and data-model specification.
- Future Android source should live under an app module such as `android/` or `app/`.
- Future tests should sit next to their target modules using standard Android conventions, for example `src/test/` and `src/androidTest/`.

Keep design decisions in `docs/focuswell-product-spec.md` until implementation files exist. Do not scatter product rules across comments or issue notes.

## Build, Test, and Development Commands

There is no buildable app module yet.

Useful commands now:

```powershell
git status --short
git log --oneline
```

Once the Android app is created, prefer the Gradle wrapper from the project:

```powershell
.\gradlew.bat build
.\gradlew.bat test
.\gradlew.bat connectedAndroidTest
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

No test framework is configured yet. When implementation begins, add focused tests for:

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
