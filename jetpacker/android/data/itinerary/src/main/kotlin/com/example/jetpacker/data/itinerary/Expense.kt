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
import java.util.UUID

@Entity(tableName = "expenses")
data class Expense(
  @PrimaryKey @JvmField val id: String = UUID.randomUUID().toString(),
  @JvmField val title: String,
  @JvmField val amount: Double,
  @JvmField val currency: String,
  @JvmField val category: String,
  @JvmField val tripId: String = "",
  @JvmField val timestamp: Long = System.currentTimeMillis(),
)
