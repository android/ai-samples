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

package com.example.jetpacker.core.flags

import android.content.Context

object FeatureFlags {
  const val KEY_DEMO_LANGUAGE = "demo_language"
  const val KEY_OVERRIDE_CURRENT_TIME_MILLIS = "override_current_time_millis"
  const val EXTRA_OVERRIDE_TIME = "override_time"
  const val KEY_ENABLE_TRIP_SUMMARY_AND_TIPS = "enable_trip_summary_and_tips"
  const val KEY_ENABLE_SURPRISE_ME = "enable_surprise_me"
  const val KEY_ENABLE_ITINERARY_ENRICHMENT = "enable_itinerary_enrichment"
  const val KEY_ENABLE_EXPENSE_MANAGEMENT = "enable_expense_management"
  const val KEY_ENABLE_VOICE_NOTES = "enable_voice_notes"

  private var appContext: Context? = null

  fun initialize(context: Context) {
    appContext = context.applicationContext
  }

  private fun readFlag(keyName: String, defaultValue: Boolean): Boolean {
    val context = appContext ?: return defaultValue
    return context
      .getSharedPreferences("debug_settings", Context.MODE_PRIVATE)
      .getBoolean(keyName, defaultValue)
  }

  private fun readStringFlag(keyName: String, defaultValue: String): String {
    val context = appContext ?: return defaultValue
    return context
      .getSharedPreferences("debug_settings", Context.MODE_PRIVATE)
      .getString(keyName, defaultValue) ?: defaultValue
  }

  // Voice / Demo Language Settings
  val DEFAULT_DEMO_LANGUAGE = "nl"

  val DEMO_LANGUAGE: String
    get() = readStringFlag(KEY_DEMO_LANGUAGE, DEFAULT_DEMO_LANGUAGE)

  // Existing flags
  val ENABLE_TRIP_SUMMARY_AND_TIPS: Boolean
    get() = readFlag(KEY_ENABLE_TRIP_SUMMARY_AND_TIPS, true)

  val ENABLE_SURPRISE_ME: Boolean
    get() = readFlag(KEY_ENABLE_SURPRISE_ME, false)

  // Offline features
  val ENABLE_ITINERARY_ENRICHMENT: Boolean
    get() = readFlag(KEY_ENABLE_ITINERARY_ENRICHMENT, true)

  val ENABLE_EXPENSE_MANAGEMENT: Boolean
    get() = readFlag(KEY_ENABLE_EXPENSE_MANAGEMENT, true)

  val ENABLE_VOICE_NOTES: Boolean
    get() = readFlag(KEY_ENABLE_VOICE_NOTES, true)
  private fun readLongFlag(keyName: String, defaultValue: Long): Long {
    val context = appContext ?: return defaultValue
    return context
      .getSharedPreferences("debug_settings", Context.MODE_PRIVATE)
      .getLong(keyName, defaultValue)
  }

  val OVERRIDE_CURRENT_TIME_MILLIS: Long?
    get() {
      val valMillis = readLongFlag(KEY_OVERRIDE_CURRENT_TIME_MILLIS, 0L)
      return if (valMillis > 0L) valMillis else null
    }

  fun toggleFlag(context: Context, keyName: String, value: Boolean) {
    context
      .getSharedPreferences("debug_settings", Context.MODE_PRIVATE)
      .edit()
      .putBoolean(keyName, value)
      .apply()
  }

  fun putStringFlag(context: Context, keyName: String, value: String) {
    context
      .getSharedPreferences("debug_settings", Context.MODE_PRIVATE)
      .edit()
      .putString(keyName, value)
      .apply()
  }

  fun putLongFlag(context: Context, keyName: String, value: Long) {
    context
      .getSharedPreferences("debug_settings", Context.MODE_PRIVATE)
      .edit()
      .putLong(keyName, value)
      .apply()
  }
}
