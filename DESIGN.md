# FocusWell Design

FocusWell uses Material 3 Expressive as a usability system. Expressive treatment is allowed when it makes state easier to read or an action easier to choose. Accounting, settings, and destructive flows stay calm.

Product behavior is defined in [docs/focuswell-product-spec.md](docs/focuswell-product-spec.md). This file defines reusable UI rules.

## Material 3 Sources

The current design pass used Kimi WebBridge to read the official Material 3 pages in this order:

- Foundations: accessibility, content design, customization, design tokens, interaction states, and layout.
- Styles: color, elevation, icons, motion, shape, and typography.
- Components: app bars, navigation bar, navigation rail, navigation drawer, buttons, button groups, FABs, icon buttons, split buttons, toolbars, cards, lists, chips, text fields, switches, dialogs, bottom sheets, menus, progress indicators, loading indicators, snackbars, tabs, and search.

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

## Product Register

FocusWell is a daily-use Android tool. It can feel warm and tactile, but it must not feel like a reward game, streak app, or decorative timer poster.

Product register: design serves the task. Earned familiarity is more important than novelty. Use expressive styling only where it improves state recognition, choice clarity, or a transition between active modes.

Physical scene: the app is checked quickly on a phone at a desk, on a couch, or late at night when attention is limited. The UI should read in low energy moments without becoming dim, punitive, or celebratory.

The first viewport should answer:

1. Can I spend leisure time now?
2. What is currently running?
3. What is the next safe action?

## Screen Roles

Today is expressive. It owns the reserve well motif, active timers, large actions, tracker completion, depleted state, and wind-down state.

Balance is auditable. Rows are amount-first and compact.

History is editable. It shows enough context to identify and correct records.

Settings is plain. It should be predictable, dense enough to scan, and cautious around destructive actions.

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
- Balance, History, Settings: quieter standard motion.
- Depleted and wind-down: slower spatial motion.

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

The app icon uses the same simplified reserve well motif as Today: a few rounded well-wall strokes and one waterline. It should not become a bucket, tank, cup, or abstract mark unrelated to the in-app motif.

## Components

Use Material components directly when they fit.

Global shell:

- Use short navigation bar on compact phone widths.
- Use navigation rail on wider widths when the implementation can adapt safely.
- Do not add a navigation drawer for new UI. Material 3 Expressive recommends expanded navigation rail instead.
- Do not add a bottom app bar for new UI. Use a docked toolbar when bottom actions are needed.
- Use app bars to describe the current page and hold at most one or two essential actions.
- Use toolbars for frequent page actions.

Today:

- Reserve summary: custom status surface with the well motif.
- Start actions: two large labeled buttons.
- Focus active state: a primary-container task surface with elapsed time, session type/tag, and the current projected earning if ended now.
- Leisure active state: a tertiary/secondary reserve surface with remaining time, sleep-protection state, low-balance messages, and determinate reserve progress.
- Focus and leisure must not share the same visual structure. Focus answers "what am I doing and earning"; leisure answers "how much can I safely keep spending."
- Leisure ending is a guarded action. Use a visible hold-to-confirm control with pressed state, fill progress, haptic feedback, and tap feedback. Do not rely on Toast alone.
- Daily trackers: compact rail plus scannable tracker tiles.
- Indeterminate short waits use the new loading indicator when available; otherwise keep Material progress small and contextual.

Balance:

- Ledger rows are amount-first.
- Summary is calm and compact.
- Deltas use sign and semantic color.
- Use lists or filled surfaces, not decorative cards, when rows are repeated.

History:

- Filter/group controls switch record categories.
- Rows may be filled surfaces because edit/delete actions need containment.
- Edit flows use sheets or dialogs depending on field count.
- Prefer connected button groups over segmented buttons when the dependency exposes the expressive component. Until then, keep selection controls visually compact and clearly selected.

Settings:

- Groups are plain filled surfaces.
- Creation forms stay inline when short.
- Clear all data uses a dedicated confirmation screen, not a small dialog.
- Destructive reset must offer export first and require a typed phrase before the action enables.

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
