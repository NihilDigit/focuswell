# FocusWell Product Concept

FocusWell is a local-first Android app for turning focused work into leisure
time that can be spent later. It is designed for people who need external time
sense and low-friction guardrails without streak pressure.

This document is a product concept and implementation boundary, not a complete
future roadmap or a line-by-line implementation spec. It remains useful because
it keeps the product vocabulary, accounting model, and current non-goals in one
place. The current implementation and tests are the source of truth for exact
screen behavior.

## Product Principles

- High-energy days can support low-energy days.
- Entertainment balance is a reserve, not a daily expiring reward.
- Time accounting should feel like a ledger, not a moral judgment.
- Focus tracking should reduce time blindness without creating progress
  anxiety.
- Entertainment tracking must stay nearly frictionless.
- Records are editable, but balance changes remain auditable.
- Plan defines configurable earning inputs: focus tags, daily trackers, tracker rewards, and rule tracker targets.
- Rules define configurable accounting boundaries: daily grant, daily boundary,
  sleep-protection start, and sleep-protection cost multiplier.
- The UI can feel lively, but the accounting layer should stay calm and
  trustworthy.

## Core Model

FocusWell has one active mode at a time:

- none
- focus
- leisure
- depleted

All timestamps are stored in UTC. The app evaluates the FocusWell day using the
device's current system time zone with the configured daily boundary.

Default rules:

```text
Daily window: 04:00 -> next day 03:59
Daily grant: +60 minutes
Late-night leisure cost: 01:00-04:00 costs 2x
```

The app stores canonical local state in Room. JSON export/import remains the
manual backup and recovery path.

## Navigation Model

Primary destinations:

- Today: current reserve, active mode, daily trackers, and depleted state.
- Balance: ledger-backed account view, 7-day net chart, records, and record
  editing or deletion.
- Ideas: captured ideas, an inbox, and four sorting quadrants.
- Plan: focus tags, daily trackers, tracker rewards, and rule tracker targets.
- Settings: appearance, accounting rules, app update checks, JSON
  backup/restore, and destructive reset.

History is not a primary destination. Historical records are part of Balance so
account review and record correction stay in the same place.

## Leisure Reserve

The leisure reserve stores entertainment time.

Rules:

- A new FocusWell day grants the configured daily minutes.
- The reserve carries over across days.
- Overdraft is not allowed.
- All reserve changes are ledger entries.
- Deleted or edited records create ledger adjustments instead of rewriting
  history silently.

The displayed reserve is derived from the ledger sum and clamped at zero. The
ledger remains the audit trail.

## Focus Sessions

A focus session earns reserve from active duration.

Inputs:

- task, with a default title when omitted
- session type: input or output
- optional tag multiplier
- optional local usage correction when Android usage access is already granted
- outcome: as planned, partial, drifted, or interrupted

Settlement:

```text
adjustedActiveMinutes = max(0, activeDurationMinutes - nonFocusAppMinutes)
earned = adjustedActiveMinutes * sessionTypeRate * tagMultiplier * outcomeMultiplier
```

Paused time does not count toward active duration. The current implementation
stores accumulated pause duration rather than interval-level pause history.

Usage correction is local-only and appears only when Android usage access has
already been granted. The user can mark app usage as focus-related during
settlement so it is excluded from the deduction.

Session type rates:

```text
Input:  0.5
Output: 0.25
```

Outcome multipliers:

```text
As planned:  1.0
Partial:     0.8
Drifted:     0.3
Interrupted: 1.0
```

Tag multipliers are snapshotted onto records so later tag changes do not
silently recalculate historical sessions.

Start Focus may offer up to five recent focus task chips. Choosing one only
fills the task field; it does not start a session.

While focus is running, the main timer shows seconds. Settlement and ledger
amounts use minute units because the accounting model is minute-based.

## Leisure Sessions

A leisure session spends reserve from its timestamped duration.

Rules:

- Normal leisure costs 1 minute per real minute.
- The configured sleep-protection window costs the configured multiplier per real minute.
- If the reserve is exhausted, the leisure record ends at the depletion instant.
- The app enters depleted mode when active leisure drains the reserve.
- The depleted state can be dismissed by the user.

While leisure is running, the main remaining-time display shows seconds. The
elapsed-time badge uses minute units as a coarse usage statistic. The progress
bar represents remaining leisure time and shrinks from right to left.

## Daily Trackers

Daily trackers are evaluated within the configured system-time-zone daily
window. Each tracker has a configurable minute reward. At the day boundary,
completed non-archived trackers are settled once into the ledger as positive
reserve adjustments, then the daily tracker state resets for the new day.

Current tracker kinds:

- Boolean tracker
- Wake-time tracker
- Rule tracker based on focus minutes for a tag

Rule trackers use same-day non-deleted focus records. Current default rule
trackers target 180 minutes for `math` and `408`.

Default tracker rewards:

- `math` and `408` rule trackers: 60 minutes each.
- `Aerobic`, `Vocabulary`, `CodeWars`, and `Wake by 9`: 15 minutes each.

## Ideas

Ideas are separate from focus settlement. During an active focus session, the
user can quickly capture a single idea at a time. Captured ideas go directly to
the Ideas inbox and do not require review at the end of the focus session.

Ideas can be sorted into:

- Inbox: captured, not sorted.
- Do now: important and urgent.
- Schedule: important, not urgent.
- Contain: urgent, but keep it bounded.
- Explore: interesting without pressure.

An idea may contain a small local checklist. These items stay inside the idea
for later consideration and do not become daily trackers, focus settlement
notes, or ledger entries.

## Records And Ledger

Current editable records:

- focus records
- leisure records

Editing a focus record creates a ledger entry for the delta between old and new
earned minutes. Deleting a focus or leisure record marks it deleted and writes a
compensating ledger adjustment.

The app preserves the original calculation fields on records, including tag
multiplier and type rate.

## Persistence

Room is the canonical local persistence layer for:

- active mode
- accounting rules
- tags
- daily trackers
- focus records
- leisure records
- ideas
- ledger entries

Room schema files are exported under `app/app/schemas` so future schema changes
can be reviewed and migrated deliberately.

The current Room cutover is intentionally destructive with respect to old
SharedPreferences state. There was no meaningful production user data to
migrate. JSON import/export is still supported for explicit backup transport.

## Reminders

FocusWell uses the backend only for reminder delivery, not as the source of
truth for timers or the ledger.

```text
Room = timer and ledger truth
Backend = active reminder mirror
QStash = delayed callback scheduler
FCM = Android notification delivery
App = timestamp-based settlement and UI
```

Reminder callbacks use `sessionId + revision` to avoid notifying stale sessions.
FCM payloads are data-only high-priority Android messages; the Android app
renders local notifications.

Focus and leisure sessions may also schedule persistent check-ins at 1 hour, 3
hours, and 5 hours. These reminders are enabled by default and can be disabled
from Settings. They are a time-sense aid only; they do not change settlement or
ledger accounting.

Settings has a Push switch for remote reminder delivery. Off means the user has
disabled remote reminders, Firebase token registration is missing, or Android
notification permission is missing. Turning it on requests notification
permission when needed and refreshes FCM registration.

Debug builds use the normal Android debug key. Release builds, whether local or
CI-built, must use the same release keystore. A release build should fail if the
release signing material is not available.

## Browser Extension

The companion browser extension is a local Chrome/Edge whitelist gate. It is
not part of the Android ledger, does not earn or spend reserve, and does not
talk to the reminder backend.

When enabled, the extension only allows top-level browser pages whose URLs
match enabled regular-expression rules. The default rules cover Claude,
ChatGPT, Google search result pages, and Wikipedia. The popup records local
extension stats: today's enabled time, total enabled time, allowed whitelist
navigations, blocked navigations, per-rule usage, and recent behavior.

The extension UI keeps regex maintenance out of the main screen. Home shows a
timer, a single enable/disable control, a rule toggle grid parsed from the
current JSON rules, and today's not-allowed count. Settings contains the full
stats and JSON rule editor.

## Clear All Data

Clear all data removes local records, active mode, trackers, tags, and ledger
state, then reseeds the default local model. It also rotates the reminder device
identity so old callbacks become stale.

## Current Non-Goals

These are not part of the current implementation:

- account sync
- cross-device ledger merging
- automatic Android app blocking
- syncing browser extension state into the Android ledger
- interval-level pause audit
- WorkManager-based daily maintenance
- DataStore migration for lightweight settings
- final user-facing release copy generated by CI

## Release Evidence

Before claiming a release is usable, verify:

- Android JVM tests pass.
- Room instrumentation tests pass on a connected Android device.
- Debug or release APK installs and launches on a connected Android device.
- Minified release APK builds successfully with R8.
- Backend typecheck and tests pass when backend code is touched or release CI
  runs.
