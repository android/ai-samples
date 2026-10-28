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

import com.example.jetpacker.data.itinerary.EventDao
import com.example.jetpacker.data.itinerary.EventType
import com.example.jetpacker.data.itinerary.TimelineEvent
import com.example.jetpacker.data.trips.DummyData
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.first
import org.json.JSONObject

/**
 * Handles cleaning, deduplication, and payload construction for booking events.
 */
object BookingEventNormalizer {

  private val SHORT_DATE_FORMAT =
    DateTimeFormatter.ofPattern("MMM dd", Locale.US).withZone(ZoneOffset.UTC)

  fun formatEpochShortDate(epochMillis: Long): String {
    return runCatching { SHORT_DATE_FORMAT.format(Instant.ofEpochMilli(epochMillis)) }
      .getOrDefault("DATE")
  }

  fun cleanHotelTitle(event: TimelineEvent): String {
    val detailName = DummyData.hotelDetails.find { it.eventId == event.id }?.name?.trim()
    if (!detailName.isNullOrEmpty()) return detailName
    val stripped =
      event.title
        .replace(Regex("^(Check-in|Check in)\\s*[:@\\-]\\s*", RegexOption.IGNORE_CASE), "")
        .trim()
    if (stripped.isNotEmpty() && !stripped.equals(event.title, ignoreCase = true)) {
      return stripped
    }
    return event.location.trim().ifEmpty { event.title }
  }

  fun normalizeBookableEvents(events: List<TimelineEvent>): List<TimelineEvent> {
    val bookableTypes =
      setOf(
        EventType.TRANSPORTATION,
        EventType.ACCOMMODATION,
        EventType.ACTIVITY,
        EventType.CULTURE,
        EventType.FOOD_AND_DRINK,
      )
    val filtered = events.filter { it.type in bookableTypes }
    val accommodationEvents = filtered.filter { it.type == EventType.ACCOMMODATION }
    val checkins = accommodationEvents.filter {
      it.title.contains("Check-in", ignoreCase = true) ||
        it.title.contains("Check in", ignoreCase = true)
    }
    val checkouts = accommodationEvents.filter {
      it.title.contains("Checkout", ignoreCase = true) ||
        it.title.contains("Check-out", ignoreCase = true) ||
        it.title.contains("Check out", ignoreCase = true)
    }
    val matchedCheckouts = mutableSetOf<TimelineEvent>()
    val mergedByCheckinId = mutableMapOf<String, TimelineEvent>()

    for (checkin in checkins) {
      val matchingCheckout = checkouts.find {
        it.location.equals(checkin.location, ignoreCase = true) && it.timestamp > checkin.timestamp
      }
      val cleanTitle = cleanHotelTitle(checkin)
      if (matchingCheckout != null) {
        matchedCheckouts.add(matchingCheckout)
      }
      mergedByCheckinId[checkin.id] =
        checkin.copy(title = cleanTitle.ifEmpty { checkin.title })
    }

    val result = mutableListOf<TimelineEvent>()
    for (event in filtered) {
      if (event.type == EventType.ACCOMMODATION) {
        val merged = mergedByCheckinId[event.id]
        if (merged != null) {
          result.add(merged)
        } else if (!matchedCheckouts.contains(event)) {
          val cleanTitle = cleanHotelTitle(event)
          result.add(event.copy(title = cleanTitle.ifEmpty { event.title }))
        }
      } else {
        result.add(event)
      }
    }
    return result
  }

  suspend fun buildEventJson(
    event: TimelineEvent,
    eventDao: EventDao,
    tripTitle: String? = null,
    tripLocation: String? = null,
    titleOverride: String? = null,
    typeOverride: String? = null,
  ): JSONObject {
    val eventJson = JSONObject()
    eventJson.put("id", event.id)
    eventJson.put("type", typeOverride ?: event.type.name)
    eventJson.put("title", titleOverride ?: event.title)
    eventJson.put("location", event.location)
    eventJson.put("extraInfo", event.extraInfo.orEmpty())
    eventJson.put("description", event.description.orEmpty())
    eventJson.put("timestamp", event.timestamp)
    eventJson.put("dateLabel", formatEpochShortDate(event.timestamp))
    if (!tripTitle.isNullOrEmpty()) eventJson.put("tripTitle", tripTitle)
    if (!tripLocation.isNullOrEmpty()) eventJson.put("tripLocation", tripLocation)

    when (event.type) {
      EventType.TRANSPORTATION -> {
        val flightDetail =
          runCatching { eventDao.getFlightDetail(event.id).first() }.getOrNull()
            ?: DummyData.flightDetails.find { it.eventId == event.id }
        if (flightDetail != null) {
          val fdJson = JSONObject()
          fdJson.put("eventId", flightDetail.eventId)
          fdJson.put("airline", flightDetail.airline)
          fdJson.put("flightNum", flightDetail.flightNum)
          fdJson.put("origin", flightDetail.origin)
          fdJson.put("destination", flightDetail.destination)
          fdJson.put("gate", flightDetail.gate)
          fdJson.put("seat", flightDetail.seat)
          fdJson.put("departureTerminal", flightDetail.departureTerminal)
          fdJson.put("arrivalTerminal", flightDetail.arrivalTerminal)
          fdJson.put("arrivalGate", flightDetail.arrivalGate)
          fdJson.put("duration", flightDetail.duration)
          fdJson.put("aircraft", flightDetail.aircraft)
          fdJson.put("baggageAllowance", flightDetail.baggageAllowance)
          eventJson.put("flightDetail", fdJson)
        }
      }
      EventType.ACCOMMODATION -> {
        val hotelDetail =
          runCatching { eventDao.getHotelDetail(event.id).first() }.getOrNull()
            ?: DummyData.hotelDetails.find { it.eventId == event.id }
        if (hotelDetail != null) {
          val hdJson = JSONObject()
          hdJson.put("name", hotelDetail.name)
          hdJson.put("address", hotelDetail.address)
          hdJson.put("checkInTime", hotelDetail.checkInTime)
          hdJson.put("checkOutTime", hotelDetail.checkOutTime)
          hdJson.put("checkInDate", formatEpochShortDate(hotelDetail.checkInTime).uppercase())
          hdJson.put("checkOutDate", formatEpochShortDate(hotelDetail.checkOutTime).uppercase())
          val nights =
            ((hotelDetail.checkOutTime - hotelDetail.checkInTime) / (24 * 3600 * 1000L))
              .toInt()
              .coerceAtLeast(1)
          hdJson.put("nights", nights)
          hdJson.put("confNumber", hotelDetail.confNumber)
          hdJson.put("rating", hotelDetail.rating)
          hdJson.put("pricePerNight", hotelDetail.pricePerNight)
          hdJson.put("guests", hotelDetail.guests)
          eventJson.put("hotelDetail", hdJson)
        }
      }
      EventType.FOOD_AND_DRINK -> {
        val diningDetail =
          runCatching { eventDao.getDiningDetail(event.id).first() }.getOrNull()
            ?: DummyData.diningDetails.find { it.eventId == event.id }
        if (diningDetail != null) {
          val ddJson = JSONObject()
          ddJson.put("restaurantName", diningDetail.restaurantName)
          ddJson.put("address", diningDetail.address)
          ddJson.put("partySize", diningDetail.partySize)
          ddJson.put("rating", diningDetail.rating)
          ddJson.put("priceRange", diningDetail.priceRange)
          eventJson.put("diningDetail", ddJson)
        }
      }
      EventType.CULTURE,
      EventType.ACTIVITY -> {
        val museumDetail =
          runCatching { eventDao.getMuseumDetail(event.id).first() }.getOrNull()
            ?: DummyData.museumDetails.find { it.eventId == event.id }
        if (museumDetail != null) {
          val mdJson = JSONObject()
          mdJson.put("description", museumDetail.description)
          mdJson.put("address", museumDetail.address)
          mdJson.put("openingHours", museumDetail.openingHours)
          mdJson.put("admissionPrice", museumDetail.admissionPrice)
          mdJson.put("rating", museumDetail.rating)
          eventJson.put("museumDetail", mdJson)
        }
      }
      else -> {}
    }
    return eventJson
  }
}
