# FocusWell

FocusWell is an Android-first, ADHD-friendly time accounting app.

It turns daytime focus into a storable leisure reserve, so high-energy days can
support low-energy days without creating streak pressure or progress anxiety.

Current design spec:

- [FocusWell Product Spec](docs/focuswell-product-spec.md)

## Development

The Android debug app can be built directly from a fresh clone. The Firebase
client config is committed at `app/app/google-services.json`, and the debug
backend URL is defined in `app/app/build.gradle.kts`.

```powershell
cd app
.\gradlew.bat --no-configuration-cache testDebugUnitTest installDebug
```

Backend production secrets live in Vercel. `backend/.env.example` is only for
local backend development.
