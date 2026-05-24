# FocusWell Browser Gate

FocusWell Browser Gate is an independent Manifest V3 browser extension for Chrome and Edge.

When enabled, it works as a whitelist gate: only pages matching enabled regular expressions can open. Other `http` and `https` top-level navigations are redirected to the local blocked page.

The popup keeps the focus-time controls on the first screen: today's enabled timer, one round on/off button, rule toggles rendered from the saved JSON rule list, and today's not-allowed count. Detailed stats and regex editing live in Settings.

## Default Whitelist

- Claude: `^https?://([^/]+\\.)?claude\\.ai(/|$)`
- ChatGPT: `^https?://([^/]+\\.)?chatgpt\\.com(/|$)`
- Google search only: `^https?://www\\.google\\.[^/]+/search\\?`
- Wikipedia: `^https?://([^/]+\\.)?wikipedia\\.org(/|$)`

## Local Stats

Stats are stored locally in `chrome.storage.local`.

- Enabled time: total time the whitelist gate has been on.
- Whitelist use: count of allowed top-level navigations that matched a whitelist rule.
- Blocks: count of blocked top-level navigations.
- Recent behavior: a short local event log for starts, stops, allowed navigations, and blocks.

The timer does not measure time spent on individual sites.

The main popup renders its rule grid from the current JSON rule list. Regex editing is kept in the Settings view so the touch-first home screen stays small.

## Install For Development

1. Open `chrome://extensions` or `edge://extensions`.
2. Enable developer mode.
3. Choose "Load unpacked".
4. Select this `browser-extension` directory.

## Notes

The whitelist uses `declarativeNetRequest` dynamic rules. Regular expressions are applied to main-frame navigations only, so page assets are not individually blocked after an allowed page loads.
