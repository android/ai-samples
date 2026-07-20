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

package com.example.jetpacker.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.jetpacker.data.itinerary.ActivityDetail
import com.example.jetpacker.data.itinerary.DayTheme
import com.example.jetpacker.data.itinerary.DayThemeDao
import com.example.jetpacker.data.itinerary.DiningDetail
import com.example.jetpacker.data.itinerary.EventDao
import com.example.jetpacker.data.itinerary.Expense
import com.example.jetpacker.data.itinerary.ExpenseDao
import com.example.jetpacker.data.itinerary.FlightDetail
import com.example.jetpacker.data.itinerary.HotelDetail
import com.example.jetpacker.data.itinerary.MuseumDetail
import com.example.jetpacker.data.itinerary.TimelineEvent
import com.example.jetpacker.data.itinerary.TourDetail
import com.example.jetpacker.data.itinerary.TourDetailDao
import com.example.jetpacker.data.itinerary.VoiceNoteEntity
import com.example.jetpacker.data.trips.Trip
import com.example.jetpacker.data.trips.TripDao

@Database(
  entities =
    [
      Trip::class,
      TimelineEvent::class,
      TourDetail::class,
      DayTheme::class,
      FlightDetail::class,
      HotelDetail::class,
      DiningDetail::class,
      ActivityDetail::class,
      MuseumDetail::class,
      Expense::class,
      VoiceNoteEntity::class,
    ],
  version = 1,
  exportSchema = false,
)
abstract class JetPackerDatabase : RoomDatabase() {
  abstract fun tripDao(): TripDao

  abstract fun eventDao(): EventDao

  abstract fun tourDetailDao(): TourDetailDao

  abstract fun dayThemeDao(): DayThemeDao

  abstract fun expenseDao(): ExpenseDao
}
