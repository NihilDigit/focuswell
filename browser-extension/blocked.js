const params = new URLSearchParams(location.search);
const blockedUrl = params.get("url") || "";
const message = document.querySelector("#blockedUrl");

if (blockedUrl) {
  message.textContent = "这次访问没有放行。";
} else {
  message.textContent = "这次访问不在当前白名单内。";
}

chrome.runtime.sendMessage({ type: "record-blocked", url: blockedUrl || "blocked-page" });
