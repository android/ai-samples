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

package com.example.jetpacker.feature.booking_assistant.domain

import androidx.a2ui.model.processor.A2uiSurfaceModel
import com.example.jetpacker.data.trips.Trip
import com.example.jetpacker.feature.booking_assistant.BookableJourneyItem
import com.example.jetpacker.feature.booking_assistant.BookingChatMessage

data class BookingAssistantUiState(
  val activeSurfaces: List<A2uiSurfaceModel> = emptyList(),
  val isStreaming: Boolean = false,
  val chatMessages: List<BookingChatMessage> = emptyList(),
  val flightJourneyItems: List<BookableJourneyItem> = emptyList(),
  val activityJourneyItems: List<BookableJourneyItem> = emptyList(),
  val trip: Trip? = null,
)
