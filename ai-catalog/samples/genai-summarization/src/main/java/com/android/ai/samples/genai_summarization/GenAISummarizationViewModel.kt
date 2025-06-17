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
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.summarization.Summarization
import com.google.mlkit.genai.summarization.SummarizationRequest
import com.google.mlkit.genai.summarization.Summarizer
import com.google.mlkit.genai.summarization.SummarizerOptions
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch

class GenAISummarizationViewModel @Inject constructor() : ViewModel() {
    private val _summarizationGenerated = MutableStateFlow("")
    val summarizationGenerated: StateFlow<String> = _summarizationGenerated

    private var summarizer: Summarizer? = null

    fun summarize(textToSummarize: String, context: Context) {
        if (textToSummarize.isEmpty()) {
            _summarizationGenerated.value = context.getString(R.string.summarization_no_input)
            return
        }

        val summarizationOptions =
            SummarizerOptions.builder(context)
                .setOutputType(SummarizerOptions.OutputType.THREE_BULLETS)
                .build()
        summarizer = Summarization.getClient(summarizationOptions)

        viewModelScope.launch {
            summarizer?.let { summarizer ->

                var featureStatus = FeatureStatus.UNAVAILABLE

                try {
                    featureStatus = summarizer.checkFeatureStatus().await()
                } catch (error: Exception) {
                    Log.e("GenAISummarization", "Error checking feature status", error)
                }

                if (featureStatus == FeatureStatus.UNAVAILABLE) {
                    _summarizationGenerated.value =
                        context.getString(R.string.summarization_not_available)
                    return@launch
                }

                // If feature is downloadable, making an inference call will automatically start
                // the downloading process.
                // If feature is downloading, the inference request will automatically execute after
                // the feature has been downloaded.
                // Alternatively, you can call summarizer.downloadFeature() to monitor the
                // progress of the download.
                if (featureStatus == FeatureStatus.DOWNLOADABLE ||
                    featureStatus == FeatureStatus.DOWNLOADING
                ) {
                    _summarizationGenerated.value =
                        context.getString(R.string.summarization_downloading)
                }

                val summarizationRequest = SummarizationRequest.builder(textToSummarize).build()
                summarizer.runInference(summarizationRequest) { newText ->
                    if (_summarizationGenerated.value ==
                        context.getString(R.string.summarization_downloading)
                    ) {
                        clearGeneratedSummary()
                    }
                    _summarizationGenerated.value += newText
                }
                return@launch
            }
        }
    }

    fun clearGeneratedSummary() {
        _summarizationGenerated.value = ""
    }

    override fun onCleared() {
        summarizer?.close()
    }
}
