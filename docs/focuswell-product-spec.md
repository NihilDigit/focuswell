# FocusWell Product Spec

FocusWell is a calm time-accounting app for ADHD-friendly focus and leisure.
It turns daytime focus into storable evening entertainment time, so high-energy
days can support low-energy days.

The product is not a streak app, punishment system, or productivity scoreboard.
Its core promise is external time sense, low-friction behavior control, and an
auditable leisure reserve.

## Product Principles

- High-energy days should be able to support low-energy days.
- Entertainment balance is a reserve, not a daily expiring reward.
- Time accounting should feel like a ledger, not a moral judgment.
- Focus tracking should reduce time blindness without creating progress anxiety.
- Entertainment tracking must stay nearly frictionless, or it will be skipped.
- Records should be editable, but balance changes must remain auditable.
- The app should feel dopamine-friendly: lively, tactile, and rewarding without
  turning the ledger into a slot machine.

## Day Boundary

FocusWell uses a fixed daily window:

```text
Timezone: Asia/Shanghai
Daily boundary: 04:00
Daily window: 04:00 -> next day 03:59
```

This keeps late-night behavior attached to the user's real day instead of
splitting it at midnight.

Storage rules:

- Store all timestamps in UTC.
- Calculate daily windows with the IANA timezone `Asia/Shanghai`.
- Store `dailyDate` separately as the business date for daily trackers and
  daily grants.

## Leisure Reserve

The leisure reserve stores entertainment time.

Rules:

- Each day grants 60 minutes of entertainment time at 04:00.
- The reserve carries over across days.
- The reserve has no upper limit.
- Overdraft is not allowed.
- All reserve changes are recorded in a ledger.

The system has no hard reserve cap. The UI should still avoid making very large
balances feel like a jackpot or a hoarding target.

Suggested display treatment:

- Under 60 minutes: show exact minutes.
- 60 to 300 minutes: show hours and minutes.
- Above the threshold: use a calm summary such as `Reserve is sufficient`.

The exact full balance remains available in the ledger.

Daily grants are ledger entries:

```text
+60min
source: daily_grant
createdAt: daily boundary
```

## Focus Sessions

A focus session is the main way to earn additional entertainment time.

Required at start:

- Task
- Type
- Tag optional

Required at end:

- Result

The result input should be low friction. A one-tap result plus optional note is
preferred:

- Completed as planned
- Partially completed
- Drifted from plan
- Interrupted

### Session Types

Types describe the purpose of the session, not the surface behavior.

```text
Input:  actual duration * 0.5
Output: actual duration * 0.25
```

Input means learning, understanding, researching, practicing, or building
capacity.

Output means writing, shipping, submitting, or producing a visible deliverable.

Examples:

- Math problem practice: Input
- Paper research: Input
- Writing a paper: Output
- Learning 408 content: Input
- Building a project feature: Output

### Built-In Tags

FocusWell starts with two editable built-in tags:

```text
math: multiplier 2.0
408:  multiplier 1.5
```

Tags are editable. More tags can be added when needed. A focus session may also
be untagged; untagged focus uses a neutral `1.0` multiplier.

### Earning Formula

```text
earnedMinutes = activeDurationMinutes * typeRate * tagMultiplier
```

Examples:

```text
120min Input math
= 120 * 0.5 * 2.0
= 120min leisure earned
```

```text
120min Output 408
= 120 * 0.25 * 1.5
= 45min leisure earned
```

## Leisure Sessions

Entertainment tracking should be minimal.

The leisure screen shows only:

- Remaining time
- Elapsed time
- Low-balance reminders

No entertainment category, plan, result, or review is required.

### Leisure Cost

Normal entertainment costs 1x.

Entertainment after 01:00 costs 2x until the 04:00 day boundary.

```text
09:00-01:00: 1x cost
01:00-04:00: 2x cost
```

This should be presented as a sleep-protection rule, not a penalty.

Preferred names:

- Night protection rate
- Sleep protection cost
- Late-night mode 2x

Avoid:

- Penalty
- Violation
- Punishment
- Overtime fee

If a leisure session crosses 01:00, only the portion after 01:00 is charged at
2x.

Example:

```text
00:40-01:20 entertainment
00:40-01:00: 20min * 1x = 20min
01:00-01:20: 20min * 2x = 40min
total cost: 60min
```

### Low-Balance Reminders

Suggested reminders:

- 10 minutes remaining
- 5 minutes remaining
- 1 minute remaining
- Balance depleted

Because overdraft is not allowed, leisure tracking should stop or enter a
depleted state when the reserve reaches zero.

### Depleted State

When the leisure reserve reaches zero, the UI enters a gentle depleted state.

Avoid red-alert visuals, failure language, and gamified punishment.

Suggested copy:

```text
Today's entertainment balance is used up.
Another 60 minutes will be added tomorrow at 04:00.
You can end leisure now, or switch to wind-down mode.
```

Actions:

- End leisure
- Start wind-down
- View ledger

If technical blocking is available, it can block continued entertainment. If
blocking is not available, FocusWell should stop the leisure record and clearly
show that the reserve is depleted.

## Daily Trackers

Daily trackers are editable and evaluated within the Asia/Shanghai 04:00 daily
window.

There are two tracker kinds:

- Boolean tracker
- Rule tracker

### Default Boolean Trackers

- Aerobic exercise
- Woke up on time
- Vocabulary practice
- CodeWars

The wake-up tracker target is:

```text
wake time <= 09:00
```

First version behavior:

- Default interaction: checkbox.
- Optional expanded field: exact wake time.

This keeps the tracker low friction while preserving a path toward better
auditability.

### Default Rule Trackers

Rule trackers are based on raw focus duration, not earned leisure time.

```text
math >= 3h
408 >= 3h
```

This means:

```text
sum(activeDuration where tag = math within daily window) >= 180min
sum(activeDuration where tag = 408 within daily window) >= 180min
```

Tag multipliers do not affect tracker completion.

## Editing And Audit

All user-facing records should support CRUD:

- Focus sessions
- Leisure sessions
- Daily tracker records
- Tag configuration
- Tracker configuration

However, the leisure reserve should not be silently overwritten. It should be
derived from auditable ledger entries.

When a record changes, FocusWell records the balance delta.

Historical edits must not recalculate balance silently. The edit confirmation
screen should show:

```text
Original earned: +45min
New earned: +60min
Balance delta: +15min
```

Primary action:

```text
Save and apply balance change
```

Configuration changes, such as changing a tag multiplier from `2.0` to `1.5`,
must never automatically rewrite historical sessions.

Examples:

```text
Focus session edit:
old earned: +45min
new earned: +60min
ledger delta: +15min
```

```text
Deleted focus session:
ledger delta: -earnedMinutes
```

```text
Deleted leisure session:
ledger delta: +costMinutes
```

## Snapshot Rules

Historical sessions should keep calculation snapshots so later configuration
changes do not rewrite history.

Suggested session snapshots:

- Type name
- Type rate
- Tag name, if selected
- Tag multiplier, or `1.0` when untagged
- Earned minutes

If `math` changes from `2.0` to `1.5`, old sessions keep their original earned
amount unless explicitly recalculated through an edit flow.

## Suggested Data Objects

### Tag

```text
id
name
multiplier
color optional
archivedAt optional
createdAt
updatedAt
```

### FocusSession

```text
id
task
result
type: input | output
typeRateSnapshot
tagId optional
tagNameSnapshot optional
tagMultiplierSnapshot
startedAt
endedAt
pauseIntervals
activeDurationMinutes
earnedMinutes
dailyDate
deletedAt optional
createdAt
updatedAt
```

Pause should be supported from the first version, but kept simple:

- Pause
- Resume
- End
- Active duration excludes paused time

Pause intervals should be stored for audit, but the first version does not need
interval-level editing.

## Android App Shape

FocusWell is an Android-first app built with Jetpack Compose and Material 3.

Suggested stack:

- Jetpack Compose UI
- Material 3 / Material 3 Expressive components
- Room for local persistence
- DataStore for lightweight settings
- WorkManager for non-exact maintenance jobs
- Backend API for reminder scheduling and sync boundaries
- Upstash QStash for delayed reminder callbacks
- Firebase Cloud Messaging for Android push delivery

MVP architecture decisions:

- Local-first.
- No account system.
- Device authentication with `deviceId + installSecret`.
- Backend stores reminder state and FCM tokens, not the canonical ledger.
- Active modes are mutually exclusive: `focus | leisure | wind_down | none`.
- JSON export/import should be supported as the basic backup path.
- Clear all data should be supported in MVP.

Language:

- MVP system UI is English.
- User-entered task, result, and tag names may use any language.
- Typography is part of the product identity, so English-first UI is preferred
  for the first version.

Top-level destinations:

- Today
- Reserve
- Records
- Settings

Today is the primary working screen. It contains the active focus timer,
entertainment reserve, leisure timer, and daily trackers.

Reserve shows ledger history and balance changes.

Records provides CRUD for focus sessions, leisure sessions, tags, and trackers.

Settings contains tag multipliers, tracker configuration, notification behavior,
daily boundary, and sleep-protection settings.

### Compose UI Direction

The UI should use Material 3 Expressive carefully: expressive motion and shape
can make the app feel warm, but the app should remain operational and readable.

FocusWell can be visually dopamine-friendly. It should use richer components,
motion, playful state changes, and strong tactile feedback to make time feel
present.

Use expressive treatment for:

- Timer state transitions
- Reserve status
- Depleted state
- Wind-down state
- Daily tracker completion
- Starting, pausing, resuming, and ending sessions
- Earning leisure time from focus sessions

Good expressive directions:

- Large, legible timer surfaces.
- Shape morphs between focus, leisure, depleted, and wind-down states.
- Animated progress rings or liquid progress surfaces.
- Satisfying but brief earn animations.
- Strong haptic feedback for start, pause, resume, end, and tracker completion.
- Dynamic color support.
- Distinct visual identities for Input, Output, Leisure, and Wind-down.
- Typography with a warm display face for large timer numerals and a highly
  readable sans face for controls and records.

Avoid:

- Casino-like reward moments
- Bright warning states for depleted balance
- Large provocative displays of very high reserve balances
- Streak-first layouts

Ledger and reserve displays should stay calmer than timer surfaces. The app may
feel game-like during interaction, but the accounting layer should still feel
trustworthy.

### Typography Direction

Use typography as a core part of the expressive identity.

Recommended roles:

- Display/timer: large, energetic, stable numerals.
- UI sans: readable controls, sheets, settings, and records.
- Mono: ledger deltas, timestamps, rates, and exact numeric audit details.

Candidate direction:

```text
Display / timer: Space Grotesk
UI sans: Manrope or Roboto Flex
Mono: JetBrains Mono or Roboto Mono
```

Timer numerals must use stable/tabular number treatment so the layout does not
jitter while counting.

### Adaptive Layout

The first target is a phone, but the structure should not assume phone-only UI.

On compact width:

- Use bottom navigation.
- Keep Today as a single clear vertical flow.

On medium and expanded width:

- Use navigation rail.
- Show Today with a supporting pane for ledger or tracker detail.
- Records can become list-detail.

## UX Design

FocusWell's UX is organized into three layers:

```text
Expressive layer: Today timer, mode transitions, completion feedback
Calm layer: Reserve ledger, Records
Plain layer: Settings, destructive/reset flows
```

The app should open directly into the usable experience, not a landing page or
explanation screen.

### Top-Level Navigation

```text
Today
Reserve
Records
Settings
```

Phone:

- Bottom navigation.

Tablet, landscape, desktop-sized windows:

- Wide navigation rail.

### Today Screen

Today is the primary screen.

When no session is active, the first viewport should show:

```text
Reserve status
Large timer organism
Primary action row
Daily trackers
Tag progress strip
```

Default copy:

```text
Reserve is sufficient
Ready when you are

[Start Focus]
[Start Leisure]
```

Low reserve copy:

```text
48 min left
Use it deliberately

[Start Focus]
[Start Leisure]
```

Design decisions:

- Main visual: numeric timer inside a morphing shape organism.
- Start Focus flow: bottom sheet.
- Daily trackers: compact 2-column grid.
- Start actions: two large buttons, `Start Focus` and `Start Leisure`.
- FAB menus may be used later for secondary creation flows, not for the main
  start path.

### Daily Trackers On Today

Boolean trackers are expressive toggle pills:

```text
Aerobic
Wake by 9
Vocabulary
CodeWars
```

Rule trackers are automatic progress pills:

```text
Math 1h 40m / 3h
408 35m / 3h
```

Rule tracker completion should animate briefly, but it should not dominate the
screen.

### Start Focus Flow

Use a bottom sheet with this field order:

```text
Task
Input / Output
Tag: math / 408 / more
Start
```

The task field should receive focus first.

Show the effective earning rate in plain language:

```text
Input · math · earns 1.0x real time
Output · 408 · earns 0.375x real time
```

This is more useful than showing the raw type rate and tag multiplier separately
in the start flow.

### Active Focus

Primary content:

```text
Large elapsed timer
Task
Input · math
Earning +1.0x
```

Actions:

```text
Pause
End
```

Paused state:

```text
Paused

[Resume]
[End]
```

Ending a focus session requires a result, but the input should stay low
friction:

```text
What came out of this?

[As planned]
[Partial]
[Drifted]
[Interrupted]

Optional note/result field
[Save result]
```

After saving:

```text
+47 min added
```

The earn animation should be satisfying and brief. A good direction is a small
flow from the timer organism into the reserve indicator. Avoid large confetti or
celebration patterns.

### Start Leisure

Leisure starts with minimal friction:

```text
Start Leisure
```

Do not ask for category, plan, or result.

If reserve is low, show a small pre-start note:

```text
18 min available
```

### Active Leisure

The leisure screen should show only:

```text
Remaining
42:18

Elapsed
17:42

[End Leisure]
```

When sleep protection is active:

```text
Sleep protection 2x
```

This should appear as a small badge or supporting label, not a warning.

Low-balance reminder states:

```text
10 min left
5 min left
1 min left
```

### Depleted State

When reserve reaches zero:

```text
Balance used up
Another 60 min arrives at 04:00.

[End Leisure]
[Start Wind-down]
[View Ledger]
```

Use lower saturation and calm motion. Do not use red-alert visuals.

### Wind-Down

Wind-down is a low-stimulation state:

```text
Wind-down
12:30

No earning. No spending.

[End]
```

Animations should slow down, and the color treatment should become softer and
less saturated.

### Reserve Screen

Reserve should feel trustworthy rather than stimulating.

Top copy:

```text
Reserve is sufficient
```

If reserve is not high:

```text
48 min available
```

Daily summary:

```text
Today
+60 min daily grant
+45 min focus
-30 min leisure
Net +75 min
```

Ledger row examples:

```text
+60 Daily grant
+47 Focus · Input math
-30 Leisure
-12 Adjustment
```

Very large reserve values should use the soft display rules from the Leisure
Reserve section. Exact full values remain available in the ledger.

### Records Screen

Use tabs:

```text
Focus
Leisure
Trackers
Tags
```

Focus row:

```text
Input · math
Linear Algebra problem set
1h 12m · +72 min
```

Leisure row:

```text
Leisure
38m elapsed · -38 min
```

Historical edits use the audit confirmation flow:

```text
Original earned +45 min
New earned +60 min
Balance delta +15 min

[Save and apply balance change]
```

### Settings Screen

Suggested groups:

```text
Rules
- Daily grant: 60 min
- Day boundary: 04:00
- Sleep protection: 01:00 · 2x

Tags
- math 2.0x
- 408 1.5x

Reminders
- Push status
- Test notification

Data
- Export JSON
- Import JSON
- Clear all data
```

Clear all data should open a dedicated confirmation screen rather than a small
dialog.

### Clear All Data

MVP must include a clear-all-data function.

It should remove local user data from Room and DataStore:

- Focus sessions
- Leisure sessions
- Wind-down sessions
- Ledger entries
- Tags and tracker configuration
- Daily tracker records
- Reminder registration state
- Device auth state if the user chooses a full reset

Clear data should also call the backend to cancel active reminder plans and drop
the current FCM token. If the backend request fails, local clearing can still
complete, but the app should rotate `deviceId + installSecret` afterward so old
callbacks become stale.

UX requirements:

- Use a destructive confirmation screen, not a tiny dialog.
- State that the action removes local records and reserve history.
- Require an explicit confirmation phrase or long press.
- Offer JSON export before clearing.

No ledger entries are needed for clear-all-data because it intentionally resets
the audit universe.

## Android Timer Strategy

Timer correctness must come from persisted timestamps, not from a constantly
running in-memory countdown.

Core rule:

```text
Display time = nowUtc - persisted startedAtUtc - pause intervals
```

This avoids losing correctness when the process is killed, the screen turns off,
or the app is backgrounded.

The app does not need to remain alive in the background to keep timer accounting
correct. Running timers are reconstructed from persisted UTC timestamps when the
app returns.

### Focus Timer

Focus sessions should be stored as soon as they start.

On start:

- Insert a running FocusSession with `startedAt`.
- Store task, type, tag, type rate snapshot, and tag multiplier snapshot.

On pause:

- Append a pause interval start.

On resume:

- Close the current pause interval.

On end:

- Store `endedAt`.
- Require result.
- Calculate active duration from timestamps.
- Calculate earned minutes.
- Add ledger entry.

The UI may tick every second while visible, but the tick is only presentation.
It is not the source of truth.

### Leisure Timer

Leisure should also be timestamp-based.

On start:

- Insert a running LeisureSession with `startedAt`.
- Read current reserve.

While visible:

- Show elapsed time.
- Show remaining reserve.
- Apply 2x cost only to the part after 01:00.

When the app is backgrounded:

- Persist the session locally.
- Schedule cloud reminders through the backend if notification permission and
  an FCM token are available.
- Reconstruct elapsed time and cost from timestamps when the app returns.

On depletion:

- End or stop charging the leisure session.
- Enter depleted state.
- Do not allow negative reserve.

### Cloud Reminder Scheduling

FocusWell uses QStash and FCM for reminder delivery, not as the source of timer
truth.

```text
Room = timer and ledger truth
Backend = active reminder mirror
QStash = delayed callback scheduler
FCM = Android notification delivery
App = timestamp-based settlement and UI
```

Flow:

```text
App starts or updates a session
-> write Room immediately
-> call backend with sessionId, revision, reminder plan
-> backend stores pending reminders
-> backend schedules QStash callbacks
-> QStash calls backend later
-> backend verifies QStash signature
-> backend checks sessionId + revision + reminderId
-> backend sends high-priority FCM data message
-> Android FirebaseMessagingService renders local notification
```

The backend must treat QStash callbacks as untrusted until verified. It must
also check whether a callback is stale before sending FCM.

Staleness guard:

```text
sessionId still active
revision still current
reminderId still pending
```

Do not rely only on deleting a QStash message. Cancellation is best-effort.
Session revision is the real invalidation mechanism.

Recommended reminder kinds:

```text
focus_stale_3h
leisure_10m_left
leisure_5m_left
leisure_1m_left
leisure_depleted
late_night_rate_started
```

Focus sessions usually need only stale-session reminders. Leisure sessions need
low-balance, depleted, and late-night protection reminders.

FCM payloads should be data-only high-priority Android messages. The Android app
renders the system notification itself so notification actions and deep links
remain under app control.

### Background And System Constraints

Avoid depending on a long-running background loop. Android may stop background
work, defer alarms, or restrict services.

Preferred behavior:

- Use persisted timestamps for correctness.
- Use QStash + FCM for best-effort user-visible reminders.
- Use local in-app checks when the app is foregrounded.
- Request notification permission only at the moment the user enables timer
  reminders.
- Do not require exact alarm permission for the core timer design.

Foreground service policy:

- Do not use a foreground service for normal timer ticking.
- Do not keep a background service alive just to count time.
- A future optional foreground service may be added only if the user explicitly
  wants persistent controls outside the app.

Limitations:

- If the user force-stops the app, FCM delivery to the app may not wake it.
- If notification permission is denied, cloud reminders cannot surface as
  notifications.
- If the device is offline, reminders may be delayed.
- These limitations must not affect ledger correctness because settlement uses
  timestamps.

### Daily Grant Job

The daily 60-minute grant should be idempotent.

At app open, and from a scheduled maintenance job, FocusWell should check:

```text
Is there a daily_grant ledger entry for this dailyDate?
```

If not, create it.

This means the daily grant does not require an exact 04:00 background execution
to remain correct. The app can backfill missed grants safely.

### LeisureSession

```text
id
startedAt
endedAt
elapsedMinutes
normalCostMinutes
lateNightCostMinutes
totalCostMinutes
dailyDate
deletedAt optional
createdAt
updatedAt
```

### LedgerEntry

```text
id
deltaMinutes
sourceType: daily_grant | focus_session | leisure_session | adjustment
sourceId optional
dailyDate
note optional
createdAt
```

### DailyTrackerConfig

```text
id
name
kind: boolean | rule
ruleTagId optional
ruleDurationMinutes optional
archivedAt optional
createdAt
updatedAt
```

### DailyTrackerRecord

```text
id
trackerId
dailyDate
completed
manualValue optional
computedValue optional
createdAt
updatedAt
```

## Open Questions

- What should `wind-down mode` include in the first version?
- If app or site blocking is implemented later, what should be blocked and how
  should overrides work?
