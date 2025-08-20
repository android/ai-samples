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
import com.android.ai.samples.geminivideometadatacreation.ui.DescriptionUi
import com.android.ai.samples.geminivideometadatacreation.ui.ErrorText
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content

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
                    The description should be engaging and accurately reflect the video\'s content.
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
        { ErrorText(response.promptFeedback?.blockReasonMessage) }
    }
}
