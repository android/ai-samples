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

class BookingIntentClassifierTest {

  @Test
  fun resolveIntent_withIntentOverride_returnsOverride() {
    val intent = BookingIntentClassifier.resolveIntent(
      prompt = "Can you help me with something?",
      intentOverride = "flight",
    )
    assertThat(intent).isEqualTo("flight")
  }

  @Test
  fun resolveIntent_withDirectMatchEvent_returnsEventTypeIntent() {
    val event = TimelineEvent(
      id = "event-1",
      tripId = "trip-1",
      type = EventType.TRANSPORTATION,
      timestamp = 1000L,
      title = "Flight to Tokyo",
      location = "NRT",
    )
    val intent = BookingIntentClassifier.resolveIntent(
      prompt = "random text",
      directMatchEvent = event,
    )
    assertThat(intent).isEqualTo("flight")
  }

  @Test
  fun resolveIntent_withKeywords_identifiesCorrectIntents() {
    assertThat(BookingIntentClassifier.resolveIntent("Book a hotel in Shibuya"))
      .isEqualTo("hotel")
    assertThat(BookingIntentClassifier.resolveIntent("Reserve a table for dinner"))
      .isEqualTo("dining")
    assertThat(BookingIntentClassifier.resolveIntent("Get tickets for museum tour"))
      .isEqualTo("activity")
    assertThat(BookingIntentClassifier.resolveIntent("Check airline flights from SFO"))
      .isEqualTo("flight")
    assertThat(BookingIntentClassifier.resolveIntent("Hello, how are you?"))
      .isEqualTo("conversation")
  }

  @Test
  fun resolveTargetTitle_findsCorrectEventByIntent() {
    val events = listOf(
      TimelineEvent(
        id = "f-1",
        tripId = "trip-1",
        type = EventType.TRANSPORTATION,
        timestamp = 1000L,
        title = "SFO > HND",
        location = "Tokyo",
      ),
      TimelineEvent(
        id = "h-1",
        tripId = "trip-1",
        type = EventType.ACCOMMODATION,
        timestamp = 2000L,
        title = "Shibuya Grand Hotel",
        location = "Tokyo",
      ),
      TimelineEvent(
        id = "d-1",
        tripId = "trip-1",
        type = EventType.FOOD_AND_DRINK,
        timestamp = 3000L,
        title = "Dinner @ Tokyo Ramen",
        location = "Tokyo",
      ),
    )

    val hotelTitle = BookingIntentClassifier.resolveTargetTitle(
      prompt = "I need a hotel room",
      resolvedIntent = "hotel",
      tripEvents = events,
    )
    assertThat(hotelTitle).isEqualTo("Shibuya Grand Hotel")

    val flightTitle = BookingIntentClassifier.resolveTargetTitle(
      prompt = "Book flight",
      resolvedIntent = "flight",
      tripEvents = events,
    )
    assertThat(flightTitle).isEqualTo("SFO > HND")
  }

  @Test
  fun getCategory_mapsEventTypesAndKeywordsAccurately() {
    val events = listOf(
      TimelineEvent(
        id = "event-1",
        tripId = "trip-1",
        type = EventType.ACTIVITY,
        timestamp = 1000L,
        title = "Sunset Cruise",
        location = "Bay",
      )
    )

    assertThat(BookingIntentClassifier.getCategory("Sunset Cruise", events)).isEqualTo("Activity")
    assertThat(BookingIntentClassifier.getCategory("SFO > CDG", emptyList())).isEqualTo("Flight")
    assertThat(BookingIntentClassifier.getCategory("Four Seasons", emptyList())).isEqualTo("Hotel")
    assertThat(BookingIntentClassifier.getCategory("Bistro Lunch", emptyList())).isEqualTo("Dining")
  }
}
