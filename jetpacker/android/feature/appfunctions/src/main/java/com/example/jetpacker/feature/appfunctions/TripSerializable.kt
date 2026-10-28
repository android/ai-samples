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

package com.example.jetpacker.feature.appfunctions

import androidx.appfunctions.AppFunctionSerializable

/**
 * Tracks the currently active trip ID in the UI session so AppFunctions can resolve omitted
 * `tripId` parameters to the user's active trip.
 */
object ActiveTripSession {
  @Volatile var activeTripId: String? = null
  @Volatile var isReadOnlyBudgetCheck: Boolean = false
  @Volatile var pendingExpenseTitle: String? = null
  @Volatile var pendingExpenseAmount: Double? = null
  @Volatile var pendingExpenseCurrency: String? = null
}

/** Represents a trip. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class TripSerializable(
  /** The trip's unique identifier. */
  val id: String,
  /** The trip's title. */
  val title: String,
  /** The trip's destination location. */
  val location: String,
  /** The trip's start date in milliseconds. */
  val startDate: Long,
  /** The trip's end date in milliseconds. */
  val endDate: Long,
  /** A list of participants. */
  val participants: List<String>,
  /** The trip's total budget. */
  val budget: Double,
  /** The currency code of the trip (e.g. USD, EUR, JPY). */
  val currency: String,
)

/** Represents an expense associated with a trip. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class ExpenseSerializable(
  /** The expense's unique identifier. */
  val id: String,
  /** The trip ID this expense is associated with. */
  val tripId: String,
  /** The expense title or description. */
  val title: String,
  /** The amount spent. */
  val amount: Double,
  /** The currency code (e.g., USD, EUR). */
  val currency: String,
  /** The expense category (e.g., Food, Transport). */
  val category: String,
  /** The timestamp of the expense. */
  val timestamp: Long,
  /** The title of the trip this expense was added to. */
  val tripTitle: String? = null,
  /** The expense amount converted to USD. */
  val amountUsd: Double? = null,
  /** Total spent across the trip in USD (matches the Expenses screen total). */
  val tripTotalSpentUsd: Double? = null,
  /** Remaining trip budget in USD out of the $2,000 USD budget. */
  val tripRemainingBudgetUsd: Double? = null,
  /** Remaining trip budget converted to Japanese Yen (JPY). */
  val tripRemainingBudgetJpy: Double? = null,
  /** The native currency code of the trip (e.g. EUR, USD, JPY). */
  val tripCurrency: String? = null,
  /** Total budget for the trip in its native currency. */
  val tripBudget: Double? = null,
  /** Total spent across the trip in its native currency. */
  val tripTotalSpent: Double? = null,
  /** Remaining trip budget in the trip's native currency. */
  val tripRemainingBudget: Double? = null,
  /** Execution status of the tool call (e.g., SUCCESS). */
  val status: String? = "SUCCESS",
  /** Human-readable completion message instructing the agent not to repeat the call. */
  val message: String? = null,
)

/** Represents an event in a trip itinerary. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class TimelineEventSerializable(
  /** The event's unique identifier. */
  val id: String,
  /** The trip ID this event belongs to. */
  val tripId: String,
  /** The type of event (e.g., TRANSPORTATION, ACCOMMODATION, FOOD_AND_DRINK). */
  val type: String,
  /** The timestamp of the event. */
  val timestamp: Long,
  /** The title of the event. */
  val title: String,
  /** The location of the event. */
  val location: String,
  /** A description of the event. */
  val description: String?,
  /** The title of the trip this event belongs to. */
  val tripTitle: String? = null,
  /** Optional title of a matched itinerary event when added as a reminder. */
  val matchedEventTitle: String? = null,
  /** Execution status of the tool call (e.g., SUCCESS). */
  val status: String? = "SUCCESS",
  /** Human-readable completion message instructing the agent not to repeat the call. */
  val message: String? = null,
)

/** Represents a voice note or memo associated with a trip. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class VoiceNoteSerializable(
  /** The voice note's unique identifier. */
  val id: String,
  /** The trip ID this voice note is linked to. */
  val tripId: String,
  /** The title of the voice note. */
  val title: String,
  /** The transcription text of the voice note. */
  val transcription: String,
  /** The timestamp when it was recorded. */
  val timestamp: Long,
  /** Optional ID of the itinerary event this voice note was matched to. */
  val matchedEventId: String? = null,
  /** Optional title of the itinerary event this voice note was matched to. */
  val matchedEventTitle: String? = null,
  /** Execution status of the tool call (e.g., SUCCESS). */
  val status: String? = "SUCCESS",
  /** Human-readable completion message instructing the agent not to repeat the call. */
  val message: String? = null,
)

/** Represents a theme assigned to a specific day of a trip. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class DayThemeSerializable(
  /** The theme's unique identifier. */
  val id: String,
  /** The trip ID this theme belongs to. */
  val tripId: String,
  /** The date in YYYY-MM-DD format. */
  val date: String,
  /** The descriptive theme for the day. */
  val theme: String,
)

/**
 * Represents rich subtype details for a specific itinerary event (flight, hotel, museum, dining, or
 * tour).
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class EventDetailsSerializable(
  /** The unique identifier of the itinerary event. */
  val eventId: String,
  /** The title of the event. */
  val title: String,
  /** The type of the event (e.g., TRANSPORTATION, ACCOMMODATION, CULTURE, FOOD_AND_DRINK). */
  val type: String,
  /** The location or address of the event. */
  val location: String,
  /** Museum or attraction admission ticket price (e.g., "€28", "€17", "$25"). */
  val admissionPrice: String? = null,
  /** Opening hours for a museum or attraction. */
  val openingHours: String? = null,
  /** Hotel nightly rate (e.g., "€220/night"). */
  val pricePerNight: String? = null,
  /** Hotel or booking confirmation number. */
  val confirmationNumber: String? = null,
  /** Airline and flight number if applicable. */
  val flightInfo: String? = null,
  /** Flight baggage allowance (e.g., "2 x 23kg Checked, 1 Carry-on"). */
  val baggageAllowance: String? = null,
  /** Visitor tips, notes, or instructions for this event. */
  val tips: List<String> = emptyList(),
  /** Summary description of the event details. */
  val description: String? = null,
)
