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

import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

import com.example.jetpacker.data.itinerary.DayTheme
import com.example.jetpacker.data.itinerary.DayThemeDao
import com.example.jetpacker.data.itinerary.EventDao
import com.example.jetpacker.data.itinerary.EventType
import com.example.jetpacker.data.itinerary.Expense
import com.example.jetpacker.data.itinerary.ExpenseDao
import com.example.jetpacker.data.itinerary.TimelineEvent
import com.example.jetpacker.data.itinerary.VoiceNoteEntity
import com.example.jetpacker.data.trips.Trip
import com.example.jetpacker.data.trips.TripDao
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

@RequiresApi(36)
@AndroidEntryPoint
@AppFunctionServiceEntryPoint(
    serviceName = "JetPackerAppFunctionService",
    appFunctionXmlFileName = "jetpacker_app_function_service"
)
abstract class BaseJetPackerAppFunctionService : AppFunctionService() {
    @Inject internal lateinit var tripDao: TripDao
    @Inject internal lateinit var expenseDao: ExpenseDao
    @Inject internal lateinit var eventDao: EventDao
    @Inject internal lateinit var dayThemeDao: DayThemeDao

    /**
     * Looks for trips based on optional filters like id, title (name), location, and dates.
     *
     * @param id The unique identifier of the trip.
     * @param title The title or name of the trip.
     * @param location The destination location.
     * @param startDate The minimum start date in milliseconds.
     * @param endDate The maximum end date in milliseconds.
     * @return A list of trips matching the filters.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun searchTrip(
        id: String? = null,
        title: String? = null,
        location: String? = null,
        startDate: Long? = null,
        endDate: Long? = null
    ): List<TripSerializable> {
        return withContext(Dispatchers.IO) {
            tripDao.getAllTrips().first()
                .filter { trip ->
                    (id.isNullOrEmpty() || trip.id == id) &&
                    (title.isNullOrEmpty() || trip.title.contains(title, ignoreCase = true)) &&
                    (location.isNullOrEmpty() || trip.location.contains(location, ignoreCase = true)) &&
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
    @AppFunction(isDescribedByKDoc = true)
    suspend fun createTrip(
        title: String,
        location: String,
        startDate: Long,
        endDate: Long
    ): TripSerializable {
        val trip = Trip(
            id = UUID.randomUUID().toString(),
            title = title,
            location = location,
            startDate = startDate,
            endDate = endDate
        )
        return withContext(Dispatchers.IO) {
            tripDao.insertTrip(trip)
            trip.toSerializable()
        }
    }

    /**
     * Adds an expense to a trip.
     *
     * @param tripId The ID of the trip to add the expense to.
     * @param title The title of the expense.
     * @param amount The amount of the expense.
     * @param currency The currency of the expense.
     * @param category The category of the expense.
     * @return The added expense.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun addExpense(
        tripId: String,
        title: String,
        amount: Double,
        currency: String,
        category: String
    ): ExpenseSerializable {
        val expense = Expense(
            tripId = tripId,
            title = title,
            amount = amount,
            currency = currency,
            category = category
        )
        return withContext(Dispatchers.IO) {
            expenseDao.insertExpense(expense)
            expense.toSerializable()
        }
    }

    /**
     * Lists all expenses for a specific trip.
     *
     * @param tripId The ID of the trip.
     * @return A list of expenses for the trip.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun getExpenses(tripId: String): List<ExpenseSerializable> {
        return withContext(Dispatchers.IO) {
            expenseDao.getExpensesForTrip(tripId).first().map { it.toSerializable() }
        }
    }

    /**
     * Retrieves the itinerary events for a specific trip.
     *
     * @param tripId The ID of the trip.
     * @return A list of events in the itinerary.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun getItinerary(tripId: String): List<TimelineEventSerializable> {
        return withContext(Dispatchers.IO) {
            eventDao.getEventsForTrip(tripId).first().map { it.toSerializable() }
        }
    }

    /**
     * Adds a new event to a trip's itinerary.
     *
     * @param tripId The ID of the trip.
     * @param title The title of the event.
     * @param type The type of event (e.g., ACTIVITY, FOOD_AND_DRINK).
     * @param timestamp The time of the event.
     * @param location The location where the event takes place.
     * @param description An optional description of the event.
     * @return The added event.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun addItineraryEvent(
        tripId: String,
        title: String,
        type: String,
        timestamp: Long,
        location: String,
        description: String?
    ): TimelineEventSerializable {
        val eventType = try {
            EventType.valueOf(type.uppercase())
        } catch (_: IllegalArgumentException) {
            EventType.ACTIVITY
        }
        val event = TimelineEvent(
            id = UUID.randomUUID().toString(),
            tripId = tripId,
            type = eventType,
            timestamp = timestamp,
            title = title,
            location = location,
            description = description
        )
        return withContext(Dispatchers.IO) {
            eventDao.insertEvent(event)
            event.toSerializable()
        }
    }

    /**
     * Retrieves voice notes associated with a trip.
     *
     * @param tripId The ID of the trip.
     * @return A list of voice notes.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun getVoiceNotes(tripId: String): List<VoiceNoteSerializable> {
        return withContext(Dispatchers.IO) {
            eventDao.getVoiceNotesForTrip(tripId).first().map { it.toSerializable() }
        }
    }

    /**
     * Adds a voice note transcription to a trip.
     *
     * @param tripId The ID of the trip.
     * @param title The title of the voice note.
     * @param transcription The text content of the voice note.
     * @return The added voice note.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun addVoiceNote(
        tripId: String,
        title: String,
        transcription: String
    ): VoiceNoteSerializable {
        val note = VoiceNoteEntity(
            id = UUID.randomUUID().toString(),
            tripId = tripId,
            title = title,
            transcription = transcription,
            timestamp = System.currentTimeMillis(),
            matchingEventsJson = "[]"
        )
        return withContext(Dispatchers.IO) {
            eventDao.insertVoiceNote(note)
            note.toSerializable()
        }
    }

    /**
     * Retrieves day themes for a specific trip.
     *
     * @param tripId The ID of the trip.
     * @return A list of day themes.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun getDayThemes(tripId: String): List<DayThemeSerializable> {
        return withContext(Dispatchers.IO) {
            dayThemeDao.getThemesForTrip(tripId).first().map { it.toSerializable() }
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
    @AppFunction(isDescribedByKDoc = true)
    suspend fun setDayTheme(
        tripId: String,
        date: String,
        theme: String
    ): DayThemeSerializable {
        val dayTheme = DayTheme(
            id = UUID.randomUUID().toString(),
            tripId = tripId,
            date = date,
            theme = theme
        )
        return withContext(Dispatchers.IO) {
            dayThemeDao.insertTheme(dayTheme)
            dayTheme.toSerializable()
        }
    }

    private fun Trip.toSerializable() = TripSerializable(
        id = id,
        title = title,
        location = location,
        startDate = startDate,
        endDate = endDate,
        participants = participants
    )

    private fun Expense.toSerializable() = ExpenseSerializable(
        id = id,
        tripId = tripId,
        title = title,
        amount = amount,
        currency = currency,
        category = category,
        timestamp = timestamp
    )

    private fun TimelineEvent.toSerializable() = TimelineEventSerializable(
        id = id,
        tripId = tripId,
        type = type.name,
        timestamp = timestamp,
        title = title,
        location = location,
        description = description
    )

    private fun VoiceNoteEntity.toSerializable() = VoiceNoteSerializable(
        id = id,
        tripId = tripId,
        title = title,
        transcription = transcription,
        timestamp = timestamp
    )

    private fun DayTheme.toSerializable() = DayThemeSerializable(
        id = id,
        tripId = tripId,
        date = date,
        theme = theme
    )
}
