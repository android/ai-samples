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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch

sealed class GenAISummarizationUiState {
    data object Initial : GenAISummarizationUiState()
    data object CheckingFeatureStatus : GenAISummarizationUiState()
    data class DownloadingFeature(
        val bytesToDownload: Long,
        val bytesDownloaded: Long,
    ) : GenAISummarizationUiState()

    data class Generating(val generatedOutput: String) : GenAISummarizationUiState()
    data class Success(val generatedOutput: String) : GenAISummarizationUiState()
    data class Error(val errorMessage: String) : GenAISummarizationUiState()
}

class GenAISummarizationViewModel @Inject constructor(val context: Application) : AndroidViewModel(context) {
    private val _uiState = MutableStateFlow<GenAISummarizationUiState>(GenAISummarizationUiState.Initial)
    val uiState: StateFlow<GenAISummarizationUiState> = _uiState.asStateFlow()

    private var summarizer = Summarization.getClient(
        SummarizerOptions.builder(context)
            .setOutputType(SummarizerOptions.OutputType.THREE_BULLETS)
            .build(),
    )
    private var summarizationJob: Job? = null

    fun summarize(textToSummarize: String) {
        if (textToSummarize.isEmpty()) {
            _uiState.value = GenAISummarizationUiState.Error(context.getString(R.string.summarization_no_input))
            return
        }

        summarizationJob = viewModelScope.launch {
            var featureStatus = FeatureStatus.UNAVAILABLE

            try {
                _uiState.value = GenAISummarizationUiState.CheckingFeatureStatus
                featureStatus = summarizer.checkFeatureStatus().await()
            } catch (error: Exception) {
                _uiState.value = GenAISummarizationUiState.Error(context.getString(R.string.summarization_feature_check_fail))
                Log.e("GenAISummarization", "Error checking feature status", error)
            }

            if (featureStatus == FeatureStatus.UNAVAILABLE) {
                _uiState.value = GenAISummarizationUiState.Error(context.getString(R.string.summarization_not_available))
                return@launch
            }

            // If feature is downloadable, making an inference call will automatically start
            // the downloading process.
            // If feature is downloading, the inference request will automatically execute after
            // the feature has been downloaded.
            // Alternatively, you can call summarizer.downloadFeature() to monitor the
            // progress of the download.
            // Calling downloadFeature() while the feature is already downloading will not start another download.
            if (featureStatus == FeatureStatus.DOWNLOADABLE || featureStatus == FeatureStatus.DOWNLOADING) {
                summarizer.downloadFeature(
                    object : DownloadCallback {
                        override fun onDownloadStarted(bytesToDownload: Long) {
                            _uiState.value = GenAISummarizationUiState.DownloadingFeature(bytesToDownload, 0)
                        }

                        override fun onDownloadProgress(bytesDownloaded: Long) {
                            (_uiState.value as? GenAISummarizationUiState.DownloadingFeature)?.bytesToDownload?.let { bytesToDownload ->
                                _uiState.value = GenAISummarizationUiState.DownloadingFeature(bytesToDownload, bytesDownloaded)
                            }
                        }

                        override fun onDownloadCompleted() {
                            viewModelScope.launch {
                                generateSummarization(summarizer, textToSummarize)
                            }
                        }

                        override fun onDownloadFailed(exception: GenAiException) {
                            Log.e("GenAISummarization", "Download failed", exception)
                            _uiState.value = GenAISummarizationUiState.Error(context.getString(R.string.summarization_download_failed))
                        }
                    },
                )
            } else {
                generateSummarization(summarizer, textToSummarize)
            }
        }
    }

    private suspend fun generateSummarization(summarizer: Summarizer, textToSummarize: String) {
        _uiState.value = GenAISummarizationUiState.Generating("")
        val summarizationRequest = SummarizationRequest.builder(textToSummarize).build()

        try {
            // Instead of using await() here, alternatively you can attach a FutureCallback<SummarizationResult>
            summarizer.runInference(summarizationRequest) { newText ->
                (_uiState.value as? GenAISummarizationUiState.Generating)?.let { generatingState ->
                    _uiState.value = generatingState.copy(generatedOutput = generatingState.generatedOutput + newText)
                }
            }.await()
        } catch (genAiException: GenAiException) {
            Log.e("GenAISummarization", "Error generating summary with error code: ${genAiException.errorCode}", genAiException)
            val errorMessage = genAiException.message ?: context.getString(R.string.summarization_generation_error)
            _uiState.value = GenAISummarizationUiState.Error(errorMessage)
        }

        (_uiState.value as? GenAISummarizationUiState.Generating)?.generatedOutput?.let { generatedOutput ->
            _uiState.value = GenAISummarizationUiState.Success(generatedOutput)
        }
    }

    fun clearGeneratedSummary() {
        _uiState.value = GenAISummarizationUiState.Initial
        summarizationJob?.cancel()
    }

    override fun onCleared() {
        summarizer.close()
    }
}
