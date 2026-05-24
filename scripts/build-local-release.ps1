param(
  [string]$VersionName = "",
  [switch]$SkipGitHubSecretCheck
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$appRoot = Join-Path $repoRoot "app"
$signingFile = Join-Path $appRoot "release-signing.properties"

if (-not $SkipGitHubSecretCheck) {
  $requiredSecrets = @(
    "FOCUSWELL_RELEASE_KEYSTORE_BASE64",
    "FOCUSWELL_RELEASE_STORE_PASSWORD",
    "FOCUSWELL_RELEASE_KEY_ALIAS",
    "FOCUSWELL_RELEASE_KEY_PASSWORD"
  )
  $secretNames = gh secret list --repo NihilDigit/focuswell --json name --jq ".[].name"
  foreach ($secret in $requiredSecrets) {
    if ($secretNames -notcontains $secret) {
      throw "Missing GitHub Secret: $secret"
    }
  }
}

if (-not (Test-Path -LiteralPath $signingFile)) {
  throw "Missing $signingFile. Copy app/release-signing.properties.example and fill it with the same release keystore used by GitHub Actions."
}

$gradleArgs = @("testDebugUnitTest", "assembleRelease")
if ($VersionName.Trim().Length -gt 0) {
  $gradleArgs += "-PfocuswellVersionName=$VersionName"
}

Push-Location $appRoot
try {
  & .\gradlew.bat @gradleArgs
  if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
  }
} finally {
  Pop-Location
}
