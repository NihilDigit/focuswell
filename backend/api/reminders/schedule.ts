import type { VercelRequest, VercelResponse } from "@vercel/node";
import { handleError, parseSchedulePlan, requirePost, sendJson } from "../../src/http";
import { createReminderService } from "../../src/runtime";

const service = createReminderService();

export default async function handler(request: VercelRequest, response: VercelResponse): Promise<void> {
  try {
    requirePost(request);
    const reminders = await service.schedulePlan(parseSchedulePlan(request.body));
    sendJson(response, 200, { ok: true, reminders });
  } catch (error) {
    await handleError(response, error);
  }
}
