import type { VercelRequest, VercelResponse } from "@vercel/node";
import { sendJson } from "../src/http";

export default function handler(_request: VercelRequest, response: VercelResponse): void {
  sendJson(response, 200, { ok: true });
}
