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

package com.example.jetpacker.feature.home

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jetpacker.data.itinerary.EventDao
import com.example.jetpacker.data.itinerary.ExpenseDao
import com.example.jetpacker.data.itinerary.TourDetailDao
import com.example.jetpacker.data.trips.DummyData
import com.example.jetpacker.data.trips.Trip
import com.example.jetpacker.data.trips.TripDao
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * ViewModel responsible for managing and exposing the dashboard list of user trips,
 * and handling database cleanups/initializations.
 */
@HiltViewModel
@SuppressLint("GlobalCoroutineDispatchers")
open class HomeViewModel
@Inject
constructor(
  private val tripDao: TripDao,
  private val eventDao: EventDao,
  private val tourDetailDao: TourDetailDao,
  private val expenseDao: ExpenseDao,
) : ViewModel() {
  private val _uiState = MutableStateFlow(HomeUiState())
  open val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

  init {
    loadTrips()
  }

  private fun loadTrips() {
    viewModelScope.launch {
      tripDao
        .getAllTrips()
        .onStart {
          _uiState.update { current -> current.copy(isLoading = true) }
          // Simulating network or database latency to showcase the loading animation
          delay(1500L)
        }
        .collectLatest { trips ->
          if (trips.isEmpty()) {
            prepopulateDatabase()
          } else {
            // Sort trips naturally by timestamp
            val sortedTrips = trips.sortedBy { it.endDate }
            _uiState.update { current -> current.copy(trips = sortedTrips, isLoading = false) }
          }
        }
    }
  }

  private fun prepopulateDatabase() {
    viewModelScope.launch {
      tripDao.insertTrips(DummyData.trips)
      eventDao.insertEvents(DummyData.events)
      tourDetailDao.insertTourDetails(DummyData.tourDetails)
      DummyData.flightDetails.forEach { eventDao.insertFlightDetail(it) }
      DummyData.hotelDetails.forEach { eventDao.insertHotelDetail(it) }
      DummyData.diningDetails.forEach { eventDao.insertDiningDetail(it) }
      DummyData.museumDetails.forEach { eventDao.insertMuseumDetail(it) }
    }
  }

  open fun deleteTrip(trip: Trip) {
    viewModelScope.launch {
      eventDao.deleteEventsForTrip(trip.id)
      tripDao.deleteTrip(trip)
    }
  }
}
