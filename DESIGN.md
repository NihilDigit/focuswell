# FocusWell Design

FocusWell uses Material 3 Expressive as a usability system. Expressive treatment is allowed when it makes state easier to read or an action easier to choose. Accounting, settings, and destructive flows stay calm.

Product behavior is defined in [docs/focuswell-product-spec.md](docs/focuswell-product-spec.md). This file defines reusable UI rules.

When a UI surface grows, split by user-facing role before adding more branches
to the same file. Entry screens own layout composition and navigation state;
row rendering, sheets, dialogs, drawing, pure UI state reducers, and formatting
helpers belong in dedicated files with focused tests where behavior can drift.

## Material 3 Sources

The current design pass used Kimi WebBridge to read the official Material 3 pages in this order:

- Foundations: accessibility, content design, customization, design tokens, interaction states, and layout.
- Styles: color, elevation, icons, motion, shape, and typography.
- Components: app bars, navigation bar, navigation rail, navigation drawer, buttons, button groups, FABs, icon buttons, split buttons, toolbars, cards, lists, chips, text fields, switches, dialogs, bottom sheets, menus, progress indicators, loading indicators, snackbars, tabs, and search.

The Plan/Settings audit was checked again against the current Material 3 pages for lists, icon buttons, navigation bar, bottom sheets, layout, and icons.

Durable implementation references:

- `https://m3.material.io/foundations/accessible-design/overview`
- `https://m3.material.io/foundations/customization`
- `https://m3.material.io/foundations/design-tokens/overview`
- `https://m3.material.io/foundations/interaction-states`
- `https://m3.material.io/foundations/layout/understanding-layout/overview`
- `https://m3.material.io/styles/color/overview`
- `https://m3.material.io/styles/elevation/overview`
- `https://m3.material.io/styles/icons/overview`
- `https://m3.material.io/styles/motion/overview`
- `https://m3.material.io/styles/shape/overview`
- `https://m3.material.io/styles/typography/overview`
- `https://m3.material.io/components`
- `https://m3.material.io/components/lists/overview`
- `https://m3.material.io/components/icon-buttons/overview`
- `https://m3.material.io/components/navigation-bar/overview`
- `https://m3.material.io/components/bottom-sheets/overview`

## Product Register

FocusWell is a daily-use Android tool. It can feel warm and tactile, but it must not feel like a reward game, streak app, or decorative timer poster.

Product register: design serves the task. Earned familiarity is more important than novelty. Use expressive styling only where it improves state recognition, choice clarity, or a transition between active modes.

Physical scene: the app is checked quickly on a phone at a desk, on a couch, or late at night when attention is limited. The UI should read in low energy moments without becoming dim, punitive, or celebratory.

The browser extension is used inside Chrome or Edge as a brief control surface, often with touch or a small popup. It should expose the current whitelist state without inviting rule editing during a focus moment.

The first viewport should answer:

1. Can I spend leisure time now?
2. What is currently running?
3. What is the next safe action?

## Screen Roles

Today is expressive. It owns the reserve well motif, active timers, large actions, tracker completion, and depleted state.

Balance is auditable. Rows are amount-first and compact.

Plan is configurable. It owns user-defined earning inputs: tags, daily trackers, tracker rewards, and rule tracker targets.

Ideas is an organizing surface. It holds captured thoughts separately from focus settlement so the user can sort them later without interrupting the current session.

History is not a primary screen. Historical records are part of Balance.

Settings is plain. It should be predictable, dense enough to scan, and cautious around destructive actions.

The browser extension popup is narrower than the Android app and has only two surfaces: Home and Settings. Home owns today's whitelist timer, one round enable/disable control, a rule toggle grid rendered from the current JSON rules, and a single lightweight not-allowed count. Settings owns detailed stats, recent behavior, and JSON rule editing.

## Color And Elevation

Use Material color roles, not ad hoc colors. The theme is the source of truth; screen code should consume roles and component defaults rather than hardcoded ARGB values.

- Primary: focus, current navigation, and primary actions.
- Secondary: trackers and supporting selected controls.
- Tertiary: leisure and sleep-protection context.
- Error: destructive actions only.
- `surface`: app background and large reading areas.
- `surfaceContainer`: ordinary grouped content and rows.
- `surfaceContainerHigh`: active or raised content.
- `primaryContainer`: reserve status.
- `tertiaryContainer`: leisure status.

Color strategy is restrained with purposeful committed moments. Today can use larger areas of primary or tertiary container color because it is the state surface. Balance, History, and Settings stay mostly neutral.

Dynamic color is enabled where the platform supplies it. The static fallback theme must remain complete and accessible because older devices and some user settings will not provide dynamic color.

Support contrast as a user need. Keep text and icon colors on their paired roles. Do not lower opacity to create hierarchy when it harms contrast.

Surface color is not elevation. Most resting surfaces should sit at level 0 or 1 through tonal separation. Reserve level 2 for bars or content that scrolls behind another surface. Reserve level 3 for modal sheets and dialogs. Levels 4 and 5 belong to hover, drag, or other directly interacted states.

Use visible shadows only when overlap would otherwise be unclear.

## Shape

Use shape to show grouping and state, not decoration.

Material scale:

- 0dp: none.
- 4dp: extra small.
- 8dp: small.
- 12dp: medium.
- 16dp: large.
- 20dp: large increased.
- 28dp: extra large.
- 32dp: extra large increased.
- 48dp: extra extra large.
- Full: pills and circles.

FocusWell shape roles:

- Expressive: Today reserve, active timers, primary action pairs, tracker completion.
- Calm: Balance, History, Settings, ledger rows.
- Full/circle: badges, icon marks, navigation indicators.

Prefer Material shapes for component containers. Use asymmetry only for a small number of FocusWell-specific state surfaces: the reserve well, active timer, and paired start controls. Do not introduce abstract decorative shapes into ledger, settings, or forms.

Avoid large or unusual shapes on dense information. When nesting rounded containers, keep inner roundness visually smaller than the outer radius.

## Motion

Motion should show state. It should not perform for its own sake.

Follow the Material physics model where public APIs allow it:

- Spatial motion: position, size, rounded corners, layout, progress.
- Effects motion: color and opacity.

Spatial motion may use spring overshoot for key interactions. Effects motion must not overshoot.

Mapping:

- Start focus/leisure: default spatial plus fast effects.
- Pause, resume, and end controls: fast spatial plus fast effects.
- Tracker completion: fast spatial for shape/progress, fast effects for color.
- Destination switching: use quiet fade-through motion. Primary destinations are same-level places, not a forward/back stack, so avoid directional page slides.
- Balance, History, Settings: quieter standard motion.
- Depleted: slower spatial motion.

Most transitions should feel immediate. Use expressive springs for active-mode surfaces and small control morphs; use standard, low-bounce motion for navigation, settings, history, and accounting views.

Avoid continuous decorative motion. Ongoing motion is reserved for active state, active reserve burn, or a real environmental change.

Current Compose Material3 exposes `MotionScheme` internally in the resolved library version, so custom elements use local spring specs that follow the same spatial/effects separation. Replace them with public Material motion tokens when they become available.

## Typography

Use Material roles by meaning:

- Display: short, important numerals only.
- Headline: current state and top-level screen titles.
- Title: sections and row titles.
- Body: explanations and secondary text.
- Label: buttons, chips, badges, and navigation.

The 2025 expressive type scale adds emphasized styles. In FocusWell, emphasis belongs to the reserve amount, the active mode title, and the next safe action. It does not belong to settings labels, row metadata, or destructive confirmation text.

FocusWell uses Google Sans Flex as the app typeface. Body, label, title, and most headline roles use the normal width family for compact reading. Display roles and the largest headline role use a rounded, 72pt, semi-expanded ExtraBold instance for reserve and timer hero moments. Do not use that expressive instance inside rows, settings, labels, or dense controls.

Do not use display type in settings, list rows, or labels. Prefer tabular figures for timers, ledger amounts, counts, and changing numeric values. Use monospace only as a fallback.

Large text should stay near a 1.2 line-height ratio. Body and label text should stay near 1.5. Text contrast must meet the Material thresholds: 3:1 for large text, 4.5:1 for small text.

## Icons

Target Material Symbols rounded style. Compose Material Icons are acceptable as a temporary fallback, but keep icon choices centralized enough to replace later.

Rules:

- Standard icons are 24dp with 48dp touch targets.
- 20dp icons with 40dp targets are only for dense mouse/keyboard layouts, not phone UI.
- Match icon size and weight with adjacent text.
- Selected navigation should have a visible selected state beyond color where possible.
- Navigation items must keep labels.

The app icon uses the same simplified reserve well motif as Today: a few rounded well-wall strokes and one waterline. Debug, release, adaptive, round, and legacy launcher fallbacks must resolve to the same FocusWell icon artwork. It should not become a bucket, tank, cup, Android template, or abstract mark unrelated to the in-app motif.

## Components

Use Material components directly when they fit.

Global shell:

- Use short navigation bar on compact phone widths.
- Use navigation rail on wider widths when the implementation can adapt safely.
- Primary destinations are Today, Balance, Ideas, Plan, and Settings.
- Do not add a navigation drawer for new UI. Material 3 Expressive recommends expanded navigation rail instead.
- Do not add a bottom app bar for new UI. Use a docked toolbar when bottom actions are needed.
- Use app bars to describe the current page and hold at most one or two essential actions.
- Use toolbars for frequent page actions.

Today:

- Reserve summary: custom status surface with the well motif.
- Start actions: two large labeled buttons.
- Start Focus sheet may show a single horizontal row of up to five recent task chips. Chips fill the task field only; they do not start the session.
- Focus active state: a primary-container task surface with elapsed time, session type/tag, and the current projected earning if ended now.
- During an active focus session, primary navigation is hidden. Idea capture remains one-way into Inbox; reviewing, sorting, settings, and planning wait until focus ends.
- Focus active state may include a quiet idea-capture action. Captured ideas go to Ideas Inbox and do not appear in focus settlement.
- Leisure active state: a tertiary/secondary reserve surface with remaining time, sleep-protection state, low-balance messages, and determinate reserve progress.
- Focus and leisure must not share the same visual structure. Focus answers "what am I doing and earning"; leisure answers "how much can I safely keep spending."
- Leisure ending is a guarded action. Use a visible hold-to-confirm control with pressed state, fill progress, haptic feedback, and tap feedback. Do not rely on Toast alone.
- Manual phone-use settlement is a small Today action below the primary Focus/Leisure pair only when the current settlement window has billable phone-use content. It opens the same Correction card review used by morning check-in, not a new destination.
- Morning check-in is a blocking accounting gate, not a dismissible modal. Use a full-screen three-step flow: reward-only Income, phone-use Correction, then Settlement. Income includes automatic reward items such as Daily Grant, settled trackers, and wake bonus. Income and normal Settlement use animated checklist rows and `+Xm`/`-Xm` amounts rather than the reserve well. Correction reviews one phone-use segment per card; swiping right marks Fair Use and swiping left counts the segment.
- Correction cards use a low-emphasis filled-card container. The main recall aid is a pure vertical-bar timeline plus a compact app list. Do not show app icons in this surface; map packages by hash to a categorical base palette, harmonize those colors toward the FocusWell seed, then normalize HCT tone/chroma for the current theme. Slice marks are hard-edged variable-width vertical bars with no capsule track; empty space represents time without counted screen use. Visually merge nearby slices from the same package to reduce noise. Use a small color dot for the app list and group non-top packages into Others.
- Morning check-in segment order follows local clock recall, not the internal business-day traversal: late night `00:00-04:00` first, then morning, afternoon, and evening.
- Frozen Daily Grant should be calm and literal: an iced or snowflake treatment on a configured daily grant times three component, such as `60m x3` with the default grant. It freezes future unconditional grants only; do not use error color or failure language.
- Daily trackers: compact determinate progress plus scannable list rows. The summary progress represents completed count only, independent of which tracker was completed. Individual tracker rows own the specific completion state through circular checkbox/timer leading controls; rule trackers show progress as a readable ring around the timer icon, not as a second row bar. Reward minutes are neutral while open and become green `+Xm` when completed. Completion has accounting meaning because each tracker has a configurable minute reward settled at the configured day boundary.
- Indeterminate short waits use the new loading indicator when available; otherwise keep Material progress small and contextual.

Balance:

- Today owns the available leisure reserve. The Leisure well's top-end chip shows today's net movement so the account state stays visible without duplicating Balance.
- Balance starts with a 7-day net chart and then shows ledger-backed records.
- The 7-day net chart uses ledger entries as the source of truth, includes coordinate marks, and scales its y-axis from the largest absolute daily movement in the visible 7-day window.
- Records use compact icon-only filter chips, not tabs. Keep `All`, `Focus`, `Leisure`, and `Adjust` on one row on compact phone widths with content descriptions for accessibility.
- Record rows are amount-first and compact. Focus earning uses primary, leisure spending uses tertiary, and each record type gets a quiet inline type icon. Destructive actions use error only inside details/edit surfaces.
- Manual balance records are added from a FAB and saved as ledger adjustments.
- Focus outcome states use the same icon and color mapping everywhere they appear.
- Focus and leisure CRUD lives in Balance. Delete actions should not be permanently visible in the list; show them in an edit/details sheet.
- Non-zero minute amounts below one minute display as `<1m`, `+<1m`, or `-<1m`; avoid rounding live accounting UI down to `0m`.
- Use lists or filled surfaces, not decorative cards, when rows are repeated.

Plan:

- Plan is the management surface for earning structure. Keep it calmer than Today but more operational than Settings.
- Tags and Daily trackers use scannable rows. Details and edits live in bottom sheets, not inline form rows.
- Tracker rewards are not high-frequency controls. Show them in row metadata and edit them in the tracker sheet.
- Add actions live in section headers. Avoid permanently visible creation forms.
- Section add actions use icon buttons with clear content descriptions; avoid making them look like primary task buttons.

Ideas:

- Ideas uses grouped list sections for Inbox, Do now, Schedule, Contain, and Explore.
- Idea rows are quiet filled surfaces with compact colored quadrant labels on the left. The label is vertically centered against the row content. Rows without checklist items do not reserve checklist space. Tapping the row edits the idea; the trailing archive icon is the explicit removal action.
- Idea filter chips are standalone controls without an enclosing background container. Filter changes should animate item placement rather than hard-cutting the list.
- Retagging stays drag-to-chip from the list surface. The bottom drag guidance and the final drop action must read from the same resolved target.
- Idea edit sheets own the idea text and small checklist items. Quadrant changes stay on the organizing surface, not inside the edit sheet.
- Explore means low-pressure curiosity, not deletion. Archive is the explicit removal action.

History:

- History is not a primary destination. Historical records are part of Balance.

Settings:

- Groups are plain filled surfaces.
- Keep Settings limited to appearance, rules, and data management. Tags and trackers belong in Plan.
- Update checks belong in Settings as a quiet maintenance row. Show current/latest version, explicit check/download/install actions, and keep the GitHub release page as a fallback.
- After a verified GitHub Release APK download, the app should immediately hand
  the APK to the Android package installer. The release page remains a fallback,
  not the primary completion path.
- Rules are compact stepper rows, not long forms. Daily grant, day boundary, wake time, sleep-protection start, and sleep rate are adjustable from Settings.
- Charge-free app selection belongs in Settings Rules. The entry row shows the selected count and opens a plain full-screen list of installed user apps with app icon, title, package name, and checkbox state. Selected apps sort above unselected apps and may use subtle lift/reorder motion to keep selection state obvious.
- Reminder preferences live with rules as compact switch rows. The Push row sits beside Long reminders; off may mean the user disabled remote reminder delivery, FCM registration is missing, or notification permission is missing. Turning it on should request permission and refresh registration. Long reminders should explain the 1h, 3h, and 5h checkpoints without implying any accounting effect.
- Clear all data uses a dedicated confirmation screen, not a small dialog.
- Destructive reset must offer export first and require a typed phrase before the action enables.

Browser extension:

- The popup uses the Android app icon as the extension icon so the companion surface is recognizable.
- Home keeps regex text hidden. Rules appear as large grid toggles generated from the saved JSON rule list.
- The enable/disable control is circular and grouped with today's timer in one status card.
- The blocked page uses restrained copy: "回到专注范围" and a larger status line. It should not lead the user toward Settings or rule editing.
- Settings may expose the raw JSON editor because regex editing is an infrequent maintenance action.

Component-specific rules:

- Buttons: labels are concise sentence case. Filled buttons are primary actions; tonal buttons are secondary actions; outlined/text buttons are low emphasis.
- FABs: use only for the single most important screen action. Do not use small FABs or surface-color FABs.
- Cards: use elevated, filled, or outlined variants only when a single subject needs containment. Avoid repeated decorative card grids.
- Lists: keep item structure consistent. Use highlighted selection states where records can be selected or edited.
- Search: use contained search when search becomes a primary screen entry point.
- Dialogs: reserve for important prompts and high-risk confirmations. One dialog completes one task.
- Bottom sheets: secondary content on compact and medium screens. Main content does not live in a sheet.
- Snackbars: short process feedback only. They should not interrupt or shame the user.

## Accessibility

Touch targets should be at least 48dp on phone UI. Main actions should be larger.

Color must not be the only state indicator. Use text, icon, shape, or position as well.

Low reserve and depleted states should stay calm. Avoid shame copy, alarm language, and red warning surfaces unless an action is destructive.
