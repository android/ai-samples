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

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
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
    data class Error(val errorMessageStringRes: Int) : GenAISummarizationUiState()
}

class GenAISummarizationViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow<GenAISummarizationUiState>(GenAISummarizationUiState.Initial)
    val uiState: StateFlow<GenAISummarizationUiState> = _uiState.asStateFlow()

    private var summarizer: Summarizer? = null

    fun summarize(textToSummarize: String, context: Context) {
        if (textToSummarize.isEmpty()) {
            _uiState.value = GenAISummarizationUiState.Error(R.string.summarization_no_input)
            return
        }

        viewModelScope.launch {
            val summarizationOptions =
                SummarizerOptions.builder(context)
                    .setOutputType(SummarizerOptions.OutputType.THREE_BULLETS)
                    .build()
            summarizer = Summarization.getClient(summarizationOptions)

            summarizer?.let { summarizer ->
                var featureStatus = FeatureStatus.UNAVAILABLE

                try {
                    _uiState.value = GenAISummarizationUiState.CheckingFeatureStatus
                    featureStatus = summarizer.checkFeatureStatus().await()
                } catch (error: Exception) {
                    Log.e("GenAISummarization", "Error checking feature status", error)
                }

                if (featureStatus == FeatureStatus.UNAVAILABLE) {
                    _uiState.value = GenAISummarizationUiState.Error(R.string.summarization_not_available)
                    return@launch
                }

                // If feature is downloadable, making an inference call will automatically start
                // the downloading process.
                // If feature is downloading, the inference request will automatically execute after
                // the feature has been downloaded.
                // Alternatively, you can call summarizer.downloadFeature() to monitor the
                // progress of the download.
                if (featureStatus == FeatureStatus.DOWNLOADABLE || featureStatus == FeatureStatus.DOWNLOADING) {
                    summarizer.downloadFeature(
                        object : DownloadCallback {
                            override fun onDownloadStarted(bytesToDownload: Long) {
                                _uiState.value = GenAISummarizationUiState.DownloadingFeature(bytesToDownload, 0)
                            }

                            override fun onDownloadProgress(bytesDownloaded: Long) {
                                val bytesToDownload = (_uiState.value as GenAISummarizationUiState.DownloadingFeature).bytesToDownload
                                _uiState.value = GenAISummarizationUiState.DownloadingFeature(bytesToDownload, bytesDownloaded)
                            }

                            override fun onDownloadCompleted() {
                                viewModelScope.launch {
                                    generateSummarization(summarizer, textToSummarize)
                                }
                            }

                            override fun onDownloadFailed(exception: GenAiException) {
                                _uiState.value = GenAISummarizationUiState.Error(R.string.summarization_download_failed)
                            }
                        },
                    )
                } else {
                    generateSummarization(summarizer, textToSummarize)
                }
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

        val generatedOutput = (_uiState.value as GenAISummarizationUiState.Generating).generatedOutput
        _uiState.value = GenAISummarizationUiState.Success(generatedOutput)
    }

    fun clearGeneratedSummary() {
        _uiState.value = GenAISummarizationUiState.Initial
    }

    override fun onCleared() {
        summarizer?.close()
    }
}
