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

import com.example.jetpacker.feature.itinerary_enrichment.DayThemeItem
import com.example.jetpacker.feature.itinerary_enrichment.TripSummaryAndTipsProvider
import com.example.jetpacker.feature.itinerary_enrichment.DailyThemeProvider
import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import com.example.jetpacker.core.ui.JetPackerTheme
import com.example.jetpacker.data.itinerary.DayTheme
import com.example.jetpacker.data.itinerary.DayThemeDao
import com.example.jetpacker.data.itinerary.TourDetail
import com.example.jetpacker.data.itinerary.TourDetailDao
import com.example.jetpacker.data.itinerary.EventDao
import com.example.jetpacker.data.itinerary.EventType
import com.example.jetpacker.data.itinerary.TimelineEvent
import com.example.jetpacker.data.itinerary.TripSessionIdentifier
import com.example.jetpacker.data.itinerary.VoiceNoteEntity
import com.example.jetpacker.data.trips.DummyData
import com.example.jetpacker.data.trips.Trip
import com.example.jetpacker.data.trips.TripDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf

class ItineraryScreenshotTest {
  @PreviewTest
  @Preview(showBackground = true)
  @Composable
  fun ItineraryScreenScreenshotPreview() {
    val fakeEventDao =
      object : EventDao {
        override fun getEventsForTrip(tripId: String): Flow<List<TimelineEvent>> = emptyFlow()

        override fun getAllSessionIds() = flowOf(emptyList<TripSessionIdentifier>())

        override suspend fun deleteEventsForTrip(tripId: String) {}

        override suspend fun insertEvents(events: List<TimelineEvent>) {}

        override suspend fun insertEvent(event: TimelineEvent) {}

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

        override suspend fun insertMuseumDetail(detail: com.example.jetpacker.data.itinerary.MuseumDetail) {}

        override fun getFlightDetail(eventId: String) = flowOf(null)

        override fun getHotelDetail(eventId: String) = flowOf(null)

        override fun getDiningDetail(eventId: String) = flowOf(null)

        override fun getMuseumDetail(eventId: String) = flowOf(null)

        override fun getVoiceNotesForTrip(
          tripId: String
        ): Flow<List<VoiceNoteEntity>> = flowOf(emptyList())

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

        override fun generateTripSummaryAndTips(events: List<TimelineEvent>, trip: Trip): Flow<String> =
          flowOf("Amazing trip summaries and tips!")

        override fun generateTripSummary(
          events: List<TimelineEvent>,
          trip: Trip,
          voiceNotes: List<VoiceNoteEntity>,
        ): Flow<String> = flowOf("Amazing summary!")

        override fun generateUpcomingTip(events: List<TimelineEvent>, trip: Trip): Flow<String> =
          flowOf("Upcoming tip!")
      }

    val savedStateHandle = SavedStateHandle(mapOf("tripId" to "trip1"))

    val fakeDailyThemeProvider =
      object : DailyThemeProvider {
        override suspend fun generateDailyThemes(events: List<TimelineEvent>): List<DayThemeItem> =
          emptyList()
      }

    val fakeDayThemeDao =
      object : DayThemeDao {
        override fun getThemesForTrip(
          tripId: String
        ): Flow<List<DayTheme>> = flowOf(emptyList())

        override suspend fun deleteThemesForTrip(tripId: String) {}

        override suspend fun insertThemes(
          themes: List<DayTheme>
        ) {}

        override suspend fun insertTheme(theme: DayTheme) {}

        override suspend fun deleteAllThemes() {}
      }

    val fakeTripDao =
      object : TripDao {
        override fun getAllTrips() = flowOf(emptyList<Trip>())

        override fun getTripById(tripId: String) = flowOf(null)

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

    val fakeViewModel =
      object :
        ItineraryViewModel(
          savedStateHandle = savedStateHandle,
          eventDao = fakeEventDao,
          tourDetailDao = fakeTourDetailDao,
          dayThemeDao = fakeDayThemeDao,
          tripDao = fakeTripDao,
          tripSummaryAndTipsProvider = fakeTripSummaryAndTipsProvider,
          dailyThemeProvider = fakeDailyThemeProvider,
        ) {
        private val state =
          MutableStateFlow(
            ItineraryUiState(
              items =
                run {
                  val parisEvents = DummyData.events.filter { it.tripId == "2026-3" }
                  val uiItems = mutableListOf<ItineraryUiListItem>()
                  uiItems.add(ItineraryUiListItem.Header("Aug 12, 2026", "Arrival & Check-in", 1, 3))
                  uiItems.addAll(parisEvents.take(2).map { ItineraryUiListItem.Event(it) })
                  uiItems.add(ItineraryUiListItem.Header("Aug 13, 2026", "Art & Haute Cuisine", 2, 3))
                  uiItems.addAll(parisEvents.drop(2).take(2).map { ItineraryUiListItem.Event(it) })
                  uiItems.add(
                    ItineraryUiListItem.Header("Aug 14, 2026", "Royal Palaces & River Views", 3, 3)
                  )
                  uiItems.addAll(parisEvents.drop(4).take(2).map { ItineraryUiListItem.Event(it) })
                  uiItems
                },
              tripSummaryAndTips = "Relaxing, romantic, and cultural.",
              isTripSummaryAndTipsSupported = true,
              isGenerating = false,
              showAddEventDialog = false,
            )
          )
        override val uiState: StateFlow<ItineraryUiState> = state
      }

    JetPackerTheme {
      ItineraryScreen(
        contentPadding = PaddingValues(0.dp),
        onBack = {},
        viewModel = fakeViewModel,
        onEventClick = { _, _ -> },
      )
    }
  }
}
