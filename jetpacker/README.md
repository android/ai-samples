# JetPacker

<table align="center">
  <tr>
    <td align="center"><b>Home Screen</b></td>
    <td align="center"><b>Trip Itinerary</b></td>
    <td align="center"><b>Flight Detail</b></td>
  </tr>
  <tr>
    <td align="center"><img src="screenshots/home.png" width="210" alt="Home Screen" /></td>
    <td align="center"><img src="screenshots/trip.png" width="210" alt="Trip Itinerary" /></td>
    <td align="center"><img src="screenshots/detail.png" width="210" alt="Flight Detail Screen" /></td>
  </tr>
</table>

## Overview
JetPacker provides users with powerful tools to manage their upcoming trips, build out rich itineraries, record voice notes, manage travel expenses, generate on-device "Trip Summaries and Tips", generate AI reviews, chat with hotel staff via automatic translation, get real-time museum assistant guidance, and contribute to Android's intelligence system with trip management functions through [AppFunctions](https://d.android.com/ai/appfunctions).

## Architecture
This project is built using modern Android architecture components:
- **UI**: Jetpack Compose
- **Dependency Injection**: Dagger/Hilt
- **Local Persistence**: Room Database
- **State Management**: ViewModels with StateFlow
- **On-Device AI**: ML Kit GenAI (Prompt, Speech Recognition, Translation)
- **Cloud & Hybrid AI**: Firebase AI Logic (Gemini grounded with URL/Maps/Search, and hybrid models with on-device fallback)
- **App Security**: Firebase App Check (with Play Integrity and Debug Provider)
- **Assistant Integration**: Android [AppFunctions](https://d.android.com/ai/appfunctions) (`androidx.appfunctions`)

## Module Overview
JetPacker follows a clean, multi-module Android structure organized by responsibility and domain:

### Core Modules (`:core:*`)
- **`:core:ui`**: Shared Jetpack Compose design system components (`JetPackerFab`, `JetPackerToolbar`, themes, custom typography).
- **`:core:flags`**: Centralized feature toggle system (`FeatureFlags`) controlling optional runtime capabilities.
- **`:core:speech`**: On-device speech recognition wrappers and audio processing utilities.

### Data Modules (`:data:*`)
- **`:data:db`**: Room database configuration, entities, and local DAOs.
- **`:data:trips`**: Repository layer and data models managing high-level trip records.
- **`:data:itinerary`**: Repository layer and data models managing daily itinerary events (`TimelineEvent`, `EventType`).

### Feature Modules (`:feature:*`)
- **`:feature:home`**: Dashboard screen displaying the user's upcoming trips list.
- **`:feature:create_trip`**: Unified trip form module handling both new trip creation and existing trip editing (`EditTripScreen`).
- **`:feature:detail`**: Detailed view screens for specific itinerary events (Flights, Hotels, Restaurants, Museums, Tours).
- **`:feature:appfunctions`**: Android AppFunctions framework integration (`JetPackerAppFunctionService`) contributing structured functions to Android's intelligence system for trips, expenses, itineraries, and voice notes.
- **`:feature:trip`**: Top-level trip container shell (`TripScreen`) holding bottom navigation and orchestrating trip sub-tabs.
  - **`:feature:trip:itinerary`**: Pure itinerary timeline screen displaying daily scheduled events.
    - **`:feature:trip:itinerary:enrichment`**: On-device AI summaries and tips (`TripSummaryAndTipsCard`) and dynamic daily theme generators.
  - **`:feature:trip:expenses`**: Expense tracking screen and automated receipt parser.
  - **`:feature:trip:voice_notes`**: Audio voice note recorder and real-time speech-to-text transcription screen.

## Getting Started

This project is built using the standard Android Gradle build system, allowing developers to quickly build, run, and experiment with the application locally using Android Studio.

### Configuration Setup

1. **Local Properties Setup**:
   - Navigate to the `android` directory and copy `local.properties.example` to `local.properties`:
     ```bash
     cd android
     cp local.properties.example local.properties
     ```
   - Update `sdk.dir` inside `local.properties` with your local Android SDK directory path.

2. **Firebase Setup (`google-services.json`)**:
   - Register the application in your Firebase Project Console.
   - Download the project's custom `google-services.json` and place it in the `android/app/` directory (overwriting the mock placeholder file).

3. **Firebase App Check Debug Attestation**:
   - Run the application on an emulator or a connected device.
   - Filter your logcat logs for `DebugAppCheckProvider`. You will see a log similar to:
     ```text
     Enter this debug secret into the allow list in the Firebase Console for your project: a8c2dd4c-7f7f-4764-b653-ef6c114ba27e
     ```
   - Copy the debug token and register it in the **Firebase Console** under **App Check** -> **Apps** -> **Manage Debug Tokens**.


### Building Using Gradle
To compile the application and run unit tests using Gradle, execute the following commands in your terminal (from the `android` directory):

```bash
# Navigate to the android directory
cd android

# Build a debug APK
./gradlew :app:assembleDebug

# Run unit tests
./gradlew test
```

## On-Device AI Features
JetPacker integrates local on-device AI capabilities using ML Kit. These features run entirely on-device and can be toggled or customized in `android/core/flags/src/main/kotlin/com/example/jetpacker/core/flags/FeatureFlags.kt`:
- **ENABLE_TRIP_SUMMARY_AND_TIPS**: Generates an on-device card summary of the current trip using ML Kit GenAI Prompt.
- **ENABLE_ITINERARY_ENRICHMENT**: Local enrichment for timeline events.
- **ENABLE_EXPENSE_MANAGEMENT**: Local expense management receipt scanner and parser.
- **ENABLE_VOICE_NOTES**: On-device speech recognition and transcription for voice notes.

## Cloud-Hybrid & Online AI Features
JetPacker also integrates online hybrid features using Firebase AI Logic (Gemini API) and ML Kit:
- **ENABLE_MUSEUM_ASSISTANT**: Museum Assistant chatbot with URL, Maps, and Search grounding (uses Gemini 2.5 flash-lite).
- **ENABLE_REVIEW_GENERATION**: Topic-selected review generator (uses Gemini 2.5 flash-lite on-device with cloud fallback).
- **Hotel Support Chat**: Receives hotel receptionist assistance with real-time ML Kit + Gemini translation.

## AppFunctions Integration
JetPacker uses Android's [**AppFunctions**](https://d.android.com/ai/appfunctions) library (`androidx.appfunctions`) in `:feature:appfunctions` to contribute to Android's intelligence system with structured travel actions and data queries via `JetPackerAppFunctionService`:
- **Trip Management**: `searchTrip` (filter by ID, name, location, and dates) and `createTrip`.
- **Itinerary Events**: `getItinerary` and `addItineraryEvent` (activity, dining, accommodation, etc.).
- **Expense Tracking**: `getExpenses` and `addExpense` (amount, currency, category).
- **Voice Notes**: `getVoiceNotes` and `addVoiceNote` (recorded transcriptions linked to trips).
- **Day Themes**: `getDayThemes` and `setDayTheme` for daily trip customization.


## IDE Setup & Development

To work on JetPacker locally, open the project in **Android Studio**:

1. Open Android Studio and select **Open an Existing Project** (or **File > Open**).
2. Select the `android` directory folder within the repository.
3. Android Studio will automatically sync Gradle and prepare the project.
