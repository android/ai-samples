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

package com.example.jetpacker.feature.booking_assistant.domain

import com.example.jetpacker.feature.booking_assistant.BookingAssistantViewModel
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BookingDirectiveParserTest {

  @Test
  fun extractA2uiDirectives_singleObjectPayload_returnsSingleItem() {
    val singleObject =
      """{"version":"v0.9","createSurface":{"surfaceId":"hotel_Flight"}}"""
    val directives = BookingAssistantViewModel.extractA2uiDirectives(singleObject)
    assertThat(directives).hasSize(1)
    assertThat(directives[0]).contains("\"surfaceId\":\"hotel_Flight\"")
  }

  @Test
  fun extractA2uiDirectives_arrayPayload_unwrapsAllDirectives() {
    val arrayPayload =
      """[{"version":"v0.9","createSurface":{"surfaceId":"s1"}},{"version":"v0.9","updateComponents":{"surfaceId":"s1"}}]"""
    val directives = BookingAssistantViewModel.extractA2uiDirectives(arrayPayload)
    assertThat(directives).hasSize(2)
    assertThat(directives[0]).contains("\"createSurface\"")
    assertThat(directives[1]).contains("\"updateComponents\"")
  }

  @Test
  fun extractA2uiDirectives_nonA2uiText_returnsEmptyList() {
    val plainText = "Hello! How can I help you book your trip today?"
    val directives = BookingAssistantViewModel.extractA2uiDirectives(plainText)
    assertThat(directives).isEmpty()
  }

  @Test
  fun extractA2uiDirectives_malformedArray_fallsBackToEmptyList() {
    val malformed = """[{"version":"v0.9","createSurface":{"surfaceId":"""
    val directives = BookingAssistantViewModel.extractA2uiDirectives(malformed)
    assertThat(directives).isEmpty()
  }
}
