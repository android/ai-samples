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
package com.android.ai.samples.imagen

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.ImagenAspectRatio
import com.google.firebase.ai.type.ImagenGenerationConfig
import com.google.firebase.ai.type.ImagenImageFormat
import com.google.firebase.ai.type.PublicPreviewAPI
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface ImagenUIState {
    data object Initial : ImagenUIState
    data object Loading : ImagenUIState
    data class ImageGenerated(
        val bitmap: Bitmap,
        val contentDescription: String,
    ) : ImagenUIState
    data class Error(val message: String) : ImagenUIState
}

class ImagenViewModel @Inject constructor() : ViewModel() {

    private val _uiState: MutableStateFlow<ImagenUIState> = MutableStateFlow(ImagenUIState.Initial)
    val uiState: StateFlow<ImagenUIState> = _uiState

    @OptIn(PublicPreviewAPI::class)
    private val imagenModel = Firebase.ai(backend = GenerativeBackend.vertexAI()).imagenModel(
        modelName = "imagen-4.0-generate-preview-06-06",
        generationConfig = ImagenGenerationConfig(
            numberOfImages = 1,
            aspectRatio = ImagenAspectRatio.SQUARE_1x1,
            imageFormat = ImagenImageFormat.jpeg(compressionQuality = 75),
        ),
    )

    @OptIn(PublicPreviewAPI::class)
    fun generateImage(prompt: String) {
        _uiState.value = ImagenUIState.Loading

        viewModelScope.launch {
            try {
                val imageResponse = imagenModel.generateImages(
                    prompt = prompt,
                )
                val image = imageResponse.images.first()

                val bitmapImage = image.asBitmap()
                _uiState.value = ImagenUIState.ImageGenerated(bitmapImage, contentDescription = prompt)
            } catch (e: Exception) {
                _uiState.value = ImagenUIState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
