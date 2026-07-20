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
package com.android.ai.samples.nanobanana.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.ai.samples.nanobanana.data.NanobananaDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class NanobananaViewModel @Inject constructor(private val nanobananaDataSource: NanobananaDataSource) : ViewModel() {

    private val _uiState: MutableStateFlow<NanobananaUIState> = MutableStateFlow(NanobananaUIState.Initial)
    val uiState: StateFlow<NanobananaUIState> = _uiState

    fun generateImage(prompt: String) {
        _uiState.value = NanobananaUIState.Loading

        viewModelScope.launch {
            try {
                val bitmap = nanobananaDataSource.generateImage(prompt)
                _uiState.value = NanobananaUIState.ImageGenerated(bitmap, contentDescription = prompt)
            } catch (e: Exception) {
                _uiState.value = NanobananaUIState.Error(e.message)
            }
        }
    }
}
