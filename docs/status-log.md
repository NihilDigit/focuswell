# FocusWell Status Log

Last updated: 2026-05-23

## Current State

- Android app package: `dev.nihildigit.focuswell`.
- Firebase project created: `FocusWell` / `focuswell-dc4ec`.
- Firebase Android app registered and `app/app/google-services.json` is present.
- Vercel project deployed: `https://backend-seven-eosin-45.vercel.app`.
- Backend typecheck passed on 2026-05-22:
  `bun run check`.
- GitHub repository secret names are present for Firebase:
  `FIREBASE_ADMINSDK_JSON`, `FIREBASE_CLIENT_EMAIL`, `FIREBASE_PRIVATE_KEY`, `FIREBASE_PROJECT_ID`.
- Vercel production runtime env is configured as of 2026-05-22:
  `FIREBASE_PROJECT_ID`, `FIREBASE_CLIENT_EMAIL`, `FIREBASE_PRIVATE_KEY`,
  `KV_REST_API_URL`, `KV_REST_API_TOKEN`, `QSTASH_BASE_URL`,
  `QSTASH_TOKEN`, `QSTASH_CURRENT_SIGNING_KEY`, `QSTASH_NEXT_SIGNING_KEY`,
  `QSTASH_CALLBACK_URL`.
- Production backend redeployed and aliased to `https://backend-seven-eosin-45.vercel.app`.
- Production backend smoke test passed on 2026-05-22:
  `FOCUSWELL_BACKEND_URL=https://backend-seven-eosin-45.vercel.app bun run src/http-smoke.ts`.
- Android unit tests passed on 2026-05-22:
  `.\gradlew.bat --no-configuration-cache testDebugUnitTest`.
- Physical Android device is connected and the debug app was installed/launched on 2026-05-22:
  device `2dd9d428` / `CPH2691`, app process `dev.nihildigit.focuswell` running as pid `28258`.
- Recent startup logcat check showed no `AndroidRuntime` / `FATAL EXCEPTION` entries for FocusWell.

## Implemented

- Local-first FocusWell app with focus/leisure/wind-down/depleted modes.
- Editable tags, daily trackers, rule trackers, records CRUD, JSON import/export, and clear-all-data.
- System-time-zone 04:00 business-day handling and daily +60m leisure grant.
- Room-backed local persistence for timers, records, trackers, tags, and ledger entries.
- Firebase Messaging client integration on Android.
- Backend Vercel single-entry API for register, schedule, cancel, QStash fire, and health.
- Backend clients for Upstash Redis REST, QStash REST scheduling/signature verification, and Firebase HTTP v1 FCM.

## Not Finished

- No known implementation blocker remains for the current debug-device setup.

## Next Resume Steps

1. Keep the installed debug app on the connected physical device for hands-on UX testing.
2. Re-run backend smoke and `.\gradlew.bat --no-configuration-cache testDebugUnitTest installDebug`
   after future backend or app changes.

## Safety Notes

- Do not commit Firebase Admin SDK private keys.
- Root `.gitignore` and `backend/.gitignore` ignore common service-account and env files.
- `google-services.json` is committed for Android client configuration; it is not the Admin SDK private key.
