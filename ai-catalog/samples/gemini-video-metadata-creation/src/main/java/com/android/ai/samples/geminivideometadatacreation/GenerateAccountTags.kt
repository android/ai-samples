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
import com.android.ai.samples.geminivideometadatacreation.ui.AccountTagsUi
import com.android.ai.samples.geminivideometadatacreation.ui.ErrorUi
import com.example.annotations.Generable
import com.example.firebase_ai.generativeModelForList
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerateContentResponse
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Generable
@Serializable
data class AccountTag(
    val tag: String,
    val url: String,
)


/**
 * A generative model instance configured to interact with the Vertex AI Gemini API
 * for generating account tags.
 *
 * This model is specifically set up with:
 * - `modelName = "gemini-2.5-flash"`: Specifies the underlying Gemini model to use.
 * - `responseMimeType = "application/json"`:  Indicates that the model is expected to
 *   return its response in JSON format.
 * - `responseSchema = accountTagsSchema`: Defines the expected structure of the JSON
 *   response. This ensures that the output can be reliably parsed into a list of
 *   `AccountTag` objects.
 *
 * This configuration allows for structured data extraction from the model's output,
 * making it easier to integrate the generated tags into the application.
 */
private val accountTagsModel = Firebase.ai(backend = GenerativeBackend.vertexAI())
    .generativeModelForList<AccountTag>(modelName = "gemini-2.5-flash")

/**
 * Calls the Vertex AI Gemini API to generate relevant account tags for the given video.
 * The Gemini API analyzes the video content and the provided prompt to suggest accounts
 * that would be suitable for tagging in the video's description or comments.
 * This is intended to help increase the video's reach and engagement.
 *
 * @param videoUri The URI of the video to generate tags for.
 * @return A composable function that displays the generated tags or an error message.
 */
suspend fun generateAccountTags(videoUri: Uri): @Composable () -> Unit {
    // Execute the model call with our custom prompt
    val response = accountTagsModel
        .generateContent(
            content {
                fileData(videoUri.toString(), "video/mp4")
                text(
                    """
                        Suggest relevant accounts to tag in the video's description or comments to 
                        increase its reach and engagement.
                    """.trimIndent(),
                )
            },
        )

    val accountTags = response.typedContent
    if (accountTags != null) {
        return { AccountTagsUi(accountTags) }
    } else {
        // Failure - display an error text
        return {
            ErrorUi(response.promptFeedback?.blockReasonMessage)
        }
    }
}
