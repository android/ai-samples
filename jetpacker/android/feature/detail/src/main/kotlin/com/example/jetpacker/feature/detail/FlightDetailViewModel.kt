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

package com.example.jetpacker.feature.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jetpacker.data.itinerary.EventDao
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FlightDetailUiState(
  val flightNumber: String? = null,
  val route: String? = null,
  val departureCode: String? = null,
  val departureCity: String? = null,
  val departureTime: String? = null,
  val arrivalCode: String? = null,
  val arrivalCity: String? = null,
  val arrivalTime: String? = null,
  val date: String? = null,
  val duration: String? = null,
  val departureTerminal: String? = null,
  val departureGate: String? = null,
  val arrivalTerminal: String? = null,
  val arrivalGate: String? = null,
  val boardingTime: String? = null,
  val passenger: String? = null,
  val seat: String? = null,
  val cabin: String? = null,
  val bookingRef: String? = null,
  val aircraft: String? = null,
  val baggageAllowance: String? = null,
)

/**
 * ViewModel for loading and exposing the state of a specific flight event's details.
 */
@HiltViewModel
class FlightDetailViewModel
@Inject
constructor(savedStateHandle: SavedStateHandle, private val eventDao: EventDao) : ViewModel() {

  private val _uiState = MutableStateFlow(FlightDetailUiState())
  val uiState: StateFlow<FlightDetailUiState> = _uiState.asStateFlow()

  private var eventId: String = savedStateHandle["eventId"] ?: ""

  init {
    loadDetail(eventId)
  }

  fun loadDetail(id: String) {
    if (id.isNotEmpty()) {
      eventId = id
    }
    viewModelScope.launch {
      if (eventId.isNotEmpty()) {
        kotlinx.coroutines.flow
          .combine(eventDao.getEventById(eventId), eventDao.getFlightDetail(eventId)) {
            event,
            detail ->
            Pair(event, detail)
          }
          .collectLatest { (e, d) ->
            if (e != null && d != null) {
              val departureInstant = Instant.ofEpochMilli(e.timestamp)
              val formatter =
                DateTimeFormatter.ofPattern("hh:mm a", Locale.US).withZone(ZoneId.systemDefault())
              val dateFormatter =
                DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.US)
                  .withZone(ZoneId.systemDefault())

              _uiState.update {
                FlightDetailUiState(
                  flightNumber = "${d.airline} ${d.flightNum}",
                  route = "${d.origin} • ${d.destination}",
                  departureCode = d.origin,
                  departureCity = e.location, // Using location as city
                  departureTime = formatter.format(departureInstant),
                  arrivalCode = d.destination,
                  arrivalCity = "Destination", // Dummy
                  arrivalTime =
                    formatter.format(departureInstant.plusSeconds(7200)), // Dummy 2h later
                  date = dateFormatter.format(departureInstant).uppercase(),
                  departureGate = d.gate ?: "",
                  boardingTime =
                    formatter.format(departureInstant.minusSeconds(1800)), // 30m before
                  seat = d.seat ?: "",
                  bookingRef = e.sessionId ?: "JS123",
                  departureTerminal = d.departureTerminal ?: "Terminal 1",
                  arrivalTerminal = d.arrivalTerminal ?: "Terminal 2",
                  arrivalGate = d.arrivalGate ?: "",
                  duration = d.duration ?: "2h 00m",
                  aircraft = d.aircraft ?: "Airbus A320",
                  baggageAllowance = d.baggageAllowance ?: "1 x 23kg Checked",
                )
              }
            }
          }
      }
    }
  }
}
