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
package com.android.ai.samples.genai_summarization

import android.app.Application
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.ai.samples.geminimultimodal.R
import com.google.mlkit.genai.common.DownloadCallback
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.summarization.Summarization
import com.google.mlkit.genai.summarization.SummarizationRequest
import com.google.mlkit.genai.summarization.Summarizer
import com.google.mlkit.genai.summarization.SummarizerOptions
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch

sealed class GenAISummarizationUiState {
    data object Initial : GenAISummarizationUiState()
    data class Generating(val generatedOutput: String) : GenAISummarizationUiState()
    data class Success(val generatedOutput: String) : GenAISummarizationUiState()
    data class Error(@StringRes val errorMessageStringRes: Int) : GenAISummarizationUiState()
}

sealed class GenAISummarizationFeatureState {
    data object Initial : GenAISummarizationFeatureState()
    data class Downloading(
        val bytesToDownload: Long,
        val bytesDownloaded: Long,
    ) : GenAISummarizationFeatureState()

    data object Downloaded : GenAISummarizationFeatureState()
    data object Unavailable : GenAISummarizationFeatureState()
}

data class ScreenUIState(
    val uiState: GenAISummarizationUiState,
    val featureState: GenAISummarizationFeatureState,
)

class GenAISummarizationViewModel @Inject constructor(context: Application) : AndroidViewModel(context) {
    private val _uiState = MutableStateFlow<GenAISummarizationUiState>(GenAISummarizationUiState.Initial)
    val uiState: StateFlow<GenAISummarizationUiState> = _uiState.asStateFlow()

    private val _featureState = MutableStateFlow<GenAISummarizationFeatureState>(GenAISummarizationFeatureState.Initial)
    val featureState: StateFlow<GenAISummarizationFeatureState> = _featureState.asStateFlow()

    val screenUiState: StateFlow<ScreenUIState> =
        combine(uiState, featureState) { uiState, featureState -> ScreenUIState(uiState, featureState) }
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                ScreenUIState(GenAISummarizationUiState.Initial, GenAISummarizationFeatureState.Initial),
            )

    private var summarizer = Summarization.getClient(
        SummarizerOptions.builder(context)
            .setOutputType(SummarizerOptions.OutputType.THREE_BULLETS)
            .build(),
    )
    private var summarizationJob: Job? = null
    private var downloadJob: Job? = null

    init {
        downloadFeature()
    }

    fun downloadFeature() {
        downloadJob = viewModelScope.launch {
            val featureStatus = summarizer.checkFeatureStatus().await()
            when (featureStatus) {
                FeatureStatus.AVAILABLE -> {
                    _featureState.value = GenAISummarizationFeatureState.Downloaded
                }

                FeatureStatus.UNAVAILABLE -> {
                    _featureState.value = GenAISummarizationFeatureState.Unavailable
                }

                else -> {
                    // If feature is downloadable, making an inference call will automatically start
                    // the downloading process.
                    // If feature is downloading, the inference request will automatically execute after
                    // the feature has been downloaded.
                    // Alternatively, you can call summarizer.downloadFeature() to monitor the
                    // progress of the download.
                    // Calling downloadFeature() while the feature is already downloading will not start another download.
                    summarizer.downloadFeature(
                        object : DownloadCallback {
                            override fun onDownloadStarted(bytesToDownload: Long) {
                                _featureState.value = GenAISummarizationFeatureState.Downloading(bytesToDownload, 0)
                            }

                            override fun onDownloadProgress(bytesDownloaded: Long) {
                                (_featureState.value as? GenAISummarizationFeatureState.Downloading)?.bytesToDownload?.let { bytesToDownload ->
                                    _featureState.value = GenAISummarizationFeatureState.Downloading(bytesToDownload, bytesDownloaded)
                                }
                            }

                            override fun onDownloadCompleted() {
                                _featureState.value = GenAISummarizationFeatureState.Downloaded
                            }

                            override fun onDownloadFailed(exception: GenAiException) {
                                Log.e("GenAISummarization", "Download failed", exception)
                                _featureState.value = GenAISummarizationFeatureState.Unavailable
                            }
                        },
                    )
                }
            }
        }
    }

    fun summarize(textToSummarize: String) {
        if (featureState.value is GenAISummarizationFeatureState.Unavailable) {
            _uiState.value = GenAISummarizationUiState.Error(R.string.summarization_not_available)
            return
        }

        summarizationJob = viewModelScope.launch {
            featureState.first { it is GenAISummarizationFeatureState.Downloaded }
                .also {
                    generateSummarization(summarizer, textToSummarize)
                }
        }
    }

    private suspend fun generateSummarization(summarizer: Summarizer, textToSummarize: String) {
        _uiState.value = GenAISummarizationUiState.Generating("")
        val summarizationRequest = SummarizationRequest.builder(textToSummarize).build()

        summarizer.runInference(summarizationRequest) { newText ->
            val generatedOutput = (_uiState.value as GenAISummarizationUiState.Generating).generatedOutput
            _uiState.value = GenAISummarizationUiState.Generating(generatedOutput + newText)
        }.await()
        // Instead of using await() here, alternatively you can attach a FutureCallback<SummarizationResult>

        (_uiState.value as? GenAISummarizationUiState.Generating)?.generatedOutput?.let { generatedOutput ->
            _uiState.value = GenAISummarizationUiState.Success(generatedOutput)
        }
    }

    fun clearGeneratedSummary() {
        _uiState.value = GenAISummarizationUiState.Initial
        downloadJob?.cancel()
        summarizationJob?.cancel()
    }

    override fun onCleared() {
        summarizer.close()
    }
}
