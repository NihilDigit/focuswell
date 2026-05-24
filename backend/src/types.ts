export type Device = {
  deviceId: string;
  installSecretHash: string;
  fcmToken?: string;
  createdAt: string;
  lastSeenAt: string;
};

export type ReminderKind =
  | "focus_stale_3h"
  | "focus_duration_1h"
  | "focus_duration_3h"
  | "focus_duration_5h"
  | "leisure_duration_1h"
  | "leisure_duration_3h"
  | "leisure_duration_5h"
  | "leisure_10m_left"
  | "leisure_5m_left"
  | "leisure_1m_left"
  | "leisure_depleted"
  | "late_night_rate_started";

export type ReminderPlan = {
  reminderId: string;
  deviceId: string;
  sessionId: string;
  revision: number;
  kind: ReminderKind;
  dueAtUtc: string;
  qstashMessageId?: string;
  status: "pending" | "fired" | "cancelled";
};

export type ReminderPayload = {
  deviceId: string;
  sessionId: string;
  revision: number;
  reminderId: string;
};

export type ReminderMessage = {
  title: string;
  body: string;
  tag: string;
};

export type ReminderDeliveryTelemetry = {
  reminderId: string;
  kind: ReminderKind;
  dueAtUtc: string;
  firedAtUtc: string;
};
