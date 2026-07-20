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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RestaurantDetailUiState(
  val restaurantName: String? = null,
  val cuisineType: String? = null,
  val rating: String? = null,
  val reviewCount: String? = null,
  val priceRange: String? = null,
  val date: String? = null,
  val time: String? = null,
  val guests: String? = null,
  val reservationName: String? = null,
  val address: String? = null,
  val placeId: String? = null,
  val phone: String? = null,
)

/**
 * ViewModel for loading and exposing the state of a specific dining/restaurant reservation event.
 */
@HiltViewModel
class RestaurantDetailViewModel
@Inject
constructor(savedStateHandle: SavedStateHandle, private val eventDao: EventDao) : ViewModel() {

  private val _uiState = MutableStateFlow(RestaurantDetailUiState())
  val uiState: StateFlow<RestaurantDetailUiState> = _uiState.asStateFlow()

  init {
    savedStateHandle.get<String>("eventId")?.let { loadDetail(it) }
  }

  fun loadDetail(eventId: String) {
    viewModelScope.launch {
      if (eventId.isNotEmpty()) {
        combine(eventDao.getDiningDetail(eventId), eventDao.getEventById(eventId)) { d, e ->
            if (d != null) {
              val instant = Instant.ofEpochMilli(d.reservationTime)
              val formatter =
                DateTimeFormatter.ofPattern("hh:mm a", Locale.US).withZone(ZoneId.systemDefault())
              val dateFormatter =
                DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.US)
                  .withZone(ZoneId.systemDefault())

              RestaurantDetailUiState(
                restaurantName = d.restaurantName,
                cuisineType = e?.description ?: "Local Cuisine",
                rating = d.rating ?: "",
                reviewCount = d.reviewCount ?: "",
                priceRange = d.priceRange ?: "",
                date = dateFormatter.format(instant),
                time = formatter.format(instant),
                guests = "${d.partySize} People",
                address = d.address,
                placeId = e?.placeId,
                phone = d.phone ?: "+1 555-0199",
              )
            } else null
          }
          .collectLatest { state -> state?.let { s -> _uiState.update { s } } }
      }
    }
  }
}
