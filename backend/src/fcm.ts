import { createSign } from "node:crypto";
import type { ReminderDeliveryTelemetry, ReminderMessage } from "./types";

export type FcmClient = {
  send(token: string, message: ReminderMessage, telemetry: ReminderDeliveryTelemetry): Promise<"sent" | "expired" | "disabled">;
};

export class DisabledFcmClient implements FcmClient {
  async send(): Promise<"disabled"> {
    return "disabled";
  }
}

export class HttpV1FcmClient implements FcmClient {
  private cachedAccessToken: { token: string; expiresAtMs: number } | null = null;

  constructor(
    private readonly projectId: string,
    private readonly clientEmail: string,
    private readonly privateKey: string,
  ) {}

  async send(token: string, message: ReminderMessage, telemetry: ReminderDeliveryTelemetry): Promise<"sent" | "expired"> {
    const accessToken = await this.accessToken();
    const response = await fetch(`https://fcm.googleapis.com/v1/projects/${this.projectId}/messages:send`, {
      method: "POST",
      headers: {
        authorization: `Bearer ${accessToken}`,
        "content-type": "application/json",
      },
      body: JSON.stringify(fcmRequestBody(token, message, telemetry)),
    });

    if (response.ok) return "sent";
    const text = await response.text();
    if (response.status === 404 || text.includes("UNREGISTERED") || text.includes("INVALID_ARGUMENT")) return "expired";
    throw new Error(`fcm-send-${response.status}`);
  }

  private async accessToken(): Promise<string> {
    if (this.cachedAccessToken && this.cachedAccessToken.expiresAtMs > Date.now() + 60_000) {
      return this.cachedAccessToken.token;
    }

    const jwt = signServiceAccountJwt(this.clientEmail, this.privateKey);
    const response = await fetch("https://oauth2.googleapis.com/token", {
      method: "POST",
      headers: { "content-type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({
        grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
        assertion: jwt,
      }),
    });
    if (!response.ok) throw new Error(`fcm-token-${response.status}`);

    const body = (await response.json()) as { access_token?: string; expires_in?: number };
    if (!body.access_token || !body.expires_in) throw new Error("fcm-token-invalid-response");
    this.cachedAccessToken = {
      token: body.access_token,
      expiresAtMs: Date.now() + body.expires_in * 1000,
    };
    return body.access_token;
  }
}

export function fcmRequestBody(token: string, message: ReminderMessage, telemetry: ReminderDeliveryTelemetry): unknown {
  return {
    message: {
      token,
      data: {
        tag: message.tag,
        title: message.title,
        body: message.body,
        reminderId: telemetry.reminderId,
        kind: telemetry.kind,
        dueAtUtc: telemetry.dueAtUtc,
        firedAtUtc: telemetry.firedAtUtc,
      },
      android: {
        priority: "HIGH",
      },
    },
  };
}

function signServiceAccountJwt(clientEmail: string, privateKey: string): string {
  const now = Math.floor(Date.now() / 1000);
  const header = base64UrlJson({ alg: "RS256", typ: "JWT" });
  const payload = base64UrlJson({
    iss: clientEmail,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: "https://oauth2.googleapis.com/token",
    iat: now,
    exp: now + 3600,
  });
  const input = `${header}.${payload}`;
  const signature = createSign("RSA-SHA256").update(input).sign(privateKey.replace(/\\n/g, "\n"), "base64url");
  return `${input}.${signature}`;
}

function base64UrlJson(value: unknown): string {
  return Buffer.from(JSON.stringify(value)).toString("base64url");
}
