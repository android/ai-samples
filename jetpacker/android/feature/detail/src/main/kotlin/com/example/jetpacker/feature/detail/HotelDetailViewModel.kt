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

data class HotelDetailUiState(
  val hotelName: String? = null,
  val location: String? = null,
  val rating: String? = null,
  val ratingCount: String? = null,
  val pricePerNight: String? = null,
  val checkInDate: String? = null,
  val checkInTime: String? = null,
  val checkOutDate: String? = null,
  val checkOutTime: String? = null,
  val roomType: String? = null,
  val guests: String? = null,
  val address: String? = null,
  val phone: String? = null,
  val language: String? = null,
)

/**
 * ViewModel for loading and exposing the state of a specific hotel accommodation event's details.
 */
@HiltViewModel
class HotelDetailViewModel
@Inject
constructor(savedStateHandle: SavedStateHandle, private val eventDao: EventDao) : ViewModel() {

  private val _uiState = MutableStateFlow(HotelDetailUiState())
  val uiState: StateFlow<HotelDetailUiState> = _uiState.asStateFlow()

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
          .combine(eventDao.getEventById(eventId), eventDao.getHotelDetail(eventId)) { event, detail
            ->
            Pair(event, detail)
          }
          .collectLatest { (e, d) ->
            if (e != null && d != null) {
              val checkInInstant = Instant.ofEpochMilli(d.checkInTime)
              val checkOutInstant = Instant.ofEpochMilli(d.checkOutTime)
              val formatter =
                DateTimeFormatter.ofPattern("HH:mm", Locale.US).withZone(ZoneId.systemDefault())
              val dateFormatter =
                DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US)
                  .withZone(ZoneId.systemDefault())

              _uiState.update {
                HotelDetailUiState(
                  hotelName = d.name,
                  location = e.location,
                  rating = d.rating ?: "",
                  ratingCount = d.ratingCount ?: "",
                  pricePerNight = d.pricePerNight ?: "",
                  checkInDate = dateFormatter.format(checkInInstant),
                  checkInTime = formatter.format(checkInInstant),
                  checkOutDate = dateFormatter.format(checkOutInstant),
                  checkOutTime = formatter.format(checkOutInstant),
                  guests = d.guests ?: "",
                  phone = d.phone ?: "",
                  address = d.address,
                  roomType = "Booked Room", // Dummy
                  language = e.language,
                )
              }
            }
          }
      }
    }
  }
}
