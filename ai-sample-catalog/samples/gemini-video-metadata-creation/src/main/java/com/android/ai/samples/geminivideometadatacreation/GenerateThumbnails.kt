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

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import com.android.ai.samples.geminivideometadatacreation.player.extractListOfThumbnails
import com.android.ai.samples.geminivideometadatacreation.ui.ErrorUi
import com.android.ai.samples.geminivideometadatacreation.ui.ThumbnailsUi
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerateContentResponse
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.serialization.json.Json

/**
 * Defines the schema for the expected response from the generative model.
 * The model is expected to return a JSON array of long integers,
 * where each long represents a timestamp in milliseconds for a suggested thumbnail.
 */
private val thumbnailsSchema = Schema.array(items = Schema.long("thumbnail timestamp in milliseconds"))

/**
 * Initializes the generative model for thumbnail generation.
 * This model uses the "gemini-2.5-flash" model via the Vertex AI backend.
 * It's configured to expect a JSON response with a specific schema (`thumbnailsSchema`)
 * defining an array of long integers representing thumbnail timestamps.
 */
private val thumbnailsModel = Firebase.ai(backend = GenerativeBackend.vertexAI())
    .generativeModel(
        modelName = "gemini-2.5-flash",
        // Tell Firebase AI the exact format of the response.
        generationConfig {
            responseMimeType = "application/json"
            responseSchema = thumbnailsSchema
        },
    )

/**
 * Generates thumbnail suggestions for a given video using a generative model.
 *
 * This function sends a request to the `thumbnailsModel` with the video URI and a prompt
 * asking for three engaging and visually appealing thumbnails. The model is expected to
 * return a JSON array of timestamps (in milliseconds) corresponding to suggested thumbnail frames.
 *
 * If the model returns a valid response:
 *  - The JSON response is parsed to extract the list of timestamps.
 *  - `extractListOfThumbnails` is called to generate bitmap images for these timestamps.
 *  - A Composable function `ThumbnailsUi` is returned, which will display the generated thumbnails.
 *
 * If the model returns an error or invalid data:
 *  - An `ErrorUi` Composable is returned, displaying an appropriate error message.
 *
 * @param videoUri The URI of the video for which to generate thumbnails.
 * @param context The Android Context, used for accessing video frames.
 * @return A Composable function that will either display the generated thumbnails or an error message.
 */
suspend fun generateThumbnails(videoUri: Uri, context: Context): @Composable () -> Unit {
    // Execute the model call with our custom prompt
    val response: GenerateContentResponse = thumbnailsModel
        .generateContent(
            content {
                fileData(videoUri.toString(), "video/mp4")
                text(
                    """
                    Get three engaging and visually appealing thumbnails for this video.
                    Focus on capturing peak moments that create curiosity.
                    """.trimIndent(),
                )
            },
        )

    val responseText = response.text
    if (responseText != null) {
        // Successful response - parse the JSON and download the thumbnails asynchronously
        try {
            val thumbnails: List<Long> = Json.decodeFromString(responseText)
            val thumbnailBitmaps = extractListOfThumbnails(context, videoUri, thumbnails)
            return { ThumbnailsUi(thumbnails, thumbnailBitmaps) }
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
