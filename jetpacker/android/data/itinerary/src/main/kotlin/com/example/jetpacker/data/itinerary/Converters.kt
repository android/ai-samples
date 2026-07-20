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

import androidx.room.TypeConverter

class Converters {
  @TypeConverter
  fun fromListInt(value: List<Int>?): String {
    return value?.joinToString(",") ?: ""
  }

  @TypeConverter
  fun toListInt(value: String?): List<Int> {
    if (value.isNullOrEmpty()) return emptyList()
    return value.split(",").mapNotNull { it.toIntOrNull() }
  }

  @TypeConverter
  fun fromListString(value: List<String>?): String {
    return value?.joinToString("||") ?: ""
  }

  @TypeConverter
  fun toListString(value: String?): List<String> {
    if (value.isNullOrEmpty()) return emptyList()
    return value.split("||")
  }
}
