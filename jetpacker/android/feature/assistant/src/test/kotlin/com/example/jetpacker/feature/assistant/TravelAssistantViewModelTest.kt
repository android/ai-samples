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

package com.example.jetpacker.feature.assistant

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.jetpacker.data.itinerary.ActivityDetail
import com.example.jetpacker.data.itinerary.DiningDetail
import com.example.jetpacker.data.itinerary.EventDao
import com.example.jetpacker.data.itinerary.Expense
import com.example.jetpacker.data.itinerary.ExpenseDao
import com.example.jetpacker.data.itinerary.FlightDetail
import com.example.jetpacker.data.itinerary.HotelDetail
import com.example.jetpacker.data.itinerary.MuseumDetail
import com.example.jetpacker.data.itinerary.TimelineEvent
import com.example.jetpacker.data.itinerary.TravelDocument
import com.example.jetpacker.data.itinerary.TravelDocumentDao
import com.example.jetpacker.data.itinerary.TravelDocumentManager
import com.example.jetpacker.data.itinerary.TripSessionIdentifier
import com.example.jetpacker.data.itinerary.VoiceNoteEntity
import com.example.jetpacker.data.trips.Trip
import com.example.jetpacker.data.trips.TripDao
import com.example.jetpacker.feature.assistant.documents.DocumentSemanticSearch
import com.example.jetpacker.feature.assistant.model.TravelAgentUiState.Idle
import com.google.common.truth.Truth.assertThat
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.GenerativeModel
import java.lang.reflect.Proxy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class TravelAssistantViewModelTest {

  private val testDispatcher = StandardTestDispatcher()

  private lateinit var fakeEventDao: TestEventDao
  private lateinit var fakeTripDao: TestTripDao
  private lateinit var fakeDocumentDao: TestTravelDocumentDao
  private lateinit var fakeExpenseDao: TestExpenseDao
  private lateinit var viewModel: TravelAssistantViewModel
  private lateinit var application: Application

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    application = ApplicationProvider.getApplicationContext()
    fakeEventDao = TestEventDao()
    fakeTripDao = TestTripDao()
    fakeDocumentDao = TestTravelDocumentDao()
    fakeExpenseDao = TestExpenseDao()

    val travelDocumentManager = TravelDocumentManager(application, fakeDocumentDao)
    viewModel =
      TravelAssistantViewModel(
          application = application,
          eventDao = fakeEventDao,
          tripDao = fakeTripDao,
          travelDocumentDao = fakeDocumentDao,
          travelDocumentManager = travelDocumentManager,
          expenseDao = fakeExpenseDao,
          documentSemanticSearch = DocumentSemanticSearch(fakeDocumentDao, travelDocumentManager),
        )
        .apply { testDispatcher = this@TravelAssistantViewModelTest.testDispatcher }
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun testSanitizeAssistantText_stripsControlTokensAndCodeBlocks() {
    val raw =
      "<ctrl42>```tool_code\ncall:get_flight_itinerary{}\n```\nHere is your **flight** information (tripId: 2026-4)."
    val cleaned = sanitizeAssistantText(raw)
    assertThat(cleaned).isEqualTo("Here is your flight information.")
  }

  @Test
  fun testInitialState_isIdle() {
    assertThat(viewModel.uiState.value).isEqualTo(Idle)
    assertThat(viewModel.samplePrompts).isNotEmpty()
  }

  @Test
  fun testClearChat_resetsStateToIdle() = runTest {
    viewModel.sendPrompt("Show my flight")
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.clearChat()
    testDispatcher.scheduler.advanceUntilIdle()

    assertThat(viewModel.uiState.value).isEqualTo(Idle)
  }

  @Test
  fun testSamplePrompts_containsPrompts() {
    assertThat(viewModel.samplePrompts).isNotEmpty()
    assertThat(viewModel.samplePrompts.first()).contains("expenses")
  }

  @Test
  fun testDynamicTripDaoQuery_reflectsInsertedTrips() = runTest {
    val trip =
      Trip(
        id = "trip-tokyo-2026",
        title = "Tokyo Explorer",
        location = "Tokyo, Japan",
        startDate = 1791590400000L,
        endDate = 1792108800000L,
      )
    fakeTripDao.insertTrip(trip)

    val allTrips = fakeTripDao.getAllTrips().first()
    assertThat(allTrips).contains(trip)
  }

  @Test
  fun testIsSupported_returnsTrueWhenAvailable() = runTest {
    val fakeGenerativeModel =
      Proxy.newProxyInstance(
        GenerativeModel::class.java.classLoader,
        arrayOf(GenerativeModel::class.java),
      ) { _, method, _ ->
        when (method.name) {
          "checkStatus" -> FeatureStatus.AVAILABLE
          else -> null
        }
      } as GenerativeModel

    viewModel.testGenerativeModel = fakeGenerativeModel
    val supported = viewModel.isSupported()
    assertThat(supported).isTrue()
  }

  @Test
  fun testIsSupported_downloadsWhenDownloadable() = runTest {
    var checkCount = 0
    val fakeGenerativeModel =
      Proxy.newProxyInstance(
        GenerativeModel::class.java.classLoader,
        arrayOf(GenerativeModel::class.java),
      ) { _, method, _ ->
        when (method.name) {
          "checkStatus" -> {
            checkCount++
            if (checkCount == 1) FeatureStatus.DOWNLOADABLE else FeatureStatus.AVAILABLE
          }
          "download" -> flowOf(DownloadStatus.DownloadCompleted)
          else -> null
        }
      } as GenerativeModel

    viewModel.testGenerativeModel = fakeGenerativeModel
    val supported = viewModel.isSupported()
    assertThat(supported).isTrue()
    assertThat(checkCount).isAtLeast(2)
  }

  @Test
  fun testIsSupported_returnsFalseWhenStatusThrows() = runTest {
    val fakeGenerativeModel =
      Proxy.newProxyInstance(
        GenerativeModel::class.java.classLoader,
        arrayOf(GenerativeModel::class.java),
      ) { _, method, _ ->
        if (method.name == "checkStatus") {
          throw RuntimeException("Model check failed")
        }
        null
      } as GenerativeModel

    viewModel.testGenerativeModel = fakeGenerativeModel
    val supported = viewModel.isSupported()
    assertThat(supported).isFalse()
  }

  @Test
  fun testSetTripId_updatesCurrentTripId() = runTest {
    assertThat(viewModel.currentTripId).isNull()

    viewModel.setTripId("trip-tokyo-2026")
    assertThat(viewModel.currentTripId).isEqualTo("trip-tokyo-2026")

    viewModel.setTripId(null)
    assertThat(viewModel.currentTripId).isNull()
  }
}

private class TestTripDao : TripDao {
  val trips = mutableListOf<Trip>()

  override fun getAllTrips(): Flow<List<Trip>> = flowOf(trips.toList())

  override fun getTripById(tripId: String): Flow<Trip?> = flowOf(trips.find { it.id == tripId })

  override suspend fun insertTrip(trip: Trip) {
    trips.add(trip)
  }

  override suspend fun insertTrips(trips: List<Trip>) {
    this.trips.addAll(trips)
  }

  override suspend fun deleteTrip(trip: Trip) {
    trips.remove(trip)
  }

  override suspend fun deleteAllTrips() {
    trips.clear()
  }
}

private class TestTravelDocumentDao : TravelDocumentDao {
  val documents = mutableListOf<TravelDocument>()

  override fun getAllDocuments(): Flow<List<TravelDocument>> = flowOf(documents.toList())

  override fun getDocumentsByType(documentType: String): Flow<List<TravelDocument>> =
    flowOf(documents.filter { it.documentType.equals(documentType, ignoreCase = true) })

  override fun getDocumentsForTrip(tripId: String): Flow<List<TravelDocument>> =
    flowOf(documents.filter { it.tripId == tripId })

  override fun getDocumentsForEvent(eventId: String): Flow<List<TravelDocument>> =
    flowOf(documents.filter { it.eventId == eventId })

  override suspend fun getDocumentById(documentId: String): TravelDocument? = documents.find {
    it.documentId == documentId
  }

  override suspend fun insertDocument(document: TravelDocument) {
    documents.add(document)
  }

  override suspend fun linkDocumentToEvent(documentId: String, eventId: String?, tripId: String?) {
    val idx = documents.indexOfFirst { it.documentId == documentId || it.id == documentId }
    if (idx != -1) {
      val existing = documents[idx]
      documents[idx] = existing.copy(eventId = eventId, tripId = tripId ?: existing.tripId)
    }
  }

  override suspend fun linkDocumentToTrip(documentId: String, tripId: String?) {
    val idx = documents.indexOfFirst { it.documentId == documentId || it.id == documentId }
    if (idx != -1) {
      val existing = documents[idx]
      documents[idx] = existing.copy(tripId = tripId)
    }
  }

  override suspend fun deleteDocument(document: TravelDocument) {
    documents.remove(document)
  }

  override suspend fun deleteDocumentById(id: String) {
    documents.removeAll { it.documentId == id }
  }

  override suspend fun deleteAllDocuments() {
    documents.clear()
  }
}

private class TestEventDao : EventDao {
  val events = mutableListOf<TimelineEvent>()
  val voiceNotes = mutableListOf<VoiceNoteEntity>()

  override fun getAllSessionIds(): Flow<List<TripSessionIdentifier>> = flowOf(emptyList())

  override fun getEventsForTrip(tripId: String): Flow<List<TimelineEvent>> =
    flowOf(events.filter { it.tripId == tripId })

  override suspend fun insertEvent(event: TimelineEvent) {
    events.removeAll { it.id == event.id }
    events.add(event)
  }

  override suspend fun insertEvents(events: List<TimelineEvent>) {
    events.forEach { insertEvent(it) }
  }

  override suspend fun deleteEventsForTrip(tripId: String) {
    events.removeAll { it.tripId == tripId }
  }

  override suspend fun deleteEvent(event: TimelineEvent) {
    events.remove(event)
  }

  override suspend fun insertFlightDetail(detail: FlightDetail) {}

  override suspend fun insertHotelDetail(detail: HotelDetail) {}

  override suspend fun insertDiningDetail(detail: DiningDetail) {}

  override suspend fun insertActivityDetail(detail: ActivityDetail) {}

  override suspend fun insertMuseumDetail(detail: MuseumDetail) {}

  override fun getFlightDetail(eventId: String): Flow<FlightDetail?> = flowOf(null)

  override fun getHotelDetail(eventId: String): Flow<HotelDetail?> = flowOf(null)

  override fun getDiningDetail(eventId: String): Flow<DiningDetail?> = flowOf(null)

  override fun getMuseumDetail(eventId: String): Flow<MuseumDetail?> = flowOf(null)

  override fun getEventById(eventId: String): Flow<TimelineEvent?> =
    flowOf(events.find { it.id == eventId })

  override suspend fun deleteAllEvents() {
    events.clear()
  }

  override suspend fun deleteAllFlightDetails() {}

  override suspend fun deleteAllHotelDetails() {}

  override suspend fun deleteAllDiningDetails() {}

  override suspend fun deleteAllActivityDetails() {}

  override fun getVoiceNotesForTrip(tripId: String): Flow<List<VoiceNoteEntity>> =
    flowOf(voiceNotes.filter { it.tripId == tripId })

  override suspend fun insertVoiceNote(note: VoiceNoteEntity) {
    voiceNotes.add(note)
  }

  override suspend fun deleteVoiceNoteById(id: String) {
    voiceNotes.removeAll { it.id == id }
  }
}

private class TestExpenseDao : ExpenseDao {
  val expenses = mutableListOf<Expense>()

  override fun getAllExpenses(): Flow<List<Expense>> = flowOf(expenses.toList())

  override fun getExpensesForTrip(tripId: String): Flow<List<Expense>> =
    flowOf(expenses.filter { it.tripId == tripId })

  override suspend fun insertExpense(expense: Expense) {
    expenses.add(expense)
  }

  override suspend fun deleteExpense(expense: Expense) {
    expenses.remove(expense)
  }

  override suspend fun deleteAllExpenses() {
    expenses.clear()
  }
}
