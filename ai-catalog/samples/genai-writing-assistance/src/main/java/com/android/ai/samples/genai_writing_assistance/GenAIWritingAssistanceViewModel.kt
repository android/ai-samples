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

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.ai.samples.geminimultimodal.R
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.proofreading.Proofreader
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
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch

class GenAIWritingAssistanceViewModel @Inject constructor() : ViewModel() {
    private val _resultGenerated = MutableStateFlow("")
    val resultGenerated: StateFlow<String> = _resultGenerated

    private var proofreader: Proofreader? = null
    private var rewriter: Rewriter? = null

    fun proofread(text: String, context: Context) {
        if (text.isEmpty()) {
            _resultGenerated.value = context.getString(R.string.genai_writing_assistance_no_input)
            return
        }

        val proofreadOptions = ProofreaderOptions.builder(context)
            .setLanguage(ProofreaderOptions.Language.ENGLISH)
            // If input was transcript of speech-to-text, this should be InputType.SPEECH
            .setInputType(ProofreaderOptions.InputType.KEYBOARD)
            .build()

        proofreader = Proofreading.getClient(proofreadOptions)

        viewModelScope.launch {
            proofreader?.let { proofreader ->

                var proofreadFeatureStatus = FeatureStatus.UNAVAILABLE

                try {
                    proofreadFeatureStatus = proofreader.checkFeatureStatus().await()
                } catch (error: Exception) {
                    Log.e("GenAIProofread", "Error checking feature status", error)
                }

                if (proofreadFeatureStatus == FeatureStatus.UNAVAILABLE) {
                    _resultGenerated.value =
                        context.getString(R.string.genai_writing_assistance_not_available)
                    return@launch
                }

                // If feature is downloadable, making an inference call will automatically start
                // the downloading process.
                // If feature is downloading, the inference request will automatically execute after
                // the feature has been downloaded.
                // Alternatively, you can call proofreader.downloadFeature() to monitor the
                // progress of the download.
                if (proofreadFeatureStatus == FeatureStatus.DOWNLOADABLE ||
                    proofreadFeatureStatus == FeatureStatus.DOWNLOADING
                ) {
                    _resultGenerated.value =
                        context.getString(R.string.genai_writing_assistance_downloading)
                }

                val proofreadRequest = ProofreadingRequest.builder(text).build()
                // More than 1 result may be generated. Results are returned in descending order of
                // quality of confidence. Here we use the first result which has the highest quality
                // of confidence.
                val results = proofreader.runInference(proofreadRequest).await()
                _resultGenerated.value = results.results[0].text
                return@launch
            }
        }
    }

    fun rewrite(text: String, rewriteStyle: Int, context: Context) {
        if (text.isEmpty()) {
            _resultGenerated.value = context.getString(R.string.genai_writing_assistance_no_input)
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
                    rewriteFeatureStatus = rewriter.checkFeatureStatus().await()
                } catch (error: Exception) {
                    Log.e("GenAIRewrite", "Error checking feature status", error)
                }

                if (rewriteFeatureStatus == FeatureStatus.UNAVAILABLE) {
                    _resultGenerated.value =
                        context.getString(R.string.genai_writing_assistance_not_available)
                    return@launch
                }

                // If feature is downloadable, making an inference call will automatically start
                // the downloading process.
                // If feature is downloading, the inference request will automatically execute after
                // the feature has been downloaded.
                // Alternatively, you can call rewriter.downloadFeature() to monitor the
                // progress of the download.
                if (rewriteFeatureStatus == FeatureStatus.DOWNLOADABLE ||
                    rewriteFeatureStatus == FeatureStatus.DOWNLOADING
                ) {
                    _resultGenerated.value =
                        context.getString(R.string.genai_writing_assistance_downloading)
                }

                val rewriteRequest = RewritingRequest.builder(text).build()
                // More than 1 result may be generated. Results are returned in descending order of
                // quality of confidence. Here we use the first result which has the highest quality of
                // confidence.
                val results = rewriter.runInference(rewriteRequest).await()
                _resultGenerated.value = results.results[0].text
                return@launch
            }
        }
    }

    fun clearGeneratedText() {
        _resultGenerated.value = ""
    }

    override fun onCleared() {
        proofreader?.close()
        rewriter?.close()
    }
}
