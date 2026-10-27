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

package com.example.jetpacker.feature.assistant.documents

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.ImagePart
import com.google.mlkit.genai.prompt.ModelPreference
import com.google.mlkit.genai.prompt.ModelReleaseStage
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import com.google.mlkit.genai.prompt.generationConfig
import com.google.mlkit.genai.prompt.modelConfig
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONObject

/** Structured data extracted from a travel document photo by on-device Gemini Nano. */
data class ExtractedDocumentData(
  val title: String,
  val documentType: String,
  val documentNumber: String = "",
  val issuingCountry: String = "",
  val expiryDateEpochMs: Long = 0L,
  val summary: String = "",
)

/**
 * On-device Gemini Nano multimodal vision extractor for travel documents, driver licenses, and
 * vouchers.
 */
@Singleton
class DocumentVisionScanner @Inject constructor() {

  companion object {
    private const val TAG = "DocumentVisionScanner"
  }

  private val generativeModel by lazy {
    val previewFastConfig = generationConfig {
      modelConfig = modelConfig {
        releaseStage = ModelReleaseStage.PREVIEW
        preference = ModelPreference.FULL
      }
    }
    Generation.getClient(previewFastConfig)
  }

  suspend fun isSupported(): Boolean {
    return try {
      var status = generativeModel.checkStatus()
      if (status == 1) { // 1 is DOWNLOADABLE
        Log.d(TAG, "Model is downloadable. Triggering download...")
        generativeModel.download().collect { Log.d(TAG, "Downloading progress...") }
        status = generativeModel.checkStatus()
      }
      status == 3 // 3 is AVAILABLE
    } catch (e: GenAiException) {
      if (e.errorCode == 606) {
        Log.w(TAG, "Feature not found, disabling.", e)
        false
      } else {
        throw e
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error checking Gemini Nano status", e)
      false
    }
  }

  suspend fun extractDocument(bitmap: Bitmap): ExtractedDocumentData {
    val prompt =
      """
      You are an expert on-device multimodal travel assistant.
      Examine the provided image carefully. This image could be:
      1. An official ID or travel document (Driver's License, Passport, Visa, Boarding Pass, Flight Ticket, Booking Confirmation).
      2. A real-world travel photo or note (Hotel Wi-Fi card/network info, Museum or artwork plaque, Subway/train schedule or transit map, Baggage claim ticket or luggage tag, Restaurant menu or specials board, Parking spot sign, Hiking trailhead map, or Receipt).

      Extract all key information and return strictly valid JSON with these keys:
      1. "title": Concise, human-friendly title under 6 words (e.g. "Driver's License - Netherlands", "Hotel Wi-Fi & Breakfast Info", "Louvre Museum Plaque - Mona Lisa", "Baggage Claim Tag - UA991", "Tokyo Metro Ginza Line Map", "Level P2 Parking Spot").
      2. "documentType": Must be one of: "DRIVER_LICENSE", "PASSPORT", "VISA", "TICKET", "HOTEL", "WIFI", "MUSEUM", "TRANSIT", "BAGGAGE", "PARKING", "MENU", "MAP", "NOTE", "OTHER".
      3. "documentNumber": Any visible ID number, license number, confirmation code, ticket number, or wi-fi password (e.g. "DL-••••1234", "WiFi: HotelGuest2026", "Spot: P2-4B").
      4. "issuingCountry": Issuing country, state, city, or venue name if visible (e.g. "Netherlands", "California", "Tokyo", "Grand Hotel").
      5. "expiryDate": Expiration date in ISO format "YYYY-MM-DD" if present on the document, or null if not applicable or not found.
         IMPORTANT FOR DRIVER'S LICENSES (especially EU/Dutch/standard ISO cards):
         - Field 4b ("4b", "Geldig tot", "Valid until") is the EXPIRATION DATE. Always use 4b as the expiryDate.
         - Field 4a ("4a", "Afgiften", "Date of issue") is the issue date, NOT the expiry date.
         - Field 3 ("3", "Geboortedatum", "Date of birth") is the birth date, NOT the expiry date.
         - For non-expired items or non-documents without expiry dates, return null.
      6. "summary": A helpful 2-4 sentence summary of all essential details read from the image (names, dates, Wi-Fi password, opening/closing hours, platform/terminal numbers, baggage carousel, artist notes, ingredients, directions, etc.).

      Return ONLY the JSON object.
      """
        .trimIndent()

    val request = generateContentRequest(ImagePart(bitmap), TextPart(prompt)) {}

    var fullResponse = ""
    try {
      generativeModel.generateContentStream(request).collect { response ->
        val text = response.candidates.firstOrNull()?.text ?: ""
        fullResponse += text
      }
      Log.i(TAG, "Raw Gemini Nano extraction response:\n$fullResponse")
    } catch (e: Exception) {
      Log.e(TAG, "Error generating vision content", e)
    }

    val extracted = parseExtractionJson(fullResponse)
    Log.i(TAG, "Extracted document data: $extracted")
    return extracted
  }

  private fun parseExtractionJson(rawText: String): ExtractedDocumentData {
    if (rawText.isBlank()) {
      return ExtractedDocumentData(
        title = "Scanned Travel Photo",
        documentType = "OTHER",
        summary = "Photo scanned on device.",
      )
    }

    val cleaned =
      rawText.substringAfter("```json").substringBefore("```").trim().ifEmpty { rawText.trim() }

    return try {
      val json = JSONObject(if (cleaned.startsWith("{")) cleaned else rawText)
      val title = json.optString("title", "Scanned Document").ifBlank { "Scanned Document" }
      val docType =
        json.optString("documentType", "OTHER").uppercase().let { type ->
          when {
            type.contains("DRIVER") || type.contains("LICENSE") -> "DRIVER_LICENSE"
            type.contains("PASS") -> "PASSPORT"
            type.contains("VISA") -> "VISA"
            type.contains("TICKET") || type.contains("FLIGHT") -> "TICKET"
            type.contains("HOTEL") -> "HOTEL"
            type.contains("INSUR") -> "INSURANCE"
            type.contains("WIFI") -> "WIFI"
            type.contains("MUSEUM") || type.contains("ART") -> "MUSEUM"
            type.contains("TRANSIT") ||
              type.contains("TRAIN") ||
              type.contains("METRO") ||
              type.contains("SUBWAY") -> "TRANSIT"
            type.contains("BAGGAGE") || type.contains("LUGGAGE") -> "BAGGAGE"
            type.contains("PARKING") -> "PARKING"
            type.contains("MENU") || type.contains("FOOD") || type.contains("RESTAURANT") -> "MENU"
            type.contains("MAP") || type.contains("TRAIL") -> "MAP"
            type.contains("NOTE") -> "NOTE"
            else -> type
          }
        }
      val docNum = json.optString("documentNumber", "")
      val country = json.optString("issuingCountry", "")
      val expiryStr = json.optString("expiryDate", "")
      val summary = json.optString("summary", "")

      val expiryEpochMs = parseDateToEpochMs(expiryStr)

      ExtractedDocumentData(
        title = title,
        documentType = docType,
        documentNumber = docNum,
        issuingCountry = country,
        expiryDateEpochMs = expiryEpochMs,
        summary = summary,
      )
    } catch (e: Exception) {
      Log.w(TAG, "Failed to parse JSON response: $rawText", e)
      val isDriverLicense =
        rawText.contains("driver", ignoreCase = true) ||
          rawText.contains("license", ignoreCase = true)
      val isPassport = rawText.contains("passport", ignoreCase = true)
      val isFlight =
        rawText.contains("flight", ignoreCase = true) ||
          rawText.contains("airline", ignoreCase = true)
      val isWifi =
        rawText.contains("wifi", ignoreCase = true) || rawText.contains("wi-fi", ignoreCase = true)
      val isMuseum =
        rawText.contains("museum", ignoreCase = true) ||
          rawText.contains("artist", ignoreCase = true)

      val type =
        when {
          isDriverLicense -> "DRIVER_LICENSE"
          isPassport -> "PASSPORT"
          isFlight -> "TICKET"
          isWifi -> "WIFI"
          isMuseum -> "MUSEUM"
          else -> "OTHER"
        }
      val fallbackTitle =
        when {
          isDriverLicense -> "Driver's License"
          isPassport -> "Passport"
          isFlight -> "Flight Ticket"
          isWifi -> "Wi-Fi Info"
          isMuseum -> "Museum Exhibit"
          else -> "Scanned Travel Photo"
        }

      ExtractedDocumentData(title = fallbackTitle, documentType = type, summary = rawText.take(300))
    }
  }

  private fun parseDateToEpochMs(dateStr: String): Long {
    val clean = dateStr.trim().removeSurrounding("\"").substringBefore('T').trim()
    if (clean.isBlank() || clean.equals("null", ignoreCase = true)) return 0L

    val patterns =
      listOf(
        "yyyy-MM-dd",
        "dd-MM-yyyy",
        "dd.MM.yyyy",
        "dd/MM/yyyy",
        "MM/dd/yyyy",
        "yyyy/MM/dd",
        "yyyy.MM.dd",
        "d-M-yyyy",
        "d.M.yyyy",
        "d/M/yyyy",
        "d MMM yyyy",
        "MMMM d, yyyy",
      )

    for (pattern in patterns) {
      try {
        val formatter = DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH)
        val localDate = LocalDate.parse(clean, formatter)
        return localDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
      } catch (_: Exception) {
        // try next pattern
      }
    }
    Log.w(TAG, "Could not parse date string into epoch millis: '$dateStr'")
    return 0L
  }
}
