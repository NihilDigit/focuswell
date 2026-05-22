# FocusWell Design System

FocusWell follows Material 3 Expressive as a product design system, not as a visual skin. Expressive choices must make the app easier to read, easier to act on, and easier to trust.

The canonical product rules live in `docs/focuswell-product-spec.md`. This file defines how those rules appear in UI.

## Material 3 Expressive Reading

Source pages read with Kimi WebBridge:

- `https://m3.material.io/`
- `https://m3.material.io/blog/building-with-m3-expressive`
- `https://m3.material.io/styles/color/system/overview`
- `https://m3.material.io/styles/motion/overview`
- `https://m3.material.io/styles/shape/overview-principles`
- `https://m3.material.io/styles/typography/overview`
- `https://m3.material.io/components/button-groups/overview`
- `https://m3.material.io/components/split-button/overview`
- `https://m3.material.io/components/toolbars/overview`
- `https://m3.material.io/components/progress-indicators/overview`

The useful reading for this app:

M3 Expressive is an evolution of Material 3 that uses color, motion, components, typography, and shape to make products more usable and emotionally legible. It is not a separate version of Material, and it does not replace ordinary product discipline.

The expressive update matters because it is researched as a usability system. The goal is better hierarchy, faster recognition of key UI elements, clearer personal style, and more useful interfaces. A screen is not expressive because one element is large or round; it is expressive when the user can immediately see what matters and act without decoding the layout.

For FocusWell, this means the UI should answer three questions without explanation:

1. Can I spend leisure time now?
2. What is currently running?
3. What is the next safe action?

Everything else is secondary.

## Product Register

FocusWell is an operational Android app. It should feel warm and tactile, but it is still a tool. Familiar controls are preferred when they reduce effort.

Design scene:

A student checks the app many times during a day and late at night. The UI must work when attention is fragmented, energy is low, and self-control is already strained.

Default mode:

- Low cognitive load.
- Direct actions.
- Large targets.
- No moralizing copy.
- No decorative complexity in accounting views.

## UX Architecture

The app has three layers.

Expressive layer:

- Today
- Active focus
- Active leisure
- Depleted state
- Wind-down
- Daily tracker completion

Calm layer:

- Balance ledger
- History records
- Audit deltas

Plain layer:

- Settings
- Import/export
- Clear all data

Expressive treatment must be strongest in the first layer and weakest in the third.

## Navigation Labels

Current labels `Reserve` and `Records` are implementation-oriented. User-facing labels should describe user intent.

Recommended top-level labels:

- `Today`: current work and immediate actions.
- `Balance`: leisure balance and ledger.
- `History`: editable past sessions and configurations that affect records.
- `Settings`: rules, backup, reminders, destructive flows.

`Balance` is not a second timer page. It is the auditable account of leisure time.

`History` is not the ledger. It is where past focus, leisure, trackers, and tags can be reviewed or edited.

## Today Screen Model

Today is the working surface. It should not open as a dashboard or a decorative timer poster.

Idle state hierarchy:

1. Reserve status in plain language.
2. Primary actions: start focus, start leisure.
3. Daily trackers.
4. Small ledger or progress context only if it helps the next action.

Active focus hierarchy:

1. Elapsed focus time.
2. Task.
3. Session type and tag.
4. Earn rate in plain language.
5. Pause and end actions.

Active leisure hierarchy:

1. Remaining leisure time.
2. Elapsed leisure time.
3. Sleep protection state, only when relevant.
4. End leisure.

Idle Today should not spend most of the first viewport on a zero timer. A timer earns scale when a session is running.

## Balance And History

Balance should feel trustworthy. It uses quieter surfaces, compact rows, and clear deltas.

Balance rows should prefer this structure:

```text
+60  Daily grant
+47  Focus · Input math
-30  Leisure
-12  Adjustment
```

The amount is the scanning anchor. The label explains source and context.

History supports editing. It should show enough context to identify a record and enough accounting detail to understand the effect of edits.

Focus history row:

```text
Input · math
Linear Algebra problem set
1h 12m · +72 min
```

Leisure history row:

```text
Leisure
38m elapsed · -38 min
```

## Color Strategy

Use Material color roles instead of ad hoc colors.

Color roles:

- Primary: focus, current navigation, primary action.
- Secondary: calm support, trackers, selected supporting controls.
- Tertiary: leisure and sleep-protection context.
- Error: destructive actions only. Do not use red for low reserve or depleted leisure.

Surface roles:

- `surface`: app background and plain settings.
- `surfaceContainer`: rows and ordinary grouped content.
- `surfaceContainerHigh`: active state surfaces.
- `primaryContainer`: reserve summary when it is the primary status.
- `tertiaryContainer`: leisure-specific status.

Dynamic color can be supported, but only if the semantic roles remain clear. If dynamic color makes focus, leisure, and accounting indistinct, use the product palette.

Avoid one-note palettes. FocusWell may use green, blue-gray, and amber roles, but no screen should read as a single tinted slab unless it is a deliberate state surface.

## Shape Strategy

Shape communicates state and grouping.

Use roundness for:

- Primary actions.
- Running session surfaces.
- Daily tracker pills.
- Depleted and wind-down states, with softer contrast.

Use calmer rectangular shapes for:

- Ledger rows.
- History rows.
- Settings groups.
- Import/export forms.

Shape morphing should be tied to interaction or state:

- Idle to focus.
- Focus to paused.
- Leisure to low balance.
- Tracker open to done.

Do not use unusual shapes as decoration on accounting screens.

## FocusWell Motif

The product should carry a visible `well` motif on Today, but it should read as a state symbol rather than an illustration.

Use the motif for:

- Current leisure reserve.
- Active reserve changes.
- Depleted or wind-down transitions.

The motif can use custom drawing, animated water, ripples, and shape morphing. Keep it symbolic: concentric line-art openings, waterline, and reflection are preferable to a literal bucket, tank, measuring cup, or decorative scene.

The motif must not compete with the primary number. The reserve amount and next action stay readable first; the well gives the state a memorable physical model second.

Do not use the motif in Balance or History rows. Those screens must remain auditable.

Balance must not reuse the Today leisure well card. It should use compact ledger-style summary surfaces with amount-first accounting.

Idle Today may use a separate sandglass motif to suggest readiness and pending action. Keep it in the background and away from button labels.

Daily trackers should read as the day's reset rail: a compact progress line plus scannable tracker tiles. Avoid plain card stacks and avoid row layouts that force tracker names to break into unreadable fragments.

## Motion Strategy

Motion should show state, not perform.

Use expressive spring-like motion for:

- Starting focus or leisure.
- Pausing and resuming focus.
- Completing a tracker.
- Transitioning into depleted or wind-down state.

Use quieter standard motion for:

- Navigation.
- Opening History tabs.
- Settings forms.
- Import/export dialogs.

Spatial motion is for position, size, shape, and layout changes. Effects motion is for color and opacity. Avoid overshoot on color and opacity changes.

Target feel:

- Fast for buttons and chips.
- Default for mode transitions.
- Slow only for wind-down or large active-state changes.

## Typography

Use Material type roles consistently.

Recommended mapping:

- Display or headline: active timer values only.
- Headline: top-level screen title or active mode title.
- Title: section labels and row titles.
- Body: explanations and secondary text.
- Label: buttons, chips, badges, navigation labels.

Emphasized type is for priority, not decoration. Use it for the active timer, the current reserve status, selected tabs, and important balance deltas.

Timer numerals need stable width. If a font or API supports tabular numbers, use it for timer text.

Do not use display-sized type in settings, rows, or labels.

## Components

Prefer current Material 3 components and expressive variants when they match the task.

### Component Map

Use this map before implementing a screen. If a UI element does not fit one of these roles, reconsider whether it should exist.

Today idle:

- Reserve summary: custom status surface using Material color roles.
- Start actions: two high-emphasis Material buttons, not chips.
- Optional quick-start later: split button for `Start Focus` plus recent tasks.
- Daily trackers: toggleable pill rows or cards, with checkbox/icon state.
- Rule trackers: linear progress indicator inside a compact tracker row.

Active focus:

- Main timer: circular determinate progress only when there is a meaningful duration target; otherwise use a large timer surface without fake progress.
- Actions: button group for `Pause`/`Resume` and `End` if Compose supports the expressive variant; otherwise use consistent Material buttons with fixed sizes.
- End result: modal bottom sheet or dialog with choice controls. Prefer bottom sheet when choices plus note need room.

Active leisure:

- Remaining time: dominant timer surface.
- Cost progress: determinate linear or circular progress tied to real reserve burn.
- Sleep protection: noninteractive badge.
- End action: single primary button.

Balance:

- Ledger: list rows, not cards by default.
- Summary: calm filled surface.
- Deltas: leading numeric text with clear sign and semantic color only when it helps scanning.

History:

- View switch: connected button group when available; otherwise filter chips are acceptable only as a temporary implementation.
- Rows: list items or filled cards only when row actions need a contained surface.
- Edit flow: dialog for small edits; full-screen dialog or bottom sheet if edit fields grow.

Settings:

- Groups: plain filled cards or sectioned lists.
- Destructive reset: full-screen confirmation or a dedicated route, not a tiny alert.

### Button Groups

- Use for mutually related actions and selections.
- Prefer connected button groups over old segmented controls when available in Compose.
- Good uses: Input/Output, History tabs, result choices.
- Keep sizes and color style consistent inside a group.
- Use shape change for selected or pressed states, not as static decoration.
- Do not use button groups when nothing is selectable.

FocusWell usage:

- `Input` / `Output` in Start Focus.
- `Focus` / `Leisure` / `Trackers` / `Tags` in History.
- `As planned` / `Partial` / `Drifted` / `Interrupted` when ending focus.

### Split Button

- Use only when one primary action has adjacent related alternatives.
- Possible future use: Start Focus with a quick-start menu.
- Keep the leading label short.
- Keep the trailing affordance as an expand/collapse menu control.
- Align the menu to the trailing button when possible.

FocusWell usage:

- Future: `Start Focus` with recent tasks or templates in the trailing menu.
- Not for `Start Leisure`, because leisure must remain one-tap.

### Toolbars

- Use for groups of frequent actions.
- Do not show a toolbar at the same time as bottom navigation on phone.
- Possible future use: editing mode inside History, not the default Today screen.
- Use standard toolbar color when the content should remain primary.
- Use vibrant toolbar color only for temporary modes such as bulk edit.
- Keep navigation and local controls visually distinct.

FocusWell usage:

- Future: History edit mode or multi-select archive/delete.
- Not for Today while bottom navigation is visible.

### FAB

Use a FAB only for one primary constructive action on a screen.

FocusWell should not use a FAB on Today because the primary actions need labels and are central to the layout. A FAB can be considered on History or Settings only if there is a single creation action and no competing bottom navigation conflict.

### Progress Indicators

- Use for real progress, not decoration.
- Circular progress fits active timer state.
- Linear progress fits rule trackers.
- Wavy or high-expression progress belongs only where it improves recognition of an active process.
- Keep one visual model per process. Do not mix circular and linear for the same meaning.
- Use determinate progress only when the denominator is known.
- Do not show a progress ring around an idle zero timer.

FocusWell usage:

- Active leisure: reserve consumption has a known denominator, so determinate progress is valid.
- Rule trackers: `Math 1h 40m / 3h` uses linear progress.
- Active focus: elapsed time has no fixed endpoint unless the app adds a planned duration, so avoid fake percentage.

### Chips

- Use for filters, tags, and small status.
- Do not use clickable chips as static labels.
- Do not use a single standalone chip.
- Use filter chips for History filters and tag selection.
- Use input chips only for user-entered tags when editing.
- Use suggestion chips only for generated suggestions.

FocusWell usage:

- Start Focus tag selection.
- History filtering.
- Optional suggested result choices only if they are a set; the final save action remains a button.

### Cards, Lists, And Surfaces

Cards group one topic with optional actions. Lists scan better for repeated records.

FocusWell usage:

- Today active state can use expressive surfaces because it is the current task.
- Balance ledger should use lists, not a stack of oversized cards.
- History records can use compact filled cards if edit/delete actions need a contained row.
- Settings groups can use filled cards, but content inside them should stay plain.

Do not force every section into a card. If spacing and a heading create clearer hierarchy, use no card.

### Bottom Sheets

Use modal bottom sheets for mobile task flows that need more room than a menu but less commitment than a full screen.

FocusWell usage:

- Start Focus.
- End Focus result capture if result options plus note feel cramped in a dialog.
- Import JSON may remain a dialog unless the text field becomes hard to use.

Bottom sheets must not become a place to hide the main action. The main start path should remain visible on Today.

### Dialogs

- Use for short confirmation and interruption.
- Clear all data needs a dedicated confirmation screen or a stronger full-width flow, not a small alert.
- Dialog headlines must state the decision clearly.
- Confirmation buttons go closest to the edge.
- Maximum two actions.

FocusWell usage:

- Delete one record.
- Confirm edit balance delta.
- Import failure acknowledgement.

Not acceptable:

- Clear all data as a normal two-button alert.
- Low balance reminders as dialogs.

### Navigation

Compact width:

- Bottom navigation for `Today`, `Balance`, `History`, `Settings`.
- Do not add a bottom toolbar on top of bottom navigation.

Medium and expanded width:

- Navigation rail.
- Today may gain a supporting pane for Balance or trackers.

Navigation icons must reinforce the label:

- Today: timer or today icon.
- Balance: account balance, wallet, or savings-style icon.
- History: history or list icon.
- Settings: settings icon.

## Accessibility And Interaction

Every primary action needs a clear accessible label.

Touch targets should be at least 48dp. Main actions should be larger.

Color must not be the only state indicator. Use text, icon, shape, or position as well.

Low reserve and depleted states should be calm. Avoid alarm language, red surfaces, and shame-oriented copy.

## Current UI Corrections

The previous UI pass was too decorative. Specific corrections before more visual polish:

- Rename `Reserve` to `Balance`.
- Rename `Records` to `History`.
- Reduce idle zero-timer dominance on Today.
- Make active timer scale conditional on an actual active session.
- Replace decorative static chips with noninteractive badges.
- Make Balance rows amount-first and ledger-like.
- Keep History as the edit surface, separate from Balance.
- Move expressive shape and motion into state transitions, trackers, and active modes.
- Keep Settings plain and predictable.

## Non-Goals

Do not build:

- A streak app.
- A punishment UI.
- A casino-like reward screen.
- A decorative timer poster.
- A ledger hidden behind gamified visuals.

FocusWell can feel lively. It must still feel accountable.
