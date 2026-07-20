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

import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.example.jetpacker.data.trips.Trip
import com.google.common.truth.Truth.assertThat
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TripSummaryAndTipsCardTest {

  @Test
  fun parseMarkdownToAnnotatedString_boldText_isBold() {
    val text = "This is **bold** text"
    val result = parseMarkdownToAnnotatedString(text)

    assertThat(result.text).isEqualTo("This is bold text")
    val span = result.spanStyles.find { it.item.fontWeight == FontWeight.Bold }
    assertThat(span).isNotNull()
    assertThat(span?.start).isEqualTo(8)
    assertThat(span?.end).isEqualTo(12)
  }

  @Test
  fun parseMarkdownToAnnotatedString_italicText_isItalic() {
    val text = "This is *italic* text"
    val result = parseMarkdownToAnnotatedString(text)

    assertThat(result.text).isEqualTo("This is italic text")
    val span = result.spanStyles.find { it.item.fontStyle == FontStyle.Italic }
    assertThat(span).isNotNull()
    assertThat(span?.start).isEqualTo(8)
    assertThat(span?.end).isEqualTo(14)
  }

  @Test
  fun parseMarkdownToAnnotatedString_bothBoldAndItalic_areParsed() {
    val text = "This is **bold** and *italic* text"
    val result = parseMarkdownToAnnotatedString(text)

    assertThat(result.text).isEqualTo("This is bold and italic text")

    val boldSpan = result.spanStyles.find { it.item.fontWeight == FontWeight.Bold }
    assertThat(boldSpan).isNotNull()
    assertThat(boldSpan?.start).isEqualTo(8)
    assertThat(boldSpan?.end).isEqualTo(12)

    val italicSpan = result.spanStyles.find { it.item.fontStyle == FontStyle.Italic }
    assertThat(italicSpan).isNotNull()
    assertThat(italicSpan?.start).isEqualTo(17)
    assertThat(italicSpan?.end).isEqualTo(23)
  }

  @Test
  fun parseMarkdownToAnnotatedString_bulletPointAndBold_isHandledCorrectly() {
    val text = "* **Name**"
    val result = parseMarkdownToAnnotatedString(text)

    assertThat(result.text).isEqualTo("* Name")
    val boldSpan = result.spanStyles.find { it.item.fontWeight == FontWeight.Bold }
    assertThat(boldSpan).isNotNull()
    assertThat(boldSpan?.start).isEqualTo(2)
    assertThat(boldSpan?.end).isEqualTo(6)

    val italicSpan = result.spanStyles.find { it.item.fontStyle == FontStyle.Italic }
    assertThat(italicSpan).isNull()
  }

  @Test
  fun parseMarkdownToAnnotatedString_noFormatting_returnsPlainText() {
    val text = "This is plain text"
    val result = parseMarkdownToAnnotatedString(text)

    assertThat(result.text).isEqualTo("This is plain text")
    assertThat(result.spanStyles).isEmpty()
  }

  @Test
  fun getTripMessage_nullTrip_returnsDefault() {
    val result = getTripMessage(null)
    assertThat(result).isEqualTo("Get ready for your trip")
  }

  @Test
  fun getTripMessage_upcomingTrip_returnsDefault() {
    val now =
      ZonedDateTime.of(
          2026,
          5,
          20,
          12,
          15,
          0,
          0,
          ZoneId.of("America/Los_Angeles"),
        )
        .toInstant()
        .toEpochMilli()
    val trip =
      Trip(
        id = "1",
        title = "Paris",
        location = "France",
        startDate = now + 100000,
        endDate = now + 200000,
      )
    val result = getTripMessage(trip)
    assertThat(result).isEqualTo("Get ready for your trip")
  }

  @Test
  fun getTripMessage_currentTrip_returnsEnjoy() {
    val now =
      ZonedDateTime.of(
          2026,
          5,
          20,
          12,
          15,
          0,
          0,
          ZoneId.of("America/Los_Angeles"),
        )
        .toInstant()
        .toEpochMilli()
    val trip =
      Trip(
        id = "1",
        title = "Paris",
        location = "France",
        startDate = now - 100000,
        endDate = now + 100000,
      )
    val result = getTripMessage(trip)
    assertThat(result).isEqualTo("Tips for the next few days")
  }

  @Test
  fun getTripMessage_pastTrip_returnsHope() {
    val now =
      ZonedDateTime.of(
          2026,
          5,
          20,
          12,
          15,
          0,
          0,
          ZoneId.of("America/Los_Angeles"),
        )
        .toInstant()
        .toEpochMilli()
    val trip =
      Trip(
        id = "1",
        title = "Paris",
        location = "France",
        startDate = now - 200000,
        endDate = now - 100000,
      )
    val result = getTripMessage(trip)
    assertThat(result).isEqualTo("Trip summary")
  }
}
