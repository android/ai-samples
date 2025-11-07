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
package com.android.ai.samples.genai_image_prompt

import android.app.Application
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.GenerateContentResponse
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.ImagePart
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "GenAIImagePromptVM"

sealed class GenAIImagePromptUiState {
    data object Initial : GenAIImagePromptUiState()
    data object CheckingFeatureStatus : GenAIImagePromptUiState()
    data class DownloadingFeature(
        val bytesToDownload: Long,
        val bytesDownloaded: Long,
    ) : GenAIImagePromptUiState()

    data class Result(val outputText: String) : GenAIImagePromptUiState()
    data class Error(@StringRes val errorMessageStringRes: Int) : GenAIImagePromptUiState()
}

class GenAIImagePromptViewModel @Inject constructor(val context: Application) : AndroidViewModel(context) {
    private val _uiState = MutableStateFlow<GenAIImagePromptUiState>(GenAIImagePromptUiState.Initial)
    val uiState: StateFlow<GenAIImagePromptUiState> = _uiState.asStateFlow()

    private var generativeModel: GenerativeModel? = null

    init {
        viewModelScope.launch {
            val client = Generation.getClient()
            generativeModel = client
            client.warmup()

            val status = client.checkStatus()
            when (status) {
                FeatureStatus.UNAVAILABLE -> {
                    _uiState.value = GenAIImagePromptUiState.Error(R.string.genai_image_prompt_not_available)
                }
                FeatureStatus.DOWNLOADABLE -> {
                    _uiState.value = GenAIImagePromptUiState.DownloadingFeature(0L, 0L) // Initial state for download
                    launch {
                        val downloadStatusFlow: Flow<DownloadStatus> = client.download()
                        downloadStatusFlow.collect { downloadStatus ->
                            when (downloadStatus) {
                                is DownloadStatus.DownloadStarted -> {
                                    // bytesToDownload might not be available immediately, update when available
                                }
                                is DownloadStatus.DownloadProgress -> {
                                    _uiState.update {
                                        val downloadingState = it as? GenAIImagePromptUiState.DownloadingFeature
                                        downloadingState?.copy(
                                            bytesDownloaded = downloadStatus.totalBytesDownloaded,
                                        ) ?: it
                                    }
                                }
                                is DownloadStatus.DownloadCompleted -> {
                                    // Feature downloaded, ready to use
                                }
                                is DownloadStatus.DownloadFailed -> {
                                    Log.e(TAG, "Download failed: ${downloadStatus.e.message}")
                                    _uiState.value = GenAIImagePromptUiState.Error(R.string.image_prompt_download_failed)
                                }
                                else -> {
                                    // Handle other potential DownloadStatus states if necessary
                                }
                            }
                        }
                    }
                }
                FeatureStatus.AVAILABLE -> {
                    // Model is available, ready for inference
                }
                else -> {
                    // Handle other potential FeatureStatus states if necessary
                }
            }
        }
    }

    fun clearGeneratedText() {
        _uiState.value = GenAIImagePromptUiState.Initial
    }

    fun generateImagePrompt(imageUri: Uri?, promptText: String) {
        if (imageUri == null) {
            _uiState.value = GenAIImagePromptUiState.Error(R.string.genai_image_prompt_no_image_selected)
            return
        }
        if (promptText.isBlank()) {
            _uiState.value = GenAIImagePromptUiState.Error(R.string.genai_image_prompt_no_text_input)
            return
        }

        viewModelScope.launch {
            _uiState.value = GenAIImagePromptUiState.Result("") // Clear previous result and show loading
            try {
                val currentModel = generativeModel
                if (currentModel != null) {
                    val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
                    var fullResponse = ""
                    currentModel.generateContentStream(
                        generateContentRequest(
                            ImagePart(bitmap),
                            TextPart(promptText),
                        ) {
                            temperature = 0.2f
                            topK = 10
                            candidateCount = 1
                        },
                    ).collect { chunk: GenerateContentResponse ->
                        val newChunkReceived = chunk.candidates.firstOrNull()?.text
                        if (newChunkReceived != null) {
                            fullResponse += newChunkReceived
                            _uiState.value = GenAIImagePromptUiState.Result(fullResponse)
                        }
                    }
                } else {
                    _uiState.value = GenAIImagePromptUiState.Error(R.string.genai_image_prompt_model_not_ready)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Image prompt generation failed", e)
                _uiState.value = GenAIImagePromptUiState.Error(R.string.image_prompt_generation_failed)
            }
        }
    }

    override fun onCleared() {
        generativeModel?.close()
        super.onCleared()
    }
}
