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

package com.example.jetpacker.feature.itinerary_enrichment

import android.util.Log
import com.example.jetpacker.data.itinerary.TimelineEvent
import com.example.jetpacker.data.itinerary.VoiceNoteEntity
import com.example.jetpacker.data.trips.Trip
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.ModelPreference
import com.google.mlkit.genai.prompt.ModelReleaseStage
import com.google.mlkit.genai.prompt.generationConfig
import com.google.mlkit.genai.prompt.modelConfig
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

private enum class TripStatus {
  PAST,
  CURRENT,
  UPCOMING,
}

@Singleton
class TripSummaryAndTipsProviderImpl @Inject constructor() : TripSummaryAndTipsProvider {
  private val generativeModel by lazy {
    val previewFastConfig = generationConfig {
      modelConfig = modelConfig {
        releaseStage = ModelReleaseStage.PREVIEW
        // Fast model is not good at if/else instructions for whether or not to generate 'phrases
        // to learn' in prompt.
        preference = ModelPreference.FAST
      }
    }
    Generation.getClient(previewFastConfig)
  }

  override suspend fun isSupported(): Boolean {
    return try {
      val status = generativeModel.checkStatus()
      status != 0 // 0 is UNAVAILABLE
    } catch (e: GenAiException) {
      if (e.errorCode == 606) {
        Log.w("TripSummaryAndTips", "Feature 646 not found, disabling trip summary and tips.", e)
        false
      } else {
        throw e
      }
    }
  }

  override fun generateTripSummaryAndTips(events: List<TimelineEvent>, trip: Trip): Flow<String> = flow {
    var status =
      try {
        generativeModel.checkStatus()
      } catch (e: GenAiException) {
        if (e.errorCode == 606) {
          Log.w("TripSummaryAndTips", "Feature 646 not found in generateTripSummaryAndTips, disabling.", e)
          emit("Trip summaries and tips are not available on this device.")
          return@flow
        } else {
          throw e
        }
      }
    if (status == 1) { // 1 is DOWNLOADABLE
      Log.d("TripSummaryAndTips", "Model is downloadable. Triggering download...")
      generativeModel.download().collect { Log.d("TripSummaryAndTips", "Downloading progress...") }
      Log.d("TripSummaryAndTips", "Download flow completed. Re-checking status...")
      status = generativeModel.checkStatus()
    }

    if (status != 3) { // 3 is AVAILABLE
      Log.e("TripSummaryAndTips", "Model is not available (status: $status). Skipping trip summary and tips.")
      emit("Model is downloading or unavailable. Please try again later.")
      return@flow
    }

    val prompt =
      "Given this itinerary for a trip: $events, " +
        "generate the following: " +
        "overall trip summary in 1 sentence less than 15 words without a bolded title, " +
        "1 tip on how to prepare for this trip with a bolded title \"Tip\""

    val promptWithShortPhrases =
      "Given this itinerary for a trip: $events, " +
        "generate the following: " +
        "overall trip summary in 1 sentence less than 15 words without a bolded title, " +
        "1 tip on how to prepare for this trip with a bolded title \"Tip\", " +
        "2 common short phrases to learn for the trip, with a bolded title \"Useful Phrases\". Format each strictly as '* Foreign Phrase - English Translation' (e.g., '* Bonjour - Hello')."

    val countriesWithEnglishLocale = listOf("USA", "Ireland", "UK", "Australia")
    val location = trip.location

    val selectedPrompt =
      if (countriesWithEnglishLocale.any { location.contains(it) }) {
        prompt
      } else {
        promptWithShortPhrases
      }

    try {
      generativeModel.generateContentStream(selectedPrompt).collect { chunk ->
        val textChunk = chunk.candidates.firstOrNull()?.text ?: ""
        emit(textChunk)
      }
    } catch (e: Exception) {
      Log.e("TripSummaryAndTips", "Generation stream failed", e)
      throw e
    }
  }

  override fun generateTripSummary(
    events: List<TimelineEvent>,
    trip: Trip,
    voiceNotes: List<VoiceNoteEntity>,
  ): Flow<String> = flow {
    var status =
      try {
        generativeModel.checkStatus()
      } catch (e: GenAiException) {
        if (e.errorCode == 606) {
          emit("Trip summaries and tips are not available on this device.")
          return@flow
        } else {
          throw e
        }
      }
    if (status == 1) {
      generativeModel.download().collect {}
      status = generativeModel.checkStatus()
    }
    if (status != 3) {
      emit("Model is downloading or unavailable. Please try again later.")
      return@flow
    }

    val prompt =
      "Given these voice notes for a trip: $voiceNotes, use a reminiscent tone to" +
        " generate a short 2 sentence summary of the highlights. Only return the summary."
    try {
      generativeModel.generateContentStream(prompt).collect { chunk ->
        val textChunk = chunk.candidates.firstOrNull()?.text ?: ""
        emit(textChunk)
      }
    } catch (e: Exception) {
      throw e
    }
  }

  override fun generateUpcomingTip(events: List<TimelineEvent>, trip: Trip): Flow<String> = flow {
    var status =
      try {
        generativeModel.checkStatus()
      } catch (e: GenAiException) {
        if (e.errorCode == 606) {
          emit("Trip summaries and tips are not available on this device.")
          return@flow
        } else {
          throw e
        }
      }
    if (status == 1) {
      generativeModel.download().collect {}
      status = generativeModel.checkStatus()
    }
    if (status != 3) {
      emit("Model is downloading or unavailable. Please try again later.")
      return@flow
    }

    val prompt =
      "Given this itinerary for the upcoming events of a trip: $events, " +
        "generate 2 short tips. " +
        "return only the 2 short tips."
    try {
      generativeModel.generateContentStream(prompt).collect { chunk ->
        val textChunk = chunk.candidates.firstOrNull()?.text ?: ""
        emit(textChunk)
      }
    } catch (e: Exception) {
      throw e
    }
  }
}
