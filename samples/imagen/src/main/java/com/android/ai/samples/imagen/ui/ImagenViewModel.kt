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
package com.android.ai.samples.imagen.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.ai.samples.imagen.data.ImagenDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ImagenViewModel @Inject constructor(private val imagenDataSource: ImagenDataSource) : ViewModel() {

    private val _uiState: MutableStateFlow<ImagenUIState> = MutableStateFlow(ImagenUIState.Initial)
    val uiState: StateFlow<ImagenUIState> = _uiState

    fun generateImage(prompt: String) {
        _uiState.value = ImagenUIState.Loading

        viewModelScope.launch {
            try {
                val bitmap = imagenDataSource.generateImage(prompt)
                _uiState.value = ImagenUIState.ImageGenerated(bitmap, contentDescription = prompt)
            } catch (e: Exception) {
                _uiState.value = ImagenUIState.Error(e.message)
            }
        }
    }
}
