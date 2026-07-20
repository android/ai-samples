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

import com.example.jetpacker.feature.itinerary_enrichment.DailyThemeProvider
import com.example.jetpacker.feature.itinerary_enrichment.DayThemeItem
import com.example.jetpacker.feature.itinerary_enrichment.TripSummaryAndTipsProvider
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.jetpacker.data.itinerary.ActivityDetail
import com.example.jetpacker.data.itinerary.DayTheme
import com.example.jetpacker.data.itinerary.DayThemeDao
import com.example.jetpacker.data.itinerary.DiningDetail
import com.example.jetpacker.data.itinerary.EventDao
import com.example.jetpacker.data.itinerary.EventType
import com.example.jetpacker.data.itinerary.FlightDetail
import com.example.jetpacker.data.itinerary.HotelDetail
import com.example.jetpacker.data.itinerary.MuseumDetail
import com.example.jetpacker.data.itinerary.TimelineEvent
import com.example.jetpacker.data.itinerary.TourDetail
import com.example.jetpacker.data.itinerary.TourDetailDao
import com.example.jetpacker.data.itinerary.TripSessionIdentifier
import com.example.jetpacker.data.itinerary.VoiceNoteEntity
import com.example.jetpacker.data.trips.Trip
import com.example.jetpacker.data.trips.TripDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
@OptIn(ExperimentalCoroutinesApi::class)
class ItineraryViewModelTest {

  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
  }

  @Test
  fun generateTripSummaryAndTips_updatesUiState_whenSupported() = runTest {
    val fakeSavedStateHandle = SavedStateHandle(mapOf("tripId" to "trip123"))

    val mockEvents =
      listOf(
        TimelineEvent(
          id = "1",
          tripId = "trip123",
          type = EventType.TRANSPORTATION,
          timestamp = System.currentTimeMillis(),
          title = "Flight to Paris",
          location = "CDG Airport",
          description = null,
          extraInfo = null,
          imageResList = emptyList(),
        )
      )

    val mockTrip =
      Trip(id = "trip123", title = "Test Trip", location = "Paris", startDate = 0L, endDate = 0L)

    val fakeEventDao =
      object : EventDao {
        override fun getAllSessionIds() = flowOf(emptyList<TripSessionIdentifier>())

        override fun getEventsForTrip(tripId: String) = flowOf(mockEvents)

        override suspend fun insertEvent(event: TimelineEvent) {}

        override suspend fun insertEvents(events: List<TimelineEvent>) {}

        override suspend fun deleteEventsForTrip(tripId: String) {}

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

        override suspend fun insertMuseumDetail(detail: MuseumDetail) {}

        override fun getFlightDetail(eventId: String) = flowOf(null)

        override fun getHotelDetail(eventId: String) = flowOf(null)

        override fun getDiningDetail(eventId: String) = flowOf(null)

        override fun getMuseumDetail(eventId: String) = flowOf(null)

        override fun getVoiceNotesForTrip(tripId: String) =
          flowOf(emptyList<VoiceNoteEntity>())

        override suspend fun insertVoiceNote(
          note: VoiceNoteEntity
        ) {}

        override suspend fun deleteVoiceNoteById(id: String) {}

        override fun getEventById(eventId: String) = flowOf(null)

        override suspend fun deleteAllEvents() {}

        override suspend fun deleteAllFlightDetails() {}

        override suspend fun deleteAllHotelDetails() {}

        override suspend fun deleteAllDiningDetails() {}

        override suspend fun deleteAllActivityDetails() {}
      }

    val fakeTripSummaryAndTipsProvider =
      object : TripSummaryAndTipsProvider {
        override suspend fun isSupported(): Boolean = true

        override fun generateTripSummaryAndTips(events: List<TimelineEvent>, trip: Trip): Flow<String> {
          return flowOf("Mocked energetic trip summary and tips!")
        }

        override fun generateTripSummary(
          events: List<TimelineEvent>,
          trip: Trip,
          voiceNotes: List<VoiceNoteEntity>,
        ): Flow<String> = flowOf("Mocked energetic trip summary and tips!")

        override fun generateUpcomingTip(events: List<TimelineEvent>, trip: Trip): Flow<String> =
          flowOf("")
      }

    val fakeDailyThemeProvider =
      object : DailyThemeProvider {
        override suspend fun generateDailyThemes(events: List<TimelineEvent>): List<DayThemeItem> {
          return emptyList()
        }
      }

    val fakeDayThemeDao =
      object : DayThemeDao {
        override fun getThemesForTrip(tripId: String) = flowOf(emptyList<DayTheme>())

        override suspend fun insertTheme(theme: DayTheme) {}

        override suspend fun insertThemes(themes: List<DayTheme>) {}

        override suspend fun deleteThemesForTrip(tripId: String) {}

        override suspend fun deleteAllThemes() {}
      }

    val fakeTripDao =
      object : TripDao {
        override fun getAllTrips() = flowOf(emptyList<Trip>())

        override fun getTripById(tripId: String) = flowOf(mockTrip)

        override suspend fun insertTrip(trip: Trip) {}

        override suspend fun insertTrips(trips: List<Trip>) {}

        override suspend fun deleteTrip(trip: Trip) {}

        override suspend fun deleteAllTrips() {}
      }

    val fakeTourDetailDao =
      object : TourDetailDao {
        override fun getTourDetailByEventId(eventId: String) = flowOf(null)
        override fun getTourDetailById(id: String) = flowOf(null)
        override suspend fun insertTourDetail(tourDetail: TourDetail) {}
        override suspend fun insertTourDetails(tourDetails: List<TourDetail>) {}
        override suspend fun deleteAllTourDetails() {}
      }

    val viewModel =
      ItineraryViewModel(
        fakeSavedStateHandle,
        fakeEventDao,
        fakeTourDetailDao,
        fakeDayThemeDao,
        fakeTripDao,
        fakeTripSummaryAndTipsProvider,
        fakeDailyThemeProvider,
      )

    advanceUntilIdle()

    val uiState = viewModel.uiState.first()
    assertTrue(uiState.isTripSummaryAndTipsSupported)
    assertEquals("Mocked energetic trip summary and tips!", uiState.tripSummaryAndTips)
    assertFalse(uiState.isTripSummaryAndTipsLoading)
  }

  @Test
  fun generateTripSummaryAndTips_updatesUiState_whenUnsupported() = runTest {
    val fakeSavedStateHandle = SavedStateHandle(mapOf("tripId" to "trip123"))

    val mockEvents =
      listOf(
        TimelineEvent(
          id = "1",
          tripId = "trip123",
          type = EventType.TRANSPORTATION,
          timestamp = System.currentTimeMillis(),
          title = "Flight to Paris",
          location = "CDG Airport",
          description = null,
          extraInfo = null,
          imageResList = emptyList(),
        )
      )

    val mockTrip =
      Trip(id = "trip123", title = "Test Trip", location = "Paris", startDate = 0L, endDate = 0L)

    val fakeEventDao =
      object : EventDao {
        override fun getAllSessionIds() = flowOf(emptyList<TripSessionIdentifier>())

        override fun getEventsForTrip(tripId: String) = flowOf(mockEvents)

        override suspend fun insertEvent(event: TimelineEvent) {}

        override suspend fun insertEvents(events: List<TimelineEvent>) {}

        override suspend fun deleteEventsForTrip(tripId: String) {}

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

        override suspend fun insertMuseumDetail(detail: MuseumDetail) {}

        override fun getFlightDetail(eventId: String) = flowOf(null)

        override fun getHotelDetail(eventId: String) = flowOf(null)

        override fun getDiningDetail(eventId: String) = flowOf(null)

        override fun getMuseumDetail(eventId: String) = flowOf(null)

        override fun getVoiceNotesForTrip(tripId: String) =
          flowOf(emptyList<VoiceNoteEntity>())

        override suspend fun insertVoiceNote(
          note: VoiceNoteEntity
        ) {}

        override suspend fun deleteVoiceNoteById(id: String) {}

        override fun getEventById(eventId: String) = flowOf(null)

        override suspend fun deleteAllEvents() {}

        override suspend fun deleteAllFlightDetails() {}

        override suspend fun deleteAllHotelDetails() {}

        override suspend fun deleteAllDiningDetails() {}

        override suspend fun deleteAllActivityDetails() {}
      }

    val fakeTripSummaryAndTipsProvider =
      object : TripSummaryAndTipsProvider {
        override suspend fun isSupported(): Boolean = false

        override fun generateTripSummaryAndTips(events: List<TimelineEvent>, trip: Trip) = flowOf("")

        override fun generateTripSummary(
          events: List<TimelineEvent>,
          trip: Trip,
          voiceNotes: List<VoiceNoteEntity>,
        ): Flow<String> = flowOf("")

        override fun generateUpcomingTip(events: List<TimelineEvent>, trip: Trip): Flow<String> =
          flowOf("")
      }

    val fakeDailyThemeProvider =
      object : DailyThemeProvider {
        override suspend fun generateDailyThemes(events: List<TimelineEvent>): List<DayThemeItem> =
          emptyList()
      }

    val fakeDayThemeDao =
      object : DayThemeDao {
        override fun getThemesForTrip(tripId: String) = flowOf(emptyList<DayTheme>())

        override suspend fun insertTheme(theme: DayTheme) {}

        override suspend fun insertThemes(themes: List<DayTheme>) {}

        override suspend fun deleteThemesForTrip(tripId: String) {}

        override suspend fun deleteAllThemes() {}
      }

    val fakeTripDao =
      object : TripDao {
        override fun getAllTrips() = flowOf(emptyList<Trip>())

        override fun getTripById(tripId: String) = flowOf(mockTrip)

        override suspend fun insertTrip(trip: Trip) {}

        override suspend fun insertTrips(trips: List<Trip>) {}

        override suspend fun deleteTrip(trip: Trip) {}

        override suspend fun deleteAllTrips() {}
      }

    val fakeTourDetailDao =
      object : TourDetailDao {
        override fun getTourDetailByEventId(eventId: String) = flowOf(null)
        override fun getTourDetailById(id: String) = flowOf(null)
        override suspend fun insertTourDetail(tourDetail: TourDetail) {}
        override suspend fun insertTourDetails(tourDetails: List<TourDetail>) {}
        override suspend fun deleteAllTourDetails() {}
      }

    val viewModel =
      ItineraryViewModel(
        fakeSavedStateHandle,
        fakeEventDao,
        fakeTourDetailDao,
        fakeDayThemeDao,
        fakeTripDao,
        fakeTripSummaryAndTipsProvider,
        fakeDailyThemeProvider,
      )

    advanceUntilIdle()

    val uiState = viewModel.uiState.first()
    assertFalse(uiState.isTripSummaryAndTipsSupported)
  }

  @Test
  fun generateTripSummaryAndTips_usesPersistedSummary_whenPresent() = runTest {
    val fakeSavedStateHandle = SavedStateHandle(mapOf("tripId" to "trip123"))

    val mockEvents =
      listOf(
        TimelineEvent(
          id = "1",
          tripId = "trip123",
          type = EventType.TRANSPORTATION,
          timestamp = System.currentTimeMillis(),
          title = "Flight to Paris",
          location = "CDG Airport",
          description = null,
          extraInfo = null,
          imageResList = emptyList(),
        )
      )

    val mockTrip =
      Trip(
        id = "trip123",
        title = "Test Trip",
        location = "Paris",
        startDate = 0L,
        endDate = 0L,
        tripSummaryAndTips = "Persisted trip summary and tips",
      )

    val fakeEventDao =
      object : EventDao {
        override fun getAllSessionIds() =
          kotlinx.coroutines.flow.flowOf(emptyList<TripSessionIdentifier>())

        override fun getEventsForTrip(tripId: String) = kotlinx.coroutines.flow.flowOf(mockEvents)

        override suspend fun insertEvent(event: TimelineEvent) {}

        override suspend fun insertEvents(events: List<TimelineEvent>) {}

        override suspend fun deleteEventsForTrip(tripId: String) {}

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

        override suspend fun insertMuseumDetail(detail: MuseumDetail) {}

        override fun getFlightDetail(eventId: String) = flowOf(null)

        override fun getHotelDetail(eventId: String) = flowOf(null)

        override fun getDiningDetail(eventId: String) = flowOf(null)

        override fun getMuseumDetail(eventId: String) = flowOf(null)

        override fun getVoiceNotesForTrip(tripId: String) =
          flowOf(emptyList<VoiceNoteEntity>())

        override suspend fun insertVoiceNote(
          note: VoiceNoteEntity
        ) {}

        override suspend fun deleteVoiceNoteById(id: String) {}

        override fun getEventById(eventId: String) = flowOf(null)

        override suspend fun deleteAllEvents() {}

        override suspend fun deleteAllFlightDetails() {}

        override suspend fun deleteAllHotelDetails() {}

        override suspend fun deleteAllDiningDetails() {}

        override suspend fun deleteAllActivityDetails() {}
      }

    val fakeTripSummaryAndTipsProvider =
      object : TripSummaryAndTipsProvider {
        override suspend fun isSupported(): Boolean = true

        override fun generateTripSummaryAndTips(
          events: List<TimelineEvent>,
          trip: Trip,
        ): kotlinx.coroutines.flow.Flow<String> {
          return kotlinx.coroutines.flow.flowOf("Should not be called!")
        }

        override fun generateTripSummary(
          events: List<TimelineEvent>,
          trip: Trip,
          voiceNotes: List<VoiceNoteEntity>,
        ): Flow<String> = flowOf("")

        override fun generateUpcomingTip(events: List<TimelineEvent>, trip: Trip): Flow<String> =
          flowOf("")
      }

    val fakeDailyThemeProvider =
      object : DailyThemeProvider {
        override suspend fun generateDailyThemes(events: List<TimelineEvent>): List<DayThemeItem> =
          emptyList()
      }

    val fakeDayThemeDao =
      object : DayThemeDao {
        override fun getThemesForTrip(tripId: String) =
          kotlinx.coroutines.flow.flowOf(emptyList<DayTheme>())

        override suspend fun insertTheme(theme: DayTheme) {}

        override suspend fun insertThemes(themes: List<DayTheme>) {}

        override suspend fun deleteThemesForTrip(tripId: String) {}

        override suspend fun deleteAllThemes() {}
      }

    val fakeTripDao =
      object : TripDao {
        override fun getAllTrips() = kotlinx.coroutines.flow.flowOf(emptyList<Trip>())

        override fun getTripById(tripId: String) = kotlinx.coroutines.flow.flowOf(mockTrip)

        override suspend fun insertTrip(trip: Trip) {}

        override suspend fun insertTrips(trips: List<Trip>) {}

        override suspend fun deleteTrip(trip: Trip) {}

        override suspend fun deleteAllTrips() {}
      }

    val fakeTourDetailDao =
      object : TourDetailDao {
        override fun getTourDetailByEventId(eventId: String) = flowOf(null)
        override fun getTourDetailById(id: String) = flowOf(null)
        override suspend fun insertTourDetail(tourDetail: TourDetail) {}
        override suspend fun insertTourDetails(tourDetails: List<TourDetail>) {}
        override suspend fun deleteAllTourDetails() {}
      }

    val viewModel =
      ItineraryViewModel(
        fakeSavedStateHandle,
        fakeEventDao,
        fakeTourDetailDao,
        fakeDayThemeDao,
        fakeTripDao,
        fakeTripSummaryAndTipsProvider,
        fakeDailyThemeProvider,
      )

    advanceUntilIdle()

    val uiState = viewModel.uiState.first()
    assertTrue(uiState.isTripSummaryAndTipsSupported)
    assertEquals("Persisted trip summary and tips", uiState.tripSummaryAndTips)
    assertFalse(uiState.isTripSummaryAndTipsLoading)
  }

  @Test
  fun testMultiDayEventGrouping() = runTest {
    val mockTrip = Trip(id = "trip123", title = "Test Trip", location = "Paris", startDate = 0L, endDate = 0L)
    val multiDayEvents = listOf(
      TimelineEvent(
        id = "1",
        tripId = "trip123",
        type = EventType.TRANSPORTATION,
        timestamp = 1779192000000, // Day 1
        title = "Flight",
        location = "CDG",
        description = null,
        extraInfo = null,
        imageResList = emptyList(),
      ),
      TimelineEvent(
        id = "2",
        tripId = "trip123",
        type = EventType.ACCOMMODATION,
        timestamp = 1779278400000, // Day 2 (+24h)
        title = "Hotel",
        location = "Paris",
        description = null,
        extraInfo = null,
        imageResList = emptyList(),
      )
    )

    val fakeSavedStateHandle = SavedStateHandle(mapOf("tripId" to "trip123"))

    val fakeEventDao = object : EventDao {
      override fun getAllSessionIds() = flowOf(emptyList<TripSessionIdentifier>())
      override fun getEventsForTrip(tripId: String) = flowOf(multiDayEvents)
      override suspend fun insertEvent(event: TimelineEvent) {}
      override suspend fun insertEvents(events: List<TimelineEvent>) {}
      override suspend fun deleteEventsForTrip(tripId: String) {}
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
      override fun getTourDetailByEventId(eventId: String) = flowOf(null)
      override fun getTourDetailById(id: String) = flowOf(null)
      override suspend fun insertTourDetail(tourDetail: TourDetail) {}
      override suspend fun insertTourDetails(tourDetails: List<TourDetail>) {}
      override suspend fun deleteAllTourDetails() {}
    }

    val fakeDayThemeDao = object : DayThemeDao {
      override fun getThemesForTrip(tripId: String) = flowOf(emptyList<DayTheme>())
      override suspend fun insertTheme(theme: DayTheme) {}
      override suspend fun insertThemes(themes: List<DayTheme>) {}
      override suspend fun deleteThemesForTrip(tripId: String) {}
      override suspend fun deleteAllThemes() {}
    }

    val fakeTripDao = object : TripDao {
      override fun getAllTrips() = flowOf(listOf(mockTrip))
      override fun getTripById(tripId: String) = flowOf(mockTrip)
      override suspend fun insertTrip(trip: Trip) {}
      override suspend fun insertTrips(trips: List<Trip>) {}
      override suspend fun deleteTrip(trip: Trip) {}
      override suspend fun deleteAllTrips() {}
    }

    val fakeTripSummaryAndTipsProvider = object : TripSummaryAndTipsProvider {
      override suspend fun isSupported(): Boolean = true
      override fun generateTripSummaryAndTips(events: List<TimelineEvent>, trip: Trip) = flowOf("Summary")
      override fun generateTripSummary(events: List<TimelineEvent>, trip: Trip, voiceNotes: List<VoiceNoteEntity>) = flowOf("Summary")
      override fun generateUpcomingTip(events: List<TimelineEvent>, trip: Trip) = flowOf("")
    }

    val fakeDailyThemeProvider = object : DailyThemeProvider {
      override suspend fun generateDailyThemes(events: List<TimelineEvent>) = emptyList<DayThemeItem>()
    }

    val viewModel = ItineraryViewModel(
      fakeSavedStateHandle,
      fakeEventDao,
      fakeTourDetailDao,
      fakeDayThemeDao,
      fakeTripDao,
      fakeTripSummaryAndTipsProvider,
      fakeDailyThemeProvider,
    )
    advanceUntilIdle()
    val items = viewModel.uiState.first().items
    val headers = items.filterIsInstance<ItineraryUiListItem.Header>()
    assertEquals(2, headers.size)
    assertEquals(1, headers[0].dayNumber)
    assertEquals(2, headers[1].dayNumber)
  }
}
