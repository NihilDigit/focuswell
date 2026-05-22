import type { ReminderMessage } from "./types";

export type FcmClient = {
  send(token: string, message: ReminderMessage): Promise<"sent" | "expired" | "disabled">;
};

export class DisabledFcmClient implements FcmClient {
  async send(): Promise<"disabled"> {
    return "disabled";
  }
}
