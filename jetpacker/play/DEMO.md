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
3. In Google Play Console, navigate to **Internal testing** > **Create new release**.
4. Upload the generated `.aab` bundle and confirm AppFunctions are listed.
5. Save the release and add your test Google account to the internal testers list.

### 3. Install JetPacker on the Test Device

Install the APK on an Android 16+ device:

```shell
adb install -r ~/jetpacker-play.apk
```

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

Reset the demo data first with `RESET_DEMO` (see Demo Controls below), and start each prompt with `@JetPacker`.

| Prompt | Function | Expected Result |
| :--- | :--- | :--- |
| `@JetPacker find my trip to Mountain View` | `searchTrip` | ADS '26 Summit, Oct 27–31 |
| `@JetPacker what's on my itinerary for ADS '26?` | `getItinerary` | Flight UA 991, Redwood Grove Hotel, Developer Keynote |
| `@JetPacker what's the confirmation number for my hotel in Mountain View?` | `getEventDetails` | RED-11223 at The Redwood Grove Hotel |
| `@JetPacker how much budget do I have left for ADS '26?` | `getExpenses` | \$150 spent; \$1,850 left of \$2,000 |
| `@JetPacker add 45 dollars for the airport taxi to my ADS trip` | `addExpense` | Logged; \$1,805 left |
| `@JetPacker add a team dinner on October 29 at 7 PM to my ADS trip` | `addItineraryEvent` | New dinner event added on Oct 29 |
| `@JetPacker save a voice note on my ADS trip: grab the keynote slides` | `addVoiceNote` | Note saved to trip |
| `@JetPacker show my voice notes for ADS '26` | `getVoiceNotes` | Displays saved note |
| `@JetPacker set the theme for October 28 on my ADS trip to "Agents day"` | `setDayTheme` | Daily theme saved |
| `@JetPacker what are the day themes for ADS '26?` | `getDayThemes` | Lists "Agents day" on Oct 28 |
| `@JetPacker create a trip to Lisbon from November 12 to 16` | `createTrip` | New Lisbon trip created |

## Demo Controls

To reset or test scenarios via ADB (with the JetPacker app open):

```shell
export JETPACKER_APP_ID="com.example.jetpacker"

# Restore the ADS '26 expenses to the $150 base:
adb shell am broadcast -a com.example.jetpacker.RESET_DEMO -p "$JETPACKER_APP_ID"

# Reseed the whole database:
adb shell am broadcast -a com.example.jetpacker.RESET_DATABASE -p "$JETPACKER_APP_ID"

# Pin the app clock to Oct 28, 2026, 09:00 PDT:
adb shell am start -n "$JETPACKER_APP_ID/com.example.jetpacker.MainActivity" \
    --el override_time 1793203200000

# Clear all data:
adb shell pm clear "$JETPACKER_APP_ID"
```

## Troubleshooting

| Symptom | Likely Cause | Solution |
| :--- | :--- | :--- |
| Package name rejected | Another account uploaded it or name conflict | Pass a unique `--application-id` |
| `Can't find service: app_function` | Running on Android 15 or earlier | Use an Android 16 (API 36+) device |
| AppFunctions not listed in Play Console | Bundle metadata missing | Verify `base/assets/jetpacker_app_function_service.xml` in `.aab` |
| `@JetPacker` not visible in Gemini | Connected app toggle disabled | Enable in Gemini > Profile > Connected apps |
| ADB reset broadcast does nothing | App is in background/closed | Open JetPacker in foreground, then resend broadcast |
