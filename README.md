# FocusWell

FocusWell is a local-first Android app for turning focused work into leisure time you can spend later.

It is built for people who need structure without streak pressure. A focus session earns reserve. A leisure session spends reserve. The app keeps a ledger so the balance can be audited and corrected instead of treated as a vague habit score.

## Product

FocusWell uses configurable accounting rules with these defaults:

- Each day starts at 04:00 in the device's current system time zone.
- A new FocusWell day grants 60 minutes of leisure reserve.
- The morning check-in wake target defaults to 07:30 and is configurable in Rules.
- Focus earns reserve by elapsed active time, session type, and tag multiplier.
- Unlocked leisure reserve earns tiered daily savings interest at the day boundary: 5% for the first 6h, 8% from 6h to 24h, and 12% above 24h.
- Leisure spends reserve by elapsed time.
- Leisure during the ideal sleep window, 23:00-08:00 by default, spends at 2x speed.
- Edits and deletes create ledger adjustments instead of hiding history.
- Reminders use `sessionId + revision` so stale callbacks do not notify old sessions.

Today is for the current reserve, active mode, and daily trackers. Balance combines the ledger, records, and record editing. Plan manages tags and trackers. Settings holds appearance, rules, backup, and reset.

Rules are user-adjustable after install. Tags and trackers are part of the user's plan, not general app settings.

The product concept and accounting boundaries live in [docs/focuswell-product-spec.md](docs/focuswell-product-spec.md). Exact behavior should be checked against the current code and tests. UI rules live in [DESIGN.md](DESIGN.md).

## License

FocusWell is licensed under the Mozilla Public License 2.0. See [LICENSE](LICENSE).

## Android App

The app is written in Kotlin and Jetpack Compose. It includes:

- Focus, leisure, and depleted modes.
- Room-backed local persistence for timers, records, trackers, tags, and ledger entries.
- Editable focus and leisure records in Balance.
- Focus settlement with local app-usage correction when Android usage access is granted.
- Blocking morning check-in with reward income, wake bonus, phone-use correction, Fair Use review, and leisure restart locking.
- Ideas inbox and sorting surface for thoughts captured during focus.
- Daily trackers and rule-based tracker progress.
- Adjustable accounting rules and configurable tracker rewards.
- JSON export/import.
- In-app update checks against GitHub Releases, with ABI-specific APK download and checksum verification.
- Local notifications and remote FCM reminders, including long-session check-ins.
- Minified release builds with scoped Room/R8 keep rules.
- Material 3 Expressive-inspired shape, motion, typography, and icon treatment.

## Browser Extension

`browser-extension/` contains a companion Chrome/Edge extension. It is a local
whitelist gate for focused browser sessions, separate from the Android ledger
and reminder backend.

When enabled, the extension only allows top-level pages that match enabled
regular-expression rules. The default rules cover Claude, ChatGPT, Google
search result pages, and Wikipedia. The popup keeps the touch-facing controls
small: today's enabled time, a round on/off control, a rule toggle grid, and
today's not-allowed count. Detailed stats and rule JSON editing live in
Settings.

## Build

Linux/macOS:

```bash
cd app
./gradlew testDebugUnitTest assembleDebug
```

Windows PowerShell:

```powershell
cd app
.\gradlew.bat testDebugUnitTest assembleDebug
```

Install a debug build:

```bash
cd app
./gradlew installDebug
```

```powershell
cd app
.\gradlew.bat installDebug
```

Build a signed release with the same release key used by CI:

```powershell
Copy-Item app\release-signing.properties.example app\release-signing.properties
# Fill app\release-signing.properties with the same keystore and passwords stored in GitHub Secrets.
.\scripts\build-local-release.ps1 -VersionName 26.6.3
```

Release APKs are split by ABI: `arm64-v8a`, `armeabi-v7a`, and `x86_64`.

## Backend

The backend schedules reminder callbacks and sends FCM messages.

```powershell
cd backend
bun run check
bun test
```

Production backend secrets live in Vercel. Android release signing secrets live in GitHub Secrets. GitHub Secrets cannot be read back with `gh`; the local release script only verifies that the required secret names exist, then uses the untracked local signing file. Release builds fail when signing material is missing so local release APKs do not drift to another key.

## Release

Releases are manual-tag driven. Use the next `YY.M.patch` tag in the current series, such as `26.5.19`. The patch number is a change sequence within that series, not the day of month. Release CI accepts three-part numeric tags and verifies that the first two parts match the current UTC+8 year and month. After CI creates the GitHub Release and attaches APKs, edit the Release title and notes by hand.

The current public release is [26.6.3](https://github.com/NihilDigit/focuswell/releases/tag/26.6.3).
