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

import com.example.jetpacker.data.itinerary.TimelineEvent
import com.example.jetpacker.data.trips.Trip

sealed class ItineraryUiListItem {
  data class Header(
    val date: String,
    val theme: String?,
    val dayNumber: Int = 0,
    val totalDays: Int = 0,
  ) : ItineraryUiListItem()

  data class Event(val event: TimelineEvent) : ItineraryUiListItem()
}

data class ItineraryUiState(
  val trip: Trip? = null,
  val items: List<ItineraryUiListItem> = emptyList(),
  val isGenerating: Boolean = false,
  val showAddEventDialog: Boolean = false,
  val isTripSummaryAndTipsSupported: Boolean = true,
  val isTripSummaryAndTipsLoading: Boolean = false,
  val tripSummaryAndTips: String? = null,
  val showEditTripDialog: Boolean = false,
)
