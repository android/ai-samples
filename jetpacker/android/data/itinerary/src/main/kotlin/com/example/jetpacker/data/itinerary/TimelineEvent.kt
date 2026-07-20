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

package com.example.jetpacker.data.itinerary

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

enum class EventType {
  TRANSPORTATION,
  ACCOMMODATION,
  FOOD_AND_DRINK,
  CAR_RENTAL,
  ACTIVITY,
  CULTURE,
  WORK,
}

@Entity(tableName = "timeline_events")
@TypeConverters(Converters::class)
data class TimelineEvent(
  @PrimaryKey @JvmField val id: String,
  @JvmField val tripId: String = "", // Added to link to Trip
  @JvmField val type: EventType,
  @JvmField val timestamp: Long,
  @JvmField val title: String,
  @JvmField val location: String,
  @JvmField val description: String? = null,
  @JvmField val extraInfo: String? = null,
  @JvmField val sessionId: String? = null,
  @JvmField val imageResList: List<Int> = emptyList(),
  @JvmField val audioNote: String? = null,
  @JvmField val audioNotes: List<String> = emptyList(),
  @JvmField val placeId: String? = null,
  @JvmField val language: String? = null
)

@Entity(tableName = "voice_notes")
data class VoiceNoteEntity(
  @PrimaryKey @JvmField val id: String,
  @JvmField val tripId: String,
  @JvmField val title: String,
  @JvmField val transcription: String,
  @JvmField val timestamp: Long,
  @JvmField val matchingEventsJson: String,
)
