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

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerateContentResponse
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.serialization.json.Json

// The configured model that includes the desired output format.
private val linksModel = Firebase.ai(backend = GenerativeBackend.vertexAI())
    .generativeModel(
        modelName = "gemini-2.5-flash",
        // Tell Firebase AI the exact format of the response.
        generationConfig {
            responseMimeType = "application/json"
            responseSchema = Schema.array(items = Schema.string("Link"))
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
            return { ErrorText("The model returned invalid data. Debug info: ${e.message}") }
        }
    } else {
        // Failure - display an error text
        return {
            ErrorText(response.promptFeedback?.blockReasonMessage)
        }
    }
}

@Composable
private fun LinksUi(links: List<String>) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        links.forEach { link ->
            Text(
                text = link,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable {
                    val browserIntent = Intent(Intent.ACTION_VIEW, link.toUri())
                    context.startActivity(browserIntent)
                },
            )
        }
    }
}

@Composable
private fun ErrorText(blockReasonMessage: String?) {
    Text(
        """
            There was a problem generating the description. Here is some information that might help you debug:
            Block reason message: $blockReasonMessage
        """.trimIndent(),
        color = MaterialTheme.colorScheme.error,
    )
}
