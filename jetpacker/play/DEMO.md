# Demo JetPacker as a Gemini Connected App

This guide walks through configuring and running JetPacker as a connected app that Gemini calls on an Android 16+ device using AppFunctions.

## Overview

JetPacker integrates with Android's AppFunctions system service to allow on-device AI and the Gemini assistant to query itineraries, add events, log expenses, and update trips.

## Setup Steps

### 1. Create a Play Console App & Store Listing

In [Google Play Console](https://play.google.com/console), create an app entry:

| Field | Value |
| :--- | :--- |
| App name | JetPacker |
| Default language | English (United States) |
| App or game | App |
| Free or paid | Free |

Add store listing visual assets from `store_listing/`:

| Field | Value |
| :--- | :--- |
| Short description | Trips, itineraries, and expenses in one place. |
| Full description | JetPacker keeps every part of a trip in one place: flights, hotels, restaurants, museum visits, tours, and expenses. Plan trips, track expenses, and manage your itinerary. Ask Gemini about your trips, bookings, and budget, or add events, expenses, and voice notes. |
| App icon | `store_listing/icon-512.png` |
| Feature graphic | `store_listing/feature-graphic-1024x500.png` |
| Phone screenshots | `store_listing/screenshot-1-home.png`, `screenshot-2-itinerary.png`, `screenshot-3-flight.png` |

### 2. Build and Upload the App Bundle

1. Create your upload key:
   ```shell
   ./jetpacker/play/create_upload_key.sh
   ```
2. Build and sign the release bundle:
   ```shell
   ./jetpacker/play/build_play_bundle.sh \
       --keystore ~/keys/jetpacker-upload.jks \
       --application-id com.yourname.jetpacker \
       --universal-apk ~/jetpacker-play.apk
   ```
   > **Note**: Generating a universal test APK via `--universal-apk` requires `bundletool` available on your `PATH` or configured via the `BUNDLETOOL_JAR` environment variable (e.g. `export BUNDLETOOL_JAR=/path/to/bundletool-all.jar`).
3. In Google Play Console, navigate to **Internal testing** > **Create new release**.
4. Upload the generated `.aab` bundle and confirm AppFunctions are listed.
5. Save the release and add your test Google account to the internal testers list.

### 3. Install JetPacker on the Test Device

Install the APK on an Android 16+ device:

```shell
adb install -r ~/jetpacker-play.apk
```

> **Important**: The test device must have the exact same package name (`applicationId`), `versionCode`, and signing certificate installed as the bundle uploaded to Play Console. If installing directly via `adb` using `--universal-apk`, ensure the APK was generated from the same bundle/keystore. Alternatively, install directly from Google Play via the internal testing track.

Verify that JetPacker registered its AppFunctions:

```shell
adb shell dumpsys app_function | grep jetpacker
```

### 4. Connect JetPacker in Gemini

1. On your test device, open the **Gemini** app.
2. Tap your profile icon > **Connected apps**.
3. Locate **JetPacker** in the list and enable it.
4. Test invoking the connected app by typing `@JetPacker` in Gemini.

## Test Prompts

To start with clean seed data, clear the app data with `adb shell pm clear "$JETPACKER_APP_ID"` (see Demo Controls below), and start each prompt with `@JetPacker`.

| Prompt | Function | Expected Result |
| :--- | :--- | :--- |
| `@JetPacker find my trip to California` | `searchTrip` | California I/O, May 18–22, 2026 |
| `@JetPacker what's on my itinerary for California I/O?` | `getItinerary` | AMS > SFO flight, Car pickup, Check-in @ The Redwood Grove Hotel, San Francisco Bay Cruise, SFMOMA |
| `@JetPacker what expenses do I have for California I/O?` | `getExpenses` | \$190.00 total: SFO Taxi (\$45), SFMOMA Ticket (\$25), Dinner @ Quantum Bites (\$120) |
| `@JetPacker add 35 dollars for airport shuttle to my California trip` | `addExpense` | Logged; \$35.00 added |
| `@JetPacker add a team dinner on May 19 at 7 PM to my California trip` | `addItineraryEvent` | New dinner event added on May 19 |
| `@JetPacker save a voice note on my California trip: grab the keynote slides` | `addVoiceNote` | Note saved to trip |
| `@JetPacker show my voice notes for California I/O` | `getVoiceNotes` | Displays saved note |
| `@JetPacker set the theme for 2026-05-19 on my California trip to "Museum & Bay day"` | `setDayTheme` | Daily theme saved |
| `@JetPacker what are the day themes for California I/O?` | `getDayThemes` | Lists "Museum & Bay day" on 2026-05-19 |
| `@JetPacker create a trip to Lisbon from November 12 to 16` | `createTrip` | New Lisbon trip created |

## Demo Controls

To reset or test scenarios via ADB:

```shell
export JETPACKER_APP_ID="com.example.jetpacker"

# Reset all app data and restore default seed data:
adb shell pm clear "$JETPACKER_APP_ID"

# Pin the app clock to May 18, 2026, 10:00 UTC (start of California I/O trip):
adb shell am start -n "$JETPACKER_APP_ID/com.example.jetpacker.MainActivity" \
    --el override_time 1779098400000
```

## Troubleshooting

| Symptom | Likely Cause | Solution |
| :--- | :--- | :--- |
| Package name rejected | Another account uploaded it or name conflict | Pass a unique `--application-id` |
| `Can't find service: app_function` | Running on Android 15 or earlier | Use an Android 16 (API 36+) device |
| AppFunctions not listed in Play Console | Bundle metadata missing | Verify `base/root/assets/jetpacker_app_function_service.xml` in `.aab` |
| `@JetPacker` not visible in Gemini | Connected app toggle disabled | Enable in Gemini > Profile > Connected apps |
