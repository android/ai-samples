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
import com.android.ai.samples.geminivideometadatacreation.ui.ErrorText
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerateContentResponse
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

typealias AccountTags = List<AccountTag>

@Serializable
data class AccountTag(
    val tag: String,
    val url: String,
)

// The configured model that includes the desired output format.
private val accountTagsModel = Firebase.ai(backend = GenerativeBackend.vertexAI())
    .generativeModel(
        modelName = "gemini-2.5-flash",
        // Tell Firebase AI the exact format of the response.
        generationConfig {
            responseMimeType = "application/json"
            responseSchema = Schema.array(
                items = Schema.obj(
                    mapOf(
                        "tag" to Schema.string(),
                        "url" to Schema.string("The YouTube profile url for this account"),
                    ),
                ),
            )
        },
    )

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
    val response: GenerateContentResponse = accountTagsModel
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

    val responseText = response.text
    if (responseText != null) {
        // Successful response - parse the JSON and display the accountTags
        val accountTags: AccountTags =
            Json.decodeFromString<AccountTags>(responseText)
        return { AccountTagsUi(accountTags) }
    } else {
        // Failure - display an error text
        return {
            ErrorText(response.promptFeedback?.blockReasonMessage)
        }
    }
}
