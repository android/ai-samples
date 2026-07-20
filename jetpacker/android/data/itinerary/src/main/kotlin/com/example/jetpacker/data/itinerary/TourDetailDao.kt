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
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for managing custom tour details associated with timeline events.
 */
@Dao
interface TourDetailDao {
  @Query("SELECT * FROM tour_details WHERE eventId = :eventId")
  fun getTourDetailByEventId(eventId: String): Flow<TourDetail?>

  @Query("SELECT * FROM tour_details WHERE id = :id")
  fun getTourDetailById(id: String): Flow<TourDetail?>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTourDetail(tourDetail: TourDetail)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTourDetails(tourDetails: List<TourDetail>)

  @Query("DELETE FROM tour_details") suspend fun deleteAllTourDetails()
}
