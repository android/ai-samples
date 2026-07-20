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

package com.example.jetpacker.feature.itinerary

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.jetpacker.data.itinerary.ActivityDetail
import com.example.jetpacker.data.itinerary.DiningDetail
import com.example.jetpacker.data.itinerary.EventDao
import com.example.jetpacker.data.itinerary.FlightDetail
import com.example.jetpacker.data.itinerary.HotelDetail
import com.example.jetpacker.data.itinerary.MuseumDetail
import com.example.jetpacker.data.itinerary.TimelineEvent
import com.example.jetpacker.data.itinerary.TourDetail
import com.example.jetpacker.data.itinerary.TourDetailDao
import com.example.jetpacker.data.itinerary.TripSessionIdentifier
import com.example.jetpacker.data.itinerary.VoiceNoteEntity
import com.example.jetpacker.data.trips.DummyData
import com.example.jetpacker.data.trips.Trip
import com.example.jetpacker.data.trips.TripDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ItineraryViewModelTest {
  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
  }

  @Test
  fun loadEvents_updatesUiState_correctly() = runTest {
    val mockTrip = DummyData.trips.first()
    val mockEvents = DummyData.events.filter { it.tripId == mockTrip.id }

    val fakeSavedStateHandle = SavedStateHandle(mapOf("tripId" to mockTrip.id))

    val fakeEventDao =
      object : EventDao {
        override fun getEventsForTrip(tripId: String): Flow<List<TimelineEvent>> = flowOf(mockEvents)
        override fun getAllSessionIds() = flowOf(emptyList<TripSessionIdentifier>())
        override suspend fun deleteEventsForTrip(tripId: String) {}
        override suspend fun insertEvents(events: List<TimelineEvent>) {}
        override suspend fun insertEvent(event: TimelineEvent) {}
        override suspend fun deleteEvent(event: TimelineEvent) {}
        override suspend fun insertFlightDetail(detail: FlightDetail) {}
        override suspend fun insertHotelDetail(detail: HotelDetail) {}
        override suspend fun insertDiningDetail(detail: DiningDetail) {}
        override suspend fun insertActivityDetail(detail: ActivityDetail) {}
        override suspend fun insertMuseumDetail(detail: MuseumDetail) {}
        override fun getFlightDetail(eventId: String) = flowOf(null)
        override fun getHotelDetail(eventId: String) = flowOf(null)
        override fun getDiningDetail(eventId: String) = flowOf(null)
        override fun getMuseumDetail(eventId: String) = flowOf(null)
        override fun getVoiceNotesForTrip(tripId: String): Flow<List<VoiceNoteEntity>> = flowOf(emptyList())
        override suspend fun insertVoiceNote(note: VoiceNoteEntity) {}
        override suspend fun deleteVoiceNoteById(id: String) {}
        override fun getEventById(eventId: String) = flowOf(null)
        override suspend fun deleteAllEvents() {}
        override suspend fun deleteAllFlightDetails() {}
        override suspend fun deleteAllHotelDetails() {}
        override suspend fun deleteAllDiningDetails() {}
        override suspend fun deleteAllActivityDetails() {}
      }

    val fakeTripDao =
      object : TripDao {
        override fun getAllTrips(): Flow<List<Trip>> = flowOf(listOf(mockTrip))
        override fun getTripById(tripId: String): Flow<Trip?> = flowOf(mockTrip)
        override suspend fun insertTrip(trip: Trip) {}
        override suspend fun insertTrips(trips: List<Trip>) {}
        override suspend fun deleteTrip(trip: Trip) {}
        override suspend fun deleteAllTrips() {}
      }

    val fakeTourDetailDao =
      object : TourDetailDao {
        override fun getTourDetailByEventId(eventId: String): Flow<TourDetail?> = emptyFlow()
        override fun getTourDetailById(id: String): Flow<TourDetail?> = emptyFlow()
        override suspend fun insertTourDetail(tourDetail: TourDetail) {}
        override suspend fun insertTourDetails(tourDetails: List<TourDetail>) {}
        override suspend fun deleteAllTourDetails() {}
      }

    val viewModel =
      ItineraryViewModel(
        fakeSavedStateHandle,
        fakeEventDao,
        fakeTourDetailDao,
        fakeTripDao,
      )

    advanceUntilIdle()

    val uiState = viewModel.uiState.first()
    assertEquals(mockTrip, uiState.trip)
  }

  @Test
  fun testMultiDayEventGrouping() = runTest {
    val mockTrip = DummyData.trips.first()
    val baseEvent = DummyData.events.first()
    val multiDayEvents = listOf(
      baseEvent.copy(id = "1", tripId = mockTrip.id, timestamp = 1779192000000), // Day 1
      baseEvent.copy(id = "2", tripId = mockTrip.id, timestamp = 1779278400000)  // Day 2 (+24h)
    )

    val fakeSavedStateHandle = SavedStateHandle(mapOf("tripId" to mockTrip.id))

    val fakeEventDao = object : EventDao {
      override fun getEventsForTrip(tripId: String) = flowOf(multiDayEvents)
      override fun getAllSessionIds() = flowOf(emptyList<TripSessionIdentifier>())
      override suspend fun deleteEventsForTrip(tripId: String) {}
      override suspend fun insertEvents(events: List<TimelineEvent>) {}
      override suspend fun insertEvent(event: TimelineEvent) {}
      override suspend fun deleteEvent(event: TimelineEvent) {}
      override suspend fun insertFlightDetail(detail: FlightDetail) {}
      override suspend fun insertHotelDetail(detail: HotelDetail) {}
      override suspend fun insertDiningDetail(detail: DiningDetail) {}
      override suspend fun insertActivityDetail(detail: ActivityDetail) {}
      override suspend fun insertMuseumDetail(detail: MuseumDetail) {}
      override fun getFlightDetail(eventId: String) = flowOf(null)
      override fun getHotelDetail(eventId: String) = flowOf(null)
      override fun getDiningDetail(eventId: String) = flowOf(null)
      override fun getMuseumDetail(eventId: String) = flowOf(null)
      override fun getVoiceNotesForTrip(tripId: String) = flowOf(emptyList<VoiceNoteEntity>())
      override suspend fun insertVoiceNote(note: VoiceNoteEntity) {}
      override suspend fun deleteVoiceNoteById(id: String) {}
      override fun getEventById(eventId: String) = flowOf(null)
      override suspend fun deleteAllEvents() {}
      override suspend fun deleteAllFlightDetails() {}
      override suspend fun deleteAllHotelDetails() {}
      override suspend fun deleteAllDiningDetails() {}
      override suspend fun deleteAllActivityDetails() {}
    }

    val fakeTourDetailDao = object : TourDetailDao {
      override fun getTourDetailByEventId(eventId: String) = emptyFlow<TourDetail?>()
      override fun getTourDetailById(id: String) = emptyFlow<TourDetail?>()
      override suspend fun insertTourDetail(tourDetail: TourDetail) {}
      override suspend fun insertTourDetails(tourDetails: List<TourDetail>) {}
      override suspend fun deleteAllTourDetails() {}
    }

    val fakeTripDao = object : TripDao {
      override fun getAllTrips() = flowOf(listOf(mockTrip))
      override fun getTripById(tripId: String) = flowOf(mockTrip)
      override suspend fun insertTrip(trip: Trip) {}
      override suspend fun insertTrips(trips: List<Trip>) {}
      override suspend fun deleteTrip(trip: Trip) {}
      override suspend fun deleteAllTrips() {}
    }

    val viewModel = ItineraryViewModel(
      fakeSavedStateHandle,
      fakeEventDao,
      fakeTourDetailDao,
      fakeTripDao,
    )
    advanceUntilIdle()
    val items = viewModel.uiState.first().items
    val headers = items.filterIsInstance<ItineraryUiListItem.Header>()
    assertEquals(2, headers.size)
    assertEquals(1, headers[0].dayNumber)
    assertEquals(2, headers[1].dayNumber)
  }
}
