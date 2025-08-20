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
import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.ai.samples.geminivideometadatacreation.ui.ChaptersUi
import com.android.ai.samples.geminivideometadatacreation.ui.ErrorText
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.PromptFeedback
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

typealias Chapters = List<Chapter>

@Serializable
data class Chapter(
    val timestamp: Long,
    val title: String,
)

// The configured model that includes the desired output format.
private val chaptersModel = Firebase.ai(backend = GenerativeBackend.vertexAI())
    .generativeModel(
        modelName = "gemini-2.5-flash",
        // Tell Firebase AI the exact format of the response.
        generationConfig {
            responseMimeType = "application/json"
            responseSchema = Schema.array(
                items = Schema.obj(
                    mapOf(
                        "timestamp" to Schema.long("chapter start in milliseconds"),
                        "title" to Schema.string(),
                    ),
                ),
            )
        },
    )

/**
 * Generates chapters for a given video URI.
 *
 * This function sends a request to a generative AI model to analyze the
 * video and create a list of chapters. The model is instructed to generate
 * around 3-7 chapters, each with a timestamp and a descriptive title
 * (max 3 words). Chapters should be at least 10 seconds long and evenly
 * distributed throughout the video.
 *
 * The function returns a Composable function that will either display the
 * generated chapters in a UI or show an error message if the generation
 * failed.
 *
 * @param videoUri The URI of the video to generate chapters for.
 * @param onChapterClicked A callback function that is invoked when a chapter
 *                         is clicked, providing the timestamp of the
 *                         clicked chapter.
 * @return A Composable function that displays either the chapters UI or an
 *         error message.
 */
suspend fun generateChapters(videoUri: Uri, onChapterClicked: (timestamp: Long) -> Unit): @Composable () -> Unit {

    // Execute the model call with our custom prompt
    var promptFeedback: PromptFeedback? = null
    val outputStringBuilder = StringBuilder()
    chaptersModel.generateContentStream(
        content {
            fileData(videoUri.toString(), "video/mp4")
            text(
                """
                        Analyze the video and create a list of around 3-7 chapters with timestamps and descriptive titles (of max 3 words).
                        Each chapter should be at least 10 seconds long.
                        Make sure to evenly divide the chapters over the video.
                """.trimIndent(),
            )
        },
    ).collect { response ->
        if (response.promptFeedback != null) promptFeedback = response.promptFeedback
        outputStringBuilder.append(response.text)
    }
    val responseText = outputStringBuilder.toString()
    if (responseText.isNotEmpty()) {
        // Successful response - parse the JSON and display the chapters
        val chapters: Chapters =
            Json.decodeFromString<Chapters>(responseText)
        return { ChaptersUi(chapters, onChapterClicked) }
    } else {
        // Failure - display an error text
        return {
            ErrorText(promptFeedback?.blockReasonMessage,)
        }
    }
}
