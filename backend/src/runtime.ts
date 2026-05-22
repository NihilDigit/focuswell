import { DisabledFcmClient } from "./fcm";
import { DisabledQStashClient } from "./qstash";
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
  return new ReminderService(store, new DisabledQStashClient(), new DisabledFcmClient());
}
