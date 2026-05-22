import type { VercelRequest, VercelResponse } from "@vercel/node";
import { handleError, parseReminderPayload, requirePost, sendJson } from "../../src/http";
import { createReminderService } from "../../src/runtime";

const service = createReminderService();

export default async function handler(request: VercelRequest, response: VercelResponse): Promise<void> {
  try {
    requirePost(request);
    const result = await service.fire(parseReminderPayload(request.body));
    sendJson(response, 200, { ok: true, result });
  } catch (error) {
    await handleError(response, error);
  }
}
