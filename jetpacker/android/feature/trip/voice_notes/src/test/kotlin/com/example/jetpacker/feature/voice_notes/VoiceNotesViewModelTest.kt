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

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.jetpacker.core.speech.VoiceInputManager
import com.example.jetpacker.data.itinerary.ActivityDetail
import com.example.jetpacker.data.itinerary.DiningDetail
import com.example.jetpacker.data.itinerary.EventDao
import com.example.jetpacker.data.itinerary.EventType
import com.example.jetpacker.data.itinerary.FlightDetail
import com.example.jetpacker.data.itinerary.HotelDetail
import com.example.jetpacker.data.itinerary.MuseumDetail
import com.example.jetpacker.data.itinerary.TimelineEvent
import com.example.jetpacker.data.itinerary.TripSessionIdentifier
import com.example.jetpacker.data.itinerary.VoiceNoteEntity
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.genai.common.DownloadCallback
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.common.StreamingCallback
import com.google.mlkit.genai.prompt.Caches
import com.google.mlkit.genai.prompt.CountTokensResponse
import com.google.mlkit.genai.prompt.GenerateContentRequest
import com.google.mlkit.genai.prompt.GenerateContentResponse
import com.google.mlkit.genai.prompt.GenerativeModel
import java.util.concurrent.ExecutorService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class VoiceNotesViewModelTest {

  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
  }

  @Test
  fun deleteVoiceNote_removesExtractsFromEvents() = runTest {
    val tripId = "trip123"
    val eventId = "event456"
    val extractText = "Meeting at 10am"
    val matchingEventsJson = "[{\"eventId\":\"$eventId\", \"extract\":\"$extractText\"}]"

    val voiceNote =
      VoiceNoteEntity(
        id = "vn1",
        tripId = tripId,
        title = "Voice Note 1",
        transcription = "transcription",
        timestamp = 1000L,
        matchingEventsJson = matchingEventsJson,
      )

    val event =
      TimelineEvent(
        id = eventId,
        tripId = tripId,
        type = EventType.ACTIVITY,
        timestamp = 2000L,
        title = "Activity",
        location = "Office",
        audioNotes = listOf(extractText, "Another note"),
      )

    var updatedEvent: TimelineEvent? = null
    var deletedVoiceNoteId: String? = null

    val fakeEventDao =
      object : EventDao {
        override fun getAllSessionIds() =
          flowOf(emptyList<TripSessionIdentifier>())

        override fun getEventsForTrip(tripId: String) = flowOf(listOf(event))

        override fun getVoiceNotesForTrip(tripId: String) = flowOf(listOf(voiceNote))

        override suspend fun insertEvent(event: TimelineEvent) {
          updatedEvent = event
        }

        override suspend fun insertEvents(events: List<TimelineEvent>) {}

        override suspend fun deleteEventsForTrip(tripId: String) {}

        override suspend fun deleteAllActivityDetails() {}

        override suspend fun deleteEvent(event: TimelineEvent) {}

        override suspend fun insertFlightDetail(
          detail: FlightDetail
        ) {}

        override suspend fun insertHotelDetail(
          detail: HotelDetail
        ) {}

        override suspend fun insertDiningDetail(
          detail: DiningDetail
        ) {}

        override suspend fun insertActivityDetail(
          detail: ActivityDetail
        ) {}

        override suspend fun insertMuseumDetail(
          detail: MuseumDetail
        ) {}

        override fun getFlightDetail(eventId: String) = flowOf(null)

        override fun getHotelDetail(eventId: String) = flowOf(null)

        override fun getDiningDetail(eventId: String) = flowOf(null)

        override fun getMuseumDetail(eventId: String) = flowOf(null)

        override fun getEventById(eventId: String) = flowOf(null)

        override suspend fun deleteAllEvents() {}

        override suspend fun deleteAllFlightDetails() {}

        override suspend fun deleteAllHotelDetails() {}

        override suspend fun deleteAllDiningDetails() {}

        override suspend fun insertVoiceNote(note: VoiceNoteEntity) {}

        override suspend fun deleteVoiceNoteById(id: String) {
          deletedVoiceNoteId = id
        }
      }

    val viewModel =
      VoiceNotesViewModel(
        SavedStateHandle(mapOf("tripId" to tripId)),
        fakeEventDao,
        VoiceInputManager(),
      )

    // Start collecting to make StateFlow hot
    val collectJob = launch { viewModel.events.collect {} }
    advanceUntilIdle()

    viewModel.deleteVoiceNote(voiceNote)
    advanceUntilIdle()

    assertEquals(listOf("Another note"), updatedEvent?.audioNotes)
    assertEquals("vn1", deletedVoiceNoteId)
    collectJob.cancel()
  }

  @Test
  fun processVoiceNote_retriesOnBusyWithExponentialBackoff() = runTest {
    val tripId = "trip123"
    val eventId = "event456"

    val event =
      TimelineEvent(
        id = eventId,
        tripId = tripId,
        type = EventType.ACTIVITY,
        timestamp = 2000L,
        title = "Activity",
        location = "Office",
        audioNotes = emptyList(),
      )

    var insertedVoiceNote: VoiceNoteEntity? = null
    var updatedEvent: TimelineEvent? = null

    val fakeEventDao =
      object : EventDao {
        override fun getAllSessionIds() =
          flowOf(emptyList<TripSessionIdentifier>())

        override fun getEventsForTrip(tripId: String) = flowOf(listOf(event))

        override fun getVoiceNotesForTrip(tripId: String) = flowOf(emptyList<VoiceNoteEntity>())

        override suspend fun insertEvent(event: TimelineEvent) {
          updatedEvent = event
        }

        override suspend fun insertEvents(events: List<TimelineEvent>) {}

        override suspend fun deleteEventsForTrip(tripId: String) {}

        override suspend fun deleteAllActivityDetails() {}

        override suspend fun deleteEvent(event: TimelineEvent) {}

        override suspend fun insertFlightDetail(
          detail: com.example.jetpacker.data.itinerary.FlightDetail
        ) {}

        override suspend fun insertHotelDetail(
          detail: com.example.jetpacker.data.itinerary.HotelDetail
        ) {}

        override suspend fun insertDiningDetail(
          detail: com.example.jetpacker.data.itinerary.DiningDetail
        ) {}

        override suspend fun insertActivityDetail(
          detail: com.example.jetpacker.data.itinerary.ActivityDetail
        ) {}

        override suspend fun insertMuseumDetail(
          detail: com.example.jetpacker.data.itinerary.MuseumDetail
        ) {}

        override fun getFlightDetail(eventId: String) = flowOf(null)

        override fun getHotelDetail(eventId: String) = flowOf(null)

        override fun getDiningDetail(eventId: String) = flowOf(null)

        override fun getMuseumDetail(eventId: String) = flowOf(null)

        override fun getEventById(eventId: String) = flowOf(null)

        override suspend fun deleteAllEvents() {}

        override suspend fun deleteAllFlightDetails() {}

        override suspend fun deleteAllHotelDetails() {}

        override suspend fun deleteAllDiningDetails() {}

        override suspend fun insertVoiceNote(note: VoiceNoteEntity) {
          insertedVoiceNote = note
        }

        override suspend fun deleteVoiceNoteById(id: String) {}
      }

    val viewModel =
      VoiceNotesViewModel(
        SavedStateHandle(mapOf("tripId" to tripId)),
        fakeEventDao,
        VoiceInputManager(),
      )

    var callsCount = 0
    val busyException = GenAiException(null, 9)
    val expectedRewriteJson =
      """
      {
        "rewritten": "Clean Rewritten Voice Note",
        "matches": [
          {
            "eventId": "$eventId",
            "extract": "Clean Rewritten Voice Note"
          }
        ]
      }
    """
        .trimIndent()

    val fakeModel = FakeGenerativeModel {
      callsCount++
      if (callsCount == 1) {
        throw busyException
      }
      val candidateClass = Class.forName("com.google.mlkit.genai.prompt.Candidate")
      val candidateCompanionField = candidateClass.getField("Companion")
      val candidateCompanion = candidateCompanionField.get(null)
      val createCandidateMethod = candidateCompanion.javaClass.getMethod("zza", String::class.java, java.lang.Integer::class.java)
      val candidate = createCandidateMethod.invoke(candidateCompanion, expectedRewriteJson, 1)

      val responseClass = Class.forName("com.google.mlkit.genai.prompt.GenerateContentResponse")
      val responseCompanionField = responseClass.getField("Companion")
      val responseCompanion = responseCompanionField.get(null)
      val createResponseMethod = responseCompanion.javaClass.getMethod("zza", java.util.List::class.java)
      val response = createResponseMethod.invoke(responseCompanion, listOf(candidate)) as GenerateContentResponse
      response
    }

    viewModel.generativeModelTesting = fakeModel

    // Start collecting to make StateFlow hot
    val collectJob = launch { viewModel.events.collect {} }
    advanceUntilIdle()

    viewModel.processVoiceNote(
      "Dirty raw voice note transcription.",
      "Dirty raw voice note transcription.",
    )
    advanceUntilIdle()

    assertEquals(2, callsCount) // 1st was BUSY, 2nd succeeded
    assertEquals("Clean Rewritten Voice Note", updatedEvent?.audioNotes?.firstOrNull())
    assertEquals(
      "Dirty raw voice note transcription. |||| Dirty raw voice note transcription.",
      insertedVoiceNote?.transcription,
    )

    collectJob.cancel()
  }

  private class FakeGenerativeModel(
    private val onGenerate: () -> GenerateContentResponse
  ) : GenerativeModel {
    override val caches: Caches
      get() = TODO("Not implemented")

    override suspend fun getBaseModelName(): String = TODO("Not implemented")

    override suspend fun checkStatus(): Int = 3

    override suspend fun isCachingFeatureAvailable(): Boolean = TODO("Not implemented")


    override fun download():
      Flow<DownloadStatus> =
      TODO("Not implemented")

    override fun downloadForFutures(
      callback: DownloadCallback
    ): ListenableFuture<Void> = TODO("Not implemented")

    override suspend fun warmup() = TODO("Not implemented")

    override fun warmupForFutures(): ListenableFuture<Void> =
      TODO("Not implemented")

    override suspend fun countTokens(
      request: GenerateContentRequest
    ): CountTokensResponse = TODO("Not implemented")


    override fun countTokensForFutures(
      request: GenerateContentRequest
    ): ListenableFuture<
      CountTokensResponse
    > = TODO("Not implemented")

    override suspend fun getTokenLimit(): Int = TODO("Not implemented")

    override fun getTokenLimitForFutures():
      ListenableFuture<Int> = TODO("Not implemented")

    override suspend fun generateContent(
      request: GenerateContentRequest
    ): GenerateContentResponse = onGenerate()

    override suspend fun generateContent(
      request: GenerateContentRequest,
      streamingCallback: StreamingCallback,
    ): GenerateContentResponse = TODO("Not implemented")

    override fun generateContentStream(
      request: GenerateContentRequest
    ): Flow<GenerateContentResponse> =
      TODO("Not implemented")


    override suspend fun clearImplicitCaches() = TODO("Not implemented")

    override fun clearImplicitCachesForFutures():
      ListenableFuture<Void> = TODO("Not implemented")

    override fun close() {}

    override fun getWorkerExecutor(): ExecutorService = TODO("Not implemented")
  }
}
