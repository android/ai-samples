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

package com.example.jetpacker.feature.detail.review

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.example.jetpacker.core.ui.JetPackerTheme

class ReviewScreenshotTest {

  @PreviewTest
  @Preview(showBackground = true)
  @Composable
  fun ReviewScreenScreenshotPreview() {
    val topics = listOf("Accessibility", "Location", "Food Quality", "Service", "Ambiance", "Value")
    val selectedTopics = setOf(
      Topic(name = "Food Quality", positiveOpinion = true),
      Topic(name = "Service", positiveOpinion = false),
      Topic(name = "Value", positiveOpinion = true)
    )
    JetPackerTheme {
      ReviewScreenContent(
        placeName = "Le Café Marly",
        topics = topics,
        selectedTopics = selectedTopics,
        isGenerating = false,
        generatedReviewText = "Food quality was excellent and represented great value, though the service was a bit slow.",
        onTopicSelected = { _, _ -> },
        onGenerateReview = {},
        onGeneratedReviewTextChange = {},
        onPostReview = {},
        onBack = {}
      )
    }
  }
}
