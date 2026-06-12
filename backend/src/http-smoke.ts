import { sha256Hex } from "./auth";

const baseUrl = process.env.FOCUSWELL_BACKEND_URL ?? "http://localhost:8787";
const deviceId = "smoke-device";
const installSecret = "smoke-secret";

const health = await fetch(`${baseUrl}/health`);
if (!health.ok) throw new Error("health failed");

const register = await fetch(`${baseUrl}/devices/register`, {
  method: "POST",
  headers: { "content-type": "application/json" },
  body: JSON.stringify({
    deviceId,
    installSecret,
    installSecretHash: await sha256Hex(installSecret),
    nowUtc: new Date().toISOString(),
  }),
});
if (!register.ok) throw new Error("register failed");

const sessionId = "smoke-session";
const schedule = await fetch(`${baseUrl}/reminders/schedule`, {
  method: "POST",
  headers: { "content-type": "application/json" },
  body: JSON.stringify({
    deviceId,
    installSecret,
    sessionId,
    revision: 1,
    reminders: [
      {
        kind: "focus_stale_3h",
        dueAtUtc: new Date(Date.now() + 60_000).toISOString(),
      },
    ],
  }),
});
if (!schedule.ok) throw new Error("schedule failed");

const cancel = await fetch(`${baseUrl}/reminders/cancel`, {
  method: "POST",
  headers: { "content-type": "application/json" },
  body: JSON.stringify({ deviceId, installSecret, sessionId }),
});
if (!cancel.ok) throw new Error("cancel failed");

console.log("backend smoke passed");
