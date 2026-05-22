# FocusWell Status Log

Last updated: 2026-05-22

## Current State

- Android app package: `dev.nihildigit.focuswell`.
- Firebase project created: `FocusWell` / `focuswell-dc4ec`.
- Firebase Android app registered and `app/app/google-services.json` is present.
- Vercel project deployed: `https://backend-seven-eosin-45.vercel.app`.
- Backend health and disabled-mode smoke test passed before adding real env.
- Android build with Firebase config passed:
  `.\gradlew.bat --no-configuration-cache testDebugUnitTest assembleDebug`.

## Implemented

- Local-first FocusWell app with focus/leisure/wind-down/depleted modes.
- Editable tags, daily trackers, rule trackers, records CRUD, JSON import/export, and clear-all-data.
- Asia/Shanghai 04:00 business-day handling and daily +60m leisure grant.
- Firebase Messaging client integration on Android.
- Backend Vercel single-entry API for register, schedule, cancel, QStash fire, and health.
- Backend clients for Upstash Redis REST, QStash REST scheduling/signature verification, and Firebase HTTP v1 FCM.

## Not Finished

- `FIREBASE_*` Vercel env vars are not configured yet.
- Firebase Admin SDK private key has not been placed at `C:\tmp\focuswell-firebase-adminsdk.json`.
- Upstash account/resource setup was not completed; Vercel env still has no variables.
- Physical Android device was not connected during the last check, so install/run verification is still pending.

## Next Resume Steps

1. Put Firebase Admin SDK key at `C:\tmp\focuswell-firebase-adminsdk.json`.
2. Add `FIREBASE_PROJECT_ID`, `FIREBASE_CLIENT_EMAIL`, and `FIREBASE_PRIVATE_KEY` to Vercel production env.
3. Log into Upstash, create Redis + QStash resources, then add:
   `KV_REST_API_URL`, `KV_REST_API_TOKEN`, `QSTASH_TOKEN`,
   `QSTASH_CURRENT_SIGNING_KEY`, `QSTASH_NEXT_SIGNING_KEY`,
   `QSTASH_CALLBACK_URL=https://backend-seven-eosin-45.vercel.app/api/qstash/fire`.
4. Redeploy backend and run `FOCUSWELL_BACKEND_URL=https://backend-seven-eosin-45.vercel.app bun run src/http-smoke.ts`.
5. Connect physical Android device and run `.\gradlew.bat installDebug`.

## Safety Notes

- Do not commit Firebase Admin SDK private keys.
- Root `.gitignore` and `backend/.gitignore` ignore common service-account and env files.
- `google-services.json` is committed for Android client configuration; it is not the Admin SDK private key.
