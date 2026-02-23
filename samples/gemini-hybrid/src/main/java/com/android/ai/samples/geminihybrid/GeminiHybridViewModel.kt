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
package com.android.ai.samples.geminihybrid

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.ai.InferenceMode
import com.google.firebase.ai.InferenceSource
import com.google.firebase.ai.OnDeviceConfig
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.PublicPreviewAPI
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface GeminiHybridUiState {
    data object Initial : GeminiHybridUiState
    data object CheckingOnDeviceStatus : GeminiHybridUiState
    data class Generating(
        val isCloud: Boolean,
        val partialOutput: String = "",
        val isTranslation: Boolean = false
    ) : GeminiHybridUiState

    data class Success(
        val output: String,
        val isCloud: Boolean,
        val isTranslation: Boolean = false
    ) : GeminiHybridUiState

    data class Error(val message: String) : GeminiHybridUiState
}

@PublicPreviewAPI
@HiltViewModel
class GeminiHybridViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow<GeminiHybridUiState>(GeminiHybridUiState.Initial)
    val uiState: StateFlow<GeminiHybridUiState> = _uiState.asStateFlow()

    private val _inferenceMode = MutableStateFlow(InferenceMode.ONLY_ON_DEVICE)
    val inferenceMode: StateFlow<InferenceMode> = _inferenceMode.asStateFlow()

    private val _selectedTags = MutableStateFlow<List<Int>>(emptyList())
    val selectedTags: StateFlow<List<Int>> = _selectedTags.asStateFlow()

    private val _reviewText = MutableStateFlow("")
    val reviewText: StateFlow<String> = _reviewText.asStateFlow()

    private val _reviewInferenceStatus = MutableStateFlow<Int?>(null)
    val reviewInferenceStatus: StateFlow<Int?> = _reviewInferenceStatus.asStateFlow()

    private val _selectedLanguage = MutableStateFlow("Korean")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    val tags = listOf(
        R.string.location,
        R.string.view,
        R.string.service,
        R.string.comfort,
        R.string.food,
        R.string.spacious,
        R.string.natural_light,
    )

    val languageMap = mapOf(
        "Korean" to R.string.gemini_hybrid_lang_korean,
        "Spanish" to R.string.gemini_hybrid_lang_spanish,
        "French" to R.string.gemini_hybrid_lang_french,
        "German" to R.string.gemini_hybrid_lang_german
    )

    fun setInferenceMode(mode: InferenceMode) {
        _inferenceMode.value = mode
    }

    fun toggleTag(tagResId: Int) {
        _selectedTags.update { current ->
            if (current.contains(tagResId)) current - tagResId else current + tagResId
        }
    }

    fun updateReviewText(text: String) {
        _reviewText.value = text
    }

    fun setSelectedLanguage(language: String) {
        _selectedLanguage.value = language
    }

    fun generateReview(tagStrings: List<String>) {
        if (tagStrings.isEmpty()) {
            _uiState.value = GeminiHybridUiState.Error("Please select at least one tag")
            return
        }

        viewModelScope.launch {
            _uiState.value = GeminiHybridUiState.Generating(
                isCloud = _inferenceMode.value == InferenceMode.ONLY_IN_CLOUD,
                isTranslation = false
            )
            try {
                val prompt =
                    "Write a simple, short and generic hotel review positively covering the following themes: ${
                        tagStrings.joinToString(", ")
                    }. Generate a generic review strictly from themes, don't hallucinate a hotel name or a location. Return only the review text."

                val model = Firebase.ai(backend = GenerativeBackend.googleAI())
                    .generativeModel(
                        "gemini-2.5-flash-lite",
                        onDeviceConfig = OnDeviceConfig(mode = _inferenceMode.value)
                    )
                model.generateContentStream(prompt).collect { chunk ->
                    val isCloud = chunk.inferenceSource == InferenceSource.IN_CLOUD
                    _uiState.update { state ->
                        if (state is GeminiHybridUiState.Generating) {
                            state.copy(
                                isCloud = isCloud,
                                partialOutput = state.partialOutput + (chunk.text ?: "")
                            )
                        } else {
                            GeminiHybridUiState.Generating(
                                isCloud = isCloud,
                                partialOutput = chunk.text ?: "",
                                isTranslation = false
                            )
                        }
                    }
                }

                val finalState = uiState.value
                if (finalState is GeminiHybridUiState.Generating) {
                    val output = finalState.partialOutput.trimEnd()
                    _reviewText.value = output
                    _reviewInferenceStatus.value = if (finalState.isCloud) {
                        R.string.gemini_hybrid_generated_cloud
                    } else {
                        R.string.gemini_hybrid_generated_on_device
                    }
                    _uiState.value =
                        GeminiHybridUiState.Success(output, finalState.isCloud, isTranslation = false)
                }
            } catch (e: Exception) {
                Log.e("GeminiHybrid", "Inference failed", e)
                _uiState.value =
                    GeminiHybridUiState.Error(e.localizedMessage ?: "Unknown error occurred")
            }
        }
    }

    fun translate(text: String, language: String) {
        if (text.isBlank()) {
            _uiState.value = GeminiHybridUiState.Error("Text to translate cannot be empty")
            return
        }

        viewModelScope.launch {
            _uiState.value = GeminiHybridUiState.Generating(
                isCloud = _inferenceMode.value == InferenceMode.ONLY_IN_CLOUD,
                isTranslation = true
            )
            try {
                val prompt =
                    "Translate the following text to $language. Return ONLY the translated text, no explanations:\n\n$text"

                val model = Firebase.ai(backend = GenerativeBackend.googleAI())
                    .generativeModel(
                        "gemini-2.5-flash-lite",
                        onDeviceConfig = OnDeviceConfig(mode = _inferenceMode.value)
                    )

                model.generateContentStream(prompt).collect { chunk ->
                    val isCloud = chunk.inferenceSource == InferenceSource.IN_CLOUD
                    _uiState.update { state ->
                        if (state is GeminiHybridUiState.Generating) {
                            state.copy(
                                isCloud = isCloud,
                                partialOutput = state.partialOutput + (chunk.text ?: "")
                            )
                        } else {
                            GeminiHybridUiState.Generating(
                                isCloud = isCloud,
                                partialOutput = chunk.text ?: "",
                                isTranslation = true
                            )
                        }
                    }
                }

                val finalState = uiState.value
                if (finalState is GeminiHybridUiState.Generating) {
                    _uiState.value = GeminiHybridUiState.Success(
                        finalState.partialOutput,
                        finalState.isCloud,
                        isTranslation = true
                    )
                }
            } catch (e: Exception) {
                Log.e("GeminiHybrid", "Inference failed", e)
                _uiState.value =
                    GeminiHybridUiState.Error(e.localizedMessage ?: "Unknown error occurred")
            }
        }
    }

    fun reset() {
        _uiState.value = GeminiHybridUiState.Initial
        _selectedTags.value = emptyList()
        _reviewText.value = ""
        _reviewInferenceStatus.value = null
    }
}
