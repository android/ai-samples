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

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    private val _outputText = MutableStateFlow<OutputTextState>(OutputTextState.Initial)
    val outputText: StateFlow<OutputTextState> = _outputText

    fun getVideoSummary(videoSource: Uri) {
        clearOutputText()
        viewModelScope.launch {
            val promptData =
                "Summarize this video in the form of top 3-4 takeaways only. Write in the form of bullet points. Don't assume if you don't know"
            _outputText.value = OutputTextState.Loading

            try {
                val generativeModel = Firebase.ai(backend = GenerativeBackend.vertexAI()).generativeModel("gemini-2.0-flash")

                val requestContent = content {
                    fileData(videoSource.toString(), "video/mp4")
                    text(promptData)
                }
                val outputStringBuilder = StringBuilder()
                generativeModel.generateContentStream(requestContent).collect { response ->
                    outputStringBuilder.append(response.text)
                }
                _outputText.value = OutputTextState.Success(outputStringBuilder.toString())
            } catch (error: Exception) {
                _outputText.value = error.localizedMessage?.let { OutputTextState.Error(it) }!!
                Log.e(tag, "Error processing prompt : $error")
            }
        }
    }

    fun clearOutputText() {
        _outputText.value = OutputTextState.Loading
    }
}
