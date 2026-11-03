# Agent & Developer Guidelines: JetPacker

This document outlines key technical constraints, coding standards, and testing procedures for AI agents and human developers working in the JetPacker repository.

---

## 1. Code Style & Syntax Invariants

### 1.1 Avoid Fully Qualified Class Names (FQCN)
When writing or modifying Kotlin, Java, or Compose files:
- **Always Use Top-Level Imports**: Never write inline Fully Qualified Class Names (e.g., `com.example.jetpacker.core.flags.FeatureFlags`, `java.time.Instant`, `androidx.compose.ui.Modifier`) in function bodies, parameter lists, or property definitions.
- **Explicit File-Level Imports**: Add explicit `import` statements at the top of the file for all referenced types, classes, and utilities.

### 1.2 Jetpack Compose Conventions
- **Modifier Placement**: Every composable function (except top-level screen containers) must accept `modifier: Modifier = Modifier` as the first optional parameter.
- **Unidirectional Data Flow (UDF)**: ViewModels expose a single immutable `uiState: StateFlow<ScreenState>`. Composables accept states and emit lambda event callbacks (e.g., `onTripSelected: (String) -> Unit`).
- **No Direct Context References in ViewModels**: ViewModels should remain agnostic of Android UI lifecycles. Never pass `Activity` or `Context` into ViewModel methods. If `Application` is required, inject `Application` directly via constructor.

### 1.3 Hilt Dependency Injection Rules
- **No `@ApplicationContext` on `Application`**: In Hilt, `Application` is provided directly by default. Annotating constructor parameter `application: Application` with `@ApplicationContext` will cause a Dagger compiler failure (`[Dagger/MissingBinding]`). Only use `@ApplicationContext` when the parameter type is `Context`.

---

## 2. Room Database & Persistence Rules

- **Database Class**: `JetPackerDatabase` in `:data:db` (current version: 4).
- **Entities & DAOs**:
  - `Trip` & `TripDao` in `:data:trips`.
  - `TimelineEvent`, `Expense`, `TravelDocument`, `DayTheme`, `VoiceNoteEntity`, `AssistantConversation` in `:data:itinerary`.
- **Interface Parity**: When updating a DAO interface, immediately update corresponding test fakes (`FakeTripDao`, `TestTripDao`, `FakeEventDao`). Never add methods to test fakes that do not exist on the real DAO interface.
- **Schema Updates**: Any entity modification requires bumping the `version` number in `JetPackerDatabase.kt` and providing an appropriate `Migration` or fallback policy.

---

## 3. AI & On-Device Agent Constraints

### 3.1 ML Kit GenAI Agent Integration
- JetPacker uses the public Android ML Kit GenAI Agent library (`com.google.mlkit:genai-agent:1.0.0-alpha1`).
- Always use standard public Android AI dependencies.
- Use `AgentRunner(context, agent, generativeModel = previewGenerativeModel)` with `agentBuilder.enableAppFunctions(true)`.

### 3.2 Gemini Nano v4 Preview
- Configured using `ModelReleaseStage.PREVIEW` and `ModelPreference.FAST`.
- Always check on-device availability via `previewGenerativeModel.checkStatus()` and handle download / unsupported states before invoking inference.

### 3.3 Token Pruning for On-Device Tools
- On-device models have strict context window budgets. When defining tool descriptions or schemas in `TravelDaoTools.kt`, keep descriptions compact and concise to prevent exhausting token limits.

---

## 4. Build & Test Recipes

### 4.1 Running Tests
```bash
# Run all unit tests across all modules
cd jetpacker/android && ./gradlew testDebugUnitTest

# Run tests for a specific module
./gradlew :feature:assistant:testDebugUnitTest
./gradlew :data:trips:testDebugUnitTest
./gradlew :feature:trip:booking_assistant:testDebugUnitTest

# Assemble Debug APK
./gradlew :app:assembleDebug
```

### 4.2 Handling Incremental Build Quirks
If KSP or Kotlin compiler fails with unexpected unresolved symbols after switching branches or modifying interfaces:
```bash
./gradlew clean <affected_module>:compileDebugKotlin
```

---

## 5. Python Booking Assistant Server (`jetpacker/server`)

The booking assistant backend coordinates agents for flights, hotels, and activities:
```bash
cd jetpacker/server
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8000
```
- **Staging vs Production**: Configurable in `BookingAssistantViewModel.kt` (`PARALLEL_STAGING` connects to Cloud Run `/v2`, `LOCAL` connects to `localhost:8000`).
