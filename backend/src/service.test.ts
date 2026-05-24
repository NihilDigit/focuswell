import { expect, test } from "bun:test";
import { sha256Hex } from "./auth";
import { ReminderService } from "./service";
import { MemoryReminderStore } from "./store";
import type { FcmClient } from "./fcm";
import type { QStashClient } from "./qstash";
import type { ReminderDeliveryTelemetry, ReminderMessage, ReminderPayload } from "./types";

class FakeQStashClient implements QStashClient {
  published: ReminderPayload[] = [];
  cancelled: string[] = [];

  async publish(payload: ReminderPayload): Promise<{ messageId: string }> {
    this.published.push(payload);
    return { messageId: `qstash-${payload.reminderId}` };
  }

  async cancel(messageId: string): Promise<void> {
    this.cancelled.push(messageId);
  }

  async verify(): Promise<boolean> {
    return true;
  }
}

class FakeFcmClient implements FcmClient {
  sent: Array<{ token: string; message: ReminderMessage; telemetry: ReminderDeliveryTelemetry }> = [];

  async send(token: string, message: ReminderMessage, telemetry: ReminderDeliveryTelemetry): Promise<"sent"> {
    this.sent.push({ token, message, telemetry });
    return "sent";
  }
}

test("cancelled reminder callback is skipped and does not send FCM", async () => {
  const store = new MemoryReminderStore();
  const qstash = new FakeQStashClient();
  const fcm = new FakeFcmClient();
  const service = new ReminderService(store, qstash, fcm);

  await service.registerDevice({
    deviceId: "device-1",
    installSecretHash: await sha256Hex("secret-1"),
    fcmToken: "token-1",
    nowUtc: new Date().toISOString(),
  });
  const plans = await service.schedulePlan({
    deviceId: "device-1",
    installSecret: "secret-1",
    sessionId: "session-1",
    revision: 1,
    reminders: [{ kind: "focus_stale_3h", dueAtUtc: new Date(Date.now() + 60_000).toISOString() }],
  });
  const plan = plans[0];
  if (!plan) throw new Error("expected scheduled plan");

  await service.cancelSession({ deviceId: "device-1", installSecret: "secret-1", sessionId: "session-1" });
  const result = await service.fire({
    deviceId: "device-1",
    sessionId: "session-1",
    revision: 1,
    reminderId: plan.reminderId,
  });

  expect(result).toBe("skipped");
  expect(fcm.sent).toEqual([]);
  expect(qstash.cancelled).toEqual([`qstash-${plan.reminderId}`]);
});

test("older revision callback is skipped after rescheduling same session", async () => {
  const store = new MemoryReminderStore();
  const qstash = new FakeQStashClient();
  const fcm = new FakeFcmClient();
  const service = new ReminderService(store, qstash, fcm);

  await service.registerDevice({
    deviceId: "device-1",
    installSecretHash: await sha256Hex("secret-1"),
    fcmToken: "token-1",
    nowUtc: new Date().toISOString(),
  });
  const oldPlans = await service.schedulePlan({
    deviceId: "device-1",
    installSecret: "secret-1",
    sessionId: "session-1",
    revision: 1,
    reminders: [{ kind: "focus_stale_3h", dueAtUtc: new Date(Date.now() + 60_000).toISOString() }],
  });
  const oldPlan = oldPlans[0];
  if (!oldPlan) throw new Error("expected scheduled plan");
  await service.schedulePlan({
    deviceId: "device-1",
    installSecret: "secret-1",
    sessionId: "session-1",
    revision: 2,
    reminders: [{ kind: "focus_stale_3h", dueAtUtc: new Date(Date.now() + 120_000).toISOString() }],
  });

  const result = await service.fire({
    deviceId: "device-1",
    sessionId: "session-1",
    revision: 1,
    reminderId: oldPlan.reminderId,
  });

  expect(result).toBe("skipped");
  expect(fcm.sent).toEqual([]);
});

test("pending callback sends the matching reminder message and marks plan fired", async () => {
  const store = new MemoryReminderStore();
  const qstash = new FakeQStashClient();
  const fcm = new FakeFcmClient();
  const service = new ReminderService(store, qstash, fcm);

  await service.registerDevice({
    deviceId: "device-1",
    installSecretHash: await sha256Hex("secret-1"),
    fcmToken: "token-1",
    nowUtc: new Date().toISOString(),
  });
  const plans = await service.schedulePlan({
    deviceId: "device-1",
    installSecret: "secret-1",
    sessionId: "session-1",
    revision: 1,
    reminders: [{ kind: "leisure_1m_left", dueAtUtc: new Date(Date.now() + 60_000).toISOString() }],
  });
  const plan = plans[0];
  if (!plan) throw new Error("expected scheduled plan");

  const result = await service.fire({
    deviceId: "device-1",
    sessionId: "session-1",
    revision: 1,
    reminderId: plan.reminderId,
  });

  expect(result).toBe("sent");
  expect(fcm.sent).toEqual([
    {
      token: "token-1",
      message: {
        title: "1 min left",
        body: "Your leisure reserve is almost used up.",
        tag: "focuswell-leisure",
      },
      telemetry: {
        reminderId: plan.reminderId,
        kind: "leisure_1m_left",
        dueAtUtc: plan.dueAtUtc,
        firedAtUtc: expect.any(String),
      },
    },
  ]);
  expect((await store.getReminder(plan.reminderId))?.status).toBe("fired");
});

test("registering without a fresh FCM token preserves the existing token", async () => {
  const store = new MemoryReminderStore();
  const qstash = new FakeQStashClient();
  const fcm = new FakeFcmClient();
  const service = new ReminderService(store, qstash, fcm);

  await service.registerDevice({
    deviceId: "device-1",
    installSecretHash: await sha256Hex("secret-1"),
    fcmToken: "token-1",
    nowUtc: "2026-05-21T00:00:00.000Z",
  });
  await service.registerDevice({
    deviceId: "device-1",
    installSecretHash: await sha256Hex("secret-1"),
    nowUtc: "2026-05-22T00:00:00.000Z",
  });

  const device = await store.getDevice("device-1");
  expect(device?.fcmToken).toBe("token-1");
  expect(device?.createdAt).toBe("2026-05-21T00:00:00.000Z");
  expect(device?.lastSeenAt).toBe("2026-05-22T00:00:00.000Z");
});
