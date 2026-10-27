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

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.jetpacker.data.itinerary.ActivityDetail
import com.example.jetpacker.data.itinerary.DiningDetail
import com.example.jetpacker.data.itinerary.EventDao
import com.example.jetpacker.data.itinerary.FlightDetail
import com.example.jetpacker.data.itinerary.HotelDetail
import com.example.jetpacker.data.itinerary.MuseumDetail
import com.example.jetpacker.data.itinerary.TimelineEvent
import com.example.jetpacker.data.itinerary.TravelDocument
import com.example.jetpacker.data.itinerary.TravelDocumentDao
import com.example.jetpacker.data.itinerary.TravelDocumentManager
import com.example.jetpacker.data.itinerary.TripSessionIdentifier
import com.example.jetpacker.data.itinerary.VoiceNoteEntity
import com.example.jetpacker.feature.assistant.documents.DocumentAppSearchManager
import com.example.jetpacker.feature.assistant.documents.DocumentSemanticSearch
import com.example.jetpacker.feature.assistant.documents.DocumentVisionScanner
import com.example.jetpacker.feature.assistant.documents.DocumentsViewModel
import com.google.common.truth.Truth.assertThat
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Covers the write-through indexing hooks that make Demo 1.B's "save it now, ask about it now"
 * story work.
 *
 * [DocumentSemanticSearchTest] proves the index itself behaves; these tests prove the ViewModel
 * actually calls into it on every mutation. Without them a dropped `indexInVault` call would leave
 * every unit test green while the live demo silently failed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class DocumentsViewModelTest {

  private val testDispatcher = StandardTestDispatcher()

  private lateinit var context: Context
  private lateinit var documentDao: FakeVaultDocumentDao
  private lateinit var appSearchManager: DocumentAppSearchManager
  private lateinit var semanticSearch: DocumentSemanticSearch
  private lateinit var viewModel: DocumentsViewModel

  @Before
  fun setUp() {
    try {
      System.loadLibrary("icing")
    } catch (t: Throwable) {
      Assume.assumeNoException(
        "Icing native library requires an Android runtime; skipping on host JVM",
        t,
      )
    }
    Dispatchers.setMain(testDispatcher)
    context = ApplicationProvider.getApplicationContext()
    documentDao = FakeVaultDocumentDao()
    appSearchManager = DocumentAppSearchManager(context)
    val manager = TravelDocumentManager(context, documentDao)
    semanticSearch =
      DocumentSemanticSearch(
        travelDocumentDao = documentDao,
        travelDocumentManager = manager,
        appSearchManager = appSearchManager,
      )
    viewModel =
      DocumentsViewModel(
        travelDocumentManager = manager,
        eventDao = FakeVaultEventDao(),
        documentVisionScanner = DocumentVisionScanner(),
        documentSemanticSearch = semanticSearch,
      )
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun createTextDocument_isIndexedWithoutWaitingForASearch() = runTest {
    settleUntil { documentDao.snapshot().isNotEmpty() }

    viewModel.createTextDocument(
      title = "Hotel Lumiere Booking",
      content = "Wifi network LUMIERE-GUEST, password bonjour2026",
      documentType = "note",
      tripId = TRIP_ID,
    )
    settleUntil { documentDao.snapshot().any { it.title == "Hotel Lumiere Booking" } }
    val created = documentDao.snapshot().single { it.title == "Hotel Lumiere Booking" }

    // The write-through hook is the only thing that can put this id in the index; nothing else
    // re-syncs after a create.
    settleUntil { appSearchManager.getIndexedDocumentIds().contains(created.id) }

    assertThat(appSearchManager.getIndexedDocumentIds()).contains(created.id)
  }

  @Test
  fun searchVault_findsADocumentCreatedMomentsEarlier() = runTest {
    createLumiereNote()

    viewModel.onVaultQueryChange("lumiere wifi")
    viewModel.searchVault(TRIP_ID)
    settleUntil { viewModel.vaultResults.value != null }

    assertThat(viewModel.vaultResults.value?.map { it.title }).contains("Hotel Lumiere Booking")
  }

  @Test
  fun searchVault_reportsMatchCountAndOnDeviceProvenance() = runTest {
    createLumiereNote()

    viewModel.onVaultQueryChange("lumiere wifi")
    viewModel.searchVault(TRIP_ID)
    settleUntil { viewModel.vaultSearchStatus.value != null }

    assertThat(viewModel.vaultSearchStatus.value).contains("100% on-device")
    assertThat(viewModel.vaultSearchStatus.value).contains("ms")
  }

  @Test
  fun deleteDocument_dropsItFromTheIndexAndFromLiveResults() = runTest {
    val created = createLumiereNote()

    viewModel.onVaultQueryChange("lumiere wifi")
    viewModel.searchVault(TRIP_ID)
    settleUntil { viewModel.vaultResults.value != null }

    viewModel.deleteDocument(created)
    settleUntil { !appSearchManager.getIndexedDocumentIds().contains(created.id) }

    assertThat(appSearchManager.getIndexedDocumentIds()).doesNotContain(created.id)
    assertThat(viewModel.vaultResults.value?.map { it.id } ?: emptyList<String>())
      .doesNotContain(created.id)
  }

  @Test
  fun clearVaultSearch_restoresTheBrowsingList() = runTest {
    advanceUntilIdle()

    viewModel.onVaultQueryChange("lumiere wifi")
    viewModel.searchVault(TRIP_ID)
    advanceUntilIdle()

    viewModel.clearVaultSearch()

    assertThat(viewModel.vaultQuery.value).isEmpty()
    assertThat(viewModel.vaultResults.value).isNull()
    assertThat(viewModel.vaultSearchStatus.value).isNull()
  }

  @Test
  fun importDocument_indexesEmailAndFindsItInVaultSearch() = runTest {
    settleUntil { documentDao.snapshot().isNotEmpty() }
    val emlFile = File(context.cacheDir, "shinkansen_ticket.eml")
    emlFile.writeText(
      """
      From: reserve@jr-central.co.jp
      Subject: Shinkansen Nozomi 204 E-Ticket
      Content-Type: text/html; charset="UTF-8"

      <html><body><p>Car 7 Seat 12E</p><p>QR Pickup Code: JR994021</p></body></html>
      """
        .trimIndent()
    )

    viewModel.importDocument(uri = Uri.fromFile(emlFile), tripId = TRIP_ID)
    settleUntil { documentDao.snapshot().any { it.title == "Shinkansen Nozomi 204 E-Ticket" } }
    val imported = documentDao.snapshot().single { it.title == "Shinkansen Nozomi 204 E-Ticket" }
    settleUntil { appSearchManager.getIndexedDocumentIds().contains(imported.id) }

    viewModel.onVaultQueryChange("JR994021")
    viewModel.searchVault(TRIP_ID)
    settleUntil { viewModel.vaultResults.value != null }

    assertThat(viewModel.vaultResults.value?.map { it.title })
      .contains("Shinkansen Nozomi 204 E-Ticket")
  }

  /** Creates the demo note and waits until the write-through hook has indexed it. */
  private suspend fun TestScope.createLumiereNote(): TravelDocument {
    settleUntil { documentDao.snapshot().isNotEmpty() }
    viewModel.createTextDocument(
      title = "Hotel Lumiere Booking",
      content = "Wifi network LUMIERE-GUEST, password bonjour2026",
      documentType = "note",
      tripId = TRIP_ID,
    )
    settleUntil { documentDao.snapshot().any { it.title == "Hotel Lumiere Booking" } }
    val created = documentDao.snapshot().single { it.title == "Hotel Lumiere Booking" }
    settleUntil { appSearchManager.getIndexedDocumentIds().contains(created.id) }
    return created
  }

  /**
   * Drains both clocks until [condition] holds.
   *
   * The ViewModel runs on the virtual test scheduler but the indexing and search paths hop to the
   * IO dispatcher, which that scheduler does not own. `advanceUntilIdle()` alone therefore returns
   * while real work is still in flight, so each pass advances the scheduler and then parks this
   * thread to give the IO side real wall-clock time to catch up.
   */
  private suspend fun TestScope.settleUntil(condition: suspend () -> Boolean) {
    repeat(SETTLE_ATTEMPTS) {
      advanceUntilIdle()
      if (condition()) return
      Thread.sleep(SETTLE_STEP_MS)
    }
    advanceUntilIdle()
  }

  private companion object {
    const val TRIP_ID = "2026-3"
    const val SETTLE_ATTEMPTS = 100
    const val SETTLE_STEP_MS = 50L
  }
}

/**
 * Minimal in-memory [TravelDocumentDao] so the tests never touch Room.
 *
 * Every access is synchronized: the ViewModel writes from IO threads while the test thread reads,
 * and an unguarded [ArrayList] throws [java.util.ConcurrentModificationException] under that race.
 */
private class FakeVaultDocumentDao : TravelDocumentDao {
  private val backing = mutableListOf<TravelDocument>()

  /** Immutable copy, safe to iterate from the test thread. */
  fun snapshot(): List<TravelDocument> = synchronized(backing) { backing.toList() }

  override fun getAllDocuments(): Flow<List<TravelDocument>> = flowOf(snapshot())

  override fun getDocumentsByType(documentType: String): Flow<List<TravelDocument>> =
    flowOf(snapshot().filter { it.documentType.equals(documentType, ignoreCase = true) })

  override fun getDocumentsForTrip(tripId: String): Flow<List<TravelDocument>> =
    flowOf(snapshot().filter { it.tripId == tripId })

  override fun getDocumentsForEvent(eventId: String): Flow<List<TravelDocument>> =
    flowOf(snapshot().filter { it.eventId == eventId })

  override suspend fun getDocumentById(documentId: String): TravelDocument? =
    snapshot().find { it.documentId == documentId || it.id == documentId }

  override suspend fun insertDocument(document: TravelDocument) {
    synchronized(backing) { backing.add(document) }
  }

  override suspend fun linkDocumentToEvent(documentId: String, eventId: String?, tripId: String?) {
    synchronized(backing) {
      val index = backing.indexOfFirst { it.documentId == documentId || it.id == documentId }
      if (index != -1) {
        val existing = backing[index]
        backing[index] = existing.copy(eventId = eventId, tripId = tripId ?: existing.tripId)
      }
    }
  }

  override suspend fun linkDocumentToTrip(documentId: String, tripId: String?) {
    synchronized(backing) {
      val index = backing.indexOfFirst { it.documentId == documentId || it.id == documentId }
      if (index != -1) {
        backing[index] = backing[index].copy(tripId = tripId)
      }
    }
  }

  override suspend fun deleteDocument(document: TravelDocument) {
    synchronized(backing) { backing.removeAll { it.id == document.id } }
  }

  override suspend fun deleteDocumentById(id: String) {
    synchronized(backing) { backing.removeAll { it.id == id } }
  }

  override suspend fun deleteAllDocuments() {
    synchronized(backing) { backing.clear() }
  }
}

/** [EventDao] stub; this suite only needs the document side of the ViewModel. */
private class FakeVaultEventDao : EventDao {
  override fun getAllSessionIds(): Flow<List<TripSessionIdentifier>> = flowOf(emptyList())

  override fun getEventsForTrip(tripId: String): Flow<List<TimelineEvent>> = flowOf(emptyList())

  override suspend fun insertEvent(event: TimelineEvent) {}

  override suspend fun insertEvents(events: List<TimelineEvent>) {}

  override suspend fun deleteEventsForTrip(tripId: String) {}

  override suspend fun deleteEvent(event: TimelineEvent) {}

  override suspend fun insertFlightDetail(detail: FlightDetail) {}

  override suspend fun insertHotelDetail(detail: HotelDetail) {}

  override suspend fun insertDiningDetail(detail: DiningDetail) {}

  override suspend fun insertActivityDetail(detail: ActivityDetail) {}

  override suspend fun insertMuseumDetail(detail: MuseumDetail) {}

  override fun getFlightDetail(eventId: String): Flow<FlightDetail?> = flowOf(null)

  override fun getHotelDetail(eventId: String): Flow<HotelDetail?> = flowOf(null)

  override fun getDiningDetail(eventId: String): Flow<DiningDetail?> = flowOf(null)

  override fun getMuseumDetail(eventId: String): Flow<MuseumDetail?> = flowOf(null)

  override fun getEventById(eventId: String): Flow<TimelineEvent?> = flowOf(null)

  override suspend fun deleteAllEvents() {}

  override suspend fun deleteAllFlightDetails() {}

  override suspend fun deleteAllHotelDetails() {}

  override suspend fun deleteAllDiningDetails() {}

  override suspend fun deleteAllActivityDetails() {}

  override fun getVoiceNotesForTrip(tripId: String): Flow<List<VoiceNoteEntity>> =
    flowOf(emptyList())

  override suspend fun insertVoiceNote(note: VoiceNoteEntity) {}

  override suspend fun deleteVoiceNoteById(id: String) {}
}
