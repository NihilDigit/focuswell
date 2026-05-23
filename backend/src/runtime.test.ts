import { afterEach, expect, test } from "bun:test";
import { createReminderService } from "./runtime";

const originalEnv = { ...process.env };

afterEach(() => {
  for (const key of Object.keys(process.env)) {
    delete process.env[key];
  }
  Object.assign(process.env, originalEnv);
});

test("production runtime fails closed when reminder configuration is incomplete", () => {
  process.env.NODE_ENV = "production";
  delete process.env.KV_REST_API_URL;
  delete process.env.KV_REST_API_TOKEN;
  delete process.env.QSTASH_TOKEN;
  delete process.env.QSTASH_CALLBACK_URL;
  delete process.env.QSTASH_CURRENT_SIGNING_KEY;
  delete process.env.QSTASH_NEXT_SIGNING_KEY;
  delete process.env.FIREBASE_PROJECT_ID;
  delete process.env.FIREBASE_CLIENT_EMAIL;
  delete process.env.FIREBASE_PRIVATE_KEY;

  expect(() => createReminderService()).toThrow("missing-production-reminder-config");
});
