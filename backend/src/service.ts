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
    installSecret?: string;
    fcmToken?: string;
    nowUtc: string;
  }): Promise<void> {
    const existing = await this.store.getDevice(args.deviceId);
    if (existing) {
      if (!args.installSecret || !(await verifyInstallSecret(args.installSecret, existing.installSecretHash))) {
        throw new Error("unauthorized");
      }
      if (args.installSecretHash !== existing.installSecretHash) {
        throw new Error("identity-rotation-required");
      }
    }
    const fcmToken = args.fcmToken ?? existing?.fcmToken;
    const device: Device = {
      deviceId: args.deviceId,
      installSecretHash: existing?.installSecretHash ?? args.installSecretHash,
      ...(fcmToken ? { fcmToken } : {}),
      createdAt: existing?.createdAt ?? args.nowUtc,
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
    await this.cancelPlans(await this.store.listPendingForDevice(args.deviceId), "schedule_replace");

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
    await this.cancelPlans(await this.store.listPendingForSession(args.deviceId, args.sessionId), "session_cancel");
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
    if (!device?.fcmToken) {
      const firedAtUtc = new Date().toISOString();
      console.log(
        "reminder_fire",
        JSON.stringify({
          reminderId: plan.reminderId,
          kind: plan.kind,
          dueAtUtc: plan.dueAtUtc,
          firedAtUtc,
          delayMs: Date.parse(firedAtUtc) - Date.parse(plan.dueAtUtc),
          result: "disabled",
        }),
      );
      return "disabled";
    }
    const firedAtUtc = new Date().toISOString();
    const result = await this.fcm.send(device.fcmToken, messageFor(plan.kind), {
      reminderId: plan.reminderId,
      sessionId: plan.sessionId,
      revision: plan.revision,
      kind: plan.kind,
      dueAtUtc: plan.dueAtUtc,
      firedAtUtc,
    });
    console.log(
      "reminder_fire",
      JSON.stringify({
        reminderId: plan.reminderId,
        kind: plan.kind,
        dueAtUtc: plan.dueAtUtc,
        firedAtUtc,
        delayMs: Date.parse(firedAtUtc) - Date.parse(plan.dueAtUtc),
        result,
      }),
    );
    await this.store.updateReminder({ ...plan, status: result === "sent" ? "fired" : "cancelled" });
    return result;
  }

  async verifyQStash(signature: string | null, body: string, url: string): Promise<boolean> {
    return this.qstash.verify(signature, body, url);
  }

  private async authorize(deviceId: string, installSecret: string): Promise<Device> {
    const device = await this.store.getDevice(deviceId);
    if (!device) throw new Error("device-not-found");
    if (!(await verifyInstallSecret(installSecret, device.installSecretHash))) {
      throw new Error("unauthorized");
    }
    return device;
  }

  private async cancelPlans(plans: ReminderPlan[], reason: "schedule_replace" | "session_cancel"): Promise<void> {
    await Promise.all(
      plans.map(async (plan) => {
        if (plan.qstashMessageId) await this.qstash.cancel(plan.qstashMessageId);
        await this.store.updateReminder({ ...plan, status: "cancelled" });
        console.log(
          "reminder_cancel",
          JSON.stringify({
            reminderId: plan.reminderId,
            sessionId: plan.sessionId,
            kind: plan.kind,
            reason,
          }),
        );
      }),
    );
  }
}
