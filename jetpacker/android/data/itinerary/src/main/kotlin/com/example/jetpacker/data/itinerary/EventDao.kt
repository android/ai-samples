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

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class TripSessionIdentifier(@JvmField val tripId: String, @JvmField val sessionId: String?)

/**
 * Data Access Object (DAO) for managing timeline events and their detailed subtypes (flights, hotels,
 * dining, museum entries, and voice notes) stored in the Room database.
 */
@Dao
interface EventDao {
  @Query("SELECT DISTINCT tripId, sessionId FROM timeline_events WHERE sessionId IS NOT NULL")
  fun getAllSessionIds(): Flow<List<TripSessionIdentifier>>

  @Query("SELECT * FROM timeline_events WHERE tripId = :tripId ORDER BY timestamp ASC")
  fun getEventsForTrip(tripId: String): Flow<List<TimelineEvent>>

  @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertEvent(event: TimelineEvent)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertEvents(events: List<TimelineEvent>)

  @Query("DELETE FROM timeline_events WHERE tripId = :tripId")
  suspend fun deleteEventsForTrip(tripId: String)

  @Delete suspend fun deleteEvent(event: TimelineEvent)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertFlightDetail(detail: FlightDetail)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertHotelDetail(detail: HotelDetail)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertDiningDetail(detail: DiningDetail)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertActivityDetail(detail: ActivityDetail)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMuseumDetail(detail: MuseumDetail)

  @Query("SELECT * FROM flight_details WHERE eventId = :eventId")
  fun getFlightDetail(eventId: String): Flow<FlightDetail?>

  @Query("SELECT * FROM hotel_details WHERE eventId = :eventId")
  fun getHotelDetail(eventId: String): Flow<HotelDetail?>

  @Query("SELECT * FROM dining_details WHERE eventId = :eventId")
  fun getDiningDetail(eventId: String): Flow<DiningDetail?>

  @Query("SELECT * FROM museum_details WHERE eventId = :eventId")
  fun getMuseumDetail(eventId: String): Flow<MuseumDetail?>

  @Query("SELECT * FROM timeline_events WHERE id = :eventId")
  fun getEventById(eventId: String): Flow<TimelineEvent?>

  @Query("DELETE FROM timeline_events") suspend fun deleteAllEvents()

  @Query("DELETE FROM flight_details") suspend fun deleteAllFlightDetails()

  @Query("DELETE FROM hotel_details") suspend fun deleteAllHotelDetails()

  @Query("DELETE FROM dining_details") suspend fun deleteAllDiningDetails()

  @Query("DELETE FROM activity_details") suspend fun deleteAllActivityDetails()

  @Query("SELECT * FROM voice_notes WHERE tripId = :tripId ORDER BY timestamp DESC")
  fun getVoiceNotesForTrip(tripId: String): Flow<List<VoiceNoteEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertVoiceNote(note: VoiceNoteEntity)

  @Query("DELETE FROM voice_notes WHERE id = :id") suspend fun deleteVoiceNoteById(id: String)
}
