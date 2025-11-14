# Gemini Video Metadata Creation Sample

This sample is part of the [AI Sample Catalog](../../). To build and run this sample, you should clone the entire repository.

## Description

This sample demonstrates how to generate various types of video metadata (description, hashtags, chapters, account tags, links, and thumbnails) using Gemini Flash. Users can select a video, and the generative model will analyze its content to provide relevant metadata, showcasing how to enrich video content with AI-powered insights.

<div style="text-align: center;">
<img width="320" alt="Gemini Video Metadata Creation in action" src="gemini_video_metadata.png" />
</div>

## How it works

The application uses the Firebase AI SDK (see [How to run](../../#how-to-run)) for Android to interact with Gemini Flash. The core logic involves several functions (e.g., [`generateDescription`](./src/main/java/com/android/ai/samples/geminivideometadatacreation/GenerateDescription.kt), `generateHashtags`, `generateChapters`, `generateAccountTags`, `generateLinks`, `generateThumbnails`) that send video content to the Gemini API for analysis. The model processes the video and returns structured metadata based on the specific prompt.

Here is a key snippet of code that generates a video description:

```kotlin
suspend fun generateDescription(videoUri: Uri): @Composable () -> Unit {
    val response = Firebase.ai(backend = GenerativeBackend.vertexAI())
        .generativeModel(modelName = "gemini-2.5-flash")
        .generateContent(
            content {
                fileData(videoUri.toString(), "video/mp4")
                text(
                    """
                    Provide a compelling and concise description for this video in less than 100 words.
                    Don't assume if you don't know.
                    The description should be engaging and accurately reflect the video's content.
                    You should output your responses in HTML format. Use styling sparingly. You can use the following tags:
                    * Bold: <b>
                    * Italic: <i>
                    * Underline: <u>
                    * Bullet points: <ul>, <li>
                    """.trimIndent(),
                )
            },
        )

    val responseText = response.text
    return if (responseText != null) {
        { DescriptionUi(responseText) }
    } else {
        { ErrorUi(response.promptFeedback?.blockReasonMessage) }
    }
}
```

Read more about [the Gemini API](https://developer.android.com/ai/gemini) in the Android Documentation.
