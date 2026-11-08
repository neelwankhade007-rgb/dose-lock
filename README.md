# DoseLock 💊🔒
### High-Accountability Medication Reminder & Unignorable Alarm Engine

[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Platform-Android_14%2B_%28API_24--36%29-3DDC84.svg?logo=android)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4.svg?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![CameraX](https://img.shields.io/badge/CameraX-1.5.0-orange.svg)](https://developer.android.com/training/camerax)
[![Room](https://img.shields.io/badge/Room_Database-2.7.0-brightgreen.svg)](https://developer.android.com/training/data-storage/room)
[![License](https://img.shields.io/badge/License-Apache_2.0-lightgrey.svg)](LICENSE)

**DoseLock** is an aggressive, high-accountability medication reminder Android application engineered to make critical prescriptions unignorable. Designed for high-risk, time-sensitive schedules, DoseLock enforces strict medical adherence by bypassing standard phone silencing and requiring physical photo proof of medication before alarms can be turned off.

---

## 🌟 Core Features

- **🚨 Unignorable Alarm Engine (`AlarmManager` + Foreground Service)**
  - Precision scheduling with `AlarmManager.setExactAndAllowWhileIdle()` (`RTC_WAKEUP`) to trigger reliably even in Doze and low-power idle states.
  - Full-screen lock overlay (`setShowWhenLocked(true)`, `setTurnScreenOn(true)`) that automatically wakes the device and takes over the keyguard.
  - Persistent foreground audio service operating on `AudioAttributes.USAGE_ALARM` with volume enforcement to bypass silent and Do Not Disturb (DND) modes.
  - Suppresses hardware volume buttons and back gestures to prevent accidental muting or dismissal.
  - Automatic `BOOT_COMPLETED` receiver to reschedule all active alarms across device restarts.

- **🔊 Synthesized High-Gain Sound Engine**
  - Built-in real-time audio synthesizer powered by `AudioTrack` producing loud alarm waveforms at full gain (`0.95 - 0.98`):
    - **Siren (Fast Sweep):** Rapid frequency modulation between 800 Hz and 2400 Hz.
    - **Harsh Pulse (Square):** Harsh 1000 Hz square wave bursts pulsing at 8 Hz.
    - **High Beep (Rapid):** Fast electronic alert beeps at 2200 Hz.
  - Includes live audio preview buttons and a top-bar **`[TEST SOUND]`** toggle.

- **📸 "Proof of Medication" Adherence Protocol**
  - **Step 1 (Setup):** Capture a reference snapshot of your pill bottle or medication blister pack using CameraX and set a grace countdown (1, 3, 5, or 10 minutes).
  - **Step 2 (Alarm Trigger):** Loud siren loops continuously until the user taps **"Silence & Start Grace Timer"**.
  - **Step 3 (Grace Period):** Sound is temporarily muted and a live visual countdown starts with the instruction: *"Walk to your medicine and take your dose."* The saved reference photo is shown side-by-side.
  - **Step 4 (Completion vs. Escalation):**
    - **Success:** Tapping "Take Verification Photo" opens the camera to snap proof of the dose, logging the event as `TAKEN & VERIFIED` and stopping the alarm.
    - **Escalation (Breach):** If the timer reaches `00:00` without photo proof, the siren aggressively turns **BACK ON** at maximum volume, shifts into a flashing RED alert screen, and can only be silenced by capturing the verification photo.

- **⏰ Interactive Material 3 Clock Dial (`TimePicker`)**
  - Circular Android clock face with interactive draggable radial hands for hours and minutes, with AM/PM toggles.
  - Displays formatted 12-hour times (`08:30 AM`) on cards while storing 24-hour integers (`0–23`, `0–59`) for exact scheduling.

- **💾 Local Persistence (Room Database + Flow)**
  - Fully offline and private Room database with reactive Kotlin `Flow` queries and automatic migration handling.

---

## 🛠️ Tech Stack & Architecture

| Layer | Technologies |
|---|---|
| **Language** | Kotlin 2.2.10 |
| **UI Toolkit** | Jetpack Compose, Material Design 3 |
| **Architecture** | MVVM (Model-View-ViewModel) + Clean Repository Pattern |
| **Alarm & System** | `AlarmManager`, Foreground Service, `PowerManager.WakeLock`, `Vibrator` |
| **Audio Synthesizer**| `AudioTrack` (PCM 16-bit 44.1kHz), `AudioAttributes.USAGE_ALARM` |
| **Camera & Capture** | CameraX (`camera-camera2`, `camera-lifecycle`, `camera-view`) |
| **Image Loading** | Coil Compose |
| **Database** | Room Database (SQLite with KSP compiler) |
| **Concurrency** | Kotlin Coroutines & `StateFlow` / `Flow` |

---

## 📱 Screenshots & Flow

```
[ Dashboard ] ───( Tap [+ Add] )───► [ Material 3 TimePicker Dial ]
      │                                             │
      │                                    [ Capture Reference Photo ]
      ▼                                             │
[ Medication Card ] ──( Tap ▶ TEST )──► [ Alarm Locks Screen ]
                                                    │
                                  [ "Silence & Start Grace Timer" ]
                                                    │
                                          [ Grace Countdown ]
                                         (Walk to Medicine)
                                                    │
                                     [ Capture Verification Photo ]
                                                    │
                                      ✅ DOSE TAKEN & VERIFIED
```

---

## 🚀 Running the Project Locally in Android Studio

Follow these steps to clone, build, and run DoseLock on your local machine:

### Prerequisites
1. **Android Studio**: Android Studio Ladybug (2024.2+) or newer.
2. **JDK**: Java Development Kit 17 or 21 (bundled with Android Studio).
3. **Android SDK**: Android SDK API Level 34–36 installed via SDK Manager.
4. **Physical Device or Emulator**: Android device running Android 7.0 (API 24) or higher with camera permissions enabled.

### 1. Clone the Repository
```bash
git clone https://github.com/YOUR_USERNAME/DoseLock.git
cd DoseLock
```

### 2. Open Project in Android Studio
1. Open Android Studio.
2. Select **File > Open...** and choose the `DoseLock` root directory.
3. Allow Gradle to sync dependencies (Room, CameraX, Jetpack Compose, and Coil).

### 3. Configure Local Properties
Ensure `local.properties` specifies your Android SDK location if prompted:
```properties
sdk.dir=/Users/YOUR_USERNAME/Library/Android/sdk
```

### 4. Build and Run
1. Select the `app` run configuration in the top toolbar.
2. Choose a connected physical Android device or an Android Virtual Device (AVD).
3. Click the green **Run (▶)** button or press `Shift + F10`.
4. Grant the requested **Camera** and **Notification** permissions when prompted on first launch.

### 5. Running Unit Tests
Execute the local unit and Robolectric tests from the terminal:
```bash
./gradlew testDebugUnitTest
```
Or on Windows:
```bash
gradlew.bat testDebugUnitTest
```

---

## 🔒 Permissions Used & Rationale

- `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`: Required for precise timing of medication reminders via `AlarmManager`.
- `USE_FULL_SCREEN_INTENT`: Enables the alert to wake up and take over the locked screen during a scheduled dose.
- `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_MEDIA_PLAYBACK`: Keeps audio playback alive and unkillable by system background limits while the alarm is ringing.
- `WAKE_LOCK`: Prevents the CPU and screen from turning off while waiting for dose verification.
- `VIBRATE`: Provides aggressive tactile pulses alongside audio alarms.
- `CAMERA`: Captures pill reference photos and active verification photos.
- `RECEIVE_BOOT_COMPLETED`: Restores and reschedules all active medication alarms automatically if the phone reboots.

---

## 📄 License
This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
