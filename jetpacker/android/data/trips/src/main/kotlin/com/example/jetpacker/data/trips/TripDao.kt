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

package com.example.jetpacker.data.trips

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for creating, reading, updating, and deleting trip items.
 */
@Dao
interface TripDao {
  @Query("SELECT * FROM trips") fun getAllTrips(): Flow<List<Trip>>

  @Query("SELECT * FROM trips WHERE id = :tripId") fun getTripById(tripId: String): Flow<Trip?>

  @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertTrip(trip: Trip)

  @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertTrips(trips: List<Trip>)

  @Delete suspend fun deleteTrip(trip: Trip)

  @Query("DELETE FROM trips") suspend fun deleteAllTrips()
}
