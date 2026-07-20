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

import android.annotation.SuppressLint
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jetpacker.data.itinerary.ActivityDetail
import com.example.jetpacker.data.itinerary.DiningDetail
import com.example.jetpacker.data.itinerary.EventDao
import com.example.jetpacker.data.itinerary.EventType
import com.example.jetpacker.data.itinerary.FlightDetail
import com.example.jetpacker.data.itinerary.HotelDetail
import com.example.jetpacker.data.itinerary.MuseumDetail
import com.example.jetpacker.data.itinerary.TimelineEvent
import com.example.jetpacker.data.itinerary.TourDetail
import com.example.jetpacker.data.itinerary.TourDetailDao
import com.example.jetpacker.data.trips.Trip
import com.example.jetpacker.data.trips.TripDao
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.BufferedReader
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * ViewModel managing the UI state and interactions for a trip's itinerary.
 * Supports loading itinerary events, adding new events (flights, hotels, dining),
 * deleting events, and modifying trip details.
 */
@HiltViewModel
@SuppressLint("GlobalCoroutineDispatchers")
open class ItineraryViewModel
@Inject
constructor(
  savedStateHandle: SavedStateHandle,
  private val eventDao: EventDao,
  private val tourDetailDao: TourDetailDao,
  private val tripDao: TripDao,
) : ViewModel() {
  private val _uiState = MutableStateFlow(ItineraryUiState())
  open val uiState: StateFlow<ItineraryUiState> = _uiState.asStateFlow()

  private var tripId: String = savedStateHandle["tripId"] ?: ""

  init {
    loadEvents()
  }

  fun loadForTrip(id: String) {
    if (id.isEmpty() || (id == tripId && _uiState.value.items.isNotEmpty())) return
    tripId = id
    loadEvents()
  }

  private fun loadEvents() {
    viewModelScope.launch {
      if (tripId.isNotEmpty()) {
        combine(
            eventDao.getEventsForTrip(tripId),
            tripDao.getTripById(tripId),
          ) { events, trip ->
            val groupedEvents =
              events
                .groupBy {
                  Instant.ofEpochMilli(it.timestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .toString()
                }
                .toSortedMap()

            val uiItems = mutableListOf<ItineraryUiListItem>()
            val totalDays = groupedEvents.size
            groupedEvents.onEachIndexed { index, (date, eventList) ->
              uiItems.add(
                ItineraryUiListItem.Header(
                  date = date,
                  theme = null,
                  dayNumber = index + 1,
                  totalDays = totalDays,
                )
              )
              uiItems.addAll(eventList.map { ItineraryUiListItem.Event(it) })
            }

            Pair(uiItems, trip)
          }
          .collectLatest { (items, trip) ->
            _uiState.update { it.copy(items = items, trip = trip) }
          }
      } else {
        _uiState.update { it.copy(items = emptyList()) }
      }
    }
  }

  open fun generateRecommendations() {
    Log.d("ItineraryViewModel", "Cloud recommendations generation is disabled in basic release.")
  }

  open fun showAddEvent() {
    _uiState.update { it.copy(showAddEventDialog = true) }
  }

  open fun hideAddEvent() {
    _uiState.update { it.copy(showAddEventDialog = false) }
  }

  fun showEditTrip() {
    _uiState.update { it.copy(showEditTripDialog = true) }
  }

  fun hideEditTrip() {
    _uiState.update { it.copy(showEditTripDialog = false) }
  }

  open fun addEvent(
    type: EventType,
    title: String,
    location: String,
    time: String,
    extraFields: Map<String, String>,
  ) {
    viewModelScope.launch {
      val eventId = UUID.randomUUID().toString()
      val newEvent =
        TimelineEvent(
          id = eventId,
          tripId = tripId,
          type = type,
          timestamp = System.currentTimeMillis(), // Mock timestamp for now
          title = title.ifEmpty { "New Event" },
          location = location.ifEmpty { "Unknown Location" },
          description = null,
          extraInfo = null,
          imageResList = emptyList(),
        )
      if (tripId.isNotEmpty()) {
        eventDao.insertEvent(newEvent)

        when (type) {
          EventType.TRANSPORTATION -> {
            val detail =
              FlightDetail(
                eventId = eventId,
                airline = extraFields["airline"] ?: "",
                flightNum = extraFields["flightNum"] ?: "",
                origin = "Origin",
                destination = "Destination",
              )
            eventDao.insertFlightDetail(detail)
          }
          EventType.ACCOMMODATION -> {
            val detail =
              HotelDetail(
                eventId = eventId,
                name = extraFields["hotelName"] ?: "",
                address = location,
                checkInTime = System.currentTimeMillis(),
                checkOutTime = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1),
              )
            eventDao.insertHotelDetail(detail)
          }
          EventType.FOOD_AND_DRINK -> {
            val detail =
              DiningDetail(
                eventId = eventId,
                restaurantName = extraFields["restaurantName"] ?: "",
                address = location,
                reservationTime = System.currentTimeMillis(),
                partySize = DEFAULT_PARTY_SIZE,
              )
            eventDao.insertDiningDetail(detail)
          }
          else -> {}
        }
      }
      _uiState.update { it.copy(showAddEventDialog = false) }
    }
  }

  open fun deleteEvent(event: TimelineEvent) {
    viewModelScope.launch { eventDao.deleteEvent(event) }
  }

  fun updateTripDetails(
    title: String,
    location: String,
    startDate: Long,
    endDate: Long,
    participants: List<String>,
  ) {
    viewModelScope.launch {
      val currentTrip = _uiState.value.trip ?: return@launch
      val updatedTrip =
        currentTrip.copy(
          title = title,
          location = location,
          startDate = startDate,
          endDate = endDate,
          participants = participants,
        )
      tripDao.insertTrip(updatedTrip)
    }
  }

  companion object {
    private const val DEFAULT_PARTY_SIZE = 2
  }
}
