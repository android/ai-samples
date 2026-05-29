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

sealed interface GeminiStatus {
    data object Initial : GeminiStatus
    data class Generating(
        val isCloud: Boolean,
        val partialOutput: String = "",
        val isTranslation: Boolean = false
    ) : GeminiStatus

    data class Success(
        val output: String,
        val isCloud: Boolean,
        val isTranslation: Boolean = false
    ) : GeminiStatus

    data class Error(val message: String) : GeminiStatus
}

@OptIn(PublicPreviewAPI::class)
data class GeminiHybridUiState(
    val selectedMode: InferenceMode = InferenceMode.ONLY_ON_DEVICE,
    val selectedTags: List<Int> = emptyList(),
    val reviewText: String = "",
    val reviewInferenceStatus: Int? = null,
    val selectedLanguage: String = "Korean",
    val status: GeminiStatus = GeminiStatus.Initial
)

@PublicPreviewAPI
@HiltViewModel
class GeminiHybridViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(GeminiHybridUiState())
    val uiState: StateFlow<GeminiHybridUiState> = _uiState.asStateFlow()

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
        _uiState.update { it.copy(selectedMode = mode) }
    }

    fun toggleTag(tagResId: Int) {
        _uiState.update { state ->
            val newTags = if (state.selectedTags.contains(tagResId)) {
                state.selectedTags - tagResId
            } else {
                state.selectedTags + tagResId
            }
            state.copy(selectedTags = newTags)
        }
    }

    fun updateReviewText(text: String) {
        _uiState.update { it.copy(reviewText = text) }
    }

    fun setSelectedLanguage(language: String) {
        _uiState.update { it.copy(selectedLanguage = language) }
    }

    fun generateReview(tagStrings: List<String>) {
        if (tagStrings.isEmpty()) {
            _uiState.update { it.copy(status = GeminiStatus.Error("Please select at least one tag")) }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    status = GeminiStatus.Generating(
                        isCloud = it.selectedMode == InferenceMode.ONLY_IN_CLOUD,
                        isTranslation = false
                    )
                )
            }
            try {
                val prompt =
                    "Write a simple, short and generic hotel review positively covering the following themes: ${
                        tagStrings.joinToString(", ")
                    }. Generate a generic review strictly from themes, don't hallucinate a hotel name or a location. Return only the review text."

                val model = Firebase.ai(backend = GenerativeBackend.googleAI())
                    .generativeModel(
                        "gemini-2.5-flash-lite",
                        onDeviceConfig = OnDeviceConfig(mode = _uiState.value.selectedMode)
                    )
                model.generateContentStream(prompt).collect { chunk ->
                    val isCloud = chunk.inferenceSource == InferenceSource.IN_CLOUD
                    _uiState.update { state ->
                        val currentStatus = state.status
                        val newStatus = if (currentStatus is GeminiStatus.Generating) {
                            currentStatus.copy(
                                isCloud = isCloud,
                                partialOutput = currentStatus.partialOutput + (chunk.text ?: "")
                            )
                        } else {
                            GeminiStatus.Generating(
                                isCloud = isCloud,
                                partialOutput = chunk.text ?: "",
                                isTranslation = false
                            )
                        }
                        state.copy(status = newStatus)
                    }
                }

                val finalState = _uiState.value
                val finalStatus = finalState.status
                if (finalStatus is GeminiStatus.Generating) {
                    val output = finalStatus.partialOutput.trimEnd()
                    val inferenceStatusResId = if (finalStatus.isCloud) {
                        R.string.gemini_hybrid_generated_cloud
                    } else {
                        R.string.gemini_hybrid_generated_on_device
                    }
                    _uiState.update {
                        it.copy(
                            reviewText = output,
                            reviewInferenceStatus = inferenceStatusResId,
                            status = GeminiStatus.Success(output, finalStatus.isCloud, isTranslation = false)
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("GeminiHybrid", "Inference failed", e)
                _uiState.update {
                    it.copy(status = GeminiStatus.Error(e.localizedMessage ?: "Unknown error occurred"))
                }
            }
        }
    }

    fun translate(text: String, language: String) {
        if (text.isBlank()) {
            _uiState.update { it.copy(status = GeminiStatus.Error("Text to translate cannot be empty")) }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    status = GeminiStatus.Generating(
                        isCloud = it.selectedMode == InferenceMode.ONLY_IN_CLOUD,
                        isTranslation = true
                    )
                )
            }
            try {
                val prompt =
                    "Translate the following text to $language. Return ONLY the translated text, no explanations:\n\n$text"

                val model = Firebase.ai(backend = GenerativeBackend.googleAI())
                    .generativeModel(
                        "gemini-2.5-flash-lite",
                        onDeviceConfig = OnDeviceConfig(mode = _uiState.value.selectedMode)
                    )

                model.generateContentStream(prompt).collect { chunk ->
                    val isCloud = chunk.inferenceSource == InferenceSource.IN_CLOUD
                    _uiState.update { state ->
                        val currentStatus = state.status
                        val newStatus = if (currentStatus is GeminiStatus.Generating) {
                            currentStatus.copy(
                                isCloud = isCloud,
                                partialOutput = currentStatus.partialOutput + (chunk.text ?: "")
                            )
                        } else {
                            GeminiStatus.Generating(
                                isCloud = isCloud,
                                partialOutput = chunk.text ?: "",
                                isTranslation = true
                            )
                        }
                        state.copy(status = newStatus)
                    }
                }

                val finalState = _uiState.value
                val finalStatus = finalState.status
                if (finalStatus is GeminiStatus.Generating) {
                    _uiState.update {
                        it.copy(
                            status = GeminiStatus.Success(
                                finalStatus.partialOutput,
                                finalStatus.isCloud,
                                isTranslation = true
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("GeminiHybrid", "Inference failed", e)
                _uiState.update {
                    it.copy(status = GeminiStatus.Error(e.localizedMessage ?: "Unknown error occurred"))
                }
            }
        }
    }

    fun reset() {
        _uiState.value = GeminiHybridUiState()
    }
}
