import type { ReminderKind, ReminderMessage } from "./types";

export function messageFor(kind: ReminderKind): ReminderMessage {
  switch (kind) {
    case "focus_stale_3h":
      return {
        title: "Still focusing?",
        body: "This session has been running for a while. Open FocusWell when you are ready to review it.",
        tag: "focuswell-focus-stale",
      };
    case "leisure_10m_left":
      return {
        title: "10 min left",
        body: "Your leisure reserve is running low.",
        tag: "focuswell-leisure",
      };
    case "leisure_5m_left":
      return {
        title: "5 min left",
        body: "Your leisure reserve is running low.",
        tag: "focuswell-leisure",
      };
    case "leisure_1m_left":
      return {
        title: "1 min left",
        body: "Your leisure reserve is almost used up.",
        tag: "focuswell-leisure",
      };
    case "leisure_depleted":
      return {
        title: "Balance used up",
        body: "Another 60 min arrives at 04:00.",
        tag: "focuswell-leisure",
      };
    case "late_night_rate_started":
      return {
        title: "Sleep protection is active",
        body: "Leisure now uses reserve at 2x to protect tomorrow.",
        tag: "focuswell-sleep-protection",
      };
  }
}
