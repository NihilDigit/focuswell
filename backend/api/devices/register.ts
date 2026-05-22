import type { VercelRequest, VercelResponse } from "@vercel/node";
import { handleError, parseRegisterDevice, requirePost, sendJson } from "../../src/http";
import { createReminderService } from "../../src/runtime";

const service = createReminderService();

export default async function handler(request: VercelRequest, response: VercelResponse): Promise<void> {
  try {
    requirePost(request);
    await service.registerDevice(parseRegisterDevice(request.body));
    sendJson(response, 200, { ok: true });
  } catch (error) {
    await handleError(response, error);
  }
}
