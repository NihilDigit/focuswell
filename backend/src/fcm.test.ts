import { expect, test } from "bun:test";
import { fcmRequestBody } from "./fcm";

test("FCM reminder body is data-only high-priority Android message", () => {
  const body = fcmRequestBody("token-1", {
    title: "10 min left",
    body: "Your leisure reserve is running low.",
    tag: "focuswell-leisure",
  });

  expect(body).toEqual({
    message: {
      token: "token-1",
      data: {
        tag: "focuswell-leisure",
        title: "10 min left",
        body: "Your leisure reserve is running low.",
      },
      android: {
        priority: "HIGH",
      },
    },
  });
  expect(JSON.stringify(body)).not.toContain("\"notification\"");
});
