import {
  parseCancelSession,
  parseRegisterDevice,
  parseReminderPayload,
  parseSchedulePlan,
} from "../src/http";
import { createReminderService } from "../src/runtime";

const service = createReminderService();

const server = Bun.serve({
  port: Number(process.env.PORT ?? 8787),
  async fetch(request: Request) {
    const url = new URL(request.url);
    try {
      if (request.method === "GET" && url.pathname === "/health") {
        return json({ ok: true });
      }
      if (request.method === "POST" && url.pathname === "/devices/register") {
        await service.registerDevice(parseRegisterDevice(await request.json()));
        return json({ ok: true });
      }
      if (request.method === "POST" && url.pathname === "/reminders/schedule") {
        const plans = await service.schedulePlan(parseSchedulePlan(await request.json()));
        return json({ ok: true, reminders: plans });
      }
      if (request.method === "POST" && url.pathname === "/reminders/cancel") {
        await service.cancelSession(parseCancelSession(await request.json()));
        return json({ ok: true });
      }
      if (request.method === "POST" && url.pathname === "/qstash/fire") {
        const result = await service.fire(parseReminderPayload(await request.json()));
        return json({ ok: true, result });
      }
      return json({ ok: false, error: "not-found" }, 404);
    } catch (error) {
      const message = error instanceof Error ? error.message : "unknown";
      const status = message === "unauthorized" ? 401 : message === "device-not-found" ? 404 : 400;
      return json({ ok: false, error: message }, status);
    }
  },
});

console.log(`FocusWell reminder backend listening on http://localhost:${server.port}`);

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "content-type": "application/json" },
  });
}
