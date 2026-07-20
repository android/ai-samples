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

package com.example.jetpacker.feature.detail.museum_assistant

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.example.jetpacker.core.ui.JetPackerTheme

class MuseumAssistantScreenshotTest {

  @PreviewTest
  @Preview(showBackground = true)
  @Composable
  fun ChatbotScreenScreenshotPreview() {
    val messages = listOf(
      ChatMessage(id = 1, text = "Hello! Can I ask about Louvre opening hours?", isUser = true, sender = "You"),
      ChatMessage(id = 2, text = "Yes, of course! The Louvre is open from 9 AM to 6 PM every day except Tuesday.", isUser = false, sender = "Museum Assistant"),
      ChatMessage(id = 3, text = "Are there any cafes inside?", isUser = true, sender = "You"),
      ChatMessage(id = 4, text = "Thinking...", isUser = false, sender = "Museum Assistant")
    )
    JetPackerTheme {
      ChatbotContent(
        messages = messages,
        onSendMessage = {},
        onBack = {}
      )
    }
  }
}
