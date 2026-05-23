import { expect, test } from "bun:test";
import { sha256Hex } from "./auth";
import { ReminderService } from "./service";
import { MemoryReminderStore } from "./store";
import type { FcmClient } from "./fcm";
import type { QStashClient } from "./qstash";
import type { ReminderPayload } from "./types";

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
  sent: string[] = [];

  async send(token: string): Promise<"sent"> {
    this.sent.push(token);
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
