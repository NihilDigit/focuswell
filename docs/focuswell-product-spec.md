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
- wind-down
- depleted

All timestamps are stored in UTC. The app evaluates the FocusWell day using the
device's current system time zone with the configured daily boundary.

```text
Daily window: 04:00 -> next day 03:59
Daily grant: +60 minutes
Late-night leisure cost: 01:00-04:00 costs 2x
```

The app stores canonical local state in Room. JSON export/import remains the
manual backup and recovery path.

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

Settlement:

```text
earned = activeDurationMinutes * sessionTypeRate * tagMultiplier
```

Paused time does not count toward active duration. The current implementation
stores accumulated pause duration rather than interval-level pause history.

Session type rates:

```text
Input:  0.5
Output: 0.25
```

Tag multipliers are snapshotted onto records so later tag changes do not
silently recalculate historical sessions.

## Leisure Sessions

A leisure session spends reserve from its timestamped duration.

Rules:

- Normal leisure costs 1 minute per real minute.
- The configured sleep-protection window costs the configured multiplier per real minute.
- If the reserve is exhausted, the leisure record ends at the depletion instant.
- The app enters depleted mode when active leisure drains the reserve.
- The depleted state can be dismissed by the user.

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
- tags
- daily trackers
- focus records
- leisure records
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

## Clear All Data

Clear all data removes local records, active mode, trackers, tags, and ledger
state, then reseeds the default local model. It also rotates the reminder device
identity so old callbacks become stale.

## Current Non-Goals

These are not part of the current implementation:

- account sync
- cross-device ledger merging
- app or website blocking
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
