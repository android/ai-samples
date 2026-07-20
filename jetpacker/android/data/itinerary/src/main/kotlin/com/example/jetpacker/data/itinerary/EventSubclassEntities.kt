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
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import org.intellij.lang.annotations.Language

@Entity(
  tableName = "flight_details",
  foreignKeys =
    [
      ForeignKey(
        entity = TimelineEvent::class,
        parentColumns = ["id"],
        childColumns = ["eventId"],
        onDelete = ForeignKey.CASCADE,
      )
    ],
)
data class FlightDetail(
  @PrimaryKey @JvmField val eventId: String,
  @JvmField val airline: String = "",
  @JvmField val flightNum: String = "",
  @JvmField val origin: String = "",
  @JvmField val destination: String = "",
  @JvmField val gate: String? = null,
  @JvmField val seat: String? = null,
  @JvmField val departureTerminal: String? = null,
  @JvmField val arrivalTerminal: String? = null,
  @JvmField val arrivalGate: String? = null,
  @JvmField val duration: String? = null,
  @JvmField val aircraft: String? = null,
  @JvmField val baggageAllowance: String? = null,
)

@Entity(
  tableName = "hotel_details",
  foreignKeys =
    [
      ForeignKey(
        entity = TimelineEvent::class,
        parentColumns = ["id"],
        childColumns = ["eventId"],
        onDelete = ForeignKey.CASCADE,
      )
    ],
)
data class HotelDetail(
  @PrimaryKey @JvmField val eventId: String,
  @JvmField val name: String = "",
  @JvmField val address: String = "",
  @JvmField val placeId: String? = null,
  @JvmField val checkInTime: Long = 0L,
  @JvmField val checkOutTime: Long = 0L,
  @JvmField val confNumber: String? = null,
  @JvmField val rating: String? = null,
  @JvmField val ratingCount: String? = null,
  @JvmField val pricePerNight: String? = null,
  @JvmField val guests: String? = null,
  @JvmField val phone: String? = null,
  @JvmField val language: String? = null,
)

@Entity(
  tableName = "dining_details",
  foreignKeys =
    [
      ForeignKey(
        entity = TimelineEvent::class,
        parentColumns = ["id"],
        childColumns = ["eventId"],
        onDelete = ForeignKey.CASCADE,
      )
    ],
)
data class DiningDetail(
  @PrimaryKey @JvmField val eventId: String,
  @JvmField val restaurantName: String = "",
  @JvmField val address: String = "",
  @JvmField val placeId: String? = null,
  @JvmField val reservationTime: Long = 0L,
  @JvmField val partySize: Int = 1,
  @JvmField val rating: String? = null,
  @JvmField val reviewCount: String? = null,
  @JvmField val priceRange: String? = null,
  @JvmField val phone: String? = null,
)

@Entity(
  tableName = "activity_details",
  foreignKeys =
    [
      ForeignKey(
        entity = TimelineEvent::class,
        parentColumns = ["id"],
        childColumns = ["eventId"],
        onDelete = ForeignKey.CASCADE,
      )
    ],
)
data class ActivityDetail(
  @PrimaryKey @JvmField val eventId: String,
  @JvmField val activityName: String = "",
  @JvmField val address: String = "",
  @JvmField val placeId: String? = null,
  @JvmField val durationMinutes: Int = 60,
)

@Entity(
  tableName = "museum_details",
  foreignKeys =
    [
      ForeignKey(
        entity = TimelineEvent::class,
        parentColumns = ["id"],
        childColumns = ["eventId"],
        onDelete = ForeignKey.CASCADE,
      )
    ],
)
@TypeConverters(Converters::class)
data class MuseumDetail(
  @PrimaryKey @JvmField val eventId: String,
  @JvmField val description: String = "",
  @JvmField val address: String = "",
  @JvmField val openingHours: String = "",
  @JvmField val admissionPrice: String = "",
  @JvmField val ticketWebsite: String = "",
  @JvmField val rating: String = "",
  @JvmField val phone: String? = null,
  @JvmField val infoUrls: List<String> = emptyList(),
)
