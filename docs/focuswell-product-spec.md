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
  wake target, sleep-protection start, and sleep-protection cost multiplier.
  The default rhythm keeps the FocusWell day boundary at midnight and protects
  a 23:00-07:00 sleep window.
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
Daily window: 00:00 -> 23:59
Daily grant: +60 minutes
Wake target: 05:00
Sleep-protection leisure cost: 23:00-07:00 costs 2x
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
- Rule tracker based on focus minutes for a tag

Rule trackers use same-day non-deleted focus records. Current default rule
trackers target 180 minutes for `math` and `408`.

Default tracker rewards:

- `math` and `408` rule trackers: 60 minutes each.
- `Aerobic`: 30 minutes.
- `Vocabulary` and `CodeWars`: 15 minutes each.

## Morning Check-In

The first app open of a new FocusWell day requires a morning check-in. The
check-in reviews unsettled local Android usage events from the previous
business day and clusters foreground phone use outside Focus/Leisure records
while the screen is interactive into review segments.

The Income step also settles the wake bonus as an automatic completed item. The
default target is 05:00 and is configured from Settings Rules. A check-in from
one hour before the target through thirty minutes after the target earns 30
minutes. This replaces the previous `Wake by X` daily tracker pattern.

MVP clustering rules:

- A minute is occupied when foreground app usage reaches at least 50 seconds.
- Five or more occupied minutes create a review segment.
- Occupied runs separated by one minute or less are merged.
- Existing Focus and Leisure records are excluded from phone-use review.
- Cost is the actual foreground duration inside occupied minutes, not the
  rounded count of occupied minutes.
- Charge-free apps still count toward occupied minutes outside Focus and
  Leisure, but their own foreground duration is not charged. This rule does not
  make those apps focus-related during Focus settlement.
- Non-zero costs or earnings below one minute should be displayed as `<1m` or
  `-<1m`, not rounded down to `0m`.

The user marks necessary segments as Fair Use. Unmarked segments are settled as
a negative ledger entry, capped at the current reserve so overdraft is still not
allowed. If the phone-use cost exceeds the available reserve, the check-in
clears the reserve and pauses unconditional Daily Grant entries for three
business days. Focus earnings, tracker rewards, and wake/check-in bonuses remain
active during the pause.

Today also offers a small phone-use settlement entry when the current
settlement window has billable phone-use content. It uses the same Correction
card review as morning check-in, settles usage from the last phone-use
settlement point through now, and advances that settlement point so morning
check-in only reviews the remaining unsettled usage.

The check-in UI is a three-step flow:

1. Income: animate completed earning items into checked rows with `+Xm`
   amounts. This screen is reward-only and does not show phone correction.
2. Correction: show compact phone-use segments in local clock order. The user
   reviews one segment at a time, swiping right for Fair Use and left to count
   it. App names may appear as recall metadata, but the main timeline should
   stay visual and compact.
3. Settlement: animate correction and final accounting as checked rows and
   transparent numbers. Do not show the reserve well here. If the Daily Grant is
   paused, show an ice/freeze treatment on a configured daily grant times three component, for example `60m x3` with the default grant.

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

Idea filtering is an organizing action, not a mode switch. Filter changes should
animate list movement. Rows without checklist items should stay compact instead
of reserving checklist space.

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

Settings also offers manual cloud sync for the same JSON backup payload.
GitHub OAuth identifies the user, and the backend stores one plain JSON
snapshot per GitHub account. Sync compares the local ledger update timestamp
with the cloud snapshot timestamp and asks the user before uploading local data
or restoring the cloud copy.

Cloud sync is explicit backup transport, not collaborative storage. FocusWell
does not automatically merge divergent ledgers or continuously sync in the
background.

JSON export/import and cloud sync share the same typed backup payload. The app
should keep this payload behind Kotlin serialization models so local backup,
manual cloud sync, and release-update compatibility are tested against one
transport shape instead of separate hand-built JSON paths.

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

When a device schedules a new remote session, the backend cancels remaining
pending reminders for older sessions on that device. If a stale FCM still
arrives, the Android app compares it with the local active session, drops it,
and asks the backend to cancel that remote session.

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

The Android app must validate remote reminder payloads against the local active
session before showing a notification. Stale or cancelled reminders are cleanup
work; matching active-session notifications should not depend on detached
background work that may be killed after the FCM callback returns.

## Implementation Boundaries

The product model should stay readable in code:

- Accounting and settlement logic live in domain/data helpers, not Compose.
- UI screens may call ViewModel actions, but should not build JSON, query Room
  directly, or encode reminder/update protocols.
- Large entry points remain orchestration shells. Split cohesive behavior into
  named files for records, sessions, ideas, planning, daily maintenance, backup
  transport, reminders, updates, and reusable UI state.
- Time-sensitive business logic should accept a clock or timestamp so release
  and backup behavior remains deterministic in tests.
- Prefer Kotlin time APIs in product logic: `kotlin.time` for elapsed durations
  and absolute instants, `kotlinx-datetime` for local dates, times, and time
  zones. Keep Java time conversion at interop edges.

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

- cross-device ledger merging
- automatic background account sync
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
