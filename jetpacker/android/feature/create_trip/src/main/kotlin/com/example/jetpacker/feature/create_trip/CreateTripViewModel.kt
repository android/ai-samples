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

package com.example.jetpacker.feature.create_trip

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jetpacker.core.flags.FeatureFlags
import com.example.jetpacker.data.trips.Trip
import com.example.jetpacker.data.trips.TripDao
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.ModelPreference
import com.google.mlkit.genai.prompt.ModelReleaseStage
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import com.google.mlkit.genai.prompt.generationConfig
import com.google.mlkit.genai.prompt.modelConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

@HiltViewModel
open class CreateTripViewModel
@Inject
constructor(
  savedStateHandle: SavedStateHandle,
  private val tripDao: TripDao,
  @param:ApplicationContext private val context: Context? = null,
) : ViewModel() {
  private val _uiState = MutableStateFlow(CreateTripUiState())
  open val uiState: StateFlow<CreateTripUiState> = _uiState.asStateFlow()

  init {
    val tripId: String? = savedStateHandle["tripId"]
    if (!tripId.isNullOrBlank()) {
      loadTripForEditing(tripId)
    }
  }

  open fun loadTripForEditing(tripId: String) {
    viewModelScope.launch {
      tripDao.getTripById(tripId).collect { trip ->
        if (trip != null) {
          val dateFormatter =
            DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US)
          val startStr =
            Instant.ofEpochMilli(trip.startDate)
              .atZone(ZoneOffset.UTC)
              .toLocalDate()
              .format(dateFormatter)
          val endStr =
            Instant.ofEpochMilli(trip.endDate)
              .atZone(ZoneOffset.UTC)
              .toLocalDate()
              .format(dateFormatter)
          _uiState.update { current ->
            current.copy(
              isEditing = true,
              editingTripId = trip.id,
              title = trip.title,
              location = trip.location,
              startDate = startStr,
              endDate = endStr,
              participants = trip.participants,
              imageUri = trip.imageUri,
            )
          }
        }
      }
    }
  }

  private val generativeModel by lazy {
    val previewFastConfig = generationConfig {
      modelConfig = modelConfig {
        releaseStage = ModelReleaseStage.PREVIEW
        preference = ModelPreference.FAST
      }
    }
    Generation.getClient(previewFastConfig)
  }

  /**
   * Processes the voice input text, extracts trip details using the on-device ML model,
   * and updates the corresponding fields in the UI state.
   *
   * @param text The transcribed text from the voice note.
   */
  open fun processVoiceInput(text: String) {
    if (text.isBlank()) return
    _uiState.update { it.copy(isGenerating = true) }
    viewModelScope.launch {
      try {
        val status = generativeModel.checkStatus()
        if (status == FeatureStatus.AVAILABLE) {
          val currentDateStr =
            (FeatureFlags.OVERRIDE_CURRENT_TIME_MILLIS?.let {
                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
              } ?: LocalDate.now())
              .toString()
          val prompt = getVoiceInputPrompt(text, currentDateStr)

          val request = generateContentRequest(TextPart(prompt)) {}
          val response = generativeModel.generateContent(request)
          val responseText = response.candidates.firstOrNull()?.text?.trim() ?: ""

          parseAndApplyVoiceInput(responseText)
        } else {
          Log.e("CreateTripViewModel", "ML Kit model not available (status $status)")
        }
      } catch (e: Exception) {
        Log.e("CreateTripViewModel", "Error processing voice input", e)
      } finally {
        _uiState.update { it.copy(isGenerating = false) }
      }
    }
  }

  private fun getVoiceInputPrompt(text: String, currentDateStr: String): String {
    return """
      Given the voice note transcription: "$text"
      Extract the following fields for a trip form:
      - Title (inferred or direct)
      - Location
      - Start Date (YYYY-MM-DD)
      - End Date (YYYY-MM-DD)
      - Participants (comma separated list)
      
      Context:
      - Today's Date: $currentDateStr
      
      Date Extraction Instructions:
      - If the user uses vague date expressions like "later this year", "this summer", "in a few months", estimate a reasonable future date range within the current year (relative to $currentDateStr) rather than leaving them empty.
      - If they specify a duration (e.g., "a week", "two weeks"), ensure the interval between the estimated Start Date and End Date matches that duration (e.g., exactly 7 days for a week).
      
      Your response must be a valid JSON object strictly matching this structure:
      {
        "title": "...",
        "location": "...",
        "startDate": "...",
        "endDate": "...",
        "participants": ["...", "..."]
      }
      If a field cannot be extracted or is unknown, return an empty string "" for that field instead of null.
      Respond strictly in valid JSON.
    """.trimIndent()
  }

  private fun parseAndApplyVoiceInput(responseText: String) {
    try {
      val json = JSONObject(responseText.removeSurrounding("```json", "```").trim())

      val title = if (json.isNull("title")) "" else json.optString("title", "")
      val location = if (json.isNull("location")) "" else json.optString("location", "")
      val startDate = if (json.isNull("startDate")) "" else json.optString("startDate", "")
      val endDate = if (json.isNull("endDate")) "" else json.optString("endDate", "")
      val participants = json.optJSONArray("participants")

      if (title.isNotEmpty() && title != "null") onTitleChange(title)
      if (location.isNotEmpty() && location != "null") onLocationChange(location)

      val formatterIn = SimpleDateFormat("yyyy-MM-dd", Locale.US)
      val formatterOut = SimpleDateFormat("MMM dd, yyyy", Locale.US)
      val formatStr: (String) -> String = { str ->
        try {
          formatterOut.format(formatterIn.parse(str)!!)
        } catch (e: Exception) {
          str
        }
      }

      if (startDate.isNotEmpty() && startDate != "null") onStartDateChange(formatStr(startDate))
      if (endDate.isNotEmpty() && endDate != "null") onEndDateChange(formatStr(endDate))

      if (participants != null) {
        for (i in 0 until participants.length()) {
          onAddParticipant(participants.getString(i))
        }
      }
    } catch (e: Exception) {
      Log.e("CreateTripViewModel", "Failed to parse or apply voice input JSON", e)
    }
  }

  open fun onTitleChange(newTitle: String) {
    _uiState.update { it.copy(title = newTitle) }
  }

  open fun onLocationChange(newLocation: String) {
    _uiState.update { it.copy(location = newLocation) }
  }

  open fun onStartDateChange(newDate: String) {
    _uiState.update { it.copy(startDate = newDate) }
  }

  open fun onEndDateChange(newDate: String) {
    _uiState.update { it.copy(endDate = newDate) }
  }

  open fun onAddParticipant(name: String) {
    if (name.isBlank()) return
    _uiState.update { current ->
      val currentParticipants = current.participants.toMutableList()
      currentParticipants.add(name)
      current.copy(participants = currentParticipants)
    }
  }

  open fun onRemoveParticipant(name: String) {
    _uiState.update { current ->
      val currentParticipants = current.participants.toMutableList()
      currentParticipants.remove(name)
      current.copy(participants = currentParticipants)
    }
  }

  open fun onImageUriChange(newUri: String?) {
    _uiState.update { it.copy(imageUri = newUri) }
  }

  open fun createTrip() {
    if (
      _uiState.value.title.isBlank() ||
        _uiState.value.startDate.isBlank() ||
        _uiState.value.endDate.isBlank()
    )
      return

    _uiState.update { it.copy(isLoading = true) }
    viewModelScope.launch {
      val parseDate: (String) -> Long = { dateStr ->
        var parsedTime = 0L
        for (pattern in listOf("MMM dd, yyyy", "yyyy-MM-dd")) {
          try {
            val d = SimpleDateFormat(pattern, Locale.US).parse(dateStr)
            if (d != null) {
              parsedTime = d.time
              break
            }
          } catch (e: Exception) {
            // continue to next pattern
          }
        }
        parsedTime
      }
      _uiState.update { current ->
        val startTs = parseDate(current.startDate)
        val endTs = parseDate(current.endDate)

        val newTrip =
          Trip(
            id = current.editingTripId ?: UUID.randomUUID().toString(),
            title = current.title,
            location = current.location,
            startDate = startTs,
            endDate = endTs,
            participants = current.participants,
            imageUri = current.imageUri,
          )
        tripDao.insertTrip(newTrip)
        current.copy(
          isLoading = false,
          isSuccess = true,
          tripId = newTrip.id,
          title = "",
          location = "",
          startDate = "",
          endDate = "",
          participants = emptyList(),
          imageUri = null,
        )
      }
    }
  }

  open fun generateAiImage() {
    Log.d("CreateTripViewModel", "Cloud AI image generation is disabled in basic release.")
  }

  open fun resetState() {
    _uiState.update { CreateTripUiState() }
  }

  private suspend fun saveBitmapToCache(bitmap: Bitmap): String? =
    withContext(Dispatchers.IO) {
      val ctx = context ?: return@withContext null
      try {
        val cacheFile =
          File(ctx.cacheDir, "generated_trip_${UUID.randomUUID()}.jpg")
        FileOutputStream(cacheFile).use { stream ->
          bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
          stream.flush()
        }
        Uri.fromFile(cacheFile).toString()
      } catch (e: Exception) {
        Log.e("CreateTripViewModel", "Failed to save bitmap to cache", e)
        null
      }
    }
}
