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

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.jetpacker.data.itinerary.Converters

@Entity(tableName = "trips")
@TypeConverters(Converters::class)
data class Trip(
  @PrimaryKey @JvmField val id: String,
  @JvmField val title: String,
  @JvmField val location: String,
  @JvmField val startDate: Long,
  @JvmField val endDate: Long,
  @JvmField val imageRes: Int? = null,
  @JvmField val imageUri: String? = null,
  @JvmField val participants: List<String> = emptyList(),
  @JvmField val vibeCheckSummary: String? = null,
)
