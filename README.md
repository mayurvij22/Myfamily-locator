# FamilyTracker

A private family location app for Android, meant to be sideloaded onto your family's phones (it's not on the Play Store).

- Live map of every family member. Each phone uploads its location every 5 minutes from a foreground service.
- Straight-line distance from you to each member ("850 m", "12.4 km").
- **3-hour stay alert.** When a member has stayed within 150 m of one spot for 3 hours, every other phone shows "*Name* has been at *area* for 3 h 10 min". It alerts once per stay. The phones work this out themselves, with no server.
- Runs on the free Firebase **Spark** plan, with no Cloud Functions and no FCM.

Tech: Kotlin, Jetpack Compose (Material 3), Google Maps Compose, Fused Location Provider, Firebase Anonymous Auth and Cloud Firestore. minSdk 26, targetSdk 35.

---

## 1. Create the Firebase project

1. Go to <https://console.firebase.google.com> → **Add project** (Google Analytics isn't needed).
2. **Add app → Android.**
   - Package name: `com.familytracker`
   - Nickname: anything you like. You can skip the SHA-1.
3. Download **`google-services.json`** and put it at `app/google-services.json`.
   (It's gitignored. The build fails without it.)
4. **Build → Authentication → Get started → Sign-in method → Anonymous → Enable.**
5. **Build → Firestore Database → Create database.** Pick a location near you and start in **production mode**.
6. **Firestore → Rules:** replace everything with the contents of [`firestore.rules`](firestore.rules), then click **Publish**.

The rules mean:
- You can read `families/{code}/members/*` only after your own doc `families/{code}/members/{yourUid}` exists, which happens when you join.
- You can create, update and delete only your own member doc.

> The family code is the only secret. Anyone who knows it can join, so use the
> generated 8-character code and share it only with family.

## 2. Get a Maps API key

1. Go to <https://console.cloud.google.com> and select the **same project** Firebase created.
2. **APIs & Services → Library → "Maps SDK for Android" → Enable.**
3. **APIs & Services → Credentials → Create credentials → API key.**
4. Recommended: **restrict the key** to *Android apps*, package `com.familytracker`, plus your
   debug SHA-1. To get the SHA-1, run `./gradlew signingReport` and copy the `debug` variant's SHA1.
5. Add the key to `local.properties` in the project root (this file is never committed):

   ```properties
   MAPS_API_KEY=AIza...your key...
   ```

   The build passes it to the manifest through `manifestPlaceholders`.

The Maps SDK for Android is free for mobile map loads, so a family-sized app costs nothing. Google
Cloud may still ask you to attach a billing account before it enables the API.

## 3. Build the APK

Open the project in Android Studio and click **Build → Build APK(s)**, or from a terminal:

```bash
./gradlew assembleRelease      # app/build/outputs/apk/release/app-release.apk
# or
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
```

The release build is signed with your local debug key so it installs directly. Always build from
the **same computer**, because a different key can't update an app that's already installed.

## 4. Install on each phone

1. Copy the APK to the phone (USB, Google Drive or WhatsApp-to-self), or run `adb install app-release.apk`.
2. Open it and allow **Install unknown apps** for the app you opened it from.
3. Open FamilyTracker → **Get started**.
4. **The first person** enters their name, taps **Generate new code**, then **Join**. Share the code
   from the ⋮ menu → **Share family code**.
5. **Everyone else** enters their name and the same code, then taps **Join**.
6. Grant each permission when asked:
   1. Location → **While using the app** (precise).
   2. Background location → **Allow all the time**.
   3. Notifications (Android 13+).
7. Tap the black **"Battery optimization may stop tracking"** banner and choose **Allow**.
8. Then do the phone-specific steps below. **This matters most.**

## 5. Keep tracking alive (per manufacturer)

Android phone makers kill background apps aggressively. On every phone, do the steps for its brand.
For anything not covered here, <https://dontkillmyapp.com> has guides for each brand.

### All phones
- Settings → Apps → FamilyTracker → **Battery → Unrestricted** (or "Don't optimize").
- Settings → Apps → FamilyTracker → Permissions → Location → **Allow all the time** + **Use precise location**.
- Don't swipe the app away "to save battery" from Recents on brands that treat that as a force-stop
  (Xiaomi, Vivo, Oppo, Realme).

### Xiaomi / Redmi / POCO (MIUI / HyperOS)
- Settings → Apps → Manage apps → FamilyTracker:
  - **Autostart → On**
  - **Battery saver → No restrictions**
  - Other permissions → **Display pop-up windows while running in the background → Allow** (optional)
- Security app → Battery → App battery saver → FamilyTracker → **No restrictions**.
- Recents screen: long-press the FamilyTracker card → tap the **lock** icon.

### Vivo (Funtouch OS / OriginOS)
- Settings → Battery → **Background power consumption management** → FamilyTracker → **Allow**
  (on older versions: Settings → Battery → High background power consumption → enable FamilyTracker).
- i Manager → App manager → **Autostart manager** → FamilyTracker → **On**.
- Recents: pull the FamilyTracker card down to **lock** it.

### Oppo (ColorOS) and Realme (Realme UI)
- Settings → Apps → App management → FamilyTracker:
  - **Battery usage → Allow background activity: On**, **Allow auto launch: On**,
    **Allow launching other apps: On** (wording varies)
- Settings → Battery → More settings → **Optimize battery use** → FamilyTracker → **Don't optimize**.
- Recents: tap the ⋮ / long-press the card → **Lock**.

### Samsung (One UI)
- Settings → Apps → FamilyTracker → Battery → **Unrestricted**.
- Settings → Battery → Background usage limits:
  - Make sure FamilyTracker is **not** in *Sleeping apps* or *Deep sleeping apps*.
  - Add it to **Never auto sleeping apps**.
- Optional: turn off **Put unused apps to sleep**.

### Check that it works
The **"Sharing location with family"** notification should stay visible all the time. If it's gone
after a few hours, the phone killed the app; go through that brand's steps again. Tracking restarts
by itself after a reboot or an app update.

---

## How it works

```
families/{code}/members/{uid}
  uid, name, sharing,
  lat, lng, accuracy, updatedAt (epoch ms),
  stayLat, stayLng, stayingSince (epoch ms)
```

**On the tracked phone** (`location/LocationService.kt`, `location/StayTracker.kt`)
- The foreground service (`foregroundServiceType="location"`) requests fused location every 5 min
  (fastest 2 min) at `PRIORITY_HIGH_ACCURACY`. It writes each fix with `SetOptions.merge()`.
  Firestore queues writes while offline.
- Stay anchor: if a fix with accuracy ≤ 200 m is more than 150 m from the anchor, the anchor moves
  there and `stayingSince = now`. The anchor is kept in SharedPreferences, so it survives restarts.
- The ⋮ → **Share my location** switch pauses uploads and sets `sharing=false`.

**On every phone** (`location/StayAlertNotifier.kt`)
- The same service keeps a snapshot listener on all members and also re-checks every 5 min.
- It alerts for another member when `now - stayingSince ≥ 3 h` **and** `updatedAt` is within the last
  30 min **and** `sharing == true`.
- The area comes from `Geocoder` (subLocality, locality) and falls back to lat/lng.
- Each `"uid:stayingSince"` key is stored after alerting, so a stay alerts only once.

**Restart:** `BootReceiver` handles `BOOT_COMPLETED` and `MY_PACKAGE_REPLACED`.

**Leave family:** stops the service, deletes your member doc and clears the saved code.

## Project layout

```
app/src/main/java/com/familytracker/
  FamilyTrackerApp.kt          notification channels
  data/                        Prefs, Member, FamilyRepository (Auth + Firestore)
  location/                    LocationService, StayTracker, StayAlertNotifier, BootReceiver
  util/                        Format, AreaResolver (Geocoder), Permissions
  ui/MainActivity.kt           Welcome → Join → permissions → Home
  ui/AppViewModel.kt           AndroidViewModel (no Hilt)
  ui/onboarding/               Welcome, Join, Permission screens
  ui/home/                     Full-screen map, top card, bottom sheet, detail card, pulsing dot
  ui/theme/ ui/components/     Black & white theme (Inter), buttons, grey inputs, avatars
app/src/main/res/raw/          map_style_light.json, map_style_dark.json
firestore.rules
```

Run the unit tests with `./gradlew testDebugUnitTest`.

## Troubleshooting

| Symptom | Fix |
|---|---|
| Map is blank or grey with a "For development purposes only" watermark | `MAPS_API_KEY` is missing or wrong, the Maps SDK isn't enabled, or the key restriction doesn't match your SHA-1. |
| "Couldn't join" | Check the internet connection, that Anonymous Auth is enabled, that the rules are published, and that `google-services.json` belongs to this project. |
| Members never appear | The rules weren't published, or the phones are using different codes. |
| Updates stop after a while | Do the battery / autostart steps for that brand (section 5). |
| No 3-hour alert | Notifications must be allowed, the other member must still be uploading (updated within 30 min), and their location must have been accurate (≤ 200 m). |
#   M y f a m i l y - l o c a t o r  
 