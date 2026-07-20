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

package com.example.jetpacker.feature.detail.hotel_chat

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.example.jetpacker.core.ui.JetPackerTheme
import com.example.jetpacker.feature.detail.hotel_chat.HotelSupportChatViewModel.SupportChatMessage

class HotelSupportChatScreenshotTest {

  @PreviewTest
  @Preview(showBackground = true)
  @Composable
  fun HotelSupportChatScreenshotPreview() {
    val messages = listOf(
      SupportChatMessage(id = "1", text = "Hi, when does breakfast start?", senderId = "User", senderName = "You"),
      SupportChatMessage(id = "2", text = "Breakfast is served from 7am to 10am at the main restaurant.", senderId = "AI", senderName = "Hotel Support")
    )
    val translations = mapOf(
      "2" to "Frühstück wird von 7 bis 10 Uhr im Hauptrestaurant serviert."
    )
    JetPackerTheme {
      HotelSupportChatContent(
        hotelName = "JetPacker Resort",
        messages = messages,
        currentUserId = "User",
        translations = translations,
        selectedLanguage = "German",
        onSendMessage = {},
        onTranslateMessage = {},
        onSelectLanguage = {},
        onBack = {}
      )
    }
  }
}
