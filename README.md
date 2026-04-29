# Unlock Logger (Android)

`Unlock Logger` is a simple Android app that **logs device unlock events** (when the user unlocks the phone after the screen was off), stores them locally using **Room (SQLite)**, and shows the history in a basic UI.

Repo: https://github.com/Sangameshwar-1/unlock-logger-app

## What it does

- Runs a **foreground service** (`UnlockLoggerService`) to reliably monitor screen/unlock broadcasts.
- Detects unlocks using a “screen off → user present” flow:
  - `ACTION_SCREEN_OFF` sets a flag
  - `ACTION_USER_PRESENT` logs an event only if the screen was previously off
- Stores unlock timestamps in a **Room database**
- Displays:
  - total unlock count
  - last updated time
  - a list of unlock events (timestamp + relative time)
- Restarts the service on device boot via `BootReceiver`

## App components (high level)

- **`MainActivity`**
  - Starts the foreground service
  - Reads events from the DB and displays them in a `RecyclerView`
  - Allows clearing the database
  - Supports swipe-to-refresh

- **`UnlockLoggerService`**
  - Foreground service with persistent notification (“Unlock Logger Active”)
  - Registers a receiver for:
    - `Intent.ACTION_SCREEN_ON`
    - `Intent.ACTION_SCREEN_OFF`
    - `Intent.ACTION_USER_PRESENT`
  - On unlock detection, inserts an `UnlockEvent` into Room and updates the notification count

- **Receivers**
  - `UnlockReceiver`: listens for `android.intent.action.USER_PRESENT` and triggers logging via the service helper
  - `BootReceiver`: listens for `android.intent.action.BOOT_COMPLETED` to start the service after reboot

- **Database (Room)**
  - `UnlockEvent` entity (`unlock_events` table)
  - `UnlockEventDao` for insert/query/delete
  - `AppDatabase` singleton for DB access

## Permissions used

Declared in `app/src/main/AndroidManifest.xml`:

- `android.permission.RECEIVE_BOOT_COMPLETED`
- `android.permission.FOREGROUND_SERVICE`
- `android.permission.FOREGROUND_SERVICE_SPECIAL_USE`
- `android.permission.POST_NOTIFICATIONS` (required on Android 13+ to show notifications)

## Requirements

- Android Studio (recommended)
- Android SDK / Gradle
- **minSdk 26**, **targetSdk 34**, **compileSdk 34**
- Java 17 / Kotlin JVM target 17

Key dependencies:
- AndroidX + Material
- SwipeRefreshLayout
- Room (`room-runtime`, `room-ktx`) with **KSP** (`room-compiler`)

## Build & run

1. Clone:
   ```bash
   git clone https://github.com/Sangameshwar-1/unlock-logger-app.git
   cd unlock-logger-app
   ```

2. Open in **Android Studio**.

3. Sync Gradle, then run on a device/emulator (note: unlock events are best tested on a real device).

## How unlock detection works (logic)

The service logs an unlock only when it sees:

1. Screen turns off (`ACTION_SCREEN_OFF`) → sets `wasScreenOff = true`
2. User unlocks (`ACTION_USER_PRESENT`) and `wasScreenOff == true` → log event and reset flag

This avoids logging spurious `USER_PRESENT` events that didn’t follow a screen-off transition.

## Included APK

This repository includes a debug APK:

- `app-debug.apk` (see repo root)

> Note: For security and trust reasons, users should ideally build the APK from source rather than installing a random debug APK.

## Notes / limitations

- Broadcast-based unlock tracking can vary across OEMs and Android versions.
- Foreground services require a persistent notification.
- On Android 13+ you must grant notification permission, otherwise the service notification may not show correctly.

## License

No license file is currently included in the repository. If you want others to use/modify the code, add a `LICENSE` (MIT/Apache-2.0/etc.).
