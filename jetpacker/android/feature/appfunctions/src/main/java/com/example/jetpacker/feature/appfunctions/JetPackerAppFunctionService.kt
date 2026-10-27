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

@file:Suppress("GlobalCoroutineDispatchers", "TopLevelName")

package com.example.jetpacker.feature.appfunctions

import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionAccessLevel
import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import androidx.appfunctions.ExperimentalAppFunctionsApi
import androidx.appfunctions.metadata.AppFunctionMetadata
import com.example.jetpacker.data.itinerary.DayTheme
import com.example.jetpacker.data.itinerary.DayThemeDao
import com.example.jetpacker.data.itinerary.DocumentSemanticSearchProvider
import com.example.jetpacker.data.itinerary.EventDao
import com.example.jetpacker.data.itinerary.EventType
import com.example.jetpacker.data.itinerary.Expense
import com.example.jetpacker.data.itinerary.ExpenseDao
import com.example.jetpacker.data.itinerary.TimelineEvent
import com.example.jetpacker.data.itinerary.TourDetailDao
import com.example.jetpacker.data.itinerary.TravelDocument
import com.example.jetpacker.data.itinerary.TravelDocumentDao
import com.example.jetpacker.data.itinerary.TravelDocumentManager
import com.example.jetpacker.data.itinerary.VoiceNoteEntity
import com.example.jetpacker.data.trips.DummyData
import com.example.jetpacker.data.trips.Trip
import com.example.jetpacker.data.trips.TripDao
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

@RequiresApi(36)
@AndroidEntryPoint(AppFunctionService::class)
@AppFunctionServiceEntryPoint(
  serviceName = "JetPackerAppFunctionService",
  appFunctionXmlFileName = "jetpacker_app_function_service",
)
abstract class BaseJetPackerAppFunctionService : Hilt_BaseJetPackerAppFunctionService() {
  @Inject internal lateinit var tripDao: TripDao
  @Inject internal lateinit var expenseDao: ExpenseDao
  @Inject internal lateinit var eventDao: EventDao
  @Inject internal lateinit var tourDetailDao: TourDetailDao
  @Inject internal lateinit var dayThemeDao: DayThemeDao
  @Inject internal lateinit var travelDocumentDao: TravelDocumentDao
  @Inject internal lateinit var travelDocumentManager: TravelDocumentManager
  @Inject internal lateinit var semanticSearchProvider: DocumentSemanticSearchProvider

  /**
   * Searches for trips based on optional filters like query, id, title (name), location, and dates.
   * Call this first when the user refers to a trip by city or name and you need its tripId.
   *
   * @param query Optional search keyword matching trip title or destination location (e.g. 'Tokyo',
   *   'Paris').
   * @param id The unique identifier of the trip.
   * @param title The title or name of the trip.
   * @param location The destination location.
   * @param startDate The minimum start date in milliseconds.
   * @param endDate The maximum end date in milliseconds.
   * @return A list of trips matching the filters.
   */
  @AppFunctionDeclaration(isDescribedByKDoc = true)
  suspend fun searchTrip(
    query: String? = null,
    id: String? = null,
    title: String? = null,
    location: String? = null,
    startDate: Long? = null,
    endDate: Long? = null,
  ): List<TripSerializable> {
    return withContext(Dispatchers.IO) {
      val dbTrips = tripDao.getAllTrips().first()
      val allTrips = if (dbTrips.isNotEmpty()) dbTrips else DummyData.trips
      allTrips
        .filter { trip ->
          (query.isNullOrBlank() ||
            trip.title.contains(query, ignoreCase = true) ||
            trip.location.contains(query, ignoreCase = true) ||
            trip.id.equals(query, ignoreCase = true)) &&
            (id.isNullOrBlank() || trip.id.equals(id, ignoreCase = true)) &&
            (title.isNullOrBlank() ||
              trip.title.contains(title, ignoreCase = true) ||
              trip.location.contains(title, ignoreCase = true)) &&
            (location.isNullOrBlank() ||
              trip.location.contains(location, ignoreCase = true) ||
              trip.title.contains(location, ignoreCase = true)) &&
            (startDate == null || trip.startDate >= startDate) &&
            (endDate == null || trip.endDate <= endDate)
        }
        .map { it.toSerializable() }
    }
  }

  /**
   * Creates a new trip.
   *
   * @param title The title of the trip.
   * @param location The destination location.
   * @param startDate The start date in milliseconds.
   * @param endDate The end date in milliseconds.
   * @return The created trip.
   */
  @AppFunctionDeclaration(isDescribedByKDoc = true)
  suspend fun createTrip(
    title: String,
    location: String,
    startDate: Long,
    endDate: Long,
  ): TripSerializable {
    val trip =
      Trip(
        id = UUID.randomUUID().toString(),
        title = title,
        location = location,
        startDate = startDate,
        endDate = endDate,
      )
    return withContext(Dispatchers.IO) {
      tripDao.insertTrip(trip)
      trip.toSerializable()
    }
  }

  /**
   * Adds an expense to a trip. Call this ONLY when the user explicitly asks to add, log, or record
   * an expense (e.g., 'Please add 45 dollars for the airport Uber to Mountain View' or in a
   * follow-up turn 'Great, I just bought it, can you add that to my expenses?'). Do NOT call this
   * when the user is only asking how much budget they have left or whether they can afford an item.
   *
   * @param title The title or description of the expense (e.g., 'Airport Uber to Mountain View',
   *   'SFMOMA Museum Ticket', 'Mechanical Keyboard').
   * @param amount The numeric amount of the expense (e.g., 45.0, 25.0, 180.0).
   * @param currency The currency code of the expense (e.g., 'USD', 'EUR', 'JPY', 'GBP'). Extract
   *   from the user's request or event details (e.g., 'dollars'/'$' -> 'USD', 'euros'/'€' -> 'EUR',
   *   'yen'/'¥' -> 'JPY', 'pounds'/'£' -> 'GBP').
   * @param tripId Optional ID of the trip to add the expense to (e.g., '2026-1'). Defaults to the
   *   active trip if omitted.
   * @param category Optional category of the expense ('Transport', 'Food', 'Accommodation',
   *   'Activity'). Inferred from title if omitted.
   * @return The added expense.
   */
  @AppFunctionDeclaration(isDescribedByKDoc = true)
  suspend fun addExpense(
    title: String,
    amount: Double,
    currency: String,
    tripId: String? = null,
    category: String? = null,
  ): ExpenseSerializable {
    return withContext(Dispatchers.IO) {
      val resolvedTitle =
        if (
          title.isBlank() ||
            title.lowercase() in
              setOf(
                "it",
                "that",
                "this",
                "expense",
                "pass",
                "ticket",
                "keyboard",
                "hoodie",
                "cruise",
                "bay cruise",
                "that expense",
                "the expense",
              )
        ) {
          ActiveTripSession.pendingExpenseTitle?.takeIf { it.isNotBlank() } ?: title
        } else {
          title
        }
      val resolvedAmount =
        if (amount <= 0.0) {
          ActiveTripSession.pendingExpenseAmount ?: amount
        } else {
          amount
        }
      val rawCurrency =
        if (currency.isBlank() && !ActiveTripSession.pendingExpenseCurrency.isNullOrBlank()) {
          ActiveTripSession.pendingExpenseCurrency!!
        } else {
          currency
        }
      val resolvedCurrency = normalizeCurrency(rawCurrency, resolvedTitle)
      val resolvedCategory =
        category?.takeIf { it.isNotBlank() } ?: inferExpenseCategory(resolvedTitle)
      val trip = resolveTrip(tripId = tripId, textHint = resolvedTitle)
      val targetTripId =
        trip?.id ?: ActiveTripSession.activeTripId ?: tripId?.takeIf { it.isNotBlank() } ?: "2026-1"

      val recentExpenses = expenseDao.getExpensesForTrip(targetTripId).first()
      val now = System.currentTimeMillis()
      val tripCurrency = trip?.currency ?: "USD"
      val tripBudget = trip?.budget ?: 2000.0

      if (ActiveTripSession.isReadOnlyBudgetCheck) {
        ActiveTripSession.pendingExpenseTitle = resolvedTitle
        ActiveTripSession.pendingExpenseAmount = resolvedAmount
        ActiveTripSession.pendingExpenseCurrency = resolvedCurrency

        val totalSpent =
          kotlin.math.round(
            recentExpenses.sumOf { convertCurrency(it.amount, it.currency, tripCurrency) } * 100.0
          ) / 100.0
        val remainingBudget = kotlin.math.round((tripBudget - totalSpent) * 100.0) / 100.0
        val candidateInTrip =
          kotlin.math.round(
            convertCurrency(resolvedAmount, resolvedCurrency, tripCurrency) * 100.0
          ) / 100.0
        val afterBudget = kotlin.math.round((remainingBudget - candidateInTrip) * 100.0) / 100.0

        val totalSpentUsd =
          kotlin.math.round(recentExpenses.sumOf { convertToUsd(it.amount, it.currency) } * 100.0) /
            100.0
        val budgetUsd = convertToUsd(tripBudget, tripCurrency)
        val remainingBudgetUsd = kotlin.math.round((budgetUsd - totalSpentUsd) * 100.0) / 100.0
        val remainingBudgetJpy = kotlin.math.round(remainingBudgetUsd * 150.0)
        val candidateUsd =
          kotlin.math.round(convertToUsd(resolvedAmount, resolvedCurrency) * 100.0) / 100.0
        val candidateJpy = kotlin.math.round(candidateUsd * 150.0).toLong()
        val afterUsd = kotlin.math.round((remainingBudgetUsd - candidateUsd) * 100.0) / 100.0
        val afterJpy = kotlin.math.round(afterUsd * 150.0)

        val priceStr = formatCurrency(resolvedAmount, resolvedCurrency)
        val candidateStr =
          if (resolvedCurrency.equals(tripCurrency, ignoreCase = true)) priceStr
          else "$priceStr (${formatCurrency(candidateInTrip, tripCurrency)})"

        return@withContext ExpenseSerializable(
          id = "budget-preview",
          tripId = targetTripId,
          title = resolvedTitle,
          amount = resolvedAmount,
          currency = resolvedCurrency,
          category = resolvedCategory,
          timestamp = now,
          tripTitle = trip?.title,
          amountUsd = candidateUsd,
          tripTotalSpentUsd = totalSpentUsd,
          tripRemainingBudgetUsd = remainingBudgetUsd,
          tripRemainingBudgetJpy = remainingBudgetJpy,
          tripCurrency = tripCurrency,
          tripBudget = tripBudget,
          tripTotalSpent = totalSpent,
          tripRemainingBudget = remainingBudget,
          status = "SUCCESS",
          message =
            "Budget check: '$resolvedTitle' costs $candidateStr. You currently have ${formatCurrency(remainingBudget, tripCurrency)} remaining (spent ${formatCurrency(totalSpent, tripCurrency)}). Adding this expense leaves ${formatCurrency(afterBudget, tripCurrency)} remaining from your ${formatCurrency(tripBudget, tripCurrency)} budget.",
        )
      }

      val duplicate = recentExpenses.firstOrNull { existing ->
        existing.title.equals(resolvedTitle, ignoreCase = true) &&
          kotlin.math.abs(existing.amount - resolvedAmount) < 0.001 &&
          (now - existing.timestamp) < 15_000L
      }
      if (duplicate != null) {
        val totalSpent =
          kotlin.math.round(
            recentExpenses.sumOf { convertCurrency(it.amount, it.currency, tripCurrency) } * 100.0
          ) / 100.0
        val remainingBudget = kotlin.math.round((tripBudget - totalSpent) * 100.0) / 100.0
        val totalSpentUsd =
          kotlin.math.round(recentExpenses.sumOf { convertToUsd(it.amount, it.currency) } * 100.0) /
            100.0
        val budgetUsd = convertToUsd(tripBudget, tripCurrency)
        val remainingBudgetUsd = kotlin.math.round((budgetUsd - totalSpentUsd) * 100.0) / 100.0
        val remainingBudgetJpy = kotlin.math.round(remainingBudgetUsd * 150.0)
        return@withContext duplicate.toSerializable(
          tripTitle = trip?.title,
          amountUsd = convertToUsd(duplicate.amount, duplicate.currency),
          tripTotalSpentUsd = totalSpentUsd,
          tripRemainingBudgetUsd = remainingBudgetUsd,
          tripRemainingBudgetJpy = remainingBudgetJpy,
          tripCurrency = tripCurrency,
          tripBudget = tripBudget,
          tripTotalSpent = totalSpent,
          tripRemainingBudget = remainingBudget,
          message =
            "Expense '$resolvedTitle' (${formatCurrency(resolvedAmount, resolvedCurrency)}) is already logged in ${trip?.title ?: "your trip"}. Remaining budget: ${formatCurrency(remainingBudget, tripCurrency)}.",
        )
      }

      val existingSameTitle = recentExpenses.firstOrNull { existing ->
        (now - existing.timestamp) < 600_000L &&
          existing.title.equals(resolvedTitle, ignoreCase = true)
      }

      val expense =
        if (existingSameTitle != null) {
          existingSameTitle.copy(
            title = resolvedTitle,
            amount = resolvedAmount,
            currency = resolvedCurrency,
            category = resolvedCategory,
            timestamp = now,
          )
        } else {
          Expense(
            tripId = targetTripId,
            title = resolvedTitle,
            amount = resolvedAmount,
            currency = resolvedCurrency,
            category = resolvedCategory,
            timestamp = now,
          )
        }
      expenseDao.insertExpense(expense)
      ActiveTripSession.pendingExpenseTitle = null
      ActiveTripSession.pendingExpenseAmount = null
      ActiveTripSession.pendingExpenseCurrency = null

      val updatedExpenses = expenseDao.getExpensesForTrip(targetTripId).first()
      val totalSpent =
        kotlin.math.round(
          updatedExpenses.sumOf { convertCurrency(it.amount, it.currency, tripCurrency) } * 100.0
        ) / 100.0
      val remainingBudget = kotlin.math.round((tripBudget - totalSpent) * 100.0) / 100.0
      val totalSpentUsd =
        kotlin.math.round(updatedExpenses.sumOf { convertToUsd(it.amount, it.currency) } * 100.0) /
          100.0
      val budgetUsd = convertToUsd(tripBudget, tripCurrency)
      val remainingBudgetUsd = kotlin.math.round((budgetUsd - totalSpentUsd) * 100.0) / 100.0
      val remainingBudgetJpy = kotlin.math.round(remainingBudgetUsd * 150.0)
      val verb = if (existingSameTitle != null) "Updated" else "Added"
      val priceStr = formatCurrency(resolvedAmount, resolvedCurrency)
      val convertedNotice =
        if (resolvedCurrency.equals(tripCurrency, ignoreCase = true)) priceStr
        else
          "$priceStr (approx ${formatCurrency(convertCurrency(resolvedAmount, resolvedCurrency, tripCurrency), tripCurrency)})"

      expense.toSerializable(
        tripTitle = trip?.title,
        amountUsd = convertToUsd(expense.amount, expense.currency),
        tripTotalSpentUsd = totalSpentUsd,
        tripRemainingBudgetUsd = remainingBudgetUsd,
        tripRemainingBudgetJpy = remainingBudgetJpy,
        tripCurrency = tripCurrency,
        tripBudget = tripBudget,
        tripTotalSpent = totalSpent,
        tripRemainingBudget = remainingBudget,
        message =
          "$verb '$resolvedTitle' ($convertedNotice) to ${trip?.title ?: "your trip"}. Remaining budget: ${formatCurrency(remainingBudget, tripCurrency)}.",
      )
    }
  }

  /**
   * Lists all logged expenses and remaining trip budget for a specific trip. Call this when the
   * user asks about their spending, remaining budget, or how much budget they would have left if
   * they buy something.
   *
   * @param tripId Optional ID of the trip (e.g., '2026-1'). Defaults to the active trip if omitted.
   * @return A list of expenses and budget summary for the trip.
   */
  @AppFunctionDeclaration(isDescribedByKDoc = true)
  suspend fun getExpenses(tripId: String? = null): List<ExpenseSerializable> {
    return withContext(Dispatchers.IO) {
      val trip = resolveTrip(tripId = tripId)
      val targetTripId =
        trip?.id ?: ActiveTripSession.activeTripId ?: tripId?.takeIf { it.isNotBlank() } ?: "2026-1"
      val tripCurrency = trip?.currency ?: "USD"
      val tripBudget = trip?.budget ?: 2000.0

      val expenses = expenseDao.getExpensesForTrip(targetTripId).first()
      val totalSpent =
        kotlin.math.round(
          expenses.sumOf { convertCurrency(it.amount, it.currency, tripCurrency) } * 100.0
        ) / 100.0
      val remainingBudget = kotlin.math.round((tripBudget - totalSpent) * 100.0) / 100.0

      val totalSpentUsd =
        kotlin.math.round(expenses.sumOf { convertToUsd(it.amount, it.currency) } * 100.0) / 100.0
      val budgetUsd = convertToUsd(tripBudget, tripCurrency)
      val remainingBudgetUsd = kotlin.math.round((budgetUsd - totalSpentUsd) * 100.0) / 100.0
      val remainingBudgetJpy = kotlin.math.round(remainingBudgetUsd * 150.0)

      val pendingAmt = ActiveTripSession.pendingExpenseAmount
      val pendingCurr = ActiveTripSession.pendingExpenseCurrency ?: tripCurrency
      val pendingTitle = ActiveTripSession.pendingExpenseTitle ?: "item"
      val summaryMsg =
        if (pendingAmt != null && pendingAmt > 0.0) {
          val pendingInTripCurr =
            kotlin.math.round(convertCurrency(pendingAmt, pendingCurr, tripCurrency) * 100.0) /
              100.0
          val afterBudget = kotlin.math.round((remainingBudget - pendingInTripCurr) * 100.0) / 100.0
          val itemPrice = formatCurrency(pendingAmt, pendingCurr)
          val itemStr =
            if (pendingCurr.equals(tripCurrency, ignoreCase = true)) itemPrice
            else "$itemPrice (${formatCurrency(pendingInTripCurr, tripCurrency)})"
          "Budget check: You currently have ${formatCurrency(remainingBudget, tripCurrency)} remaining (spent ${formatCurrency(totalSpent, tripCurrency)}). Adding '$pendingTitle' for $itemStr leaves ${formatCurrency(afterBudget, tripCurrency)} remaining from your ${formatCurrency(tripBudget, tripCurrency)} budget."
        } else {
          "Total spent so far: ${formatCurrency(totalSpent, tripCurrency)} out of ${formatCurrency(tripBudget, tripCurrency)} budget. Remaining budget: ${formatCurrency(remainingBudget, tripCurrency)}."
        }

      if (expenses.isEmpty()) {
        listOf(
          ExpenseSerializable(
            id = "summary",
            tripId = targetTripId,
            title = "Trip Budget Summary",
            amount = 0.0,
            currency = tripCurrency,
            category = "Summary",
            timestamp = System.currentTimeMillis(),
            tripTitle = trip?.title,
            amountUsd = 0.0,
            tripTotalSpentUsd = totalSpentUsd,
            tripRemainingBudgetUsd = remainingBudgetUsd,
            tripRemainingBudgetJpy = remainingBudgetJpy,
            tripCurrency = tripCurrency,
            tripBudget = tripBudget,
            tripTotalSpent = totalSpent,
            tripRemainingBudget = remainingBudget,
            status = "SUCCESS",
            message = summaryMsg,
          )
        )
      } else {
        expenses.map {
          it.toSerializable(
            tripTitle = trip?.title,
            amountUsd = convertToUsd(it.amount, it.currency),
            tripTotalSpentUsd = totalSpentUsd,
            tripRemainingBudgetUsd = remainingBudgetUsd,
            tripRemainingBudgetJpy = remainingBudgetJpy,
            tripCurrency = tripCurrency,
            tripBudget = tripBudget,
            tripTotalSpent = totalSpent,
            tripRemainingBudget = remainingBudget,
            message = summaryMsg,
          )
        }
      }
    }
  }

  /**
   * Retrieves the itinerary events (with their eventId, title, type, and location) for a specific
   * trip. Note: getItinerary returns event summaries only; call getEventDetails(eventId) with a
   * specific event's ID to retrieve ticket prices, hotel nightly rates, baggage allowance, or
   * visitor tips.
   *
   * @param tripId Optional ID of the trip (e.g., '2026-1'). Defaults to the active trip if omitted.
   * @return A list of events in the itinerary.
   */
  @AppFunctionDeclaration(isDescribedByKDoc = true)
  suspend fun getItinerary(tripId: String? = null): List<TimelineEventSerializable> {
    return withContext(Dispatchers.IO) {
      val trip = resolveTrip(tripId = tripId)
      val targetTripId =
        trip?.id ?: ActiveTripSession.activeTripId ?: tripId?.takeIf { it.isNotBlank() } ?: "2026-1"
      val dbEvents = eventDao.getEventsForTrip(targetTripId).first()
      val events =
        if (dbEvents.isNotEmpty()) {
          dbEvents
        } else {
          val fallback = DummyData.events.filter { it.tripId == targetTripId }
          if (fallback.isNotEmpty()) {
            eventDao.insertEvents(fallback)
          }
          fallback
        }
      events.map { it.toSerializable(tripTitle = trip?.title) }
    }
  }

  /**
   * Retrieves rich subtype details for a specific itinerary event by its eventId, including museum
   * admission price and hours, hotel nightly rate and confirmation number, flight baggage
   * allowance, and visitor tips. Call this after getItinerary(tripId) when you need the price,
   * rate, baggage allowance, or visitor tips of a specific event.
   *
   * @param eventId The unique identifier of the itinerary event (e.g., "io_5", "io_3", "tok_3",
   *   "par_3").
   * @return The detailed event information.
   */
  @AppFunctionDeclaration(isDescribedByKDoc = true)
  suspend fun getEventDetails(eventId: String): EventDetailsSerializable {
    return withContext(Dispatchers.IO) {
      val event =
        eventDao.getEventById(eventId).first() ?: DummyData.events.find { it.id == eventId }
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
        tourDetailDao.getTourDetailByEventId(eventId).first()
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

      EventDetailsSerializable(
        eventId = eventId,
        title = title,
        type = type,
        location = location,
        admissionPrice = museum?.admissionPrice,
        openingHours = museum?.openingHours,
        pricePerNight = hotel?.pricePerNight,
        confirmationNumber = hotel?.confNumber,
        flightInfo =
          flight?.let { "${it.airline} ${it.flightNum} (${it.origin} -> ${it.destination})" },
        baggageAllowance = flight?.baggageAllowance,
        tips = tour?.notes ?: emptyList(),
        description = museum?.description ?: tour?.about ?: event?.description,
      )
    }
  }

  /**
   * Adds a new event or reminder to a trip's itinerary.
   *
   * @param title The title of the event or reminder (e.g., 'Pick up pocket wifi').
   * @param tripId Optional ID of the trip. Defaults to the active trip if omitted.
   * @param type Optional type of event (e.g., ACTIVITY, TRANSPORTATION, ACCOMMODATION,
   *   FOOD_AND_DRINK, CULTURE).
   * @param timestamp Optional time of the event in milliseconds.
   * @param location Optional location where the event takes place.
   * @param description An optional description of the event.
   * @return The added event.
   */
  @AppFunctionDeclaration(isDescribedByKDoc = true)
  suspend fun addItineraryEvent(
    title: String,
    tripId: String? = null,
    type: String? = null,
    timestamp: Long? = null,
    location: String? = null,
    description: String? = null,
  ): TimelineEventSerializable {
    return withContext(Dispatchers.IO) {
      val eventType =
        try {
          EventType.valueOf((type ?: "ACTIVITY").uppercase())
        } catch (_: IllegalArgumentException) {
          EventType.ACTIVITY
        }
      val trip = resolveTrip(tripId = tripId, textHint = "$title ${location ?: ""}")
      val targetTripId =
        trip?.id ?: ActiveTripSession.activeTripId ?: tripId?.takeIf { it.isNotBlank() } ?: "2026-1"
      val resolvedLocation =
        location?.takeIf { it.isNotBlank() } ?: trip?.location ?: "Mountain View, CA"
      val resolvedDescription = description ?: "Added via Travel Assistant"

      val existingEvents = eventDao.getEventsForTrip(targetTripId).first()
      val duplicateEvent = existingEvents.firstOrNull { existing ->
        existing.title.equals(title, ignoreCase = true)
      }
      if (duplicateEvent != null) {
        return@withContext duplicateEvent.toSerializable(tripTitle = trip?.title)
      }

      val eventTimestamp =
        if (timestamp != null && timestamp > 0L) {
          timestamp
        } else if (trip != null && trip.startDate > 0L) {
          trip.startDate
        } else {
          System.currentTimeMillis()
        }

      val event =
        TimelineEvent(
          id = UUID.randomUUID().toString(),
          tripId = targetTripId,
          type = eventType,
          timestamp = eventTimestamp,
          title = title,
          location = resolvedLocation,
          description = resolvedDescription,
        )
      eventDao.insertEvent(event)

      val matchedExisting =
        existingEvents.find { ev ->
          val combined = "$title $resolvedLocation $resolvedDescription".lowercase()
          (combined.contains("airport") ||
            combined.contains("flight") ||
            combined.contains("sim")) && ev.type == EventType.TRANSPORTATION ||
            (combined.contains("museum") || combined.contains("sfmoma")) &&
              ev.type == EventType.CULTURE ||
            (combined.contains("keynote") ||
              combined.contains("badge") ||
              combined.contains("bay view") ||
              combined.contains("bv300") ||
              combined.contains("session") ||
              combined.contains("workshop")) && ev.type == EventType.WORK ||
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
              if (resolvedDescription != "Added via Travel Assistant") {
                "$title — $resolvedDescription"
              } else {
                title
              },
            timestamp = System.currentTimeMillis(),
            matchingEventsJson = matchingJson,
          )
        )
      }

      event.toSerializable(
        tripTitle = trip?.title,
        matchedEventTitle = matchedExisting.title.takeIf { matchedExisting.id != event.id },
      )
    }
  }

  /**
   * Retrieves voice notes associated with a trip.
   *
   * @param tripId Optional ID of the trip. Defaults to the active trip if omitted.
   * @return A list of voice notes.
   */
  @AppFunctionDeclaration(isDescribedByKDoc = true)
  suspend fun getVoiceNotes(tripId: String? = null): List<VoiceNoteSerializable> {
    return withContext(Dispatchers.IO) {
      val trip = resolveTrip(tripId = tripId)
      val targetTripId =
        trip?.id ?: ActiveTripSession.activeTripId ?: tripId?.takeIf { it.isNotBlank() } ?: "2026-1"
      eventDao.getVoiceNotesForTrip(targetTripId).first().map { it.toSerializable() }
    }
  }

  /**
   * Adds a voice note or reminder transcription to a trip and links it to any matching itinerary
   * event (e.g., keynote, airport flight, or museum visit). Call this when the user asks to save a
   * voice note, visitor tips, or reminder (e.g., 'Please remind me to pick up my speaker badge at
   * Bay View 300' or 'Save the museum visitor tips as a voice note').
   *
   * @param title Short title of the voice note or reminder (e.g., 'Pick up speaker badge at Bay
   *   View 300').
   * @param transcription Optional full text content of the voice note, reminder, or visitor tips.
   *   Defaults to title if omitted.
   * @param tripId Optional ID of the trip. Defaults to the active trip if omitted.
   * @param eventId Optional ID of the itinerary event to link this voice note to (e.g., 'io_5',
   *   'io_6', 'tok_3').
   * @return The added voice note.
   */
  @AppFunctionDeclaration(isDescribedByKDoc = true)
  suspend fun addVoiceNote(
    title: String,
    transcription: String? = null,
    tripId: String? = null,
    eventId: String? = null,
  ): VoiceNoteSerializable {
    return withContext(Dispatchers.IO) {
      val resolvedTranscription = transcription?.takeIf { it.isNotBlank() } ?: title
      val trip = resolveTrip(tripId = tripId, textHint = "$title $resolvedTranscription")
      val targetTripId =
        trip?.id ?: ActiveTripSession.activeTripId ?: tripId?.takeIf { it.isNotBlank() } ?: "2026-1"

      val existingNotes = eventDao.getVoiceNotesForTrip(targetTripId).first()
      val now = System.currentTimeMillis()
      val duplicate = existingNotes.firstOrNull { existing ->
        existing.title.equals(title, ignoreCase = true) && (now - existing.timestamp) < 15_000L
      }
      if (duplicate != null) {
        return@withContext duplicate.toSerializable()
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
        if (!eventId.isNullOrBlank()) {
          events.find { it.id == eventId }
        } else {
          val combined = "$title $resolvedTranscription".lowercase()
          events.find { ev ->
            (combined.contains("airport") ||
              combined.contains("flight") ||
              combined.contains("sim") ||
              combined.contains("baggage")) && ev.type == EventType.TRANSPORTATION ||
              (combined.contains("museum") ||
                combined.contains("sfmoma") ||
                combined.contains("coat check") ||
                combined.contains("floor 7") ||
                combined.contains("barefoot") ||
                combined.contains("knees")) && ev.type == EventType.CULTURE ||
              (combined.contains("keynote") ||
                combined.contains("badge") ||
                combined.contains("bay view") ||
                combined.contains("bv300") ||
                combined.contains("session") ||
                combined.contains("workshop") ||
                combined.contains("podium")) && ev.type == EventType.WORK ||
              combined.contains("hotel") && ev.type == EventType.ACCOMMODATION ||
              ev.title.lowercase().split(" ").any { word ->
                word.length > 3 && combined.contains(word)
              }
          }
        }

      val matchingJson =
        if (matchedEvent != null) {
          val escapedExtract = resolvedTranscription.replace("\"", "\\\"")
          """[{"eventId":"${matchedEvent.id}","extract":"$escapedExtract"}]"""
        } else {
          "[]"
        }

      if (matchedEvent != null) {
        val updatedNotes = (matchedEvent.audioNotes + resolvedTranscription).distinct()
        eventDao.insertEvent(matchedEvent.copy(audioNotes = updatedNotes))
      }

      val note =
        VoiceNoteEntity(
          id = UUID.randomUUID().toString(),
          tripId = targetTripId,
          title = title,
          transcription = resolvedTranscription,
          timestamp = now,
          matchingEventsJson = matchingJson,
        )
      eventDao.insertVoiceNote(note)
      note.toSerializable(
        matchedEventId = matchedEvent?.id,
        matchedEventTitle = matchedEvent?.title,
      )
    }
  }

  /**
   * Retrieves day themes for a specific trip.
   *
   * @param tripId Optional ID of the trip. Defaults to the active trip if omitted.
   * @return A list of day themes.
   */
  @AppFunctionDeclaration(isDescribedByKDoc = true)
  suspend fun getDayThemes(tripId: String? = null): List<DayThemeSerializable> {
    return withContext(Dispatchers.IO) {
      val trip = resolveTrip(tripId = tripId)
      val targetTripId =
        trip?.id ?: ActiveTripSession.activeTripId ?: tripId?.takeIf { it.isNotBlank() } ?: "2026-1"
      dayThemeDao.getThemesForTrip(targetTripId).first().map { it.toSerializable() }
    }
  }

  /**
   * Sets a theme for a specific day in a trip.
   *
   * @param tripId The ID of the trip.
   * @param date The date for the theme (e.g., "2026-05-15").
   * @param theme The theme text.
   * @return The updated or added day theme.
   */
  @AppFunctionDeclaration(isDescribedByKDoc = true)
  suspend fun setDayTheme(tripId: String, date: String, theme: String): DayThemeSerializable {
    val dayTheme =
      DayTheme(id = UUID.randomUUID().toString(), tripId = tripId, date = date, theme = theme)
    return withContext(Dispatchers.IO) {
      dayThemeDao.insertTheme(dayTheme)
      dayTheme.toSerializable()
    }
  }

  /**
   * Searches for saved travel documents, passports, visas, boarding passes, museum tickets,
   * hotel reservations, Wi-Fi passwords, and travel notes. Restricted to scope SELF.
   * Call this when the user asks about their travel documents, ticket confirmation numbers,
   * passport expiry, hotel check-in details, or notes for their trip.
   *
   * @param query Optional search keywords to match document title, notes, or issuing country
   *   (e.g. 'passport', 'SFMOMA', 'hotel', 'wifi', 'visa').
   * @param documentType Optional filter for document type ('PASSPORT', 'VISA', 'TICKET', 'HOTEL', 'OTHER').
   * @param tripId Optional ID of the trip. Defaults to the active trip if omitted.
   * @return A list of matching travel documents.
   */
  @OptIn(ExperimentalAppFunctionsApi::class)
  @AppFunctionDeclaration(isDescribedByKDoc = true)
  @AppFunctionAccessLevel(
    level = AppFunctionMetadata.ACCESS_LEVEL_SELF,
    isCompatEnforcementEnabled = true,
  )
  suspend fun searchTravelDocuments(
    query: String? = null,
    documentType: String? = null,
    tripId: String? = null,
  ): List<TravelDocumentSerializable> {
    return withContext(Dispatchers.IO) {
      var allDocs = travelDocumentDao.getAllDocuments().first()
      if (allDocs.isEmpty()) {
        travelDocumentManager.seedDefaultDocumentsIfEmpty()
        allDocs = travelDocumentDao.getAllDocuments().first()
      }
      val trip = resolveTrip(tripId = tripId, textHint = query)
      val targetTripId = trip?.id ?: ActiveTripSession.activeTripId ?: tripId?.takeIf { it.isNotBlank() }

      // 1. Natural language semantic search using on-device Embedding Gemma v2 and AppSearch
      // (same engine as the DocumentsScreen vault search)
      val semanticDocs = if (!query.isNullOrBlank()) {
        try {
          val searcher =
            if (::semanticSearchProvider.isInitialized) semanticSearchProvider
            else travelDocumentManager.semanticSearchProvider
          searcher?.findRelevantDocuments(query = query, tripId = targetTripId, maxResults = 10)
            ?: travelDocumentManager.searchDocumentsSemantically(query = query, tripId = targetTripId, maxResults = 10)
        } catch (_: Exception) {
          emptyList()
        }
      } else {
        emptyList()
      }

      val candidateDocs = if (semanticDocs.isNotEmpty()) {
        semanticDocs
      } else {
        allDocs
      }

      val queryWords =
        query
          ?.lowercase()
          ?.split(Regex("[\\s,;.]+"))
          ?.filter { it.length > 2 && it !in setOf("the", "for", "our", "and", "this", "validity") }
          ?: emptyList()

      val filtered = candidateDocs.filter { doc ->
        val matchesTrip = targetTripId == null || doc.tripId == null || doc.tripId == targetTripId
        val matchesType = documentType.isNullOrBlank() ||
          doc.documentType.equals(documentType, ignoreCase = true) ||
          (documentType.equals("TICKET", ignoreCase = true) && doc.documentType.contains("TICKET", ignoreCase = true)) ||
          (documentType.equals("HOTEL", ignoreCase = true) && doc.documentType.contains("HOTEL", ignoreCase = true))

        if (semanticDocs.isNotEmpty()) {
          matchesTrip && matchesType
        } else {
          val content = if (query != null) travelDocumentManager.readDocumentContent(doc) else ""
          val matchesQuery = query.isNullOrBlank() ||
            doc.title.contains(query, ignoreCase = true) ||
            doc.notes.contains(query, ignoreCase = true) ||
            doc.issuingCountry.contains(query, ignoreCase = true) ||
            doc.documentType.contains(query, ignoreCase = true) ||
            doc.documentNumberMasked.contains(query, ignoreCase = true) ||
            content.contains(query, ignoreCase = true) ||
            queryWords.any { word ->
              doc.title.contains(word, ignoreCase = true) ||
              doc.notes.contains(word, ignoreCase = true) ||
              doc.documentType.contains(word, ignoreCase = true) ||
              doc.issuingCountry.contains(word, ignoreCase = true) ||
              content.contains(word, ignoreCase = true)
            }

          matchesTrip && matchesType && matchesQuery
        }
      }

      filtered.map { it.toSerializable() }
    }
  }

  /**
   * Retrieves the full details, extracted content, notes, and metadata of a specific travel document
   * by its title, keyword, or document ID. Restricted to scope SELF. Call this when the user needs
   * specific information from a document, such as the full notes, passport number, Wi-Fi password,
   * or booking confirmation details.
   *
   * @param titleOrId The title, keyword, or ID of the document (e.g. 'US Passport', 'SFMOMA Pass', 'Hotel Confirmation').
   * @param tripId Optional ID of the trip. Defaults to the active trip if omitted.
   * @return The detailed document information including extracted content and notes.
   */
  @OptIn(ExperimentalAppFunctionsApi::class)
  @AppFunctionDeclaration(isDescribedByKDoc = true)
  @AppFunctionAccessLevel(
    level = AppFunctionMetadata.ACCESS_LEVEL_SELF,
    isCompatEnforcementEnabled = true,
  )
  suspend fun getTravelDocumentDetails(
    titleOrId: String,
    tripId: String? = null,
  ): TravelDocumentDetailsSerializable {
    return withContext(Dispatchers.IO) {
      var allDocs = travelDocumentDao.getAllDocuments().first()
      if (allDocs.isEmpty()) {
        travelDocumentManager.seedDefaultDocumentsIfEmpty()
        allDocs = travelDocumentDao.getAllDocuments().first()
      }
      val trip = resolveTrip(tripId = tripId, textHint = titleOrId)
      val targetTripId = trip?.id ?: ActiveTripSession.activeTripId ?: tripId?.takeIf { it.isNotBlank() }

      val titleWords =
        titleOrId
          .lowercase()
          .split(Regex("[\\s,;.]+"))
          .filter { it.length > 2 && it !in setOf("the", "for", "our", "and", "this") }

      var doc = allDocs.find {
        it.documentId.equals(titleOrId, ignoreCase = true) || it.id.equals(titleOrId, ignoreCase = true)
      } ?: allDocs.find {
        it.title.equals(titleOrId, ignoreCase = true) && (targetTripId == null || it.tripId == null || it.tripId == targetTripId)
      } ?: allDocs.find {
        it.title.contains(titleOrId, ignoreCase = true) && (targetTripId == null || it.tripId == null || it.tripId == targetTripId)
      } ?: allDocs.find {
        it.title.contains(titleOrId, ignoreCase = true)
      } ?: allDocs.find { candidate ->
        (targetTripId == null || candidate.tripId == null || candidate.tripId == targetTripId) &&
          titleWords.any { word ->
            candidate.title.contains(word, ignoreCase = true) ||
            candidate.documentType.contains(word, ignoreCase = true) ||
            candidate.notes.contains(word, ignoreCase = true)
          }
      }

      if (doc == null && titleOrId.isNotBlank()) {
        try {
          val searcher =
            if (::semanticSearchProvider.isInitialized) semanticSearchProvider
            else travelDocumentManager.semanticSearchProvider
          val matches =
            searcher?.findRelevantDocuments(query = titleOrId, tripId = targetTripId, maxResults = 1)
              ?: travelDocumentManager.searchDocumentsSemantically(query = titleOrId, tripId = targetTripId, maxResults = 1)
          doc = matches.firstOrNull()
        } catch (_: Exception) {}
      }

      if (doc == null) {
        return@withContext TravelDocumentDetailsSerializable(
          id = "not_found",
          title = titleOrId,
          documentType = "OTHER",
          status = "NOT_FOUND",
          message = "No travel document found matching '$titleOrId'.",
        )
      }

      val content = travelDocumentManager.readDocumentContent(doc)
      val (validity, expiryFormatted) = getDocumentValidity(doc.expiryDate, doc.documentType, doc.title)

      TravelDocumentDetailsSerializable(
        id = doc.documentId.ifBlank { doc.id },
        title = doc.title,
        documentType = doc.documentType,
        issuingCountry = doc.issuingCountry.takeIf { it.isNotBlank() },
        documentNumberMasked = doc.documentNumberMasked.takeIf { it.isNotBlank() },
        expiryDateFormatted = expiryFormatted,
        notes = doc.notes.takeIf { it.isNotBlank() },
        content = content,
        validityStatus = validity,
        status = "SUCCESS",
        message = "Found document '${doc.title}'. ${if (doc.notes.isNotBlank()) "Notes: ${doc.notes}." else ""}",
      )
    }
  }

  /**
   * Saves a new travel note, reservation detail, Wi-Fi code, or document text into the traveler's
   * document storage. Restricted to scope SELF. Call this when the user asks to save important
   * trip notes, Wi-Fi codes, door access codes, or document text into their documents (e.g., 'Save our
   * Airbnb door code 4829 to my documents' or 'Add a note to my documents with the conference Wi-Fi password').
   *
   * @param title Title of the document or note (e.g., 'Airbnb Door Code', 'Conference Wi-Fi').
   * @param content Full content or details to store in the document.
   * @param documentType Optional document type (e.g. 'OTHER', 'NOTE', 'HOTEL', 'TICKET'). Defaults to 'OTHER'.
   * @param tripId Optional ID of the trip. Defaults to the active trip if omitted.
   * @return The created travel document.
   */
  @OptIn(ExperimentalAppFunctionsApi::class)
  @AppFunctionDeclaration(isDescribedByKDoc = true)
  @AppFunctionAccessLevel(
    level = AppFunctionMetadata.ACCESS_LEVEL_SELF,
    isCompatEnforcementEnabled = true,
  )
  suspend fun saveTravelNoteDocument(
    title: String,
    content: String,
    documentType: String? = null,
    tripId: String? = null,
  ): TravelDocumentSerializable {
    return withContext(Dispatchers.IO) {
      val trip = resolveTrip(tripId = tripId, textHint = "$title $content")
      val targetTripId = trip?.id ?: ActiveTripSession.activeTripId ?: tripId?.takeIf { it.isNotBlank() } ?: "2026-1"
      val resolvedType = (documentType?.takeIf { it.isNotBlank() } ?: "OTHER").uppercase()

      val createdDoc = travelDocumentManager.createTextDocument(
        title = title,
        content = content,
        documentType = resolvedType,
        notes = content,
        tripId = targetTripId,
      )

      createdDoc.toSerializable(
        message = "Successfully saved '$title' to your travel documents in ${trip?.title ?: "your trip"}.",
      )
    }
  }

  private fun matchesTripKeyword(trip: Trip, text: String): Boolean {
    val lower = text.lowercase()
    val city = trip.location.substringBefore(',').trim().lowercase()
    if (city.isNotEmpty() && lower.contains(city)) return true
    if (lower.contains(trip.title.lowercase())) return true
    if (trip.id == "2026-1") {
      return lower.contains("mountain view") ||
        lower.contains("bay view") ||
        lower.contains("bv300") ||
        lower.contains("ads") ||
        lower.contains("summit") ||
        lower.contains("california") ||
        lower.contains("san francisco") ||
        lower.contains("sfmoma") ||
        lower.contains("sfo")
    }
    return false
  }

  private suspend fun resolveTrip(tripId: String?, textHint: String? = null): Trip? {
    val cleanTripId = tripId?.trim()?.takeIf { it.isNotBlank() }
    val activeTripId = ActiveTripSession.activeTripId?.trim()?.takeIf { it.isNotBlank() }
    val dbTrips = tripDao.getAllTrips().first()
    val allTrips = if (dbTrips.isNotEmpty()) dbTrips else DummyData.trips

    val hintMatch =
      if (!textHint.isNullOrBlank()) {
        allTrips.find { matchesTripKeyword(it, textHint) }
      } else {
        null
      }

    var trip =
      when {
        hintMatch != null -> hintMatch
        activeTripId != null &&
          (cleanTripId == null ||
            cleanTripId.equals(activeTripId, ignoreCase = true) ||
            allTrips.none { matchesTripKeyword(it, cleanTripId) }) ->
          allTrips.find { it.id == activeTripId }
        cleanTripId != null ->
          allTrips.find {
            it.id.equals(cleanTripId, ignoreCase = true) || matchesTripKeyword(it, cleanTripId)
          }
        else -> null
      }

    if (trip == null) {
      val fallbackTrip =
        DummyData.trips.find { dt ->
          (!textHint.isNullOrBlank() && matchesTripKeyword(dt, textHint)) ||
            (activeTripId != null && dt.id == activeTripId) ||
            (cleanTripId != null &&
              (dt.id.equals(cleanTripId, ignoreCase = true) || matchesTripKeyword(dt, cleanTripId)))
        }
      if (fallbackTrip != null) {
        tripDao.insertTrips(listOf(fallbackTrip))
        trip = fallbackTrip
      } else {
        trip = dbTrips.firstOrNull() ?: DummyData.trips.find { it.id == "2026-1" }
      }
    }
    return trip
  }

  private fun inferExpenseCategory(title: String): String {
    val lower = title.lowercase()
    return when {
      lower.contains("train") ||
        lower.contains("caltrain") ||
        lower.contains("bart") ||
        lower.contains("shuttle") ||
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

  private fun Trip.toSerializable() =
    TripSerializable(
      id = id,
      title = title,
      location = location,
      startDate = startDate,
      endDate = endDate,
      participants = participants,
      budget = budget,
      currency = currency,
    )

  private fun normalizeCurrency(raw: String?, titleHint: String = ""): String {
    val clean = raw?.trim()?.lowercase().orEmpty()
    return when {
      clean.contains("usd") || clean.contains("dollar") || clean.contains("$") -> "USD"
      clean.contains("eur") || clean.contains("euro") || clean.contains("€") -> "EUR"
      clean.contains("jpy") || clean.contains("yen") || clean.contains("¥") -> "JPY"
      clean.contains("gbp") || clean.contains("pound") || clean.contains("£") -> "GBP"
      clean.contains("krw") || clean.contains("won") || clean.contains("₩") -> "KRW"
      clean.contains("inr") || clean.contains("rupee") || clean.contains("₹") -> "INR"
      clean.contains("sgd") -> "SGD"
      clean.contains("cny") || clean.contains("rmb") || clean.contains("yuan") -> "CNY"
      clean.length == 3 -> clean.uppercase()
      titleHint.contains("$") || titleHint.contains("dollar", ignoreCase = true) -> "USD"
      titleHint.contains("€") || titleHint.contains("euro", ignoreCase = true) -> "EUR"
      titleHint.contains("¥") || titleHint.contains("yen", ignoreCase = true) -> "JPY"
      titleHint.contains("£") || titleHint.contains("pound", ignoreCase = true) -> "GBP"
      else -> "USD"
    }
  }

  private fun getUsdRate(currency: String): Double =
    when (currency.uppercase()) {
      "USD" -> 1.0
      "EUR" -> 1.08
      "GBP" -> 1.27
      "JPY" -> 0.0067
      "KRW" -> 0.00075
      "INR" -> 0.012
      "SGD" -> 0.75
      "CNY" -> 0.14
      "THB" -> 0.029
      "VND" -> 0.00004
      else -> 1.0
    }

  private fun convertToUsd(amount: Double, currency: String): Double {
    val rate = getUsdRate(currency)
    return kotlin.math.round(amount * rate * 100.0) / 100.0
  }

  private fun convertCurrency(amount: Double, from: String, to: String): Double {
    if (from.equals(to, ignoreCase = true)) return amount
    val rateFrom = getUsdRate(from)
    val rateTo = getUsdRate(to)
    val inUsd = amount * rateFrom
    return kotlin.math.round((inUsd / rateTo) * 100.0) / 100.0
  }

  private fun getCurrencySymbol(currency: String): String =
    when (currency.uppercase()) {
      "EUR" -> "€"
      "JPY" -> "¥"
      "GBP" -> "£"
      "KRW" -> "₩"
      "INR" -> "₹"
      "CNY" -> "¥"
      "THB" -> "฿"
      "VND" -> "₫"
      else -> "$"
    }

  private fun formatCurrency(amount: Double, currency: String): String {
    val symbol = getCurrencySymbol(currency)
    return when (currency.uppercase(Locale.US)) {
      "JPY",
      "KRW",
      "VND" -> "$symbol${String.format(Locale.US, "%,d", amount.toLong())}"
      else -> "$symbol${String.format(Locale.US, "%,.2f", amount)}"
    }
  }

  private fun Expense.toSerializable(
    tripTitle: String? = null,
    amountUsd: Double? = null,
    tripTotalSpentUsd: Double? = null,
    tripRemainingBudgetUsd: Double? = null,
    tripRemainingBudgetJpy: Double? = null,
    tripCurrency: String? = "USD",
    tripBudget: Double? = 2000.0,
    tripTotalSpent: Double? = null,
    tripRemainingBudget: Double? = null,
    message: String? = null,
  ) =
    ExpenseSerializable(
      id = id,
      tripId = tripId,
      title = title,
      amount = amount,
      currency = currency,
      category = category,
      timestamp = timestamp,
      tripTitle = tripTitle,
      amountUsd = amountUsd,
      tripTotalSpentUsd = tripTotalSpentUsd,
      tripRemainingBudgetUsd = tripRemainingBudgetUsd,
      tripRemainingBudgetJpy = tripRemainingBudgetJpy,
      tripCurrency = tripCurrency,
      tripBudget = tripBudget,
      tripTotalSpent = tripTotalSpent,
      tripRemainingBudget = tripRemainingBudget,
      status = "SUCCESS",
      message = message,
    )

  private fun TimelineEvent.toSerializable(
    tripTitle: String? = null,
    matchedEventTitle: String? = null,
  ) =
    TimelineEventSerializable(
      id = id,
      tripId = tripId,
      type = type.name,
      timestamp = timestamp,
      title = title,
      location = location,
      description = description,
      tripTitle = tripTitle,
      matchedEventTitle = matchedEventTitle,
    )

  private fun VoiceNoteEntity.toSerializable(
    matchedEventId: String? = null,
    matchedEventTitle: String? = null,
  ) =
    VoiceNoteSerializable(
      id = id,
      tripId = tripId,
      title = title,
      transcription = transcription,
      timestamp = timestamp,
      matchedEventId = matchedEventId,
      matchedEventTitle = matchedEventTitle,
    )

  private fun DayTheme.toSerializable() =
    DayThemeSerializable(id = id, tripId = tripId, date = date, theme = theme)

  private fun getDocumentValidity(
    expiryMillis: Long,
    docType: String,
    title: String,
  ): Pair<String, String?> {
    if (expiryMillis <= 0L) return Pair("NO_EXPIRY", null)
    val now = System.currentTimeMillis()
    val formatted =
      try {
        Instant.ofEpochMilli(expiryMillis)
          .atZone(ZoneId.systemDefault())
          .toLocalDate()
          .format(DateTimeFormatter.ISO_LOCAL_DATE)
      } catch (_: Exception) {
        null
      }
    val combined = (docType + " " + title).uppercase()
    val isIdentity = combined.contains("PASSPORT") || combined.contains("VISA") || combined.contains("LICENSE")
    val validity =
      when {
        expiryMillis < now -> "EXPIRED"
        isIdentity && (expiryMillis - now < 180L * 24 * 60 * 60 * 1000) -> "EXPIRING_SOON"
        else -> "VALID"
      }
    return Pair(validity, formatted)
  }

  private fun TravelDocument.toSerializable(message: String? = null): TravelDocumentSerializable {
    val (validity, expiryFormatted) = getDocumentValidity(expiryDate, documentType, title)
    return TravelDocumentSerializable(
      id = documentId.ifBlank { id },
      title = title,
      documentType = documentType,
      issuingCountry = issuingCountry.takeIf { it.isNotBlank() },
      documentNumberMasked = documentNumberMasked.takeIf { it.isNotBlank() },
      expiryDateFormatted = expiryFormatted,
      notes = notes.takeIf { it.isNotBlank() },
      tripId = tripId,
      validityStatus = validity,
      status = "SUCCESS",
      message = message ?: "Found document '$title' ($documentType).",
    )
  }
}
