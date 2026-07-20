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

package com.example.jetpacker.feature.itinerary_enrichment

import android.util.Log
import com.example.jetpacker.data.itinerary.TimelineEvent
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.ModelPreference
import com.google.mlkit.genai.prompt.ModelReleaseStage
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import com.google.mlkit.genai.prompt.generationConfig
import com.google.mlkit.genai.prompt.modelConfig
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class DailyThemeProviderImpl @Inject constructor() : DailyThemeProvider {
  private val generativeModel by lazy {
    val previewFastConfig = generationConfig {
      modelConfig = modelConfig {
        releaseStage = ModelReleaseStage.PREVIEW
        preference = ModelPreference.FAST
      }
    }
    Generation.getClient(previewFastConfig)
  }

  override suspend fun generateDailyThemes(events: List<TimelineEvent>): List<DayThemeItem> {
    var status =
      try {
        generativeModel.checkStatus()
      } catch (e: GenAiException) {
        if (e.errorCode == 606) {
          Log.w("DailyTheme", "Feature 646 not found in generateDailyThemes, disabling.", e)
          return emptyList()
        } else {
          throw e
        }
      }
    if (status == 1) { // 1 is DOWNLOADABLE
      Log.d("DailyTheme", "Model is downloadable. Triggering download...")
      generativeModel.download().collect { Log.d("DailyTheme", "Downloading progress...") }
      status = generativeModel.checkStatus()
    }

    if (status != 3) { // 3 is AVAILABLE
      Log.e("DailyTheme", "Model is not available (status: $status). Skipping theme generation.")
      return emptyList()
    }

    val prompt =
      """
        Generate a short theme (max 3 words) for each day of the trip.
        Respond ONLY with pairs separated by a pipe character. Do NOT output JSON.
        Format example:
        2026-05-17|Arrival & Welcoming
        2026-05-18|Sightseeing Tour
        
        Current Trip Itinerary:
        ${events.joinToString("\n") { "- ${Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()} / ${it.title} in ${it.location}" }}
    """
        .trimIndent()

    return try {
      var responseText = ""
      var retryCount = 0
      val maxRetries = 3

      while (retryCount < maxRetries) {
        try {
          val request = generateContentRequest(TextPart(prompt)) {}
          val response = generativeModel.generateContent(request)
          responseText = response.candidates.firstOrNull()?.text ?: ""
          if (responseText.isNotEmpty()) break
        } catch (e: Exception) {
          if (e is kotlinx.coroutines.CancellationException) throw e
          if (e.message?.contains("ErrorCode 9") == true || e.message?.contains("BUSY") == true) {
            retryCount++
            Log.w("DailyTheme", "AI Core is busy, retrying ($retryCount/$maxRetries)...")
            kotlinx.coroutines.delay(1000L * retryCount)
          } else {
            throw e
          }
        }
      }

      if (responseText.isEmpty()) return emptyList()

      Log.d("DailyTheme", "Raw LLM output: $responseText")

      val themes = mutableListOf<DayThemeItem>()
      val uniqueDates =
        events
          .map {
            Instant.ofEpochMilli(it.timestamp)
              .atZone(ZoneId.systemDefault())
              .toLocalDate()
              .toString()
          }
          .distinct()
          .sorted()

      val extractedLines =
        responseText.lines().flatMap { it.split("|") }.map { it.trim() }.filter { it.isNotEmpty() }

      var dateIndex = 0
      extractedLines.forEach { part ->
        if (part.matches(Regex("""\d{4}-\d{2}-\d{2}"""))) {
          // It's a standalone date from broken split, skip it
        } else if (part.contains("|")) {
          val split = part.split("|")
          if (split.size == 2) {
            themes.add(DayThemeItem(split[0].trim(), split[1].trim()))
          }
        } else if (dateIndex < uniqueDates.size) {
          val cleanTheme = part.replace(Regex("""^\d{4}-\d{2}-\d{2}[^\w]*"""), "").trim()
          if (cleanTheme.isNotEmpty()) {
            themes.add(DayThemeItem(uniqueDates[dateIndex], cleanTheme))
            dateIndex++
          }
        }
      }
      Log.d("DailyTheme", "Successfully extracted ${themes.size} themes")
      themes
    } catch (e: Exception) {
      if (e is kotlinx.coroutines.CancellationException) throw e
      Log.e("DailyTheme", "Theme generation process failed", e)
      emptyList()
    }
  }
}
