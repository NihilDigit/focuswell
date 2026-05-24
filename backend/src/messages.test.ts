import { expect, test } from "bun:test";
import { messageFor } from "./messages";

test("long session reminders use checkpoint-specific notification titles", () => {
  expect(messageFor("focus_duration_1h")).toMatchObject({
    title: "Focus 1h",
    tag: "focuswell-focus-duration",
  });
  expect(messageFor("leisure_duration_5h")).toMatchObject({
    title: "Leisure 5h",
    tag: "focuswell-leisure-duration",
  });
});
