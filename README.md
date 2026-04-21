# Tap

Tap is a lightweight Android accessibility utility that detects single, double, and triple taps on the back of the phone using accelerometer and gyroscope data, then runs a user-configured action for each pattern.

Package name: `com.tap.apk`

## Features

- Single / Double / Triple tap detection (API 24+)
- Per-pattern actions:
  - Flashlight (Toggle / On / Off)
  - Open app by package name
  - Termux command execution
- Tabbed Compose settings screen
- GrapheneOS-friendly (no Google Play Services)
- Minimal permissions:
  - `android.permission.BIND_ACCESSIBILITY_SERVICE` (service declaration)
  - `android.permission.CAMERA` (runtime, flashlight only)

## Color Theme (exact)

- Primary: `#d57455`
- Surface: `#c3c2b7`
- Background: `#1e1e1d`
- On Primary: `#FFFFFF`
- On Surface: `#1e1e1d`
- On Background: `#c3c2b7`

## Screens mockup

```
[ SINGLE ] [ DOUBLE ] [ TRIPLE ]
┌──────────────────────────────────────┐
│ Action: [ Flashlight v ]             │
│ Mode:   [ Toggle v ]                 │
│ [ TEST SINGLE TAP ]                  │
│ Enabled: [x]   Cooldown: 1.0s        │
└──────────────────────────────────────┘
```

## GitHub Actions Only Build Pipeline

All builds are done in GitHub Actions:

- CI workflow: `.github/workflows/ci.yml`
  - JDK 17
  - `./gradlew testDebugUnitTest`
  - `./gradlew lint`
  - `./gradlew assembleRelease`
  - `./gradlew bundleRelease`
  - `./gradlew jacocoTestReport`
  - APK size gate (`< 7MB`)
  - Upload APK/AAB/reports artifacts

- Release workflow: `.github/workflows/release.yml`
  - Trigger on tags like `v1.0.0`
  - Build signed release APK/AAB
  - Create GitHub release and attach artifacts
  - Optional webhook notification

## Quick GitHub Setup Instructions

1. `git clone [your-repo]`
2. `git push origin main`
3. Go to Actions tab and download APK from latest run artifact
4. `adb install app/build/outputs/apk/release/app-release.apk`

Note: Step 4 path is where CI produces the release APK in the artifact package.

## GrapheneOS Setup

1. Install APK from GitHub Actions artifact.
2. Open Tap app.
3. Grant `CAMERA` permission (for flashlight action only).
4. Enable the Tap accessibility service:
   - Settings -> Accessibility -> Tap -> enable.
5. Configure Single / Double / Triple tabs and test each pattern.

## Termux Action Setup

- Install Termux (`com.termux`).
- In Tap, choose `Termux Command` and set shell command, e.g.:
  - `termux-toast "hello from tap"`
  - `am start -a android.intent.action.VIEW -d https://grapheneos.org`

## Signing for Production Releases

Set repository secrets:

- `SIGNING_KEYSTORE_BASE64`
- `SIGNING_STORE_PASSWORD`
- `SIGNING_KEY_ALIAS`
- `SIGNING_KEY_PASSWORD`

If secrets are missing, release uses debug signing for bootstrap/testing.

## Size and RAM Targets

- APK target: under 7MB in CI gate
- Runtime RAM target: under 20MB (sensor-driven service + minimal dependencies)

## Test Instructions (No local build required)

1. Push a branch or open a PR.
2. Wait for `Android CI` workflow completion.
3. Download `tap-release-artifacts`.
4. Install `app-release.apk` on device.
5. Validate:
   - Single tap triggers single action
   - Double tap (within 400ms) triggers double action
   - Triple tap (within 600ms) triggers triple action

## Project Structure

```
.
├── .github/workflows/
│   ├── ci.yml
│   └── release.yml
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── kotlin/com/tap/apk/
│       │   │   ├── MainActivity.kt
│       │   │   ├── TapAccessibilityService.kt
│       │   │   ├── actions/ActionRouter.kt
│       │   │   ├── data/TapSettingsDataStore.kt
│       │   │   ├── detection/MultiTapDetector.kt
│       │   │   ├── models/TapAction.kt
│       │   │   ├── models/TapEvent.kt
│       │   │   └── ui/
│       │   │       ├── TapSettingsScreen.kt
│       │   │       └── TapTheme.kt
│       │   └── res/
│       │       ├── values/colors.xml
│       │       ├── values/strings.xml
│       │       └── xml/tap_accessibility_service.xml
│       └── test/kotlin/com/tap/apk/detection/MultiTapDetectorTest.kt
├── build.gradle.kts
├── gradle.properties
├── gradlew
├── gradle/wrapper/gradle-wrapper.jar
├── gradle/wrapper/gradle-wrapper.properties
└── settings.gradle.kts
```
