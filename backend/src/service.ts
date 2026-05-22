import { verifyInstallSecret } from "./auth";
import type { FcmClient } from "./fcm";
import { messageFor } from "./messages";
import type { QStashClient } from "./qstash";
import type { Device, ReminderKind, ReminderPayload, ReminderPlan } from "./types";
import type { ReminderStore } from "./store";

export class ReminderService {
  constructor(
    private readonly store: ReminderStore,
    private readonly qstash: QStashClient,
    private readonly fcm: FcmClient,
  ) {}

  async registerDevice(args: {
    deviceId: string;
    installSecretHash: string;
    fcmToken?: string;
    nowUtc: string;
  }): Promise<void> {
    const device: Device = {
      deviceId: args.deviceId,
      installSecretHash: args.installSecretHash,
      ...(args.fcmToken ? { fcmToken: args.fcmToken } : {}),
      createdAt: args.nowUtc,
      lastSeenAt: args.nowUtc,
    };
    await this.store.putDevice(device);
  }

  async schedulePlan(args: {
    deviceId: string;
    installSecret: string;
    sessionId: string;
    revision: number;
    reminders: Array<{ kind: ReminderKind; dueAtUtc: string }>;
  }): Promise<ReminderPlan[]> {
    const device = await this.authorize(args.deviceId, args.installSecret);
    const oldPlans = await this.store.listPendingForSession(args.deviceId, args.sessionId);
    await Promise.all(
      oldPlans.map(async (plan) => {
        if (plan.qstashMessageId) await this.qstash.cancel(plan.qstashMessageId);
        await this.store.updateReminder({ ...plan, status: "cancelled" });
      }),
    );

    const plans: ReminderPlan[] = [];
    for (const reminder of args.reminders) {
      const reminderId = `${args.sessionId}-${args.revision}-${reminder.kind}`;
      const payload: ReminderPayload = {
        deviceId: device.deviceId,
        sessionId: args.sessionId,
        revision: args.revision,
        reminderId,
      };
      const scheduled = await this.qstash.publish(payload, reminder.dueAtUtc);
      const plan: ReminderPlan = {
        reminderId,
        deviceId: device.deviceId,
        sessionId: args.sessionId,
        revision: args.revision,
        kind: reminder.kind,
        dueAtUtc: reminder.dueAtUtc,
        qstashMessageId: scheduled.messageId,
        status: "pending",
      };
      await this.store.putReminder(plan);
      plans.push(plan);
    }
    return plans;
  }

  async cancelSession(args: {
    deviceId: string;
    installSecret: string;
    sessionId: string;
  }): Promise<void> {
    await this.authorize(args.deviceId, args.installSecret);
    const plans = await this.store.listPendingForSession(args.deviceId, args.sessionId);
    await Promise.all(
      plans.map(async (plan) => {
        if (plan.qstashMessageId) await this.qstash.cancel(plan.qstashMessageId);
        await this.store.updateReminder({ ...plan, status: "cancelled" });
      }),
    );
  }

  async fire(payload: ReminderPayload): Promise<"sent" | "skipped" | "expired" | "disabled"> {
    const plan = await this.store.getReminder(payload.reminderId);
    if (
      !plan ||
      plan.status !== "pending" ||
      plan.deviceId !== payload.deviceId ||
      plan.sessionId !== payload.sessionId ||
      plan.revision !== payload.revision
    ) {
      return "skipped";
    }
    const device = await this.store.getDevice(plan.deviceId);
    if (!device?.fcmToken) return "disabled";
    const result = await this.fcm.send(device.fcmToken, messageFor(plan.kind));
    await this.store.updateReminder({ ...plan, status: result === "sent" ? "fired" : "cancelled" });
    return result;
  }

  private async authorize(deviceId: string, installSecret: string): Promise<Device> {
    const device = await this.store.getDevice(deviceId);
    if (!device) throw new Error("device-not-found");
    if (!(await verifyInstallSecret(installSecret, device.installSecretHash))) {
      throw new Error("unauthorized");
    }
    return device;
  }
}
