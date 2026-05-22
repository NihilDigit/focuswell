import type { Device, ReminderPlan } from "./types";

export interface ReminderStore {
  getDevice(deviceId: string): Promise<Device | null>;
  putDevice(device: Device): Promise<void>;
  putReminder(plan: ReminderPlan): Promise<void>;
  getReminder(reminderId: string): Promise<ReminderPlan | null>;
  listPendingForSession(deviceId: string, sessionId: string): Promise<ReminderPlan[]>;
  updateReminder(plan: ReminderPlan): Promise<void>;
}

export class MemoryReminderStore implements ReminderStore {
  private devices = new Map<string, Device>();
  private reminders = new Map<string, ReminderPlan>();

  async getDevice(deviceId: string): Promise<Device | null> {
    return this.devices.get(deviceId) ?? null;
  }

  async putDevice(device: Device): Promise<void> {
    this.devices.set(device.deviceId, device);
  }

  async putReminder(plan: ReminderPlan): Promise<void> {
    this.reminders.set(plan.reminderId, plan);
  }

  async getReminder(reminderId: string): Promise<ReminderPlan | null> {
    return this.reminders.get(reminderId) ?? null;
  }

  async listPendingForSession(deviceId: string, sessionId: string): Promise<ReminderPlan[]> {
    return Array.from(this.reminders.values()).filter(
      (plan) =>
        plan.deviceId === deviceId &&
        plan.sessionId === sessionId &&
        plan.status === "pending",
    );
  }

  async updateReminder(plan: ReminderPlan): Promise<void> {
    this.reminders.set(plan.reminderId, plan);
  }
}
