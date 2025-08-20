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
import com.android.ai.samples.geminivideometadatacreation.ui.LinksUi
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerateContentResponse
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.serialization.json.Json

/**
 * Schema defining the expected output format for the links generation.
 * It specifies that the model should return a JSON array of strings,
 * where each string represents a "Link".
 */
private val linksSchema = Schema.array(items = Schema.string("Link"))

/**
 * The configured generative model for extracting links.
 *
 * This model is specifically configured to:
 * - Use the "gemini-2.5-flash" model.
 * - Expect a response in "application/json" format.
 * - Adhere to the `linksSchema`, which defines the expected JSON structure as an array of strings (links).
 */
private val linksModel = Firebase.ai(backend = GenerativeBackend.vertexAI())
    .generativeModel(
        modelName = "gemini-2.5-flash",
        // Tell Firebase AI the exact format of the response.
        generationConfig {
            responseMimeType = "application/json"
            responseSchema = linksSchema
        },
    )

/**
 * Analyzes a video and generates a list of relevant links to be tagged.
 *
 * This function sends the video to a generative AI model with a prompt to extract relevant links.
 * It expects the model to return a JSON array of strings, where each string is a URL.
 *
 * @param videoUri The URI of the video to be analyzed.
 * @return A Composable function that will either display the list of generated links using [LinksUi]
 *         or an error message using [ErrorText] if the model call fails or returns invalid data.
 */
suspend fun generateLinks(videoUri: Uri): @Composable () -> Unit {
    // Execute the model call with our custom prompt
    val response: GenerateContentResponse = linksModel
        .generateContent(
            content {
                fileData(videoUri.toString(), "video/mp4")
                text(
                    """
                        Analyze the video and create a list of relevant links to be tagged. 
                        Return possible 3-4 links to be shared in the video.
                    """.trimIndent(),
                )
            },
        )

    val responseText = response.text
    if (responseText != null) {
        // Successful response - parse the JSON and display the links
        try {
            val links: List<String> = Json.decodeFromString(responseText)
            return { LinksUi(links) }
        } catch (e: Exception) {
            return { ErrorUi("The model returned invalid data. Debug info: ${e.message}") }
        }
    } else {
        // Failure - display an error text
        return {
            ErrorUi(response.promptFeedback?.blockReasonMessage)
        }
    }
}
