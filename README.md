<div align="center">

# 📍 FamilyTracker

**A private, self-hosted family location app for Android.**

See where your family is on a live map, and get a heads-up when someone has stayed in one place for 3 hours.

![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)
![Google Maps](https://img.shields.io/badge/Google%20Maps-34A853?style=for-the-badge&logo=googlemaps&logoColor=white)
![Android](https://img.shields.io/badge/minSdk%2026-3DDC84?style=for-the-badge&logo=android&logoColor=white)

</div>

---

## 📖 Table of contents

- [Features](#-features)
- [Tech stack](#-tech-stack)
- [Quick start: run the app](#-quick-start-run-the-app)
  - [1. Prerequisites](#1-prerequisites)
  - [2. Clone the repo](#2-clone-the-repo)
  - [3. Set up Firebase](#3-set-up-firebase)
  - [4. Get a Google Maps API key](#4-get-a-google-maps-api-key)
  - [5. Run on a phone or emulator](#5-run-on-a-phone-or-emulator)
  - [6. Build an APK to share](#6-build-an-apk-to-share)
- [Install on family phones](#-install-on-family-phones)
- [Keep tracking alive](#-keep-tracking-alive-per-manufacturer)
- [How it works](#-how-it-works)
- [Project layout](#-project-layout)
- [Troubleshooting](#-troubleshooting)

---

## ✨ Features

| | |
|---|---|
| 🗺️ **Live family map** | Every phone uploads its location every 5 minutes from a foreground service. |
| 📏 **Distance to each member** | Straight-line distance from you, e.g. `850 m` or `12.4 km`. |
| ⏰ **3-hour stay alert** | When a member stays within 150 m of one spot for 3 hours, everyone else gets *"Name has been at Area for 3 h 10 min"*. Alerts once per stay, computed on-device. |
| 💸 **Free to run** | Works on the Firebase **Spark** (free) plan. No Cloud Functions, no FCM, no server. |
| 🔒 **Private** | Sideloaded, not on the Play Store. Only people with your family code can join. |

## 🧰 Tech stack

- **Language:** Kotlin
- **UI:** Jetpack Compose (Material 3), Google Maps Compose
- **Location:** Fused Location Provider (foreground service)
- **Backend:** Firebase Anonymous Auth + Cloud Firestore
- **SDK:** minSdk 26 · targetSdk 35 · Java 17

---

## 🚀 Quick start: run the app

### 1. Prerequisites

| Tool | Version |
|---|---|
| [Android Studio](https://developer.android.com/studio) | Latest stable |
| JDK | 17 (bundled with Android Studio) |
| Android device or emulator | Android 8.0 (API 26) or newer, **with Google Play services** |
| Google account | For Firebase and Google Cloud |

### 2. Clone the repo

```bash
git clone https://github.com/mayurvij22/Myfamily-locator.git
cd Myfamily-locator
```

Then in Android Studio: **File → Open** and select the cloned folder. Let Gradle sync finish.

### 3. Set up Firebase

1. Go to the [Firebase console](https://console.firebase.google.com) → **Add project** (Google Analytics isn't needed).
2. **Add app → Android**
   - Package name: `com.familytracker`
   - Nickname: anything. SHA-1 is optional.
3. Download **`google-services.json`** and place it at:
   ```
   app/google-services.json
   ```
   > ⚠️ This file is gitignored. **The build fails without it.**
4. **Build → Authentication → Get started → Sign-in method → Anonymous → Enable.**
5. **Build → Firestore Database → Create database.** Pick a nearby location and start in **production mode**.
6. **Firestore → Rules:** replace everything with the contents of [`firestore.rules`](firestore.rules), then click **Publish**.

<details>
<summary>What the security rules do</summary>

- You can read `families/{code}/members/*` only after your own doc `families/{code}/members/{yourUid}` exists (i.e. after you join).
- You can create, update and delete **only your own** member doc.
- Everything else is closed.

</details>

> 🔑 **The family code is the only secret.** Anyone who knows it can join, so use the generated 8-character code and share it only with family.

### 4. Get a Google Maps API key

1. Open the [Google Cloud console](https://console.cloud.google.com) and select the **same project** Firebase created.
2. **APIs & Services → Library → "Maps SDK for Android" → Enable.**
3. **APIs & Services → Credentials → Create credentials → API key.**
4. *(Recommended)* Restrict the key to **Android apps**, package `com.familytracker`, plus your debug SHA-1:
   ```bash
   ./gradlew signingReport     # copy the SHA1 of the "debug" variant
   ```
5. Add the key to **`local.properties`** in the project root (never committed):
   ```properties
   MAPS_API_KEY=AIza...your key...
   ```
   The build injects it into the manifest via `manifestPlaceholders`.

> 💡 The Maps SDK for Android is free for mobile map loads, so a family-sized app costs nothing. Google Cloud may still ask for a billing account before enabling the API.

### 5. Run on a phone or emulator

**Option A — Physical phone (recommended, real GPS)**

1. On the phone: **Settings → About phone →** tap **Build number** 7 times to enable Developer options.
2. **Settings → Developer options → USB debugging → On.**
3. Plug the phone in via USB and accept the *"Allow USB debugging?"* prompt.
4. In Android Studio, pick your phone from the device dropdown and press **▶ Run** (`Shift + F10`).

**Option B — Emulator**

1. **Tools → Device Manager → Create device.** Choose a Pixel and a system image **with Google Play** (API 26+).
2. Start the emulator, select it in the device dropdown and press **▶ Run**.
3. To fake a location: emulator **⋯ (Extended controls) → Location**, pick a point and click **Set location**.

**Option C — Command line**

```bash
./gradlew installDebug                                        # build & install on the connected device
adb shell am start -n com.familytracker/.ui.MainActivity      # launch it
```

> On Windows use `gradlew.bat` (or `.\gradlew` in PowerShell) instead of `./gradlew`.

**Run the unit tests**

```bash
./gradlew testDebugUnitTest
```

### 6. Build an APK to share

From Android Studio: **Build → Build App Bundle(s) / APK(s) → Build APK(s)**, or:

```bash
./gradlew assembleRelease   # → app/build/outputs/apk/release/app-release.apk
./gradlew assembleDebug     # → app/build/outputs/apk/debug/app-debug.apk
```

> ⚠️ The release build is signed with your local **debug key** so it installs directly. Always build from the **same computer** — an APK signed with a different key can't update an already-installed app.

---

## 📲 Install on family phones

1. Copy the APK to the phone (USB, Google Drive, WhatsApp-to-self) or run `adb install app-release.apk`.
2. Open it and allow **Install unknown apps** for the app you opened it from.
3. Open **FamilyTracker → Get started**.
4. **First person:** enter your name → **Generate new code** → **Join**. Share the code from **⋮ → Share family code**.
5. **Everyone else:** enter your name and the same code → **Join**.
6. Grant each permission when asked:
   1. Location → **While using the app** (precise)
   2. Background location → **Allow all the time**
   3. Notifications (Android 13+)
7. Tap the black **"Battery optimization may stop tracking"** banner and choose **Allow**.
8. Do the phone-specific steps below. **This matters most.**

---

## 🔋 Keep tracking alive (per manufacturer)

Android phone makers kill background apps aggressively. Follow the steps for each phone's brand. For anything not covered, see [dontkillmyapp.com](https://dontkillmyapp.com).

<details open>
<summary><b>All phones</b></summary>

- Settings → Apps → FamilyTracker → **Battery → Unrestricted** (or "Don't optimize").
- Settings → Apps → FamilyTracker → Permissions → Location → **Allow all the time** + **Use precise location**.
- Don't swipe the app away from Recents on brands that treat that as force-stop (Xiaomi, Vivo, Oppo, Realme).

</details>

<details>
<summary><b>Xiaomi / Redmi / POCO</b> (MIUI / HyperOS)</summary>

- Settings → Apps → Manage apps → FamilyTracker:
  - **Autostart → On**
  - **Battery saver → No restrictions**
  - Other permissions → **Display pop-up windows while running in the background → Allow** (optional)
- Security app → Battery → App battery saver → FamilyTracker → **No restrictions**.
- Recents: long-press the FamilyTracker card → tap the **lock** icon.

</details>

<details>
<summary><b>Vivo</b> (Funtouch OS / OriginOS)</summary>

- Settings → Battery → **Background power consumption management** → FamilyTracker → **Allow**
  (older versions: Settings → Battery → High background power consumption → enable FamilyTracker).
- i Manager → App manager → **Autostart manager** → FamilyTracker → **On**.
- Recents: pull the FamilyTracker card down to **lock** it.

</details>

<details>
<summary><b>Oppo</b> (ColorOS) / <b>Realme</b> (Realme UI)</summary>

- Settings → Apps → App management → FamilyTracker:
  - **Battery usage → Allow background activity: On**, **Allow auto launch: On**, **Allow launching other apps: On** (wording varies)
- Settings → Battery → More settings → **Optimize battery use** → FamilyTracker → **Don't optimize**.
- Recents: tap ⋮ / long-press the card → **Lock**.

</details>

<details>
<summary><b>Samsung</b> (One UI)</summary>

- Settings → Apps → FamilyTracker → Battery → **Unrestricted**.
- Settings → Battery → Background usage limits:
  - Make sure FamilyTracker is **not** in *Sleeping apps* or *Deep sleeping apps*.
  - Add it to **Never auto sleeping apps**.
- Optional: turn off **Put unused apps to sleep**.

</details>

### ✅ Check that it works

The **"Sharing location with family"** notification should stay visible at all times. If it disappears after a few hours, the phone killed the app — redo that brand's steps. Tracking restarts by itself after a reboot or app update.

---

## ⚙️ How it works

**Firestore data model**

```
families/{code}/members/{uid}
  uid, name, sharing,
  lat, lng, accuracy, updatedAt (epoch ms),
  stayLat, stayLng, stayingSince (epoch ms)
```

**On the tracked phone** — `location/LocationService.kt`, `location/StayTracker.kt`

- A foreground service (`foregroundServiceType="location"`) requests a fused location every 5 min (fastest 2 min) at `PRIORITY_HIGH_ACCURACY` and writes each fix with `SetOptions.merge()`. Firestore queues writes while offline.
- **Stay anchor:** if a fix with accuracy ≤ 200 m is more than 150 m from the anchor, the anchor moves there and `stayingSince = now`. The anchor is persisted in SharedPreferences, so it survives restarts.
- **⋮ → Share my location** pauses uploads and sets `sharing = false`.

**On every phone** — `location/StayAlertNotifier.kt`

- The same service keeps a snapshot listener on all members and re-checks every 5 min.
- It alerts for another member when `now - stayingSince ≥ 3 h` **and** `updatedAt` is within the last 30 min **and** `sharing == true`.
- The area name comes from `Geocoder` (subLocality, locality), falling back to lat/lng.
- Each `"uid:stayingSince"` key is stored after alerting, so a stay alerts only once.

**Restart:** `BootReceiver` handles `BOOT_COMPLETED` and `MY_PACKAGE_REPLACED`.
**Leave family:** stops the service, deletes your member doc and clears the saved code.

---

## 🗂️ Project layout

```
app/src/main/java/com/familytracker/
├── FamilyTrackerApp.kt        # notification channels
├── data/                      # Prefs, Member, FamilyRepository (Auth + Firestore)
├── location/                  # LocationService, StayTracker, StayAlertNotifier, BootReceiver
├── util/                      # Format, AreaResolver (Geocoder), Permissions, Navigation
└── ui/
    ├── MainActivity.kt        # Welcome → Join → Permissions → Home
    ├── AppViewModel.kt        # AndroidViewModel (no Hilt)
    ├── onboarding/            # Welcome, Join, Permission screens
    ├── home/                  # Full-screen map, top card, bottom sheet, pulsing dot
    ├── theme/                 # Black & white theme (Inter)
    └── components/            # Buttons, inputs, avatars
app/src/main/res/raw/          # map_style_light.json, map_style_dark.json
firestore.rules                # Firestore security rules
```

---

## 🛠️ Troubleshooting

| Symptom | Fix |
|---|---|
| Build fails: `File google-services.json is missing` | Download it from Firebase and put it at `app/google-services.json`. |
| Map is blank/grey or shows *"For development purposes only"* | `MAPS_API_KEY` is missing or wrong, Maps SDK isn't enabled, or the key restriction doesn't match your SHA-1. |
| "Couldn't join" | Check internet, that Anonymous Auth is enabled, the rules are published, and `google-services.json` belongs to this project. |
| Members never appear | Rules weren't published, or phones are using different codes. |
| Updates stop after a while | Do the battery / autostart steps for that brand ([Keep tracking alive](#-keep-tracking-alive-per-manufacturer)). |
| No 3-hour alert | Notifications must be allowed, the other member must still be uploading (updated within 30 min), and their location must be accurate (≤ 200 m). |
| `adb: device not found` | Enable USB debugging, re-plug the cable and accept the prompt on the phone. Check with `adb devices`. |
