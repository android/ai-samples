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

package com.example.jetpacker.feature.assistant

import com.example.jetpacker.data.itinerary.ActivityDetail
import com.example.jetpacker.data.itinerary.DiningDetail
import com.example.jetpacker.data.itinerary.EventDao
import com.example.jetpacker.data.itinerary.EventType
import com.example.jetpacker.data.itinerary.Expense
import com.example.jetpacker.data.itinerary.ExpenseDao
import com.example.jetpacker.data.itinerary.FlightDetail
import com.example.jetpacker.data.itinerary.HotelDetail
import com.example.jetpacker.data.itinerary.MuseumDetail
import com.example.jetpacker.data.itinerary.TimelineEvent
import com.example.jetpacker.data.itinerary.TripSessionIdentifier
import com.example.jetpacker.data.itinerary.VoiceNoteEntity
import com.example.jetpacker.data.trips.Trip
import com.example.jetpacker.data.trips.TripDao
import com.example.jetpacker.feature.assistant.tools.AddExpenseTool
import com.example.jetpacker.feature.assistant.tools.AddItineraryEventTool
import com.example.jetpacker.feature.assistant.tools.AddVoiceNoteTool
import com.example.jetpacker.feature.assistant.tools.GetEventDetailsTool
import com.example.jetpacker.feature.assistant.tools.GetFlightItineraryTool
import com.example.jetpacker.feature.assistant.tools.SearchTripTool
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class TravelDaoToolsTest {

  private lateinit var fakeEventDao: FakeEventDao
  private lateinit var fakeTripDao: FakeTripDao
  private lateinit var fakeExpenseDao: FakeExpenseDao

  private lateinit var flightTool: GetFlightItineraryTool
  private lateinit var expenseTool: AddExpenseTool
  private lateinit var eventTool: AddItineraryEventTool
  private lateinit var eventDetailsTool: GetEventDetailsTool
  private lateinit var voiceNoteTool: AddVoiceNoteTool
  private lateinit var searchTripTool: SearchTripTool

  @Before
  fun setUp() {
    fakeEventDao = FakeEventDao()
    fakeTripDao = FakeTripDao()
    fakeExpenseDao = FakeExpenseDao()

    flightTool = GetFlightItineraryTool(fakeEventDao, fakeTripDao)
    expenseTool = AddExpenseTool(fakeExpenseDao, fakeTripDao)
    eventTool = AddItineraryEventTool(fakeEventDao, fakeTripDao)
    eventDetailsTool = GetEventDetailsTool(fakeEventDao)
    voiceNoteTool = AddVoiceNoteTool(fakeEventDao, fakeTripDao)
    searchTripTool = SearchTripTool(fakeTripDao)
  }

  @Test
  fun getFlightItinerary_returnsMatchingFlight() = runTest {
    val trip =
      Trip(
        id = "trip-1",
        title = "Tokyo Getaway",
        location = "Tokyo",
        startDate = 1000L,
        endDate = 2000L,
      )
    fakeTripDao.trips.add(trip)

    val event =
      TimelineEvent(
        id = "ev-1",
        tripId = "trip-1",
        type = EventType.TRANSPORTATION,
        title = "Flight to Tokyo",
        timestamp = 1200L,
        location = "Tokyo",
      )
    fakeEventDao.events["trip-1"] = listOf(event)

    val flight =
      FlightDetail(
        eventId = "ev-1",
        airline = "Japan Airlines",
        flightNum = "JL005",
        origin = "JFK",
        destination = "HND",
        departureTerminal = "1",
        gate = "A4",
      )
    fakeEventDao.flights["ev-1"] = flight

    val result = flightTool.execute(mapOf("tripId" to "trip-1", "flightNumber" to "JL005"))

    assertThat(result["count"]).isEqualTo(1)
    val flights = result["flights"] as List<*>
    val first = flights.first() as Map<*, *>
    assertThat(first["flightNumber"]).isEqualTo("JL005")
    assertThat(first["airline"]).isEqualTo("Japan Airlines")
    assertThat(first["gate"]).isEqualTo("A4")
  }

  @Test
  fun addExpenseTool_insertsExpenseAndDefaultsTrip() = runTest {
    val trip =
      Trip(
        id = "2026-4",
        title = "Tokyo Explorer",
        location = "Tokyo, Japan",
        startDate = 1000L,
        endDate = 2000L,
      )
    fakeTripDao.trips.add(trip)

    val result =
      expenseTool.execute(
        mapOf(
          "title" to "Airport express train",
          "amount" to 45.0,
          "currency" to "EUR",
          "category" to "Transport",
        )
      )

    assertThat(result["status"]).isEqualTo("SUCCESS")
    assertThat(result["tripId"]).isEqualTo("2026-4")
    assertThat(fakeExpenseDao.expenses).hasSize(1)
    assertThat(fakeExpenseDao.expenses.first().title).isEqualTo("Airport express train")
    assertThat(fakeExpenseDao.expenses.first().amount).isEqualTo(45.0)
  }

  @Test
  fun addItineraryEventTool_insertsReminderEvent() = runTest {
    val trip =
      Trip(
        id = "2026-4",
        title = "Tokyo Explorer",
        location = "Tokyo, Japan",
        startDate = 1000L,
        endDate = 2000L,
      )
    fakeTripDao.trips.add(trip)

    val result =
      eventTool.execute(
        mapOf("tripId" to "2026-4", "title" to "Pick up pocket wifi", "type" to "ACTIVITY")
      )

    assertThat(result["status"]).isEqualTo("SUCCESS")
    assertThat(result["tripId"]).isEqualTo("2026-4")
    val events = fakeEventDao.events["2026-4"]
    assertThat(events).isNotNull()
    assertThat(events).hasSize(1)
    assertThat(events!!.first().title).isEqualTo("Pick up pocket wifi")
    assertThat(events.first().location).isEqualTo("Tokyo, Japan")
  }

  @Test
  fun addExpenseTool_deduplicatesIdenticalExpenseExecutedWithinWindow() = runTest {
    val trip =
      Trip(
        id = "2026-4",
        title = "Tokyo Explorer",
        location = "Tokyo, Japan",
        startDate = 1000L,
        endDate = 2000L,
      )
    fakeTripDao.trips.add(trip)

    val args =
      mapOf(
        "title" to "Airport express train",
        "amount" to 45.0,
        "currency" to "EUR",
        "category" to "Transport",
      )
    val result1 = expenseTool.execute(args)
    val result2 = expenseTool.execute(args)

    assertThat(result1["status"]).isEqualTo("SUCCESS")
    assertThat(result2["status"]).isEqualTo("SUCCESS")
    assertThat(fakeExpenseDao.expenses).hasSize(1)
  }

  @Test
  fun addItineraryEventTool_deduplicatesIdenticalReminderExecutedWithinWindow() = runTest {
    val trip =
      Trip(
        id = "2026-4",
        title = "Tokyo Explorer",
        location = "Tokyo, Japan",
        startDate = 1000L,
        endDate = 2000L,
      )
    fakeTripDao.trips.add(trip)

    val args = mapOf("tripId" to "2026-4", "title" to "Pick up pocket wifi", "type" to "ACTIVITY")
    val result1 = eventTool.execute(args)
    val result2 = eventTool.execute(args)

    assertThat(result1["status"]).isEqualTo("SUCCESS")
    assertThat(result2["status"]).isEqualTo("SUCCESS")
    val events = fakeEventDao.events["2026-4"]
    assertThat(events).isNotNull()
    assertThat(events).hasSize(1)
  }

  @Test
  fun getEventDetailsTool_returnsTokyoMuseumAdmissionAndTips() = runTest {
    // Even if DAO is empty, GetEventDetailsTool falls back to DummyData for tok_3
    val result = eventDetailsTool.execute(mapOf("eventId" to "tok_3"))

    assertThat(result["status"]).isEqualTo("SUCCESS")
    assertThat(result["eventId"]).isEqualTo("tok_3")
    assertThat(result["title"]).isEqualTo("teamLab Planets Museum")
    assertThat(result["admissionPrice"]).isEqualTo("€28")
    val tips = result["tips"] as List<*>
    assertThat(tips).contains("Wear clothes that can be rolled up to the knees")
  }

  @Test
  fun addVoiceNoteTool_matchesAirportFlightEventAndInsertsVoiceNote() = runTest {
    val trip =
      Trip(
        id = "2026-4",
        title = "Tokyo",
        location = "Tokyo, Japan",
        startDate = 1000L,
        endDate = 2000L,
      )
    fakeTripDao.trips.add(trip)

    val flightEvent =
      TimelineEvent(
        id = "tok_1",
        tripId = "2026-4",
        type = EventType.TRANSPORTATION,
        title = "Flight to Tokyo (HND)",
        timestamp = 1200L,
        location = "Haneda Airport Terminal 3",
      )
    fakeEventDao.events["2026-4"] = listOf(flightEvent)

    val result =
      voiceNoteTool.execute(
        mapOf("tripId" to "2026-4", "title" to "Buy a SIM card when I get to the airport")
      )

    assertThat(result["status"]).isEqualTo("SUCCESS")
    assertThat(result["matchedEventId"]).isEqualTo("tok_1")
    assertThat(result["matchedEventTitle"]).isEqualTo("Flight to Tokyo (HND)")
    assertThat(fakeEventDao.voiceNotes).hasSize(1)
    assertThat(fakeEventDao.voiceNotes.first().matchingEventsJson).contains("tok_1")
  }

  @Test
  fun searchTripTool_findsMatchingTripByQuery() = runTest {
    fakeTripDao.trips.add(
      Trip(
        id = "2026-4",
        title = "Tokyo",
        location = "Tokyo, Japan",
        startDate = 1000L,
        endDate = 2000L,
      )
    )

    val result = searchTripTool.execute(mapOf("query" to "Tokyo"))
    assertThat(result["count"]).isEqualTo(1)
    val trips = result["trips"] as List<*>
    val first = trips.first() as Map<*, *>
    assertThat(first["id"]).isEqualTo("2026-4")
  }
}

private class FakeTripDao : TripDao {
  val trips = mutableListOf<Trip>()

  override fun getAllTrips(): Flow<List<Trip>> = flowOf(trips.toList())

  override fun getTripById(tripId: String): Flow<Trip?> = flowOf(trips.find { it.id == tripId })

  override suspend fun insertTrip(trip: Trip) {
    trips.add(trip)
  }

  override suspend fun insertTrips(trips: List<Trip>) {
    this.trips.addAll(trips)
  }

  override suspend fun deleteTrip(trip: Trip) {
    trips.remove(trip)
  }

  override suspend fun deleteAllTrips() {
    trips.clear()
  }
}

private class FakeEventDao : EventDao {
  val events = mutableMapOf<String, List<TimelineEvent>>()
  val flights = mutableMapOf<String, FlightDetail>()
  val hotels = mutableMapOf<String, HotelDetail>()
  val museums = mutableMapOf<String, MuseumDetail>()
  val activities = mutableMapOf<String, ActivityDetail>()
  val voiceNotes = mutableListOf<VoiceNoteEntity>()

  override fun getAllSessionIds(): Flow<List<TripSessionIdentifier>> = flowOf(emptyList())

  override fun getEventsForTrip(tripId: String): Flow<List<TimelineEvent>> =
    flowOf(events[tripId] ?: emptyList())

  override suspend fun insertEvent(event: TimelineEvent) {
    val current = (events[event.tripId] ?: emptyList()).filterNot { it.id == event.id }
    events[event.tripId] = current + event
  }

  override suspend fun insertEvents(events: List<TimelineEvent>) {
    events.forEach { insertEvent(it) }
  }

  override suspend fun deleteEventsForTrip(tripId: String) {
    events.remove(tripId)
  }

  override suspend fun deleteEvent(event: TimelineEvent) {
    events[event.tripId] = (events[event.tripId] ?: emptyList()) - event
  }

  override suspend fun insertFlightDetail(detail: FlightDetail) {
    flights[detail.eventId] = detail
  }

  override suspend fun insertHotelDetail(detail: HotelDetail) {
    hotels[detail.eventId] = detail
  }

  override suspend fun insertDiningDetail(detail: DiningDetail) {}

  override suspend fun insertActivityDetail(detail: ActivityDetail) {
    activities[detail.eventId] = detail
  }

  override suspend fun insertMuseumDetail(detail: MuseumDetail) {
    museums[detail.eventId] = detail
  }

  override fun getFlightDetail(eventId: String): Flow<FlightDetail?> = flowOf(flights[eventId])

  override fun getHotelDetail(eventId: String): Flow<HotelDetail?> = flowOf(hotels[eventId])

  override fun getDiningDetail(eventId: String): Flow<DiningDetail?> = flowOf(null)

  override fun getMuseumDetail(eventId: String): Flow<MuseumDetail?> = flowOf(museums[eventId])

  override fun getEventById(eventId: String): Flow<TimelineEvent?> =
    flowOf(events.values.flatten().find { it.id == eventId })

  override suspend fun deleteAllEvents() {
    events.clear()
  }

  override suspend fun deleteAllFlightDetails() {
    flights.clear()
  }

  override suspend fun deleteAllHotelDetails() {
    hotels.clear()
  }

  override suspend fun deleteAllDiningDetails() {}

  override suspend fun deleteAllActivityDetails() {
    activities.clear()
  }

  override fun getVoiceNotesForTrip(tripId: String): Flow<List<VoiceNoteEntity>> =
    flowOf(voiceNotes.filter { it.tripId == tripId })

  override suspend fun insertVoiceNote(note: VoiceNoteEntity) {
    voiceNotes.add(note)
  }

  override suspend fun deleteVoiceNoteById(id: String) {
    voiceNotes.removeAll { it.id == id }
  }
}

private class FakeExpenseDao : ExpenseDao {
  val expenses = mutableListOf<Expense>()

  override fun getAllExpenses(): Flow<List<Expense>> = flowOf(expenses.toList())

  override fun getExpensesForTrip(tripId: String): Flow<List<Expense>> =
    flowOf(expenses.filter { it.tripId == tripId })

  override suspend fun insertExpense(expense: Expense) {
    expenses.add(expense)
  }

  override suspend fun deleteExpense(expense: Expense) {
    expenses.remove(expense)
  }

  override suspend fun deleteAllExpenses() {
    expenses.clear()
  }
}
