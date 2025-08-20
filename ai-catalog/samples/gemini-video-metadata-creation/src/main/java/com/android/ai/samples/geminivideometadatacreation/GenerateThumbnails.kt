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
import com.android.ai.samples.geminivideometadatacreation.ui.ErrorText
import com.android.ai.samples.geminivideometadatacreation.ui.ThumbnailsUi
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerateContentResponse
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.serialization.json.Json

// The configured model that includes the desired output format.
private val thumbnailsModel = Firebase.ai(backend = GenerativeBackend.vertexAI())
    .generativeModel(
        modelName = "gemini-2.5-flash",
        // Tell Firebase AI the exact format of the response.
        generationConfig {
            responseMimeType = "application/json"
            responseSchema = Schema.array(items = Schema.long("thumbnail timestamp in milliseconds"))
        },
    )

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
            return { ErrorText("The model returned invalid data. Debug info: ${e.message}") }
        }
    } else {
        // Failure - display an error text
        return {
            ErrorText(response.promptFeedback?.blockReasonMessage)
        }
    }
}
