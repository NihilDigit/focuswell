const params = new URLSearchParams(location.search);
const blockedUrl = params.get("url") || "";
const message = document.querySelector("#blockedUrl");

if (blockedUrl) {
  message.textContent = "This page was not allowed.";
} else {
  message.textContent = "This page is not in the current whitelist.";
}

chrome.runtime.sendMessage({ type: "record-blocked", url: blockedUrl || "blocked-page" });
