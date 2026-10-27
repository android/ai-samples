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
  const val KEY_DEMO_PRESET = "demo_preset"
  const val PRESET_ADS_003 = "ads_003"
  const val PRESET_DEMO_BOOTH = "demo_booth"
  const val PRESET_CUSTOM = "custom"

  const val KEY_DEMO_LANGUAGE = "demo_language"
  const val KEY_OVERRIDE_CURRENT_TIME_MILLIS = "override_current_time_millis"
  const val EXTRA_OVERRIDE_TIME = "override_time"
  const val KEY_ENABLE_TRIP_SUMMARY_AND_TIPS = "enable_trip_summary_and_tips"
  const val KEY_ENABLE_SURPRISE_ME = "enable_surprise_me"
  const val KEY_ENABLE_ITINERARY_ENRICHMENT = "enable_itinerary_enrichment"
  const val KEY_ENABLE_EXPENSE_MANAGEMENT = "enable_expense_management"
  const val KEY_ENABLE_VOICE_NOTES = "enable_voice_notes"
  const val KEY_ENABLE_REVIEW_GENERATION = "enable_review_generation"
  const val KEY_ENABLE_MUSEUM_ASSISTANT = "enable_museum_assistant"
  const val KEY_ENABLE_URL_GROUNDING = "enable_url_grounding"
  const val KEY_ENABLE_SEARCH_GROUNDING = "enable_search_grounding"
  const val KEY_ENABLE_BOOKING_ASSISTANT = "enable_booking_assistant"
  const val KEY_ENABLE_TRAVEL_ASSISTANT = "enable_travel_assistant"
  const val KEY_ENABLE_DOCUMENTS = "enable_documents"

  private var appContext: Context? = null

  fun initialize(context: Context) {
    appContext = context.applicationContext
    val prefs = appContext?.getSharedPreferences("debug_settings", Context.MODE_PRIVATE)
    if (prefs != null && !prefs.contains(KEY_DEMO_PRESET)) {
      applyPreset(context, PRESET_ADS_003)
    }
  }

  fun applyPreset(context: Context, preset: String) {
    val editor = context.getSharedPreferences("debug_settings", Context.MODE_PRIVATE).edit()
    when (preset) {
      PRESET_ADS_003 -> {
        editor
          .putString(KEY_DEMO_PRESET, PRESET_ADS_003)
          .putBoolean(KEY_ENABLE_TRAVEL_ASSISTANT, true)
          .putBoolean(KEY_ENABLE_EXPENSE_MANAGEMENT, true)
          .putBoolean(KEY_ENABLE_ITINERARY_ENRICHMENT, false)
          .putBoolean(KEY_ENABLE_TRIP_SUMMARY_AND_TIPS, false)
          .putBoolean(KEY_ENABLE_VOICE_NOTES, false)
          .putBoolean(KEY_ENABLE_DOCUMENTS, false)
          .putBoolean(KEY_ENABLE_BOOKING_ASSISTANT, false)
          .putBoolean(KEY_ENABLE_REVIEW_GENERATION, false)
          .putBoolean(KEY_ENABLE_MUSEUM_ASSISTANT, false)
          .putBoolean(KEY_ENABLE_URL_GROUNDING, false)
          .putBoolean(KEY_ENABLE_SEARCH_GROUNDING, false)
      }
      PRESET_DEMO_BOOTH -> {
        editor
          .putString(KEY_DEMO_PRESET, PRESET_DEMO_BOOTH)
          .putBoolean(KEY_ENABLE_TRAVEL_ASSISTANT, true)
          .putBoolean(KEY_ENABLE_EXPENSE_MANAGEMENT, true)
          .putBoolean(KEY_ENABLE_ITINERARY_ENRICHMENT, true)
          .putBoolean(KEY_ENABLE_TRIP_SUMMARY_AND_TIPS, true)
          .putBoolean(KEY_ENABLE_VOICE_NOTES, true)
          .putBoolean(KEY_ENABLE_DOCUMENTS, true)
          .putBoolean(KEY_ENABLE_BOOKING_ASSISTANT, true)
          .putBoolean(KEY_ENABLE_REVIEW_GENERATION, true)
          .putBoolean(KEY_ENABLE_MUSEUM_ASSISTANT, true)
          .putBoolean(KEY_ENABLE_URL_GROUNDING, true)
          .putBoolean(KEY_ENABLE_SEARCH_GROUNDING, true)
      }
    }
    editor.apply()
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

  val DEMO_PRESET: String
    get() = readStringFlag(KEY_DEMO_PRESET, PRESET_ADS_003)

  // Voice / Demo Language Settings
  val DEFAULT_DEMO_LANGUAGE = "en"

  val DEMO_LANGUAGE: String
    get() = readStringFlag(KEY_DEMO_LANGUAGE, DEFAULT_DEMO_LANGUAGE)

  // Existing flags
  val ENABLE_TRIP_SUMMARY_AND_TIPS: Boolean
    get() = readFlag(KEY_ENABLE_TRIP_SUMMARY_AND_TIPS, false)

  val ENABLE_SURPRISE_ME: Boolean
    get() = readFlag(KEY_ENABLE_SURPRISE_ME, false)

  // Offline features
  val ENABLE_ITINERARY_ENRICHMENT: Boolean
    get() = readFlag(KEY_ENABLE_ITINERARY_ENRICHMENT, true)

  val ENABLE_EXPENSE_MANAGEMENT: Boolean
    get() = readFlag(KEY_ENABLE_EXPENSE_MANAGEMENT, true)

  val ENABLE_VOICE_NOTES: Boolean
    get() = readFlag(KEY_ENABLE_VOICE_NOTES, false)

  val ENABLE_DOCUMENTS: Boolean
    get() = readFlag(KEY_ENABLE_DOCUMENTS, false)

  // Online features
  val ENABLE_REVIEW_GENERATION: Boolean
    get() = readFlag(KEY_ENABLE_REVIEW_GENERATION, false)

  val ENABLE_MUSEUM_ASSISTANT: Boolean
    get() = readFlag(KEY_ENABLE_MUSEUM_ASSISTANT, false)

  val ENABLE_URL_GROUNDING: Boolean
    get() = readFlag(KEY_ENABLE_URL_GROUNDING, false)

  val ENABLE_SEARCH_GROUNDING: Boolean
    get() = readFlag(KEY_ENABLE_SEARCH_GROUNDING, false)

  val ENABLE_BOOKING_ASSISTANT: Boolean
    get() = readFlag(KEY_ENABLE_BOOKING_ASSISTANT, false)

  val ENABLE_TRAVEL_ASSISTANT: Boolean
    get() = readFlag(KEY_ENABLE_TRAVEL_ASSISTANT, true)

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
      .putString(KEY_DEMO_PRESET, PRESET_CUSTOM)
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
