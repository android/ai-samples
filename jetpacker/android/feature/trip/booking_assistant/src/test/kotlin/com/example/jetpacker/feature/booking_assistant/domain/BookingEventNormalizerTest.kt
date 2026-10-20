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

import com.example.jetpacker.data.itinerary.EventType
import com.example.jetpacker.data.itinerary.TimelineEvent
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BookingEventNormalizerTest {

  @Test
  fun cleanHotelTitle_stripsCheckinPrefix() {
    val event = TimelineEvent(
      id = "event-hotel",
      tripId = "trip-1",
      type = EventType.ACCOMMODATION,
      timestamp = 1000L,
      title = "Check-in @ The Redwood Grove Hotel",
      location = "California",
    )

    val cleaned = BookingEventNormalizer.cleanHotelTitle(event)
    assertThat(cleaned).isEqualTo("The Redwood Grove Hotel")
  }

  @Test
  fun normalizeBookableEvents_filtersOutCheckoutEvents() {
    val checkin = TimelineEvent(
      id = "event-checkin",
      tripId = "trip-1",
      type = EventType.ACCOMMODATION,
      timestamp = 1000L,
      title = "Check-in @ Hotel Shibuya",
      location = "Tokyo",
    )
    val checkout = TimelineEvent(
      id = "event-checkout",
      tripId = "trip-1",
      type = EventType.ACCOMMODATION,
      timestamp = 3000L,
      title = "Check-out @ Hotel Shibuya",
      location = "Tokyo",
    )

    val normalized = BookingEventNormalizer.normalizeBookableEvents(listOf(checkin, checkout))
    assertThat(normalized).hasSize(1)
    assertThat(normalized.first().title).isEqualTo("Hotel Shibuya")
  }

  @Test
  fun formatEpochShortDate_formatsTimestampAccurately() {
    // 1700000000000L is Nov 14, 2023 UTC
    val formatted = BookingEventNormalizer.formatEpochShortDate(1700000000000L)
    assertThat(formatted).isEqualTo("Nov 14")
  }
}
