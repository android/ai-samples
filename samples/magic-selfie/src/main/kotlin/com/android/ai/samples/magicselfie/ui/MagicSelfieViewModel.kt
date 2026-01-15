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
package com.android.ai.samples.magicselfie.ui

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.ai.samples.magicselfie.data.MagicSelfieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class MagicSelfieViewModel @Inject constructor(private val magicSelfieRepository: MagicSelfieRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<MagicSelfieUiState>(MagicSelfieUiState.Initial)
    val uiState: StateFlow<MagicSelfieUiState> = _uiState

    fun createMagicSelfie(bitmap: Bitmap, prompt: String) {
        viewModelScope.launch {
            try {
                _uiState.value = MagicSelfieUiState.RemovingBackground
                val foregroundBitmap = magicSelfieRepository.generateForegroundBitmap(bitmap)
                _uiState.value = MagicSelfieUiState.GeneratingBackground
                val backgroundBitmap = magicSelfieRepository.generateBackground(prompt)
                val resultBitmap = magicSelfieRepository.combineBitmaps(foregroundBitmap, backgroundBitmap)
                _uiState.value = MagicSelfieUiState.Success(resultBitmap)
            } catch (e: Exception) {
                _uiState.value = MagicSelfieUiState.Error(e.message)
            }
        }
    }

    fun resetError() {
        _uiState.value = MagicSelfieUiState.Initial
    }
}
