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
package com.android.ai.samples.geminivideosummary.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.ai.samples.geminivideosummary.util.VideoItem
import com.android.ai.samples.geminivideosummary.util.sampleVideoList
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel class responsible for handling video summarization using Gemini API.
 *
 * This ViewModel interacts with the Gemini API to generate a text summary of a provided video.
 * It manages the state of the summarization process and exposes the output text through a
 * [StateFlow].
 */
class VideoSummarizationViewModel @Inject constructor() : ViewModel() {

    private val tag = "VideoSummaryVM"
    private val _uiState = MutableStateFlow(VideoSummarizationState())
    val uiState: StateFlow<VideoSummarizationState> = _uiState.asStateFlow()

    fun onVideoSelected(videoItem: VideoItem) {
        _uiState.update { it.copy(selectedVideo = videoItem, summarizationState = SummarizationState.Idle) }
    }

    fun onTtsStateChanged(newTtsState: TtsState) {
        val currentState = _uiState.value.summarizationState
        if (currentState is SummarizationState.Success) {
            _uiState.update {
                it.copy(summarizationState = currentState.copy(ttsState = newTtsState))
            }
        }
    }

    fun onTtsInitializationResult(isSuccess: Boolean, errorMessage: String?) {
        if (!isSuccess && errorMessage != null) {
            _uiState.update {
                it.copy(summarizationState = SummarizationState.Error(errorMessage))
            }
        }
    }

    fun summarize() {
        val videoSource = _uiState.value.selectedVideo?.uri ?: return
        viewModelScope.launch {
            val promptData =
                "Summarize this video in the form of top 3-4 takeaways only. Write in the form of bullet points. Don't assume if you don't know"
            _uiState.update { it.copy(summarizationState = SummarizationState.InProgress) }

            try {
                val generativeModel =
                    Firebase.ai(backend = GenerativeBackend.vertexAI())
                        .generativeModel("gemini-2.5-flash")

                val requestContent = content {
                    fileData(videoSource.toString(), "video/mp4")
                    text(promptData)
                }
                val outputStringBuilder = StringBuilder()
                generativeModel.generateContentStream(requestContent).collect { response ->
                    outputStringBuilder.append(response.text)
                }
                _uiState.update {
                    it.copy(
                        summarizationState = SummarizationState.Success(summarizedText = outputStringBuilder.toString()),
                    )
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        summarizationState = SummarizationState.Error(error.localizedMessage ?: "An unknown error occurred"),
                    )
                }
                Log.e(tag, "Error processing prompt : $error")
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(summarizationState = SummarizationState.Idle) }
    }

    fun redo() {
        _uiState.update { it.copy(summarizationState = SummarizationState.Idle) }
    }
}

sealed interface SummarizationState {
    data object Idle : SummarizationState
    data object InProgress : SummarizationState
    data class Error(val message: String) : SummarizationState
    data class Success(
        val summarizedText: String,
        val ttsState: TtsState = TtsState.Idle,
    ) : SummarizationState
}

sealed interface TtsState {
    data object Idle : TtsState
    data object Playing : TtsState
    data object Paused : TtsState
}

data class VideoSummarizationState(
    val selectedVideo: VideoItem? = sampleVideoList[0],
    val summarizationState: SummarizationState = SummarizationState.Idle,
)
