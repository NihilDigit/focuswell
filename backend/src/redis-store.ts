import type { Device, ReminderPlan } from "./types";
import type { ReminderStore } from "./store";

export class RedisReminderStore implements ReminderStore {
  constructor(
    private readonly url: string,
    private readonly token: string,
  ) {}

  async getDevice(deviceId: string): Promise<Device | null> {
    return this.getJson<Device>(deviceKey(deviceId));
  }

  async putDevice(device: Device): Promise<void> {
    await this.command(["SET", deviceKey(device.deviceId), JSON.stringify(device)]);
  }

  async putReminder(plan: ReminderPlan): Promise<void> {
    await this.command(["SET", reminderKey(plan.reminderId), JSON.stringify(plan)]);
    await this.command(["SADD", sessionKey(plan.deviceId, plan.sessionId), plan.reminderId]);
  }

  async getReminder(reminderId: string): Promise<ReminderPlan | null> {
    return this.getJson<ReminderPlan>(reminderKey(reminderId));
  }

  async listPendingForSession(deviceId: string, sessionId: string): Promise<ReminderPlan[]> {
    const ids = await this.command<string[]>(["SMEMBERS", sessionKey(deviceId, sessionId)]);
    const plans = await Promise.all(ids.map((id) => this.getReminder(id)));
    return plans.filter((plan): plan is ReminderPlan => plan !== null && plan.status === "pending");
  }

  async listPendingForDevice(deviceId: string): Promise<ReminderPlan[]> {
    let cursor = "0";
    const sessionKeys: string[] = [];
    do {
      const result = await this.command<[string, string[]]>([
        "SCAN",
        cursor,
        "MATCH",
        `${sessionKeyPrefix(deviceId)}*`,
        "COUNT",
        100,
      ]);
      cursor = result[0];
      sessionKeys.push(...result[1]);
    } while (cursor !== "0");

    const reminderIds = new Set<string>();
    for (const key of sessionKeys) {
      const ids = await this.command<string[]>(["SMEMBERS", key]);
      ids.forEach((id) => reminderIds.add(id));
    }
    const plans = await Promise.all(Array.from(reminderIds).map((id) => this.getReminder(id)));
    return plans.filter((plan): plan is ReminderPlan => plan !== null && plan.deviceId === deviceId && plan.status === "pending");
  }

  async updateReminder(plan: ReminderPlan): Promise<void> {
    await this.command(["SET", reminderKey(plan.reminderId), JSON.stringify(plan)]);
  }

  private async getJson<T>(key: string): Promise<T | null> {
    const value = await this.command<string | null>(["GET", key]);
    return value ? (JSON.parse(value) as T) : null;
  }

  private async command<T>(command: Array<string | number>): Promise<T> {
    const response = await fetch(this.url, {
      method: "POST",
      headers: {
        authorization: `Bearer ${this.token}`,
        "content-type": "application/json",
      },
      body: JSON.stringify(command),
    });
    if (!response.ok) {
      throw new Error(`redis-${response.status}`);
    }
    const body = (await response.json()) as { result?: T; error?: string };
    if (body.error) throw new Error(body.error);
    return body.result as T;
  }
}

function deviceKey(deviceId: string): string {
  return `focuswell:device:${deviceId}`;
}

function reminderKey(reminderId: string): string {
  return `focuswell:reminder:${reminderId}`;
}

function sessionKey(deviceId: string, sessionId: string): string {
  return `focuswell:session:${deviceId}:${sessionId}`;
}

function sessionKeyPrefix(deviceId: string): string {
  return `focuswell:session:${deviceId}:`;
}
