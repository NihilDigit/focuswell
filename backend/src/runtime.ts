import { DisabledFcmClient, HttpV1FcmClient } from "./fcm";
import { DisabledQStashClient, RestQStashClient } from "./qstash";
import { RedisReminderStore } from "./redis-store";
import { ReminderService } from "./service";
import { MemoryReminderStore } from "./store";

export function createReminderService(): ReminderService {
  const redisUrl = process.env.KV_REST_API_URL;
  const redisToken = process.env.KV_REST_API_TOKEN;
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
