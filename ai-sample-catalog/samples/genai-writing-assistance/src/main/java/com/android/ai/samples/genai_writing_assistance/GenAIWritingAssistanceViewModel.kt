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
package com.android.ai.samples.genai_writing_assistance

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.ai.samples.geminimultimodal.R
import com.google.mlkit.genai.common.DownloadCallback
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.proofreading.ProofreaderOptions
import com.google.mlkit.genai.proofreading.Proofreading
import com.google.mlkit.genai.proofreading.ProofreadingRequest
import com.google.mlkit.genai.rewriting.Rewriter
import com.google.mlkit.genai.rewriting.RewriterOptions
import com.google.mlkit.genai.rewriting.Rewriting
import com.google.mlkit.genai.rewriting.RewritingRequest
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch

sealed class GenAIWritingAssistanceUiState {
    data object Initial : GenAIWritingAssistanceUiState()
    data object CheckingFeatureStatus : GenAIWritingAssistanceUiState()
    data class DownloadingFeature(
        val bytesToDownload: Long,
        val bytesDownloaded: Long,
    ) : GenAIWritingAssistanceUiState()

    data object Generating : GenAIWritingAssistanceUiState()
    data class Success(val generatedOutput: String) : GenAIWritingAssistanceUiState()
    data class Error(@StringRes val errorMessageStringRes: Int) : GenAIWritingAssistanceUiState()
}

class GenAIWritingAssistanceViewModel @Inject constructor(context: Application) : AndroidViewModel(context) {
    private val _uiState = MutableStateFlow<GenAIWritingAssistanceUiState>(GenAIWritingAssistanceUiState.Initial)
    val uiState: StateFlow<GenAIWritingAssistanceUiState> = _uiState.asStateFlow()

    private val proofreader = Proofreading.getClient(
        ProofreaderOptions.builder(context)
            .setLanguage(ProofreaderOptions.Language.ENGLISH)
            // If input was transcript of speech-to-text, this should be InputType.SPEECH
            .setInputType(ProofreaderOptions.InputType.KEYBOARD)
            .build(),
    )

    private var rewriter: Rewriter? = null

    fun proofread(text: String) {
        if (text.isEmpty()) {
            _uiState.value = GenAIWritingAssistanceUiState.Error(R.string.genai_writing_assistance_no_input)
            return
        }

        viewModelScope.launch {
            var proofreadFeatureStatus = FeatureStatus.UNAVAILABLE

            try {
                _uiState.value = GenAIWritingAssistanceUiState.CheckingFeatureStatus
                proofreadFeatureStatus = proofreader.checkFeatureStatus().await()
            } catch (error: Exception) {
                _uiState.value = GenAIWritingAssistanceUiState.Error(R.string.feature_check_fail)
                Log.e("GenAIWriting", "Error checking feature status", error)
            }

            if (proofreadFeatureStatus == FeatureStatus.UNAVAILABLE) {
                _uiState.value = GenAIWritingAssistanceUiState.Error(R.string.genai_writing_assistance_not_available)
                return@launch
            }

            if (proofreadFeatureStatus == FeatureStatus.DOWNLOADABLE || proofreadFeatureStatus == FeatureStatus.DOWNLOADING) {
                proofreader.downloadFeature(
                    object : DownloadCallback {
                        override fun onDownloadStarted(bytesToDownload: Long) {
                            _uiState.value = GenAIWritingAssistanceUiState.DownloadingFeature(bytesToDownload, 0)
                        }

                        override fun onDownloadProgress(bytesDownloaded: Long) {
                            _uiState.update {
                                (it as? GenAIWritingAssistanceUiState.DownloadingFeature)?.copy(bytesDownloaded = bytesDownloaded) ?: it
                            }
                        }

                        override fun onDownloadCompleted() {
                            viewModelScope.launch {
                                runProofreadingInference(text)
                            }
                        }

                        override fun onDownloadFailed(exception: GenAiException) {
                            Log.e("GenAIWriting", "Download failed", exception)
                            _uiState.value = GenAIWritingAssistanceUiState.Error(R.string.feature_download_failed)
                        }
                    },
                )
            } else {
                runProofreadingInference(text)
            }
        }
    }

    private suspend fun runProofreadingInference(textToProofread: String) {
        val proofreadRequest = ProofreadingRequest.builder(textToProofread).build()
        // More than 1 result may be generated. Results are returned in descending order of
        // quality of confidence. Here we use the first result which has the highest quality
        // of confidence.
        _uiState.value = GenAIWritingAssistanceUiState.Generating
        val results = proofreader.runInference(proofreadRequest).await()
        _uiState.value = GenAIWritingAssistanceUiState.Success(results.results[0].text)
    }

    fun rewrite(text: String, rewriteStyle: Int, context: Context) {
        if (text.isEmpty()) {
            _uiState.value = GenAIWritingAssistanceUiState.Error(R.string.genai_writing_assistance_no_input)
            return
        }

        val rewriteOptions = RewriterOptions.builder(context)
            .setLanguage(RewriterOptions.Language.ENGLISH)
            .setOutputType(rewriteStyle)
            .build()

        rewriter = Rewriting.getClient(rewriteOptions)

        viewModelScope.launch {
            rewriter?.let { rewriter ->
                var rewriteFeatureStatus = FeatureStatus.UNAVAILABLE

                try {
                    _uiState.value = GenAIWritingAssistanceUiState.CheckingFeatureStatus
                    rewriteFeatureStatus = rewriter.checkFeatureStatus().await()
                } catch (error: Exception) {
                    _uiState.value = GenAIWritingAssistanceUiState.Error(R.string.feature_check_fail)
                    Log.e("GenAIWriting", "Error checking feature status", error)
                }

                if (rewriteFeatureStatus == FeatureStatus.UNAVAILABLE) {
                    _uiState.value = GenAIWritingAssistanceUiState.Error(R.string.genai_writing_assistance_not_available)
                    return@launch
                }

                if (rewriteFeatureStatus == FeatureStatus.DOWNLOADABLE || rewriteFeatureStatus == FeatureStatus.DOWNLOADING) {
                    rewriter.downloadFeature(
                        object : DownloadCallback {
                            override fun onDownloadStarted(bytesToDownload: Long) {
                                _uiState.value = GenAIWritingAssistanceUiState.DownloadingFeature(bytesToDownload, 0)
                            }

                            override fun onDownloadProgress(bytesDownloaded: Long) {
                                _uiState.update {
                                    (it as? GenAIWritingAssistanceUiState.DownloadingFeature)?.copy(bytesDownloaded = bytesDownloaded) ?: it
                                }
                            }

                            override fun onDownloadCompleted() {
                                viewModelScope.launch {
                                    runRewritingInference(rewriter, text)
                                }
                            }

                            override fun onDownloadFailed(exception: GenAiException) {
                                Log.e("GenAIWriting", "Download failed", exception)
                                _uiState.value = GenAIWritingAssistanceUiState.Error(R.string.feature_download_failed)
                            }
                        },
                    )
                } else {
                    runRewritingInference(rewriter, text)
                }
            }
        }
    }

    private suspend fun runRewritingInference(rewriter: Rewriter, text: String) {
        val rewriteRequest = RewritingRequest.builder(text).build()
        // More than 1 result may be generated. Results are returned in descending order of
        // quality of confidence. Here we use the first result which has the highest quality of
        // confidence.
        _uiState.value = GenAIWritingAssistanceUiState.Generating
        val results = rewriter.runInference(rewriteRequest).await()
        _uiState.value = GenAIWritingAssistanceUiState.Success(results.results[0].text)
    }

    fun clearGeneratedText() {
        _uiState.value = GenAIWritingAssistanceUiState.Initial
    }

    override fun onCleared() {
        proofreader.close()
        rewriter?.close()
    }
}
