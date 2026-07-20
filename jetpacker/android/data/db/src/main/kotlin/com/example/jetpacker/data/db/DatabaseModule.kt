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

import android.content.Context
import androidx.room.Room
import com.example.jetpacker.data.itinerary.DayThemeDao
import com.example.jetpacker.data.itinerary.EventDao
import com.example.jetpacker.data.itinerary.ExpenseDao
import com.example.jetpacker.data.itinerary.TourDetailDao
import com.example.jetpacker.data.trips.TripDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

  @Provides
  @Singleton
  fun provideDatabase(@ApplicationContext context: Context): JetPackerDatabase {
    return Room.databaseBuilder(context, JetPackerDatabase::class.java, "jetpacker_db")
      .fallbackToDestructiveMigration(dropAllTables = true)
      .build()
  }

  @Provides
  fun provideTripDao(db: JetPackerDatabase): TripDao {
    return db.tripDao()
  }

  @Provides
  fun provideEventDao(db: JetPackerDatabase): EventDao {
    return db.eventDao()
  }

  @Provides
  fun provideTourDetailDao(db: JetPackerDatabase): TourDetailDao {
    return db.tourDetailDao()
  }

  @Provides
  fun provideDayThemeDao(db: JetPackerDatabase): DayThemeDao {
    return db.dayThemeDao()
  }

  @Provides
  fun provideExpenseDao(db: JetPackerDatabase): ExpenseDao {
    return db.expenseDao()
  }
}
