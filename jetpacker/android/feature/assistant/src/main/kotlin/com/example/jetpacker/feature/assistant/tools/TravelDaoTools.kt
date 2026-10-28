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

package com.example.jetpacker.feature.assistant.tools

import android.util.Log
import com.example.jetpacker.data.itinerary.EventDao
import com.example.jetpacker.data.itinerary.EventType
import com.example.jetpacker.data.itinerary.Expense
import com.example.jetpacker.data.itinerary.ExpenseDao
import com.example.jetpacker.data.itinerary.FlightDetail
import com.example.jetpacker.data.itinerary.TimelineEvent
import com.example.jetpacker.data.itinerary.TourDetailDao
import com.example.jetpacker.data.itinerary.VoiceNoteEntity
import com.example.jetpacker.data.trips.DummyData
import com.example.jetpacker.data.trips.Trip
import com.example.jetpacker.data.trips.TripDao
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.ImagePart
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import com.google.mlkit.genai.schema.tools.FunctionSchema
import com.google.mlkit.genai.schema.tools.FunctionType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/** Tool to retrieve flight itinerary details from the Room database. */
class GetFlightItineraryTool(
  private val eventDao: EventDao,
  private val tripDao: TripDao,
  private val activeTripIdProvider: () -> String? = { null },
) {
  val name = "get_flight_itinerary"
  val description =
    "Retrieves flight itinerary details including flight number, airline, airports, departure and arrival times, terminal, gate, and status for a trip or flight number."
  val parametersSchema =
    FunctionSchema(
      type = FunctionType.OBJECT,
      properties =
        mapOf(
          "tripId" to
            FunctionSchema(
              type = FunctionType.STRING,
              description = "The unique identifier of the trip.",
            ),
          "flightNumber" to
            FunctionSchema(
              type = FunctionType.STRING,
              description = "Optional specific flight code (e.g. GA123, UA456).",
            ),
        ),
    )

  suspend fun execute(args: Map<String, Any>): Map<String, Any> {
    val tripId = args["tripId"]?.toString()?.takeIf { it.isNotBlank() } ?: activeTripIdProvider()
    val targetFlightNumber = args["flightNumber"]?.toString()

    val tripsToSearch: List<Trip> =
      if (!tripId.isNullOrBlank()) {
        val trip = tripDao.getTripById(tripId).first() ?: DummyData.trips.find { it.id == tripId }
        if (trip != null) listOf(trip) else emptyList()
      } else {
        tripDao.getAllTrips().first()
      }

    val foundFlights = mutableListOf<Map<String, Any?>>()

    for (trip in tripsToSearch) {
      val events: List<TimelineEvent> = eventDao.getEventsForTrip(trip.id).first()
      val flightEvents = events.filter { it.type == EventType.TRANSPORTATION }

      for (event in flightEvents) {
        val detail: FlightDetail? = eventDao.getFlightDetail(event.id).first()
        if (detail != null) {
          if (
            targetFlightNumber.isNullOrBlank() ||
              detail.flightNum.contains(targetFlightNumber, ignoreCase = true)
          ) {
            foundFlights.add(
              mapOf(
                "tripId" to trip.id,
                "tripTitle" to trip.title,
                "eventId" to event.id,
                "title" to event.title,
                "airline" to detail.airline,
                "flightNumber" to detail.flightNum,
                "origin" to detail.origin,
                "destination" to detail.destination,
                "departureTerminal" to (detail.departureTerminal ?: ""),
                "gate" to (detail.gate ?: ""),
                "seat" to (detail.seat ?: ""),
                "arrivalTerminal" to (detail.arrivalTerminal ?: ""),
                "arrivalGate" to (detail.arrivalGate ?: ""),
                "duration" to (detail.duration ?: ""),
              )
            )
          }
        }
      }
    }

    return if (foundFlights.isNotEmpty()) {
      mapOf("flights" to foundFlights, "count" to foundFlights.size)
    } else {
      mapOf(
        "flights" to emptyList<Map<String, Any>>(),
        "count" to 0,
        "message" to "No flight details found matching your query.",
      )
    }
  }
}

/** Tool to add an expense to a trip directly in the Room database. */
class AddExpenseTool(
  private val expenseDao: ExpenseDao,
  private val tripDao: TripDao,
  private val activeTripIdProvider: () -> String? = { null },
) {
  val name = "addExpense"
  val description =
    "Adds an expense to a trip. Call this when the user asks to add or log an expense (e.g., train ticket, meal, taxi) for a trip."
  val parametersSchema =
    FunctionSchema(
      type = FunctionType.OBJECT,
      properties =
        mapOf(
          "tripId" to
            FunctionSchema(
              type = FunctionType.STRING,
              description =
                "The unique identifier of the trip (e.g. '2026-4' for Tokyo). Optional; resolves to active or matching trip if omitted.",
            ),
          "title" to
            FunctionSchema(
              type = FunctionType.STRING,
              description =
                "The title or description of the expense (e.g. 'Airport Express Train', 'Lunch').",
            ),
          "amount" to
            FunctionSchema(
              type = FunctionType.NUMBER,
              description = "The numeric amount of the expense (e.g. 45.0).",
            ),
          "currency" to
            FunctionSchema(
              type = FunctionType.STRING,
              description =
                "The currency code of the expense (e.g. 'USD', 'EUR', 'JPY', 'GBP'). Extract from the user's request or event details (e.g. 'dollars'/'$' -> 'USD', 'euros'/'€' -> 'EUR', 'yen'/'¥' -> 'JPY').",
            ),
          "category" to
            FunctionSchema(
              type = FunctionType.STRING,
              description =
                "Expense category: 'Transport', 'Food', 'Accommodation', 'Activity', or 'Other'.",
            ),
        ),
      required = listOf("title", "amount", "currency"),
    )

  suspend fun execute(args: Map<String, Any>): Map<String, Any> {
    val title = args["title"]?.toString() ?: "Expense"
    val amount =
      when (val amt = args["amount"]) {
        is Number -> amt.toDouble()
        is String -> amt.toDoubleOrNull() ?: 0.0
        else -> 0.0
      }
    val currency = normalizeCurrency(args["currency"]?.toString(), title)
    val category =
      args["category"]?.toString()?.takeIf { it.isNotBlank() } ?: inferExpenseCategory(title)
    val tripId = args["tripId"]?.toString()?.takeIf { it.isNotBlank() }
    val activeTripId = activeTripIdProvider()?.takeIf { it.isNotBlank() }
    val tripHint =
      listOfNotNull(
          args["trip"]?.toString(),
          args["destination"]?.toString(),
          args["location"]?.toString(),
          args["tripTitle"]?.toString(),
        )
        .firstOrNull { it.isNotBlank() }

    val trips = tripDao.getAllTrips().first()
    var trip =
      if (!tripId.isNullOrBlank()) {
        trips.find { it.id == tripId }
      } else if (!tripHint.isNullOrBlank()) {
        trips.find {
          it.id == tripHint ||
            it.location.contains(tripHint, ignoreCase = true) ||
            it.title.contains(tripHint, ignoreCase = true)
        }
      } else {
        trips.find {
          title.contains(it.location.substringBefore(','), ignoreCase = true) ||
            title.contains(it.title, ignoreCase = true)
        } ?: activeTripId?.let { activeId -> trips.find { it.id == activeId } }
      }

    if (trip == null) {
      val fallbackTrip =
        DummyData.trips.find { dt ->
          (!tripId.isNullOrBlank() && dt.id == tripId) ||
            (!tripHint.isNullOrBlank() &&
              (dt.id == tripHint ||
                dt.location.contains(tripHint, ignoreCase = true) ||
                dt.title.contains(tripHint, ignoreCase = true))) ||
            (title.contains(dt.location.substringBefore(','), ignoreCase = true) ||
              title.contains(dt.title, ignoreCase = true)) ||
            (!activeTripId.isNullOrBlank() && dt.id == activeTripId)
        }
      if (fallbackTrip != null) {
        tripDao.insertTrips(listOf(fallbackTrip))
        trip = fallbackTrip
      } else {
        trip = trips.firstOrNull()
      }
    }
    val targetTripId = trip?.id ?: tripId ?: activeTripId ?: "2026-1"

    val recentExpenses = expenseDao.getExpensesForTrip(targetTripId).first()
    val now = System.currentTimeMillis()
    val duplicate = recentExpenses.firstOrNull { existing ->
      existing.title.equals(title, ignoreCase = true) &&
        kotlin.math.abs(existing.amount - amount) < 0.001 &&
        (now - existing.timestamp) < 15_000L
    }
    if (duplicate != null) {
      return mapOf(
        "status" to "SUCCESS",
        "done" to true,
        "tripId" to targetTripId,
        "tripTitle" to (trip?.title ?: "Trip"),
        "title" to title,
        "amount" to amount,
        "currency" to currency,
        "category" to category,
        "message" to
          "Expense '$title' of $amount $currency is already logged in ${trip?.title ?: "trip"}. Task complete — do not call addExpense again.",
      )
    }

    val expense =
      Expense(
        tripId = targetTripId,
        title = title,
        amount = amount,
        currency = currency,
        category = category,
        timestamp = System.currentTimeMillis(),
      )
    expenseDao.insertExpense(expense)

    return mapOf(
      "status" to "SUCCESS",
      "done" to true,
      "tripId" to targetTripId,
      "tripTitle" to (trip?.title ?: "Trip"),
      "title" to title,
      "amount" to amount,
      "currency" to currency,
      "category" to category,
      "message" to
        "Added expense '$title' of $amount $currency to ${trip?.title ?: "trip"}. Task complete — do not call addExpense again.",
    )
  }

  private fun normalizeCurrency(raw: String?, titleHint: String = ""): String {
    val combined = "${raw.orEmpty().trim()} $titleHint".lowercase()
    val token = raw?.trim()?.uppercase()
    if (token in setOf("USD", "EUR", "JPY", "GBP", "CHF", "CAD", "AUD", "KRW")) {
      return token!!
    }
    return when {
      combined.contains("usd") || combined.contains("dollar") || combined.contains("$") -> "USD"
      combined.contains("eur") || combined.contains("euro") || combined.contains("€") -> "EUR"
      combined.contains("jpy") || combined.contains("yen") || combined.contains("¥") -> "JPY"
      combined.contains("gbp") || combined.contains("pound") || combined.contains("£") -> "GBP"
      combined.contains("chf") || combined.contains("franc") -> "CHF"
      combined.contains("krw") || combined.contains("won") || combined.contains("₩") -> "KRW"
      !token.isNullOrBlank() && token.length == 3 -> token
      else -> "USD"
    }
  }

  private fun inferExpenseCategory(title: String): String {
    val lower = title.lowercase()
    return when {
      lower.contains("train") ||
        lower.contains("express") ||
        lower.contains("flight") ||
        lower.contains("airport") ||
        lower.contains("taxi") ||
        lower.contains("uber") ||
        lower.contains("lyft") ||
        lower.contains("waymo") ||
        lower.contains("bus") ||
        lower.contains("metro") ||
        lower.contains("subway") ||
        lower.contains("rail") ||
        lower.contains("shinkansen") ||
        lower.contains("suica") ||
        lower.contains("transit") -> "Transport"
      lower.contains("coffee") ||
        lower.contains("cafe") ||
        lower.contains("tea") ||
        lower.contains("lunch") ||
        lower.contains("dinner") ||
        lower.contains("breakfast") ||
        lower.contains("food") ||
        lower.contains("restaurant") ||
        lower.contains("sushi") ||
        lower.contains("ramen") ||
        lower.contains("drink") ||
        lower.contains("meal") ||
        lower.contains("snack") ||
        lower.contains("bakery") -> "Food"
      lower.contains("hotel") ||
        lower.contains("hostel") ||
        lower.contains("resort") ||
        lower.contains("stay") ||
        lower.contains("night") ||
        lower.contains("airbnb") ||
        lower.contains("lodging") ||
        lower.contains("room") -> "Accommodation"
      lower.contains("museum") ||
        lower.contains("sfmoma") ||
        lower.contains("ticket") ||
        lower.contains("admission") ||
        lower.contains("tour") ||
        lower.contains("cruise") ||
        lower.contains("helicopter") ||
        lower.contains("keyboard") ||
        lower.contains("hoodie") ||
        lower.contains("merch") ||
        lower.contains("temple") ||
        lower.contains("shrine") ||
        lower.contains("park") ||
        lower.contains("teamlab") ||
        lower.contains("gallery") -> "Activity"
      else -> "Transport"
    }
  }
}

/** Tool to add an itinerary event or reminder to a trip directly in the Room database. */
class AddItineraryEventTool(
  private val eventDao: EventDao,
  private val tripDao: TripDao,
  private val activeTripIdProvider: () -> String? = { null },
) {
  val name = "addItineraryEvent"
  val description =
    "Adds a reminder or event to a trip's itinerary. Call this when the user asks to add an itinerary item, task, or reminder (e.g., 'remind me to pick up pocket wifi')."
  val parametersSchema =
    FunctionSchema(
      type = FunctionType.OBJECT,
      properties =
        mapOf(
          "tripId" to
            FunctionSchema(
              type = FunctionType.STRING,
              description = "The unique identifier of the trip (e.g. '2026-4' for Tokyo).",
            ),
          "title" to
            FunctionSchema(
              type = FunctionType.STRING,
              description =
                "Title or description of the event or reminder (e.g. 'Pick up pocket wifi').",
            ),
          "type" to
            FunctionSchema(
              type = FunctionType.STRING,
              description =
                "Event type: 'ACTIVITY', 'TRANSPORTATION', 'ACCOMMODATION', 'DINING', or 'CULTURE'.",
            ),
          "timestamp" to
            FunctionSchema(
              type = FunctionType.INTEGER,
              description =
                "Optional epoch timestamp in milliseconds for when this event/reminder occurs.",
            ),
          "location" to
            FunctionSchema(
              type = FunctionType.STRING,
              description = "Location of the event (e.g. 'Haneda Airport', 'Tokyo, Japan').",
            ),
          "description" to
            FunctionSchema(
              type = FunctionType.STRING,
              description = "Optional additional notes or description.",
            ),
        ),
      required = listOf("title"),
    )

  suspend fun execute(args: Map<String, Any>): Map<String, Any> {
    val title = args["title"]?.toString() ?: "Reminder"
    val typeStr = args["type"]?.toString() ?: "ACTIVITY"
    val eventType =
      try {
        EventType.valueOf(typeStr.uppercase())
      } catch (_: IllegalArgumentException) {
        EventType.ACTIVITY
      }
    val rawTimestamp = (args["timestamp"] as? Number)?.toLong() ?: 0L
    val tripId = args["tripId"]?.toString()?.takeIf { it.isNotBlank() }
    val activeTripId = activeTripIdProvider()?.takeIf { it.isNotBlank() }
    val tripHint =
      listOfNotNull(
          args["trip"]?.toString(),
          args["destination"]?.toString(),
          args["location"]?.toString(),
          args["tripTitle"]?.toString(),
        )
        .firstOrNull { it.isNotBlank() }

    val allTrips = tripDao.getAllTrips().first()
    var trip =
      if (!tripId.isNullOrBlank()) {
        allTrips.find { it.id == tripId }
      } else if (!tripHint.isNullOrBlank()) {
        allTrips.find {
          it.id == tripHint ||
            it.location.contains(tripHint, ignoreCase = true) ||
            it.title.contains(tripHint, ignoreCase = true)
        }
      } else {
        allTrips.find {
          title.contains(it.location.substringBefore(','), ignoreCase = true) ||
            title.contains(it.title, ignoreCase = true)
        } ?: activeTripId?.let { activeId -> allTrips.find { it.id == activeId } }
      }

    if (trip == null) {
      val fallbackTrip =
        DummyData.trips.find { dt ->
          (!tripId.isNullOrBlank() && dt.id == tripId) ||
            (!tripHint.isNullOrBlank() &&
              (dt.id == tripHint ||
                dt.location.contains(tripHint, ignoreCase = true) ||
                dt.title.contains(tripHint, ignoreCase = true))) ||
            (title.contains(dt.location.substringBefore(','), ignoreCase = true) ||
              title.contains(dt.title, ignoreCase = true)) ||
            (!activeTripId.isNullOrBlank() && dt.id == activeTripId)
        }
      if (fallbackTrip != null) {
        tripDao.insertTrips(listOf(fallbackTrip))
        trip = fallbackTrip
      } else {
        trip = allTrips.firstOrNull()
      }
    }
    val targetTripId = trip?.id ?: tripId ?: activeTripId ?: "2026-4"
    val defaultLoc = trip?.location ?: "Tokyo, Japan"
    val location = args["location"]?.toString()?.takeIf { it.isNotBlank() } ?: defaultLoc
    val description = args["description"]?.toString() ?: "Added via Travel Assistant"

    val timestamp =
      if (rawTimestamp > 0L) {
        rawTimestamp
      } else if (trip != null && trip.startDate > 0L) {
        trip.startDate
      } else {
        System.currentTimeMillis()
      }

    val existingEvents = eventDao.getEventsForTrip(targetTripId).first()
    val duplicateEvent = existingEvents.firstOrNull { existing ->
      existing.title.equals(title, ignoreCase = true)
    }
    if (duplicateEvent != null) {
      return mapOf(
        "status" to "SUCCESS",
        "done" to true,
        "eventId" to duplicateEvent.id,
        "tripId" to targetTripId,
        "tripTitle" to (trip?.title ?: "Trip"),
        "title" to title,
        "type" to duplicateEvent.type.name,
        "location" to duplicateEvent.location,
        "message" to
          "Added reminder '$title' to ${trip?.title ?: "trip"} itinerary. Task complete — do not call addItineraryEvent again.",
      )
    }

    val event =
      TimelineEvent(
        id = UUID.randomUUID().toString(),
        tripId = targetTripId,
        type = eventType,
        timestamp = timestamp,
        title = title,
        location = location,
        description = description,
      )
    eventDao.insertEvent(event)

    // Also create a linked VoiceNoteEntity so reminders spoken on VoiceNotesScreen immediately
    // appear
    val matchedExisting =
      existingEvents.find { ev ->
        val combined = "$title $location $description".lowercase()
        (combined.contains("airport") || combined.contains("flight") || combined.contains("sim")) &&
          ev.type == EventType.TRANSPORTATION ||
          combined.contains("museum") && ev.type == EventType.CULTURE ||
          combined.contains("hotel") && ev.type == EventType.ACCOMMODATION
      } ?: event
    val escapedExtract = title.replace("\"", "\\\"")
    val matchingJson = """[{"eventId":"${matchedExisting.id}","extract":"$escapedExtract"}]"""
    val existingNotes = eventDao.getVoiceNotesForTrip(targetTripId).first()
    if (existingNotes.none { it.title.equals(title, ignoreCase = true) }) {
      eventDao.insertVoiceNote(
        VoiceNoteEntity(
          id = UUID.randomUUID().toString(),
          tripId = targetTripId,
          title = title,
          transcription =
            if (description != "Added via Travel Assistant") "$title — $description" else title,
          timestamp = System.currentTimeMillis(),
          matchingEventsJson = matchingJson,
        )
      )
    }

    return mapOf(
      "status" to "SUCCESS",
      "done" to true,
      "eventId" to event.id,
      "tripId" to targetTripId,
      "tripTitle" to (trip?.title ?: "Trip"),
      "title" to title,
      "type" to eventType.name,
      "location" to location,
      "matchedEventTitle" to matchedExisting.title,
      "message" to
        "Added reminder '$title' to ${trip?.title ?: "trip"} itinerary. Task complete — do not call addItineraryEvent again.",
    )
  }
}

/** Tool to add a voice note or reminder to a trip and link it to matching itinerary events. */
class AddVoiceNoteTool(
  private val eventDao: EventDao,
  private val tripDao: TripDao,
  private val activeTripIdProvider: () -> String? = { null },
) {
  val name = "addVoiceNote"
  val description =
    "Adds a voice note or reminder to a trip and links it to matching itinerary events (e.g. airport flight or museum visit). Call this when the user asks to save a voice note, visitor tips, or reminder (e.g., 'Please remind me to buy a sim card when I get to the airport' or 'Save the museum visitor tips as a voice note')."
  val parametersSchema =
    FunctionSchema(
      type = FunctionType.OBJECT,
      properties =
        mapOf(
          "tripId" to
            FunctionSchema(
              type = FunctionType.STRING,
              description = "The unique identifier of the trip (e.g. '2026-4' for Tokyo).",
            ),
          "title" to
            FunctionSchema(
              type = FunctionType.STRING,
              description =
                "Short title of the voice note or reminder (e.g. 'Buy a SIM card at the airport', 'Museum Visitor Tips').",
            ),
          "transcription" to
            FunctionSchema(
              type = FunctionType.STRING,
              description = "The full text content of the note, reminder, or visitor tips.",
            ),
          "eventId" to
            FunctionSchema(
              type = FunctionType.STRING,
              description =
                "Optional itinerary eventId to link this voice note to (e.g. 'tok_1', 'tok_3').",
            ),
        ),
      required = listOf("title", "transcription"),
    )

  suspend fun execute(args: Map<String, Any>): Map<String, Any> {
    val title = args["title"]?.toString()?.takeIf { it.isNotBlank() } ?: "Voice Note"
    val transcription = args["transcription"]?.toString()?.takeIf { it.isNotBlank() } ?: title
    val explicitEventId = args["eventId"]?.toString()?.takeIf { it.isNotBlank() }
    val rawTripId = args["tripId"]?.toString()?.takeIf { it.isNotBlank() }
    val activeTripId = activeTripIdProvider()?.takeIf { it.isNotBlank() }

    val allTrips = tripDao.getAllTrips().first()
    val trip =
      if (!rawTripId.isNullOrBlank()) {
        allTrips.find { it.id == rawTripId } ?: DummyData.trips.find { it.id == rawTripId }
      } else {
        allTrips.find {
          val text = "$title $transcription"
          text.contains(it.location.substringBefore(','), ignoreCase = true) ||
            text.contains(it.title, ignoreCase = true)
        }
          ?: activeTripId?.let { activeId ->
            allTrips.find { it.id == activeId } ?: DummyData.trips.find { it.id == activeId }
          }
          ?: allTrips.firstOrNull()
      }
    val targetTripId = trip?.id ?: rawTripId ?: activeTripId ?: "2026-4"

    val existingNotes = eventDao.getVoiceNotesForTrip(targetTripId).first()
    val now = System.currentTimeMillis()
    val duplicate = existingNotes.firstOrNull { existing ->
      existing.title.equals(title, ignoreCase = true) && (now - existing.timestamp) < 15_000L
    }
    if (duplicate != null) {
      return mapOf(
        "status" to "SUCCESS",
        "done" to true,
        "voiceNoteId" to duplicate.id,
        "tripId" to targetTripId,
        "tripTitle" to (trip?.title ?: "Trip"),
        "title" to duplicate.title,
        "transcription" to duplicate.transcription,
        "message" to
          "Saved voice note '$title' to ${trip?.title ?: "trip"}. Task complete — do not call addVoiceNote again.",
      )
    }

    val dbEvents = eventDao.getEventsForTrip(targetTripId).first()
    val events =
      if (dbEvents.isNotEmpty()) {
        dbEvents
      } else {
        val seeded = DummyData.events.filter { it.tripId == targetTripId }
        if (seeded.isNotEmpty()) {
          eventDao.insertEvents(seeded)
        }
        seeded
      }

    val matchedEvent =
      if (!explicitEventId.isNullOrBlank()) {
        events.find { it.id == explicitEventId }
      } else {
        val combined = "$title $transcription".lowercase()
        events.find { ev ->
          (combined.contains("airport") ||
            combined.contains("flight") ||
            combined.contains("sim") ||
            combined.contains("baggage")) && ev.type == EventType.TRANSPORTATION ||
            (combined.contains("museum") ||
              combined.contains("barefoot") ||
              combined.contains("knees")) && ev.type == EventType.CULTURE ||
            combined.contains("hotel") && ev.type == EventType.ACCOMMODATION ||
            ev.title.lowercase().split(" ").any { word ->
              word.length > 3 && combined.contains(word)
            }
        }
      }

    val matchingJson =
      if (matchedEvent != null) {
        val escapedExtract = transcription.replace("\"", "\\\"")
        """[{"eventId":"${matchedEvent.id}","extract":"$escapedExtract"}]"""
      } else {
        "[]"
      }

    if (matchedEvent != null) {
      val updatedNotes = (matchedEvent.audioNotes + transcription).distinct()
      eventDao.insertEvent(matchedEvent.copy(audioNotes = updatedNotes))
    }

    val note =
      VoiceNoteEntity(
        id = UUID.randomUUID().toString(),
        tripId = targetTripId,
        title = title,
        transcription = transcription,
        timestamp = now,
        matchingEventsJson = matchingJson,
      )
    eventDao.insertVoiceNote(note)

    return mapOf(
      "status" to "SUCCESS",
      "done" to true,
      "voiceNoteId" to note.id,
      "tripId" to targetTripId,
      "tripTitle" to (trip?.title ?: "Trip"),
      "title" to title,
      "transcription" to transcription,
      "matchedEventId" to (matchedEvent?.id ?: ""),
      "matchedEventTitle" to (matchedEvent?.title ?: ""),
      "message" to
        "Saved voice note '$title' to ${trip?.title ?: "trip"}. Task complete — do not call addVoiceNote again.",
    )
  }
}

/** Tool to search for trips by destination, title, or ID. */
class SearchTripTool(private val tripDao: TripDao) {
  val name = "searchTrip"
  val description =
    "Searches for trips matching a title, destination location, or ID. Call this first when the user refers to a trip by city or name and you need its tripId."
  val parametersSchema =
    FunctionSchema(
      type = FunctionType.OBJECT,
      properties =
        mapOf(
          "query" to
            FunctionSchema(
              type = FunctionType.STRING,
              description = "Optional search keyword matching trip title or destination location.",
            ),
          "id" to FunctionSchema(type = FunctionType.STRING, description = "Optional trip ID."),
          "title" to
            FunctionSchema(
              type = FunctionType.STRING,
              description = "Optional trip title keyword (e.g. 'Tokyo', 'Paris', 'Roman').",
            ),
          "location" to
            FunctionSchema(
              type = FunctionType.STRING,
              description = "Optional destination city or country (e.g. 'Tokyo', 'Paris', 'Rome').",
            ),
        ),
    )

  suspend fun execute(args: Map<String, Any>): Map<String, Any> {
    val query = args["query"]?.toString()?.trim()
    val id = args["id"]?.toString()?.trim()
    val title = args["title"]?.toString()?.trim()
    val location = args["location"]?.toString()?.trim()

    val dbTrips = tripDao.getAllTrips().first()
    val allTrips = if (dbTrips.isNotEmpty()) dbTrips else DummyData.trips
    val matched = allTrips.filter { trip ->
      (query.isNullOrEmpty() ||
        trip.title.contains(query, ignoreCase = true) ||
        trip.location.contains(query, ignoreCase = true) ||
        trip.id.equals(query, ignoreCase = true)) &&
        (id.isNullOrEmpty() || trip.id.equals(id, ignoreCase = true)) &&
        (title.isNullOrEmpty() ||
          trip.title.contains(title, ignoreCase = true) ||
          trip.location.contains(title, ignoreCase = true)) &&
        (location.isNullOrEmpty() ||
          trip.location.contains(location, ignoreCase = true) ||
          trip.title.contains(location, ignoreCase = true))
    }

    return mapOf(
      "status" to "SUCCESS",
      "count" to matched.size,
      "trips" to
        matched.map {
          mapOf(
            "id" to it.id,
            "tripId" to it.id,
            "title" to it.title,
            "location" to it.location,
            "startDate" to it.startDate,
            "endDate" to it.endDate,
          )
        },
    )
  }
}

/** Tool to retrieve expenses for a trip from the Room database. */
class GetExpensesTool(
  private val expenseDao: ExpenseDao,
  private val tripDao: TripDao,
  private val activeTripIdProvider: () -> String? = { null },
) {
  val name = "getExpenses"
  val description = "Retrieves all logged expenses for a specific trip."
  val parametersSchema =
    FunctionSchema(
      type = FunctionType.OBJECT,
      properties =
        mapOf(
          "tripId" to
            FunctionSchema(
              type = FunctionType.STRING,
              description = "The unique identifier of the trip (e.g. '2026-4').",
            )
        ),
    )

  private fun convertToUsd(amount: Double, currency: String): Double {
    val rate =
      when (currency.uppercase()) {
        "USD" -> 1.0
        "EUR" -> 1.08
        "GBP" -> 1.27
        "JPY" -> 0.0067
        else -> 1.0
      }
    return kotlin.math.round(amount * rate * 100.0) / 100.0
  }

  suspend fun execute(args: Map<String, Any>): Map<String, Any> {
    val tripId =
      args["tripId"]?.toString()?.takeIf { it.isNotBlank() }
        ?: activeTripIdProvider()?.takeIf { it.isNotBlank() }
        ?: tripDao.getAllTrips().first().firstOrNull()?.id
        ?: "2026-4"
    val expenses = expenseDao.getExpensesForTrip(tripId).first()
    val trip = tripDao.getTripById(tripId).first()
    val tripBudget = trip?.budget ?: 2000.0
    val tripCurrency = trip?.currency ?: "USD"
    val budgetUsd = convertToUsd(tripBudget, tripCurrency)
    val totalSpentUsd =
      kotlin.math.round(
        expenses.sumOf { convertToUsd(it.amount, it.currency) } * 100.0
      ) / 100.0
    val remainingBudgetUsd = kotlin.math.round((budgetUsd - totalSpentUsd) * 100.0) / 100.0
    val remainingBudgetJpy = kotlin.math.round(remainingBudgetUsd * 150.0)
    return mapOf(
      "tripId" to tripId,
      "count" to expenses.size,
      "totalSpentUsd" to totalSpentUsd,
      "totalBudgetUsd" to budgetUsd,
      "remainingBudgetUsd" to remainingBudgetUsd,
      "remainingBudgetJpy" to remainingBudgetJpy,
      "expenses" to
        expenses.map {
          mapOf(
            "id" to it.id,
            "title" to it.title,
            "amount" to it.amount,
            "currency" to it.currency,
            "category" to it.category,
          )
        },
      "message" to
        "Found ${expenses.size} expenses totaling $$totalSpentUsd USD out of $$budgetUsd budget ($$remainingBudgetUsd USD / ¥$remainingBudgetJpy JPY remaining).",
    )
  }
}

/** Tool to retrieve the itinerary events for a trip from the Room database. */
class GetItineraryTool(
  private val eventDao: EventDao,
  private val tripDao: TripDao,
  private val activeTripIdProvider: () -> String? = { null },
) {
  val name = "getItinerary"
  val description =
    "Retrieves the list of itinerary events (with their eventId, title, type, and location) for a specific trip. Note: getItinerary returns event summaries only; call getEventDetails(eventId) with a specific event's ID to retrieve ticket prices, hotel rates, baggage allowance, or visitor tips."
  val parametersSchema =
    FunctionSchema(
      type = FunctionType.OBJECT,
      properties =
        mapOf(
          "tripId" to
            FunctionSchema(
              type = FunctionType.STRING,
              description = "The unique identifier of the trip (e.g. '2026-4' for Tokyo).",
            )
        ),
    )

  suspend fun execute(args: Map<String, Any>): Map<String, Any> {
    val tripId =
      args["tripId"]?.toString()?.takeIf { it.isNotBlank() }
        ?: activeTripIdProvider()?.takeIf { it.isNotBlank() }
        ?: tripDao.getAllTrips().first().firstOrNull()?.id
        ?: "2026-4"
    val dbEvents = eventDao.getEventsForTrip(tripId).first()
    val events =
      if (dbEvents.isNotEmpty()) {
        dbEvents
      } else {
        val fallback = DummyData.events.filter { it.tripId == tripId }
        if (fallback.isNotEmpty()) {
          eventDao.insertEvents(fallback)
        }
        fallback
      }
    return mapOf(
      "tripId" to tripId,
      "count" to events.size,
      "events" to
        events.map {
          mapOf(
            "id" to it.id,
            "eventId" to it.id,
            "title" to it.title,
            "type" to it.type.name,
            "location" to it.location,
            "description" to (it.description ?: ""),
          )
        },
      "message" to "Found ${events.size} events in itinerary for trip ($tripId).",
    )
  }
}

/**
 * Tool to retrieve rich subtype details (museum ticket price, hotel nightly rate, flight baggage
 * allowance, visitor tips) for a specific itinerary event by its `eventId`.
 */
class GetEventDetailsTool(
  private val eventDao: EventDao,
  private val tourDetailDao: TourDetailDao? = null,
) {
  val name = "getEventDetails"
  val description =
    "Retrieves rich subtype details for a specific itinerary event by its eventId, including museum admission ticket price and opening hours, hotel nightly rate and confirmation number, flight terminal/gate/baggage allowance, dining reservation info, and visitor tips. Call this after getItinerary(tripId) when you need the price, rate, baggage allowance, or visitor tips of a specific event."
  val parametersSchema =
    FunctionSchema(
      type = FunctionType.OBJECT,
      properties =
        mapOf(
          "eventId" to
            FunctionSchema(
              type = FunctionType.STRING,
              description =
                "The unique identifier of the itinerary event (e.g. 'tok_3', 'tok_2', 'tok_1', 'par_3') returned by getItinerary.",
            )
        ),
      required = listOf("eventId"),
    )

  suspend fun execute(args: Map<String, Any>): Map<String, Any> {
    val eventId = args["eventId"]?.toString()?.trim() ?: ""
    if (eventId.isEmpty()) {
      return mapOf("status" to "ERROR", "message" to "Missing eventId parameter.")
    }

    val event = eventDao.getEventById(eventId).first() ?: DummyData.events.find { it.id == eventId }
    val flight =
      eventDao.getFlightDetail(eventId).first()
        ?: DummyData.flightDetails.find { it.eventId == eventId }
    val hotel =
      eventDao.getHotelDetail(eventId).first()
        ?: DummyData.hotelDetails.find { it.eventId == eventId }
    val museum =
      eventDao.getMuseumDetail(eventId).first()
        ?: DummyData.museumDetails.find { it.eventId == eventId }
    val dining =
      eventDao.getDiningDetail(eventId).first()
        ?: DummyData.diningDetails.find { it.eventId == eventId }
    val tour =
      tourDetailDao?.getTourDetailByEventId(eventId)?.first()
        ?: DummyData.tourDetails.find { it.eventId == eventId }

    val title =
      event?.title
        ?: tour?.title
        ?: hotel?.name
        ?: dining?.restaurantName
        ?: flight?.let { "${it.airline} ${it.flightNum}" }
        ?: eventId
    val type = event?.type?.name ?: tour?.type ?: "ACTIVITY"
    val location =
      event?.location
        ?: museum?.address
        ?: hotel?.address
        ?: dining?.address
        ?: tour?.locationAddress
        ?: ""

    val result =
      mutableMapOf<String, Any>(
        "status" to "SUCCESS",
        "eventId" to eventId,
        "title" to title,
        "type" to type,
        "location" to location,
      )

    val summaryParts = mutableListOf<String>()
    if (museum != null) {
      result["admissionPrice"] = museum.admissionPrice
      result["openingHours"] = museum.openingHours
      result["address"] = museum.address
      result["ticketWebsite"] = museum.ticketWebsite
      result["description"] = museum.description
      if (museum.admissionPrice.isNotBlank()) {
        summaryParts.add("Admission ticket: ${museum.admissionPrice}")
      }
      if (museum.openingHours.isNotBlank()) {
        summaryParts.add("Hours: ${museum.openingHours}")
      }
    }
    if (hotel != null) {
      result["hotelName"] = hotel.name
      result["pricePerNight"] = hotel.pricePerNight ?: ""
      result["confirmationNumber"] = hotel.confNumber ?: ""
      result["address"] = hotel.address
      if (!hotel.pricePerNight.isNullOrBlank()) {
        summaryParts.add("Rate: ${hotel.pricePerNight}")
      }
      if (!hotel.confNumber.isNullOrBlank()) {
        summaryParts.add("Confirmation: ${hotel.confNumber}")
      }
    }
    if (flight != null) {
      result["airline"] = flight.airline
      result["flightNumber"] = flight.flightNum
      result["origin"] = flight.origin
      result["destination"] = flight.destination
      result["departureTerminal"] = flight.departureTerminal ?: ""
      result["arrivalTerminal"] = flight.arrivalTerminal ?: ""
      result["gate"] = flight.gate ?: ""
      result["seat"] = flight.seat ?: ""
      result["baggageAllowance"] = flight.baggageAllowance ?: ""
      summaryParts.add(
        "${flight.airline} ${flight.flightNum} (${flight.origin} -> ${flight.destination})"
      )
      if (!flight.baggageAllowance.isNullOrBlank()) {
        summaryParts.add("Baggage: ${flight.baggageAllowance}")
      }
    }
    if (dining != null) {
      result["restaurantName"] = dining.restaurantName
      result["partySize"] = dining.partySize
      result["priceRange"] = dining.priceRange ?: ""
      result["address"] = dining.address
    }
    if (tour != null) {
      result["tips"] = tour.notes
      result["meetingPoint"] = tour.meetingPoint
      result["about"] = tour.about
      if (tour.notes.isNotEmpty()) {
        summaryParts.add("Visitor tips: ${tour.notes.joinToString("; ")}")
      }
    }

    val summary =
      if (summaryParts.isNotEmpty()) {
        "$title — ${summaryParts.joinToString(" • ")}"
      } else {
        "$title ($location)"
      }
    result["summary"] = summary
    result["message"] = summary
    return result
  }
}

/** Aggregates Room-backed travel tools for injection into the Agent. */
class TravelDaoTools(
  val getFlightItinerary: GetFlightItineraryTool,
  val addExpense: AddExpenseTool? = null,
  val addItineraryEvent: AddItineraryEventTool? = null,
  val addVoiceNote: AddVoiceNoteTool? = null,
  val searchTrip: SearchTripTool? = null,
  val getExpenses: GetExpensesTool? = null,
  val getItinerary: GetItineraryTool? = null,
  val getEventDetails: GetEventDetailsTool? = null,
) {
  constructor(
    eventDao: EventDao,
    tripDao: TripDao,
    expenseDao: ExpenseDao? = null,
    tourDetailDao: TourDetailDao? = null,
    activeTripIdProvider: () -> String? = { null },
  ) : this(
    getFlightItinerary = GetFlightItineraryTool(eventDao, tripDao, activeTripIdProvider),
    addExpense = expenseDao?.let { AddExpenseTool(it, tripDao, activeTripIdProvider) },
    addItineraryEvent = AddItineraryEventTool(eventDao, tripDao, activeTripIdProvider),
    addVoiceNote = AddVoiceNoteTool(eventDao, tripDao, activeTripIdProvider),
    searchTrip = SearchTripTool(tripDao),
    getExpenses = expenseDao?.let { GetExpensesTool(it, tripDao, activeTripIdProvider) },
    getItinerary = GetItineraryTool(eventDao, tripDao, activeTripIdProvider),
    getEventDetails = GetEventDetailsTool(eventDao, tourDetailDao),
  )

  fun asList(): List<Any> = buildList {
    add(getFlightItinerary)
    addExpense?.let { add(it) }
    addItineraryEvent?.let { add(it) }
    addVoiceNote?.let { add(it) }
    searchTrip?.let { add(it) }
    getExpenses?.let { add(it) }
    getItinerary?.let { add(it) }
    getEventDetails?.let { add(it) }
  }
}
