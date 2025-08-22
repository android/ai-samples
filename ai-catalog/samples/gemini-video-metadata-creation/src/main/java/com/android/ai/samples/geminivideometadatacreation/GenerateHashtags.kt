/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.ai.samples.geminivideometadatacreation

import android.net.Uri
import androidx.compose.runtime.Composable
import com.android.ai.samples.geminivideometadatacreation.ui.ErrorUi
import com.android.ai.samples.geminivideometadatacreation.ui.HashtagsUi
import com.example.firebase_ai.generativeModelForList
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerateContentResponse
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.serialization.json.Json

/**
 * A generative model instance configured to generate hashtags for video content.
 *
 * This model uses the "gemini-2.5-flash" model from Vertex AI and is specifically configured
 * to return a JSON array of strings, where each string represents a hashtag.
 * The `responseMimeType` is set to "application/json" and the `responseSchema` defines
 * the expected output format as an array of strings with the item name "Hashtag".
 */
private val hashtagsModel = Firebase.ai(backend = GenerativeBackend.vertexAI())
    .generativeModelForList<String>(modelName = "gemini-2.5-flash")

/**
 * Generates a list of relevant and trending hashtags for the given video URI.
 *
 * This function uses a pre-configured generative model to analyze the video content and
 * suggest hashtags that can maximize its visibility on social media platforms.
 *
 * @param videoUri The URI of the video for which to generate hashtags.
 * @return A Composable function that will either display a [HashtagsUi] with the generated hashtags
 *         or an [ErrorText] if the generation fails.
 */
suspend fun generateHashtags(videoUri: Uri): @Composable () -> Unit {
    // Execute the model call with our custom prompt
    val response = hashtagsModel
        .generateContent(
            content {
                fileData(videoUri.toString(), "video/mp4")
                text(
                    """
                        Generate a list of relevant and trending hashtags for this video to 
                        maximize its visibility on social media platforms.
                    """.trimIndent(),
                )
            },
        )

    val hashtags = response.typedContent
    if (hashtags != null) {
        return { HashtagsUi(hashtags) }
    } else {
        // Failure - display an error text
        return { ErrorUi(response.promptFeedback?.blockReasonMessage) }
    }
}
