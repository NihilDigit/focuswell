import type { VercelRequest, VercelResponse } from "@vercel/node";
import type { ReminderKind, ReminderPayload } from "./types";

export const reminderKinds = new Set<ReminderKind>([
  "focus_stale_3h",
  "focus_duration_1h",
  "focus_duration_3h",
  "focus_duration_5h",
  "leisure_duration_1h",
  "leisure_duration_3h",
  "leisure_duration_5h",
  "leisure_10m_left",
  "leisure_5m_left",
  "leisure_1m_left",
  "leisure_depleted",
  "late_night_rate_started",
]);

export function sendJson(response: VercelResponse, status: number, body: unknown): void {
  response.status(status).json(body);
}

export function parseRegisterDevice(value: unknown): {
  deviceId: string;
  installSecretHash: string;
  fcmToken?: string;
  nowUtc: string;
} {
  const body = asRecord(value);
  const fcmToken = optionalStringField(body, "fcmToken");
  const parsed = {
    deviceId: stringField(body, "deviceId"),
    installSecretHash: stringField(body, "installSecretHash"),
    nowUtc: optionalStringField(body, "nowUtc") ?? new Date().toISOString(),
  };
  return fcmToken ? { ...parsed, fcmToken } : parsed;
}

export function parseSchedulePlan(value: unknown): {
  deviceId: string;
  installSecret: string;
  sessionId: string;
  revision: number;
  reminders: Array<{ kind: ReminderKind; dueAtUtc: string }>;
} {
  const body = asRecord(value);
  const remindersValue = body["reminders"];
  if (!Array.isArray(remindersValue)) throw new Error("invalid-reminders");
  return {
    deviceId: stringField(body, "deviceId"),
    installSecret: stringField(body, "installSecret"),
    sessionId: stringField(body, "sessionId"),
    revision: numberField(body, "revision"),
    reminders: remindersValue.map((item) => {
      const reminder = asRecord(item);
      const kind = stringField(reminder, "kind");
      if (!reminderKinds.has(kind as ReminderKind)) throw new Error("invalid-kind");
      return {
        kind: kind as ReminderKind,
        dueAtUtc: stringField(reminder, "dueAtUtc"),
      };
    }),
  };
}

export function parseCancelSession(value: unknown): {
  deviceId: string;
  installSecret: string;
  sessionId: string;
} {
  const body = asRecord(value);
  return {
    deviceId: stringField(body, "deviceId"),
    installSecret: stringField(body, "installSecret"),
    sessionId: stringField(body, "sessionId"),
  };
}

export function parseReminderPayload(value: unknown): ReminderPayload {
  const body = asRecord(value);
  return {
    deviceId: stringField(body, "deviceId"),
    sessionId: stringField(body, "sessionId"),
    revision: numberField(body, "revision"),
    reminderId: stringField(body, "reminderId"),
  };
}

export async function handleError(response: VercelResponse, error: unknown): Promise<void> {
  const message = error instanceof Error ? error.message : "unknown";
  const status = message === "unauthorized" ? 401 : message === "device-not-found" ? 404 : 400;
  sendJson(response, status, { ok: false, error: message });
}

export function requirePost(request: VercelRequest): void {
  if (request.method !== "POST") throw new Error("method-not-allowed");
}

function asRecord(value: unknown): Record<string, unknown> {
  if (value === null || typeof value !== "object" || Array.isArray(value)) {
    throw new Error("invalid-json");
  }
  return value as Record<string, unknown>;
}

function stringField(body: Record<string, unknown>, key: string): string {
  const value = body[key];
  if (typeof value !== "string" || value.length === 0) throw new Error(`invalid-${key}`);
  return value;
}

function optionalStringField(body: Record<string, unknown>, key: string): string | undefined {
  const value = body[key];
  if (value === undefined || value === null || value === "") return undefined;
  if (typeof value !== "string") throw new Error(`invalid-${key}`);
  return value;
}

function numberField(body: Record<string, unknown>, key: string): number {
  const value = body[key];
  if (typeof value !== "number" || !Number.isFinite(value)) throw new Error(`invalid-${key}`);
  return value;
}
