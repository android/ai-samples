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
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class ImagenViewModel @Inject constructor() : ViewModel() {

    private val _imageGenerated: MutableStateFlow<Bitmap?> = MutableStateFlow(null)
    val imageGenerated: MutableStateFlow<Bitmap?> = _imageGenerated

    private val _isGenerating = MutableLiveData(false)
    val isGenerating: LiveData<Boolean> = _isGenerating

    @OptIn(PublicPreviewAPI::class)
    private val imagenModel = Firebase.ai(backend = GenerativeBackend.vertexAI()).imagenModel(
        modelName = "imagen-4.0-generate-preview-05-20",
        generationConfig = ImagenGenerationConfig(
            numberOfImages = 1,
            aspectRatio = ImagenAspectRatio.SQUARE_1x1,
            imageFormat = ImagenImageFormat.jpeg(compressionQuality = 75),
        ),
    )

    @OptIn(PublicPreviewAPI::class)
    fun generateImage(prompt: String) {
        _isGenerating.value = true
        viewModelScope.launch {
            val imageResponse = imagenModel.generateImages(
                prompt = prompt,
            )
            val image = imageResponse.images.first()

            val bitmapImage = image.asBitmap()
            _imageGenerated.value = bitmapImage
            _isGenerating.postValue(false)
        }
    }
}
