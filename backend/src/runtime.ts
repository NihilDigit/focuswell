import { DisabledFcmClient, HttpV1FcmClient } from "./fcm";
import { DisabledQStashClient, RestQStashClient } from "./qstash";
import { RedisReminderStore } from "./redis-store";
import { ReminderService } from "./service";
import { MemoryReminderStore } from "./store";

export function createReminderService(): ReminderService {
  const redisUrl = process.env.KV_REST_API_URL;
  const redisToken = process.env.KV_REST_API_TOKEN;
  requireProductionConfig({
    "KV_REST_API_URL": redisUrl,
    "KV_REST_API_TOKEN": redisToken,
    "QSTASH_TOKEN": process.env.QSTASH_TOKEN,
    "QSTASH_CALLBACK_URL": process.env.QSTASH_CALLBACK_URL,
    "QSTASH_CURRENT_SIGNING_KEY": process.env.QSTASH_CURRENT_SIGNING_KEY,
    "QSTASH_NEXT_SIGNING_KEY": process.env.QSTASH_NEXT_SIGNING_KEY,
    "FIREBASE_PROJECT_ID": process.env.FIREBASE_PROJECT_ID,
    "FIREBASE_CLIENT_EMAIL": process.env.FIREBASE_CLIENT_EMAIL,
    "FIREBASE_PRIVATE_KEY": process.env.FIREBASE_PRIVATE_KEY,
  });
  const store =
    redisUrl && redisToken
      ? new RedisReminderStore(redisUrl, redisToken)
      : new MemoryReminderStore();
  const qstash =
    process.env.QSTASH_TOKEN &&
    process.env.QSTASH_CALLBACK_URL &&
    process.env.QSTASH_CURRENT_SIGNING_KEY &&
    process.env.QSTASH_NEXT_SIGNING_KEY
      ? new RestQStashClient(
          process.env.QSTASH_TOKEN,
          process.env.QSTASH_BASE_URL ?? "https://qstash.upstash.io",
          process.env.QSTASH_CALLBACK_URL,
          process.env.QSTASH_CURRENT_SIGNING_KEY,
          process.env.QSTASH_NEXT_SIGNING_KEY,
        )
      : new DisabledQStashClient();
  const fcm =
    process.env.FIREBASE_PROJECT_ID && process.env.FIREBASE_CLIENT_EMAIL && process.env.FIREBASE_PRIVATE_KEY
      ? new HttpV1FcmClient(
          process.env.FIREBASE_PROJECT_ID,
          process.env.FIREBASE_CLIENT_EMAIL,
          process.env.FIREBASE_PRIVATE_KEY,
        )
      : new DisabledFcmClient();
  return new ReminderService(store, qstash, fcm);
}

function requireProductionConfig(values: Record<string, string | undefined>): void {
  if (process.env.NODE_ENV !== "production") return;
  const missing = Object.entries(values)
    .filter(([, value]) => !value)
    .map(([key]) => key);
  if (missing.length > 0) {
    throw new Error(`missing-production-reminder-config:${missing.join(",")}`);
  }
}
