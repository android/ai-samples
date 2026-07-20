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

package com.example.jetpacker.feature.voice_notes

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jetpacker.core.speech.VoiceInputManager
import com.example.jetpacker.data.itinerary.EventDao
import com.example.jetpacker.data.itinerary.TimelineEvent
import com.example.jetpacker.data.itinerary.VoiceNoteEntity
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.ModelPreference
import com.google.mlkit.genai.prompt.ModelReleaseStage
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import com.google.mlkit.genai.prompt.generationConfig
import com.google.mlkit.genai.prompt.modelConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

@SuppressLint("GlobalCoroutineDispatchers")
@HiltViewModel
class VoiceNotesViewModel
@Inject
constructor(
  savedStateHandle: SavedStateHandle,
  private val eventDao: EventDao,
  val voiceInputManager: VoiceInputManager,
) : ViewModel() {

  private val _tripIdFlow = MutableStateFlow(savedStateHandle.get<String>("tripId") ?: "")
  internal val tripId: String get() = _tripIdFlow.value

  fun loadForTrip(id: String) {
    if (id.isNotEmpty() && id != _tripIdFlow.value) {
      _tripIdFlow.value = id
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  val events: StateFlow<List<TimelineEvent>> =
    _tripIdFlow
      .flatMapLatest { id -> eventDao.getEventsForTrip(id) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  @OptIn(ExperimentalCoroutinesApi::class)
  val voiceNotes: StateFlow<List<VoiceNoteEntity>> =
    _tripIdFlow
      .flatMapLatest { id -> eventDao.getVoiceNotesForTrip(id) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val processingNotes = mutableStateListOf<VoiceNotePlaceholder>()

  // Visible for testing
  internal var generativeModelTesting: GenerativeModel? = null

  private val generativeModel by lazy {
    generativeModelTesting
      ?: run {
        val previewFastConfig = generationConfig {
          modelConfig = modelConfig {
            releaseStage = ModelReleaseStage.PREVIEW
            preference = ModelPreference.FAST
          }
        }
        Generation.getClient(previewFastConfig)
      }
  }

  suspend fun insertVoiceNote(note: VoiceNoteEntity) {
    eventDao.insertVoiceNote(note)
  }

  fun deleteVoiceNote(note: VoiceNoteEntity) {
    viewModelScope.launch {
      try {
        val json = JSONArray(note.matchingEventsJson)
        for (i in 0 until json.length()) {
          val obj = json.getJSONObject(i)
          val eventId = obj.getString("eventId")
          val extract = obj.getString("extract")

          val event = events.value.find { it.id == eventId }
          if (event != null) {
            val updatedAudioNotes = event.audioNotes.filter { it != extract }
            updateEvent(event.copy(audioNotes = updatedAudioNotes))
          }
        }
      } catch (e: Exception) {
        // Silently fail if JSON is malformed
      }
      eventDao.deleteVoiceNoteById(note.id)
    }
  }

  suspend fun updateEvent(event: TimelineEvent) {
    eventDao.insertEvent(event)
  }

  @SuppressLint("GlobalCoroutineDispatchers")
  fun processVoiceNote(original: String, translated: String) {
    val placeholder =
      VoiceNotePlaceholder(
        id = UUID.randomUUID().toString(),
        title = "Processing...",
        // TODO: Remove hardcoded date
        date = "2026-05-20",
      )
    processingNotes.add(0, placeholder)
    viewModelScope.launch {
      withContext(Dispatchers.Main.immediate) {
        try {
          delay(150)
          val status = generativeModel.checkStatus()
          if (status == 3) { // 3 is AVAILABLE
            // TODO: Refactor to use real current date/time instead of hardcoded May 20 12PM
            val ioBaseTime = Instant.parse("2026-05-17T00:00:00Z").toEpochMilli()
            val dayMillis = 24 * 3600 * 1000L
            val hourMillis = 3600 * 1000L
            val nowTs = ioBaseTime + 3 * dayMillis + 12 * hourMillis
            val eventsListStr =
              events.value
                .filter { it.timestamp <= nowTs }
                .joinToString("\n") { "${it.id} - ${it.type.name} - ${it.title}" }
            val prompt =
              """
            Given the voice note transcription: "$translated"
            And the following events for this trip:
            ${eventsListStr}
            
            Rewrite this transcription to remove filler words, fluff words, or nonsensical parts, while preserving its meaning.
            The rewritten text should be a clean version of user's voice note.
            Then, identify which events from the list this rewritten transcription matches to. For each matching event, extract the specific part of the REWRITTEN transcription that relates to it. Make the "extract" succinct and directly to the point, stripping out transitional context (e.g., rather than "During the flight I saw a beautiful sunset", output "I saw a beautiful sunset"). Never use the event's title or name as the "extract".
            
            Your response must be a valid JSON object strictly matching this structure:
            {"rewritten": "...", "matches": [{"eventId": "...", "extract": "..."}]}
            Respond strictly in valid JSON.
          """
                .trimIndent()

            var attempts = 0
            val maxAttempts = 4
            var currentDelay = 1000L
            var responseText = ""

            while (attempts < maxAttempts) {
              try {
                val request = generateContentRequest(TextPart(prompt)) {}
                val response = generativeModel.generateContent(request)
                responseText = response.candidates.firstOrNull()?.text?.trim() ?: ""
                break
              } catch (e: GenAiException) {
                if (e.errorCode == 9) { // 9 is ErrorCode.BUSY
                  attempts++
                  if (attempts >= maxAttempts) {
                    throw e
                  }
                  println(
                    "AICore is busy (ErrorCode 9), retrying in ${currentDelay}ms... (Attempt $attempts/$maxAttempts)"
                  )
                  delay(currentDelay)
                  currentDelay *= 2
                } else {
                  throw e
                }
              }
            }

            val json = JSONObject(responseText.removeSurrounding("```json", "```").trim())
            val matchesArray = json.optJSONArray("matches")
            // TODO: Remove hardcoded title
            val noteTitle = "May 20, 10:00 AM"

            if (matchesArray != null) {
              for (i in 0 until matchesArray.length()) {
                val obj = matchesArray.getJSONObject(i)
                val eventId = obj.getString("eventId")
                val extract = obj.getString("extract")

                events.value
                  .find { it.id == eventId }
                  ?.let { event ->
                    updateEvent(event.copy(audioNotes = event.audioNotes + extract))
                  }
              }
            }

            val noteId = UUID.randomUUID().toString()
            insertVoiceNote(
              VoiceNoteEntity(
                id = noteId,
                tripId = tripId,
                title = noteTitle,
                transcription = "$translated |||| $original",
                timestamp = System.currentTimeMillis(),
                matchingEventsJson = matchesArray?.toString() ?: "",
              )
            )
          } else {
            throw Exception("LLM Model not available (status $status)")
          }
        } catch (e: Exception) {
          println("Voice note processing failed: ${e.message}")
          val errorIdx = processingNotes.indexOf(placeholder)
          if (errorIdx != -1) {
            processingNotes[errorIdx] =
              placeholder.copy(
                title = "Failed",
                isError = true,
                errorMessage = e.message ?: "Unknown error",
              )
            delay(5000)
          }
        } finally {
          processingNotes.remove(placeholder)
        }
      }
    }
  }
}
