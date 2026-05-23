import { Receiver } from "@upstash/qstash";
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
    return false;
  }
}

export class RestQStashClient implements QStashClient {
  constructor(
    private readonly token: string,
    private readonly baseUrl: string,
    private readonly callbackUrl: string,
    private readonly currentSigningKey: string,
    private readonly nextSigningKey: string,
  ) {}

  async publish(payload: ReminderPayload, dueAtUtc: string): Promise<{ messageId: string }> {
    console.log(
      "qstash_publish",
      JSON.stringify({
        callbackUrl: this.callbackUrl,
        dueAtUtc,
        reminderId: payload.reminderId,
      }),
    );
    const response = await fetch(`${this.baseUrl}/v2/publish/${this.callbackUrl}`, {
      method: "POST",
      headers: {
        authorization: `Bearer ${this.token}`,
        "content-type": "application/json",
        "upstash-method": "POST",
        "upstash-not-before": unixSeconds(dueAtUtc).toString(),
        "upstash-retries": "2",
      },
      body: JSON.stringify(payload),
    });

    if (!response.ok) {
      console.error("qstash_publish_failed", response.status, await response.text());
      throw new Error(`qstash-publish-${response.status}`);
    }
    const body = (await response.json()) as { messageId?: string };
    if (!body.messageId) throw new Error("qstash-missing-message-id");
    return { messageId: body.messageId };
  }

  async cancel(messageId: string): Promise<void> {
    const response = await fetch(`${this.baseUrl}/v2/messages/${encodeURIComponent(messageId)}`, {
      method: "DELETE",
      headers: { authorization: `Bearer ${this.token}` },
    });
    if (!response.ok && response.status !== 404) throw new Error(`qstash-cancel-${response.status}`);
  }

  async verify(signature: string | null, body: string, url: string): Promise<boolean> {
    if (!signature) return false;
    const receiver = new Receiver({
      currentSigningKey: this.currentSigningKey,
      nextSigningKey: this.nextSigningKey,
    });
    return receiver.verify({ signature, body, url });
  }
}

function unixSeconds(value: string): number {
  const timestamp = Date.parse(value);
  if (!Number.isFinite(timestamp)) throw new Error("invalid-dueAtUtc");
  return Math.ceil(timestamp / 1000);
}
