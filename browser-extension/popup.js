const homeView = document.querySelector("#homeView");
const settingsView = document.querySelector("#settingsView");
const timerEl = document.querySelector("#timer");
const toggleButton = document.querySelector("#toggleButton");
const toggleText = document.querySelector("#toggleText");
const settingsButton = document.querySelector("#settingsButton");
const backButton = document.querySelector("#backButton");
const quickRulesEl = document.querySelector("#quickRules");
const todayBlockedEl = document.querySelector("#todayBlocked");
const totalTimeEl = document.querySelector("#totalTime");
const allowedCount = document.querySelector("#allowedCount");
const blockedCount = document.querySelector("#blockedCount");
const eventsEl = document.querySelector("#events");
const usageEl = document.querySelector("#usage");
const rulesJson = document.querySelector("#rulesJson");
const jsonStatus = document.querySelector("#jsonStatus");
const saveJsonButton = document.querySelector("#saveJsonButton");
const resetButton = document.querySelector("#resetButton");

let state = null;
let renderTimer = null;

function send(type, payload = {}) {
  return chrome.runtime.sendMessage({ type, ...payload });
}

function formatDuration(ms) {
  const totalSeconds = Math.floor(ms / 1000);
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;
  return [hours, minutes, seconds].map((part) => String(part).padStart(2, "0")).join(":");
}

function currentTodayElapsed() {
  if (!state) {
    return 0;
  }
  if (!state.enabled || !state.startedAt) {
    return state.todayElapsedMs || 0;
  }
  return (state.todayElapsedMs || 0) + Math.max(0, Date.now() - state.startedAt);
}

function currentTotalElapsed() {
  if (!state) {
    return 0;
  }
  if (!state.enabled || !state.startedAt) {
    return state.totalElapsedMs || 0;
  }
  return (state.totalElapsedMs || 0) + Math.max(0, Date.now() - state.startedAt);
}

function showView(name) {
  const settings = name === "settings";
  homeView.classList.toggle("active", !settings);
  settingsView.classList.toggle("active", settings);
}

function eventLabel(type) {
  if (type === "allowed") return "白名单";
  if (type === "blocked") return "拦截";
  if (type === "enabled") return "开启";
  if (type === "disabled") return "关闭";
  return "记录";
}

function renderQuickRules() {
  quickRulesEl.textContent = "";
  state.patterns.forEach((rule, index) => {
    const tile = document.createElement("button");
    tile.className = "rule-tile";
    tile.type = "button";
    tile.setAttribute("aria-pressed", rule.enabled !== false ? "true" : "false");

    const name = document.createElement("span");
    name.className = "rule-name";
    name.textContent = rule.label || rule.id || `Rule ${index + 1}`;

    const status = document.createElement("span");
    status.className = "rule-status";
    status.textContent = rule.enabled !== false ? "启用" : "停用";

    tile.addEventListener("click", async () => {
      const next = state.patterns.map((item, itemIndex) => (
        itemIndex === index ? { ...item, enabled: item.enabled === false } : item
      ));
      state = await send("update-patterns", { patterns: next });
      render();
    });

    tile.append(name, status);
    quickRulesEl.append(tile);
  });
}

function renderUsage() {
  usageEl.textContent = "";
  const byPattern = state.stats.byPattern || {};
  const rows = state.patterns.map((rule) => {
    const key = rule.label || rule.id;
    return [key, byPattern[key] || 0];
  });

  if (rows.every(([, count]) => count === 0)) {
    const empty = document.createElement("div");
    empty.className = "empty";
    empty.textContent = "还没有白名单命中";
    usageEl.append(empty);
    return;
  }

  rows.forEach(([label, count]) => {
    const row = document.createElement("div");
    row.className = "usage-row";
    const name = document.createElement("span");
    name.textContent = label;
    const value = document.createElement("span");
    value.textContent = `${count} 次`;
    row.append(name, value);
    usageEl.append(row);
  });
}

function renderEvents() {
  eventsEl.textContent = "";
  const events = state.lastEvents || [];
  if (events.length === 0) {
    const empty = document.createElement("div");
    empty.className = "empty";
    empty.textContent = "暂无记录";
    eventsEl.append(empty);
    return;
  }

  events.forEach((event) => {
    const row = document.createElement("div");
    row.className = "event";
    const type = document.createElement("span");
    type.className = "event-type";
    type.textContent = eventLabel(event.type);
    const title = document.createElement("span");
    title.className = "event-title";
    title.textContent = event.label ? `${event.label}: ${event.title}` : event.title;
    row.append(type, title);
    eventsEl.append(row);
  });
}

function renderJson() {
  rulesJson.value = JSON.stringify(state.patterns, null, 2);
  jsonStatus.textContent = "";
  jsonStatus.classList.remove("error");
}

function render() {
  toggleButton.setAttribute("aria-pressed", state.enabled ? "true" : "false");
  toggleText.textContent = state.enabled ? "停止" : "开启";
  timerEl.textContent = formatDuration(currentTodayElapsed());
  totalTimeEl.textContent = formatDuration(currentTotalElapsed());
  allowedCount.textContent = state.stats.allowedNavigations || 0;
  blockedCount.textContent = state.stats.blockedNavigations || 0;
  todayBlockedEl.textContent = `今日未放行 ${(state.stats.today && state.stats.today.blockedNavigations) || 0} 次`;
  renderQuickRules();
  renderUsage();
  renderEvents();
  renderJson();
}

async function boot() {
  state = await send("get-state");
  render();
  clearInterval(renderTimer);
  renderTimer = setInterval(() => {
    timerEl.textContent = formatDuration(currentTodayElapsed());
    totalTimeEl.textContent = formatDuration(currentTotalElapsed());
  }, 1000);
}

toggleButton.addEventListener("click", async () => {
  state = await send("set-enabled", { enabled: !state.enabled });
  render();
});

settingsButton.addEventListener("click", () => {
  showView("settings");
});

backButton.addEventListener("click", () => {
  showView("home");
});

saveJsonButton.addEventListener("click", async () => {
  try {
    const parsed = JSON.parse(rulesJson.value);
    if (!Array.isArray(parsed)) {
      throw new Error("JSON 顶层必须是数组。");
    }
    parsed.forEach((rule, index) => {
      if (!rule || typeof rule !== "object") {
        throw new Error(`第 ${index + 1} 项不是对象。`);
      }
      if (typeof rule.pattern !== "string" || rule.pattern.trim().length === 0) {
        throw new Error(`第 ${index + 1} 项缺少 pattern。`);
      }
      new RegExp(rule.pattern);
    });
    state = await send("update-patterns", { patterns: parsed });
    render();
    showView("settings");
    jsonStatus.textContent = "已保存";
    jsonStatus.classList.remove("error");
  } catch (error) {
    jsonStatus.textContent = error.message;
    jsonStatus.classList.add("error");
  }
});

resetButton.addEventListener("click", async () => {
  state = await send("reset-stats");
  render();
  showView("settings");
});

boot();
