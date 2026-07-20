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

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "day_themes")
data class DayTheme(
  @PrimaryKey @JvmField val id: String,
  @JvmField val tripId: String,
  @JvmField val date: String, // YYYY-MM-DD format for easy matching with events grouped by date
  @JvmField val theme: String,
)
