import {
  parseCancelSession,
  parseRegisterDevice,
  parseReminderPayload,
  parseSchedulePlan,
} from "./src/http";
import { createReminderService } from "./src/runtime";

const service = createReminderService();

export default async function handler(request: {
  method?: string;
  url?: string;
  headers?: Record<string, string | string[] | undefined>;
  body?: unknown;
  [Symbol.asyncIterator]?: () => AsyncIterableIterator<Buffer>;
}, response: {
  statusCode: number;
  setHeader: (name: string, value: string) => void;
  end: (body?: string) => void;
}): Promise<void> {
  const path = normalizePath(request.url);

  try {
    if (path === "/health") {
      send(response, 200, { ok: true, service: "focuswell-reminders" });
      return;
    }

    if (path === "/devices/register") {
      requirePost(request);
      await service.registerDevice(parseRegisterDevice(await readJson(request)));
      send(response, 200, { ok: true });
      return;
    }

    if (path === "/reminders/schedule") {
      requirePost(request);
      const reminders = await service.schedulePlan(parseSchedulePlan(await readJson(request)));
      send(response, 200, { ok: true, reminders });
      return;
    }

    if (path === "/reminders/cancel") {
      requirePost(request);
      await service.cancelSession(parseCancelSession(await readJson(request)));
      send(response, 200, { ok: true });
      return;
    }

    if (path === "/qstash/fire") {
      requirePost(request);
      const body = await readText(request);
      const verified = await service.verifyQStash(header(request, "upstash-signature"), body, process.env.QSTASH_CALLBACK_URL ?? absoluteUrl(request));
      if (!verified) throw new Error("unauthorized");
      const result = await service.fire(parseReminderPayload(JSON.parse(body) as unknown));
      send(response, 200, { ok: true, result });
      return;
    }

    send(response, 404, { ok: false, error: "not-found" });
  } catch (error) {
    sendError(response, error);
  }
}

function normalizePath(url: string | undefined): string {
  const path = new URL(url ?? "/", "https://focuswell.local").pathname;
  const withoutApi = path.startsWith("/api/") ? path.slice(4) : path;
  return withoutApi === "/" ? "/health" : withoutApi;
}

function requirePost(request: { method?: string }): void {
  if (request.method !== "POST") throw new Error("method-not-allowed");
}

function sendError(response: { statusCode: number; setHeader: (name: string, value: string) => void; end: (body?: string) => void }, error: unknown): void {
  const message = error instanceof Error ? error.message : "unknown";
  const status = message === "unauthorized" ? 401 : message === "device-not-found" ? 404 : 400;
  send(response, status, { ok: false, error: message });
}

function send(response: { statusCode: number; setHeader: (name: string, value: string) => void; end: (body?: string) => void }, status: number, body: unknown): void {
  response.statusCode = status;
  response.setHeader("content-type", "application/json; charset=utf-8");
  response.end(JSON.stringify(body));
}

async function readJson(request: {
  body?: unknown;
  [Symbol.asyncIterator]?: () => AsyncIterableIterator<Buffer>;
}): Promise<unknown> {
  if (request.body !== undefined) return request.body;
  return JSON.parse(await readText(request)) as unknown;
}

async function readText(request: {
  body?: unknown;
  [Symbol.asyncIterator]?: () => AsyncIterableIterator<Buffer>;
}): Promise<string> {
  if (typeof request.body === "string") return request.body;
  if (request.body !== undefined) return JSON.stringify(request.body);
  if (!request[Symbol.asyncIterator]) throw new Error("invalid-json");

  const chunks: string[] = [];
  for await (const chunk of request as AsyncIterable<Buffer>) {
    chunks.push(Buffer.isBuffer(chunk) ? chunk.toString("utf8") : String(chunk));
  }

  const text = chunks.join("");
  if (!text) throw new Error("invalid-json");
  return text;
}

function header(request: { headers?: Record<string, string | string[] | undefined> }, name: string): string | null {
  const value = request.headers?.[name] ?? request.headers?.[name.toLowerCase()];
  if (Array.isArray(value)) return value[0] ?? null;
  return value ?? null;
}

function absoluteUrl(request: { url?: string; headers?: Record<string, string | string[] | undefined> }): string {
  const proto = header(request, "x-forwarded-proto") ?? "https";
  const host = header(request, "x-forwarded-host") ?? header(request, "host") ?? "backend-seven-eosin-45.vercel.app";
  return new URL(request.url ?? "/", `${proto}://${host}`).toString();
}
