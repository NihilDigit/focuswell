import type { ReminderPayload } from "./types";

export type QStashClient = {
  publish(payload: ReminderPayload, dueAtUtc: string): Promise<{ messageId: string }>;
  cancel(messageId: string): Promise<void>;
  verify(signature: string | null, body: string, url: string): Promise<boolean>;
};

export class DisabledQStashClient implements QStashClient {
  async publish(): Promise<{ messageId: string }> {
    return { messageId: "disabled" };
  }

  async cancel(): Promise<void> {}

  async verify(): Promise<boolean> {
    return true;
  }
}
