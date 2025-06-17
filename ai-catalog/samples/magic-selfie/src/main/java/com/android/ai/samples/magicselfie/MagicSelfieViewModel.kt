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
package com.android.ai.samples.magicselfie

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@OptIn(PublicPreviewAPI::class)
class MagicSelfieViewModel @Inject constructor() : ViewModel() {

    private val _foregroundBitmap = MutableStateFlow<Bitmap?>(null)
    val foregroundBitmap: MutableStateFlow<Bitmap?> = _foregroundBitmap

    private val _progress = MutableLiveData<String?>(null)
    val progress: LiveData<String?> = _progress

    private val imagenModel = Firebase.ai(backend = GenerativeBackend.vertexAI()).imagenModel(
        modelName = "imagen-4.0-generate-preview-05-20",
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

    fun createMagicSelfie(bitmap: Bitmap, prompt: String) {
        val image = InputImage.fromBitmap(bitmap, 0)

        _progress.value = "Removing selfie background..."

        subjectSegmenter.process(image)
            .addOnSuccessListener {
                it.foregroundBitmap?.let {
                    _foregroundBitmap.value = it
                    generateBackground(prompt)
                }
            }.addOnFailureListener {
                _progress.postValue("Something went wrong :(")
            }
    }

    private fun generateBackground(prompt: String) {
        _progress.value = "Generating new background..."

        viewModelScope.launch {
            val imageResponse = imagenModel.generateImages(
                prompt = prompt,
            )
            val image = imageResponse.images.first()

            val bitmapImage = image.asBitmap()

            _foregroundBitmap.value = combineBitmaps(_foregroundBitmap.value!!, bitmapImage)
            _progress.postValue(null)
        }
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
