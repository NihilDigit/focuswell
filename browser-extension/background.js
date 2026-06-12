const STORAGE_KEY = "focuswellState";
const TIMER_ALARM = "focuswellTimerTick";
const BLOCK_RULE_ID = 1;
const ALLOW_RULE_ID_START = 100;

const DEFAULT_PATTERNS = [
  {
    id: "claude",
    label: "Claude",
    pattern: "^https?://([^/]+\\.)?claude\\.ai(/|$)",
    enabled: true
  },
  {
    id: "chatgpt",
    label: "ChatGPT",
    pattern: "^https?://([^/]+\\.)?chatgpt\\.com(/|$)",
    enabled: true
  },
  {
    id: "google-search",
    label: "Google search only",
    pattern: "^https?://www\\.google\\.[^/]+/search\\?",
    enabled: true
  },
  {
    id: "wikipedia",
    label: "Wikipedia",
    pattern: "^https?://([^/]+\\.)?wikipedia\\.org(/|$)",
    enabled: true
  }
];

const DEFAULT_STATE = {
  enabled: false,
  startedAt: null,
  elapsedMs: 0,
  patterns: DEFAULT_PATTERNS,
  stats: {
    enabledMs: 0,
    today: {
      date: null,
      enabledMs: 0,
      blockedNavigations: 0,
      allowedNavigations: 0
    },
    allowedNavigations: 0,
    blockedNavigations: 0,
    byPattern: {},
    byBlockedHost: {}
  },
  lastEvents: []
};

async function loadState() {
  const result = await chrome.storage.local.get(STORAGE_KEY);
  const stored = result[STORAGE_KEY] || {};
  const patterns = Array.isArray(stored.patterns) && stored.patterns.length > 0
    ? stored.patterns
    : DEFAULT_PATTERNS;

  return {
    ...DEFAULT_STATE,
    ...stored,
    patterns,
    stats: {
      ...DEFAULT_STATE.stats,
      ...(stored.stats || {}),
      today: {
        ...DEFAULT_STATE.stats.today,
        ...((stored.stats && stored.stats.today) || {})
      },
      byPattern: {
        ...DEFAULT_STATE.stats.byPattern,
        ...((stored.stats && stored.stats.byPattern) || {})
      },
      byBlockedHost: {
        ...DEFAULT_STATE.stats.byBlockedHost,
        ...((stored.stats && stored.stats.byBlockedHost) || {})
      }
    },
    lastEvents: Array.isArray(stored.lastEvents) ? stored.lastEvents : []
  };
}

async function saveState(state) {
  await chrome.storage.local.set({ [STORAGE_KEY]: state });
}

function now() {
  return Date.now();
}

function localDateKey(timestamp) {
  const date = new Date(timestamp);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function localDayStart(timestamp) {
  const date = new Date(timestamp);
  return new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime();
}

function nextLocalDayStart(timestamp) {
  const date = new Date(timestamp);
  return new Date(date.getFullYear(), date.getMonth(), date.getDate() + 1).getTime();
}

function ensureToday(stats, timestamp = now()) {
  const date = localDateKey(timestamp);
  if (!stats.today || stats.today.date !== date) {
    stats.today = {
      date,
      enabledMs: 0,
      blockedNavigations: 0,
      allowedNavigations: 0
    };
  }
}

function addEnabledDuration(stats, start, end) {
  stats.enabledMs = (stats.enabledMs || 0) + Math.max(0, end - start);

  let cursor = start;
  while (cursor < end) {
    ensureToday(stats, cursor);
    const boundary = Math.min(end, nextLocalDayStart(cursor));
    stats.today.enabledMs = (stats.today.enabledMs || 0) + Math.max(0, boundary - cursor);
    cursor = boundary;
  }
  ensureToday(stats, end);
}

function activeElapsedMs(state) {
  if (!state.enabled || !state.startedAt) {
    return state.elapsedMs || 0;
  }
  return (state.elapsedMs || 0) + Math.max(0, now() - state.startedAt);
}

function activeTodayElapsedMs(state) {
  const current = now();
  ensureToday(state.stats, current);
  if (!state.enabled || !state.startedAt) {
    return state.stats.today.enabledMs || 0;
  }
  const start = Math.max(state.startedAt, localDayStart(current));
  return (state.stats.today.enabledMs || 0) + Math.max(0, current - start);
}

function normalizePattern(pattern) {
  return (pattern || "").trim();
}

function escapeRegex(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

function ruleForPage(url) {
  const parsed = new URL(url);
  if (parsed.protocol !== "http:" && parsed.protocol !== "https:") {
    throw new Error("Only http and https pages can be added");
  }
  const pathname = parsed.pathname || "/";
  const label = pathname === "/" ? parsed.hostname : `${parsed.hostname}${pathname}`;
  return {
    id: `page-${parsed.hostname.replace(/[^a-z0-9]+/gi, "-").toLowerCase()}-${now()}`,
    label,
    pattern: `^${escapeRegex(parsed.origin)}${escapeRegex(pathname)}([?#].*)?$`,
    enabled: true
  };
}

function compilePatterns(patterns) {
  return patterns
    .filter((item) => item.enabled !== false)
    .map((item) => {
      try {
        return { item, regex: new RegExp(normalizePattern(item.pattern), "i") };
      } catch {
        return null;
      }
    })
    .filter(Boolean);
}

function findMatchingPattern(url, patterns) {
  return compilePatterns(patterns).find(({ regex }) => regex.test(url));
}

function eventFor(type, url, label) {
  let title = url;
  try {
    const parsed = new URL(url);
    title = parsed.hostname;
  } catch {
    title = url;
  }

  return {
    type,
    title,
    label: label || "",
    at: now()
  };
}

function pushEvent(state, event) {
  state.lastEvents = [event, ...(state.lastEvents || [])].slice(0, 12);
}

function hostFromUrl(url) {
  try {
    return new URL(url).hostname || "unknown";
  } catch {
    return "unknown";
  }
}

async function syncTimer() {
  const state = await loadState();
  if (!state.enabled || !state.startedAt) {
    return state;
  }

  const current = now();
  const delta = Math.max(0, current - state.startedAt);
  state.elapsedMs = (state.elapsedMs || 0) + delta;
  addEnabledDuration(state.stats, state.startedAt, current);
  state.startedAt = current;
  await saveState(state);
  return state;
}

async function updateBlockingRules(state) {
  const existing = await chrome.declarativeNetRequest.getDynamicRules();
  const removeRuleIds = existing.map((rule) => rule.id);
  const addRules = [];

  if (state.enabled) {
    const activePatterns = state.patterns.filter((item) => item.enabled !== false);
    activePatterns.forEach((item, index) => {
      addRules.push({
        id: ALLOW_RULE_ID_START + index,
        priority: 2,
        action: { type: "allow" },
        condition: {
          regexFilter: normalizePattern(item.pattern),
          resourceTypes: ["main_frame"]
        }
      });
    });

    addRules.push({
      id: BLOCK_RULE_ID,
      priority: 1,
      action: {
        type: "redirect",
        redirect: { extensionPath: "/blocked.html" }
      },
      condition: {
        urlFilter: "|http",
        resourceTypes: ["main_frame"]
      }
    });
  }

  await chrome.declarativeNetRequest.updateDynamicRules({
    removeRuleIds,
    addRules
  });
}

async function setEnabled(enabled) {
  const state = await syncTimer();
  if (enabled && !state.enabled) {
    state.enabled = true;
    state.startedAt = now();
    pushEvent(state, eventFor("enabled", "FocusWell gate", "Started"));
  } else if (!enabled && state.enabled) {
    state.enabled = false;
    state.startedAt = null;
    pushEvent(state, eventFor("disabled", "FocusWell gate", "Stopped"));
  }
  await saveState(state);
  await updateBlockingRules(state);
  await chrome.alarms.clear(TIMER_ALARM);
  if (state.enabled) {
    await chrome.alarms.create(TIMER_ALARM, { periodInMinutes: 1 });
  }
  return state;
}

async function updatePatterns(patterns) {
  const state = await loadState();
  state.patterns = patterns.map((item, index) => ({
    id: item.id || `custom-${index}-${now()}`,
    label: (item.label || "Rule").trim(),
    pattern: normalizePattern(item.pattern),
    enabled: item.enabled !== false
  })).filter((item) => item.pattern.length > 0);
  await saveState(state);
  await updateBlockingRules(state);
  return state;
}

async function addCurrentTabRule() {
  const tabs = await chrome.tabs.query({ active: true, currentWindow: true });
  const tab = tabs[0];
  if (!tab || !tab.url) {
    throw new Error("No current page found");
  }
  const rule = ruleForPage(tab.url);
  const state = await loadState();
  const existing = state.patterns.find((item) => normalizePattern(item.pattern) === rule.pattern);
  if (existing) {
    existing.enabled = true;
    pushEvent(state, eventFor("allowed", tab.url, existing.label || existing.id));
  } else {
    state.patterns = [...state.patterns, rule];
    pushEvent(state, eventFor("allowed", tab.url, rule.label));
  }
  await saveState(state);
  await updateBlockingRules(state);
  return state;
}

async function resetStats() {
  const state = await syncTimer();
  state.elapsedMs = state.enabled ? 0 : 0;
  state.startedAt = state.enabled ? now() : null;
  state.stats = { ...DEFAULT_STATE.stats, byPattern: {}, byBlockedHost: {} };
  ensureToday(state.stats);
  state.lastEvents = [];
  await saveState(state);
  return state;
}

async function getPublicState() {
  const state = await loadState();
  ensureToday(state.stats);
  return {
    ...state,
    totalElapsedMs: state.elapsedMs || 0,
    todayElapsedMs: state.stats.today.enabledMs || 0
  };
}

async function recordAllowedNavigation(url) {
  const state = await loadState();
  if (!state.enabled) {
    return;
  }
  const match = findMatchingPattern(url, state.patterns);
  if (!match) {
    return;
  }

  const key = match.item.label || match.item.id;
  ensureToday(state.stats);
  state.stats.allowedNavigations += 1;
  state.stats.today.allowedNavigations += 1;
  state.stats.byPattern[key] = (state.stats.byPattern[key] || 0) + 1;
  pushEvent(state, eventFor("allowed", url, key));
  await saveState(state);
}

async function recordBlockedNavigation(url) {
  const state = await loadState();
  if (!state.enabled) {
    return;
  }

  const host = hostFromUrl(url);
  ensureToday(state.stats);
  state.stats.blockedNavigations += 1;
  state.stats.today.blockedNavigations += 1;
  state.stats.byBlockedHost[host] = (state.stats.byBlockedHost[host] || 0) + 1;
  pushEvent(state, eventFor("blocked", url, "Blocked"));
  await saveState(state);
}

chrome.runtime.onInstalled.addListener(async () => {
  const state = await loadState();
  await saveState(state);
  await updateBlockingRules(state);
});

chrome.runtime.onStartup.addListener(async () => {
  const state = await loadState();
  await updateBlockingRules(state);
  if (state.enabled) {
    await chrome.alarms.create(TIMER_ALARM, { periodInMinutes: 1 });
  }
});

chrome.alarms.onAlarm.addListener((alarm) => {
  if (alarm.name === TIMER_ALARM) {
    syncTimer();
  }
});

chrome.webNavigation.onCommitted.addListener((details) => {
  if (details.frameId !== 0 || !details.url.startsWith("http")) {
    return;
  }
  recordAllowedNavigation(details.url);
});

chrome.runtime.onMessage.addListener((message, _sender, sendResponse) => {
  const respond = async () => {
    if (message.type === "get-state") {
      return getPublicState();
    }
    if (message.type === "set-enabled") {
      await setEnabled(Boolean(message.enabled));
      return getPublicState();
    }
    if (message.type === "update-patterns") {
      await updatePatterns(message.patterns || []);
      return getPublicState();
    }
    if (message.type === "add-current-tab-rule") {
      await addCurrentTabRule();
      return getPublicState();
    }
    if (message.type === "reset-stats") {
      await resetStats();
      return getPublicState();
    }
    if (message.type === "record-blocked") {
      await recordBlockedNavigation(message.url || "unknown");
      return getPublicState();
    }
    return getPublicState();
  };

  respond().then(sendResponse);
  return true;
});
