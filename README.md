# Unlock Logger (Android) — CI/CD Demo Project

This project is an Android app that logs device unlock events and is also designed to demonstrate a practical **CI/CD pipeline for Android** using **GitHub Actions**.

Repo: https://github.com/Sangameshwar-1/unlock-logger-app

## Objective

**Primary objective:** implement a complete CI/CD setup for an Android app:
- Build on every push / pull request
- Run checks (tests/lint)
- Upload build artifacts (APK)
- Create release builds on version tags (optional)
- (Optional) Publish to GitHub Releases

## What the app does (feature summary)

- Foreground service monitors unlock events (screen off → user present)
- Stores unlock timestamps locally using Room (SQLite)
- Displays total count and unlock history
- Restarts on device boot

## Tech stack

- Android (minSdk 26, targetSdk 34, compileSdk 34)
- Kotlin / Java 17
- Room (SQLite) + KSP
- AndroidX + Material

---

## CI/CD with GitHub Actions

### 1) Continuous Integration (CI)

Triggered on:
- `push` to `main`
- `pull_request` to `main`

Pipeline steps:
- Checkout code
- Setup JDK 17
- Setup Android SDK
- Cache Gradle
- Run:
  - `./gradlew test`
  - `./gradlew lint` (recommended)
  - `./gradlew assembleDebug`
- Upload artifact:
  - `app/build/outputs/apk/debug/app-debug.apk`

### 2) Continuous Delivery (CD) — optional release pipeline

Triggered on tags:
- `v*` (example: `v1.0.0`)

Pipeline steps:
- Build Release APK/AAB
- (Optional) Sign the Release build using secrets
- Create a GitHub Release and attach the artifact

### Badges (after workflows are added)

After adding workflows, you can add badges like:

- CI: `https://github.com/Sangameshwar-1/unlock-logger-app/actions/workflows/android-ci.yml/badge.svg`
- Release: `https://github.com/Sangameshwar-1/unlock-logger-app/actions/workflows/android-release.yml/badge.svg`

---

## Build & run locally

```bash
git clone https://github.com/Sangameshwar-1/unlock-logger-app.git
cd unlock-logger-app
./gradlew assembleDebug
```

Then install (example):
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## License

No license file is currently included.
