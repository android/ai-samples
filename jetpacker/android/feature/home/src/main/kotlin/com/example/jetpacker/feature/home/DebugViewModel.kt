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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jetpacker.data.itinerary.DayThemeDao
import com.example.jetpacker.data.itinerary.EventDao
import com.example.jetpacker.data.itinerary.ExpenseDao
import com.example.jetpacker.data.itinerary.TourDetailDao
import com.example.jetpacker.data.trips.DummyData
import com.example.jetpacker.data.trips.TripDao
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
open class DebugViewModel
@Inject
constructor(
  private val tripDao: TripDao?,
  private val eventDao: EventDao?,
  private val tourDetailDao: TourDetailDao?,
  private val expenseDao: ExpenseDao?,
  private val dayThemeDao: DayThemeDao?,
) : ViewModel() {

  open fun resetDatabase() {
    viewModelScope.launch {
      // Clear all tables through DAOs
      tripDao?.deleteAllTrips()
      eventDao?.deleteAllEvents()
      eventDao?.deleteAllFlightDetails()
      eventDao?.deleteAllHotelDetails()
      eventDao?.deleteAllDiningDetails()
      eventDao?.deleteAllActivityDetails()
      tourDetailDao?.deleteAllTourDetails()
      expenseDao?.deleteAllExpenses()
      dayThemeDao?.deleteAllThemes()

      // Repopulate with mock data
      tripDao?.insertTrips(DummyData.trips)
      eventDao?.insertEvents(DummyData.events)
      tourDetailDao?.insertTourDetails(DummyData.tourDetails)
      DummyData.flightDetails.forEach { eventDao?.insertFlightDetail(it) }
      DummyData.hotelDetails.forEach { eventDao?.insertHotelDetail(it) }
      DummyData.diningDetails.forEach { eventDao?.insertDiningDetail(it) }
      DummyData.activityDetails.forEach { eventDao?.insertActivityDetail(it) }
      DummyData.museumDetails.forEach { eventDao?.insertMuseumDetail(it) }
      DummyData.expenses.forEach { expenseDao?.insertExpense(it) }
      DummyData.voiceNotes.forEach { eventDao?.insertVoiceNote(it) }
    }
  }
}
