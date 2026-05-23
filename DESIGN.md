# FocusWell Design

FocusWell uses Material 3 Expressive as a usability system. Expressive treatment is allowed when it makes state easier to read or an action easier to choose. Accounting, settings, and destructive flows stay calm.

Product behavior is defined in [docs/focuswell-product-spec.md](docs/focuswell-product-spec.md). This file defines reusable UI rules.

## M3E References

The current design pass used Kimi WebBridge to read the official Material pages for:

- Elevation: `https://m3.material.io/styles/elevation/overview`
- Icons: `https://m3.material.io/styles/icons/overview`
- Motion: `https://m3.material.io/styles/motion/overview/how-it-works`
- Shape: `https://m3.material.io/styles/shape/overview-principles`
- Typography: `https://m3.material.io/styles/typography/overview`

Supporting pages were also read for applying elevation, applying icons, motion specs, shape scale, shape morphing, and applying type.

## Product Register

FocusWell is a daily-use Android tool. It can feel warm and tactile, but it must not feel like a reward game, streak app, or decorative timer poster.

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

Use Material color roles, not ad hoc colors:

- Primary: focus, current navigation, and primary actions.
- Secondary: trackers and supporting selected controls.
- Tertiary: leisure and sleep-protection context.
- Error: destructive actions only.
- `surfaceContainer`: ordinary grouped content and rows.
- `surfaceContainerHigh`: active or raised content.
- `primaryContainer`: reserve status.
- `tertiaryContainer`: leisure status.

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

Avoid continuous decorative motion. Ongoing motion is reserved for active state, active reserve burn, or a real environmental change.

Current Compose Material3 exposes `MotionScheme` internally in the resolved library version, so custom elements use local spring specs that follow the same spatial/effects separation. Replace them with public Material motion tokens when they become available.

## Typography

Use Material roles by meaning:

- Display: short, important numerals only.
- Headline: current state and top-level screen titles.
- Title: sections and row titles.
- Body: explanations and secondary text.
- Label: buttons, chips, badges, and navigation.

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

Today:

- Reserve summary: custom status surface with the well motif.
- Start actions: two large labeled buttons.
- Active timers: dominant surfaces only while a session is running.
- Daily trackers: compact rail plus scannable tracker tiles.

Balance:

- Ledger rows are amount-first.
- Summary is calm and compact.
- Deltas use sign and semantic color.

History:

- Filter/group controls switch record categories.
- Rows may be filled surfaces because edit/delete actions need containment.
- Edit flows use sheets or dialogs depending on field count.

Settings:

- Groups are plain filled surfaces.
- Creation forms stay inline when short.
- Clear all data uses a dedicated confirmation screen, not a small dialog.
- Destructive reset must offer export first and require a typed phrase before the action enables.

## Accessibility

Touch targets should be at least 48dp on phone UI. Main actions should be larger.

Color must not be the only state indicator. Use text, icon, shape, or position as well.

Low reserve and depleted states should stay calm. Avoid shame copy, alarm language, and red warning surfaces unless an action is destructive.
