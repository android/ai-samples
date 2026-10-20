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

/**
 * Classifies conversational inputs and timeline events into booking intents and categories.
 */
object BookingIntentClassifier {

  private val HOTEL_KEYWORDS =
    setOf(
      "hotel",
      "stay",
      "room",
      "suite",
      "check-in",
      "checkout",
      "check out",
      "redwood",
      "shelbourne",
      "meurice",
      "shibuya",
      "artemide",
      "four seasons",
      "grove",
      "seasons",
    )

  private val DINING_KEYWORDS =
    setOf(
      "dinner",
      "lunch",
      "restaurant",
      "table",
      "quantum",
      "dining",
      "ramen",
      "ichiran",
      "jungsik",
      "verne",
      "bistro",
      "cafe",
    )

  private val ACTIVITY_KEYWORDS =
    setOf(
      "museum",
      "activity",
      "ticket",
      "pass",
      "sfmoma",
      "cruise",
      "tour",
      "louvre",
      "teamlab",
      "colosseum",
      "vatican",
      "cliffs",
    )

  private val FLIGHT_KEYWORDS =
    setOf(
      "flight",
      "fly",
      "plane",
      "airline",
      "airport",
      "seat",
      "boarding",
      "outbound",
      "return",
      "sfo",
      "cdg",
      "jfk",
      "ams",
      "nrt",
      "hnd",
    )

  fun resolveIntent(
    prompt: String,
    directMatchEvent: TimelineEvent? = null,
    intentOverride: String? = null,
  ): String {
    if (intentOverride != null) return intentOverride

    directMatchEvent?.type?.let { type ->
      return when (type) {
        EventType.ACCOMMODATION -> "hotel"
        EventType.FOOD_AND_DRINK -> "dining"
        EventType.CULTURE,
        EventType.ACTIVITY -> "activity"
        EventType.TRANSPORTATION -> "flight"
        else -> "conversation"
      }
    }

    val lower = prompt.lowercase()
    return when {
      HOTEL_KEYWORDS.any { lower.contains(it) } -> "hotel"
      DINING_KEYWORDS.any { lower.contains(it) } -> "dining"
      ACTIVITY_KEYWORDS.any { lower.contains(it) } -> "activity"
      FLIGHT_KEYWORDS.any { lower.contains(it) } -> "flight"
      else -> "conversation"
    }
  }

  fun resolveTargetTitle(
    prompt: String,
    resolvedIntent: String,
    tripEvents: List<TimelineEvent>,
    directMatchEvent: TimelineEvent? = null,
    targetTitleOverride: String? = null,
  ): String {
    if (targetTitleOverride != null) return targetTitleOverride
    if (directMatchEvent != null) return directMatchEvent.title

    val lower = prompt.lowercase()
    return when (resolvedIntent) {
      "hotel" ->
        tripEvents.firstOrNull { it.type == EventType.ACCOMMODATION }?.title
          ?: "The Redwood Grove Hotel"
      "dining" ->
        (tripEvents.firstOrNull {
            it.type == EventType.FOOD_AND_DRINK && it.title.contains("Dinner", ignoreCase = true)
          } ?: tripEvents.firstOrNull { it.type == EventType.FOOD_AND_DRINK })
          ?.title ?: "Dinner @ Quantum Bites"
      "activity" ->
        tripEvents
          .firstOrNull { it.type == EventType.CULTURE || it.type == EventType.ACTIVITY }
          ?.title ?: "SFMOMA Museum Visit"
      "flight" -> {
        if (lower.contains("return") || lower.contains("inbound") || lower.contains("back")) {
          tripEvents.lastOrNull { it.type == EventType.TRANSPORTATION }?.title ?: "CDG > SFO"
        } else {
          tripEvents.firstOrNull { it.type == EventType.TRANSPORTATION }?.title ?: "CDG > SFO"
        }
      }
      else -> ""
    }
  }

  fun getCategory(title: String, tripEvents: List<TimelineEvent>): String {
    val event =
      tripEvents.find {
        it.title.equals(title, ignoreCase = true) ||
          it.location.equals(title, ignoreCase = true) ||
          it.id == title
      }
    event?.type?.let { type ->
      return when (type) {
        EventType.TRANSPORTATION -> "Flight"
        EventType.ACCOMMODATION -> "Hotel"
        EventType.ACTIVITY,
        EventType.CULTURE -> "Activity"
        EventType.FOOD_AND_DRINK -> "Dining"
        else -> "Activity"
      }
    }

    val lower = title.lowercase()
    return when {
      title.contains(" > ") || lower.contains("flight") -> "Flight"
      HOTEL_KEYWORDS.any { lower.contains(it) } -> "Hotel"
      DINING_KEYWORDS.any { lower.contains(it) } -> "Dining"
      else -> "Activity"
    }
  }
}
