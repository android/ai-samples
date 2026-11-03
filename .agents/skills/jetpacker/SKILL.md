---
name: jetpacker
description: >-
  Comprehensive guide for developing, building, testing, architecting, and debugging
  the JetPacker Android travel companion application and its Python booking backend.
  Use when compiling or running unit tests with Gradle, inspecting or updating Room database
  schemas, editing ViewModels and Compose screens, configuring on-device ML Kit / Gemini Nano,
  working with Jetpack A2UI booking flows, or adding AppFunctions.
---

# JetPacker Development & Architecture Skill

This skill provides step-by-step procedures, architectural overviews, and build/test recipes for working with JetPacker.

---

## 1. Quick Navigation & Module Layout

JetPacker is organized into an Android multi-module project under `jetpacker/android/` and a Python backend under `jetpacker/server/`.

- **Application Shell**:
  - `jetpacker/android/app` (`:app`): Top-level navigation host, Hilt graph, theme.
- **Feature Modules**:
  - `jetpacker/android/feature/home` (`:feature:home`): Upcoming trips list, quick actions.
  - `jetpacker/android/feature/create_trip` (`:feature:create_trip`): Create/edit trip destination and date picker.
  - `jetpacker/android/feature/trip` (`:feature:trip`): Trip dashboard and bottom bar container.
  - `jetpacker/android/feature/trip/itinerary` (`:feature:trip:itinerary`): Daily timeline events & summaries.
  - `jetpacker/android/feature/trip/expenses` (`:feature:trip:expenses`): Budgeting, expense logging, receipt OCR.
  - `jetpacker/android/feature/trip/voice_notes` (`:feature:trip:voice_notes`): On-device speech recognition audio recorder.
  - `jetpacker/android/feature/trip/booking_assistant` (`:feature:trip:booking_assistant`): Jetpack A2UI dynamic card streaming UI.
  - `jetpacker/android/feature/assistant` (`:feature:assistant`): On-device Gemini Nano v4 Travel Assistant, Document Vault, and Chat History.
  - `jetpacker/android/feature/detail` (`:feature:detail`): Detail screens (Museum Assistant, AI Reviews, Hotel Support Chat).
  - `jetpacker/android/feature/appfunctions` (`:feature:appfunctions`): Android system AppFunctions service.
- **Data & Core Modules**:
  - `jetpacker/android/data/db` (`:data:db`): Room database v4 (`JetPackerDatabase`), DAOs, Hilt module.
  - `jetpacker/android/data/trips` (`:data:trips`): Trip repository, entities, and Flow streams.
  - `jetpacker/android/data/itinerary` (`:data:itinerary`): Itinerary events, expenses, documents, and DAOs.
  - `jetpacker/android/core/ui` (`:core:ui`): Design system, toolbar, FAB, tokens.
  - `jetpacker/android/core/flags` (`:core:flags`): Feature toggles (`FeatureFlags`).

---

## 2. Build & Test Recipes

Execute Gradle commands from the `jetpacker/android` directory:

### Run All Unit Tests
```bash
cd jetpacker/android
./gradlew testDebugUnitTest
```

### Run Targeted Module Tests
```bash
# Travel Assistant & Document Vault
./gradlew :feature:assistant:testDebugUnitTest

# Trips Repository & DAOs
./gradlew :data:trips:testDebugUnitTest

# Booking Assistant & A2UI
./gradlew :feature:trip:booking_assistant:testDebugUnitTest

# AppFunctions
./gradlew :feature:appfunctions:testDebugUnitTest
```

### Build APK
```bash
./gradlew :app:assembleDebug
```

### Fix Stale Compiler / KSP Caches
If encountering unresolved references after switching branches or updating interfaces:
```bash
./gradlew clean :feature:assistant:compileDebugKotlin
```

---

## 3. Database Updates & Room Guidelines

- **Database Class**: `JetPackerDatabase.kt` (`jetpacker/android/data/db/src/main/kotlin/com/example/jetpacker/data/db/JetPackerDatabase.kt`).
- When modifying an entity:
  1. Increment `version = N + 1` in `@Database(...)`.
  2. If adding fields, provide appropriate default values or migration steps.
  3. Ensure test fake DAOs (`FakeTripDao`, `TestTripDao`, `FakeEventDao`) match the interface in `data/trips` and `data/itinerary` exactly.

---

## 4. On-Device AI: Gemini Nano & Travel Assistant

The on-device assistant lives in `TravelAssistantViewModel.kt` (`jetpacker/android/feature/assistant/src/main/kotlin/com/example/jetpacker/feature/assistant/TravelAssistantViewModel.kt`).

- **Model Setup**:
  Uses `previewGenerativeModel` configured with `ModelReleaseStage.PREVIEW` and `ModelPreference.FAST`.
- **Dynamic Tool Registration**:
  Tools in `TravelDaoTools.kt` (`jetpacker/android/feature/assistant/src/main/kotlin/com/example/jetpacker/feature/assistant/tools/TravelDaoTools.kt`) query the local Room database (`TripDao`).
- **Token Pruning Invariant**: Keep tool descriptions and schema payloads concise to stay well within on-device context windows (~1,200 tokens saved per turn).
- **Public API Rule**: Always use the public Android ML Kit GenAI Agent library (`com.google.mlkit:genai-agent:1.0.0-alpha1`).

---

## 5. Booking Assistant & Jetpack A2UI

- **UI Renderer**: Uses `androidx.a2ui` Compose renderer to dynamically render flight, hotel, and activity cards streamed from the backend.
- **Backend Service**:
  ```bash
  cd jetpacker/server
  pip install -r requirements.txt
  uvicorn main:app --host 0.0.0.0 --port 8000
  ```
- **Reverse Port for Local Emulation**:
  ```bash
  adb reverse tcp:8000 tcp:8000
  ```
- Switch between `BackendEndpoint.LOCAL`, `PARALLEL_STAGING`, and `PRODUCTION` in `BookingAssistantViewModel.kt` (`jetpacker/android/feature/trip/booking_assistant/src/main/kotlin/com/example/jetpacker/feature/booking_assistant/BookingAssistantViewModel.kt`).

---

## 6. AppFunctions Integration (`:feature:appfunctions`)

Exposes travel data and actions to Android system intelligence:
- Entry point: `JetPackerAppFunctionService.kt` (`jetpacker/android/feature/appfunctions/src/main/java/com/example/jetpacker/feature/appfunctions/JetPackerAppFunctionService.kt`).
- Annotated with `@AppFunctionService`.
- Functions must use `@AppFunctionSerializable` inputs/outputs.
