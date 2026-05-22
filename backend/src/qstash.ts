import { createHash, createHmac, timingSafeEqual } from "node:crypto";
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

export class RestQStashClient implements QStashClient {
  constructor(
    private readonly token: string,
    private readonly callbackUrl: string,
    private readonly currentSigningKey: string,
    private readonly nextSigningKey: string,
  ) {}

  async publish(payload: ReminderPayload, dueAtUtc: string): Promise<{ messageId: string }> {
    const response = await fetch(`https://qstash.upstash.io/v2/publish/${encodeURIComponent(this.callbackUrl)}`, {
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

    if (!response.ok) throw new Error(`qstash-publish-${response.status}`);
    const body = (await response.json()) as { messageId?: string };
    if (!body.messageId) throw new Error("qstash-missing-message-id");
    return { messageId: body.messageId };
  }

  async cancel(messageId: string): Promise<void> {
    const response = await fetch(`https://qstash.upstash.io/v2/messages/${encodeURIComponent(messageId)}`, {
      method: "DELETE",
      headers: { authorization: `Bearer ${this.token}` },
    });
    if (!response.ok && response.status !== 404) throw new Error(`qstash-cancel-${response.status}`);
  }

  async verify(signature: string | null, body: string, url: string): Promise<boolean> {
    if (!signature) return false;
    return verifyJwt(signature, body, url, this.currentSigningKey) || verifyJwt(signature, body, url, this.nextSigningKey);
  }
}

function unixSeconds(value: string): number {
  const timestamp = Date.parse(value);
  if (!Number.isFinite(timestamp)) throw new Error("invalid-dueAtUtc");
  return Math.ceil(timestamp / 1000);
}

function verifyJwt(token: string, body: string, url: string, key: string): boolean {
  const parts = token.split(".");
  if (parts.length !== 3 || !parts[0] || !parts[1] || !parts[2]) return false;

  const expected = new Uint8Array(createHmac("sha256", key).update(`${parts[0]}.${parts[1]}`).digest());
  const actual = new Uint8Array(Buffer.from(parts[2], "base64url"));
  if (expected.length !== actual.length || !timingSafeEqual(expected, actual)) return false;

  const payload = JSON.parse(Buffer.from(parts[1], "base64url").toString("utf8")) as {
    iss?: string;
    sub?: string;
    exp?: number;
    nbf?: number;
    body?: string;
  };
  const now = Math.floor(Date.now() / 1000);
  const bodyHash = createHash("sha256").update(body).digest("base64url");
  return payload.iss === "Upstash" && payload.sub === url && (payload.exp ?? 0) >= now && (payload.nbf ?? 0) <= now && payload.body === bodyHash;
}
