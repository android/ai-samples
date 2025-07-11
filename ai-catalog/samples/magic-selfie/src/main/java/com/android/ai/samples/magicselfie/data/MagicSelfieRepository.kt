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
import android.graphics.Canvas
import android.graphics.Paint
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.ImagenAspectRatio
import com.google.firebase.ai.type.ImagenGenerationConfig
import com.google.firebase.ai.type.ImagenImageFormat
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.suspendCoroutine
import kotlin.math.roundToInt

@Singleton
class MagicSelfieRepository @Inject constructor() {
    @OptIn(PublicPreviewAPI::class)
    private val imagenModel = Firebase.ai(backend = GenerativeBackend.vertexAI()).imagenModel(
        modelName = "imagen-4.0-generate-preview-06-06",
        generationConfig = ImagenGenerationConfig(
            numberOfImages = 1,
            aspectRatio = ImagenAspectRatio.PORTRAIT_3x4,
            imageFormat = ImagenImageFormat.jpeg(compressionQuality = 75),
        ),
    )

    private val subjectSegmenter = SubjectSegmentation.getClient(
        SubjectSegmenterOptions.Builder()
            .enableForegroundBitmap()
            .build(),
    )

    suspend fun generateForegroundBitmap(bitmap: Bitmap): Bitmap {
        val image = InputImage.fromBitmap(bitmap, 0)
        return suspendCoroutine { continuation ->
            subjectSegmenter.process(image)
                .addOnSuccessListener {
                    it.foregroundBitmap?.let { foregroundBitmap ->
                        continuation.resumeWith(Result.success(foregroundBitmap))
                    }
                }
                .addOnFailureListener {
                    continuation.resumeWith(Result.failure(it))
                }
        }
    }

    @OptIn(PublicPreviewAPI::class)
    suspend fun generateBackground(prompt: String): Bitmap {
        val imageResponse = imagenModel.generateImages(
            prompt = prompt,
        )
        val image = imageResponse.images.first()
        return image.asBitmap()
    }

    fun combineBitmaps(foreground: Bitmap, background: Bitmap): Bitmap {
        val height = background.height
        val width = background.width

        val resultBitmap = Bitmap.createBitmap(width, height, background.config!!)
        val canvas = Canvas(resultBitmap)
        val paint = Paint()
        canvas.drawBitmap(background, 0f, 0f, paint)

        var foregroundHeight = foreground.height
        var foregroundWidth = foreground.width
        val ratio = foregroundWidth.toFloat() / foregroundHeight.toFloat()

        foregroundHeight = height
        foregroundWidth = (foregroundHeight * ratio).roundToInt()

        val scaledForeground = Bitmap.createScaledBitmap(foreground, foregroundWidth, foregroundHeight, false)

        val left = (width - scaledForeground.width) / 2f
        val top = (height - scaledForeground.height.toFloat())
        canvas.drawBitmap(scaledForeground, left, top, paint)

        return resultBitmap
    }
}
