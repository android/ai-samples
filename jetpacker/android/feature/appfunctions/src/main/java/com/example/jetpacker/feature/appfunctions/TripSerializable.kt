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
 * Represents a trip.
 */
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
)

/**
 * Represents an expense associated with a trip.
 */
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
)

/**
 * Represents an event in a trip itinerary.
 */
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
)

/**
 * Represents a voice note or memo associated with a trip.
 */
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
)

/**
 * Represents a theme assigned to a specific day of a trip.
 */
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
