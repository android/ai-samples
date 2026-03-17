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
package com.android.ai.samples.magicselfie.data

import android.graphics.Bitmap
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.asImageOrNull
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MagicSelfieRepository @Inject constructor() {
    private val generativeModel = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
        modelName = "gemini-3.1-flash-image-preview",
        generationConfig = generationConfig {
            responseModalities = listOf(ResponseModality.IMAGE)
        },
        systemInstruction = content {
            text("In the provided image, replace the background with the one described in the prompt. Keep the person in the foreground exactly as they are.")
        },
    )

    suspend fun generateMagicSelfie(bitmap: Bitmap, prompt: String): Bitmap {
        val content = content {
            image(bitmap)
            text(prompt)
        }
        val response = generativeModel.generateContent(content)
        val resultImage = response.candidates.firstOrNull()?.content?.parts?.firstNotNullOfOrNull { it.asImageOrNull() }
        return resultImage ?: throw Exception("Failed to generate magic selfie")
    }
}
