/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.jetpacker.feature.detail.review

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.ai.InferenceMode
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
import kotlinx.coroutines.CancellationException

@HiltViewModel
class ReviewScreenViewModel @Inject constructor(savedStateHandle: SavedStateHandle) : ViewModel() {
  val topics = listOf("Accessibility", "Location", "Food Quality", "Service", "Ambiance", "Value")

  private val _selectedTopics = MutableStateFlow<Set<Topic>>(emptySet())
  val selectedTopics: StateFlow<Set<Topic>> = _selectedTopics.asStateFlow()

  private val _isGenerating = MutableStateFlow(false)
  val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

  private val _generatedReviewText = MutableStateFlow("")
  val generatedReviewText: StateFlow<String> = _generatedReviewText.asStateFlow()

  fun toggleTopic(topicName: String, positiveOpinion: Boolean) {
    val topic = Topic(topicName, positiveOpinion)
    val oppositeTopic = Topic(topicName, !positiveOpinion)
    _selectedTopics.update { current ->
      if (current.contains(topic)) {
        current - topic
      } else {
        (current - oppositeTopic) + topic
      }
    }
  }

  fun onGeneratedReviewTextChange(newText: String) {
    _generatedReviewText.value = newText
  }

  @OptIn(PublicPreviewAPI::class)
  private val generativeModel =
    Firebase.ai(backend = GenerativeBackend.googleAI())
      .generativeModel(
        modelName = "gemini-2.5-flash-lite",
        onDeviceConfig = OnDeviceConfig(mode = InferenceMode.PREFER_ON_DEVICE),
      )

  fun generateReview(placeName: String) {
    if (_selectedTopics.value.isEmpty()) return

    viewModelScope.launch {
      _isGenerating.value = true
      try {
        val topicsString = _selectedTopics.value.joinToString(", ") {
          "${it.name} (${if (it.positiveOpinion) "positive" else "negative"})"
        }
        val prompt =
          "Generate a very short review for ${placeName}, based on these topics: $topicsString. Don't generate a title, just the body of the review. Don't use markdown. Don't extrapolate on the topics, just say if it's good or bad."
        val response = generativeModel.generateContent(prompt)
        _generatedReviewText.value = response.text?.trim() ?: ""
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        // In a real app, handle error UI
      } finally {
        _isGenerating.value = false
      }
    }
  }

  fun addReview(placeId: String, context: Context) {
    val text = _generatedReviewText.value
    if (text.isBlank()) return
    _generatedReviewText.value = ""
    _selectedTopics.value = emptySet()

    copyAndOpenMapsReview(context, text, placeId)
  }

  private fun copyAndOpenMapsReview(context: Context, reviewText: String, placeId: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("User Review", reviewText)
    clipboard.setPrimaryClip(clip)

    val uri = Uri.parse("https://search.google.com/local/writereview/mobile?placeid=$placeId")

    val intent =
      Intent(Intent.ACTION_VIEW, uri).apply {
        setPackage("com.google.android.apps.maps")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }

    if (intent.resolveActivity(context.packageManager) != null) {
      context.startActivity(intent)
    } else {
      context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
  }
}

data class PlaceReviewed(val name: String, val placeId: String)

data class Topic(val name: String, val positiveOpinion: Boolean)
