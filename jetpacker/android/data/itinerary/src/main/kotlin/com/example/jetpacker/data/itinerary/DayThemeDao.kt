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
 * Data Access Object (DAO) for managing AI-generated or custom day themes associated with trips.
 */
@Dao
interface DayThemeDao {
  @Query("SELECT * FROM day_themes WHERE tripId = :tripId")
  fun getThemesForTrip(tripId: String): Flow<List<DayTheme>>

  @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertTheme(theme: DayTheme)

  @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertThemes(themes: List<DayTheme>)

  @Query("DELETE FROM day_themes WHERE tripId = :tripId")
  suspend fun deleteThemesForTrip(tripId: String)

  @Query("DELETE FROM day_themes") suspend fun deleteAllThemes()
}
