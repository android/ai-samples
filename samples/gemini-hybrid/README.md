# Gemini Hybrid Sample

This sample is part of the [AI Sample Catalog](../../). To build and run this sample, you should clone the entire repository.

## Description

This sample demonstrates a hybrid approach to generative AI, utilizing both on-device (Gemini Nano via ML Kit) and cloud-based (Gemini via Firebase AI SDK) models. It showcases how to fallback to the cloud when on-device capabilities are unavailable or when more complex reasoning is required.

## How it works

The application first attempts to perform a task (e.g., summarization) using the on-device Gemini Nano model through the ML Kit GenAI API. If the model is not supported on the device or fails to download, it seamlessly falls back to the Gemini Flash model in the cloud using the Firebase AI SDK.

### Key Snippets

#### On-Device Inference (ML Kit)
```kotlin
val summarizer = Summarization.getClient(options)
val featureStatus = summarizer.checkFeatureStatus().await()
if (featureStatus == FeatureStatus.READY) {
    summarizer.runInference(request) { ... }.await()
}
```

#### Cloud Inference (Firebase AI)
```kotlin
val generativeModel = Firebase.ai.generativeModel("gemini-1.5-flash")
val response = generativeModel.generateContent(prompt)
```

Read more about [Gemini on Android](https://developer.android.com/ai/gemini) in the official documentation.
