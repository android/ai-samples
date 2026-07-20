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

@file:OptIn(
  androidx.compose.ui.ExperimentalMediaQueryApi::class,
  androidx.compose.ui.ExperimentalComposeUiApi::class
)

package com.example.jetpacker.feature.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.LocalUiMediaScope
import androidx.compose.ui.UiMediaScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest

@OptIn(ExperimentalComposeUiApi::class)
private val initMediaQuery = run {
  ComposeUiFlags.isMediaQueryIntegrationEnabled = true
  true
}

@OptIn(ExperimentalComposeUiApi::class)
class MockUiMediaScope(
  override val windowWidth: Dp = 400.dp,
  override val windowHeight: Dp = 800.dp,
  override val windowPosture: UiMediaScope.Posture = UiMediaScope.Posture.Flat,
  override val pointerPrecision: UiMediaScope.PointerPrecision = UiMediaScope.PointerPrecision.Coarse,
  override val keyboardKind: UiMediaScope.KeyboardKind = UiMediaScope.KeyboardKind.Virtual,
  override val hasCamera: Boolean = true,
  override val hasMicrophone: Boolean = true,
  override val viewingDistance: UiMediaScope.ViewingDistance = UiMediaScope.ViewingDistance.Near
) : UiMediaScope

class HomeScreenshotTest {
  @PreviewTest
  @Preview(showBackground = true)
  @Composable
  fun LoadingStateScreenshotPreview() {
    MaterialTheme { LoadingState() }
  }

  @OptIn(ExperimentalComposeUiApi::class)
  @PreviewTest
  @Preview(showBackground = true)
  @Composable
  fun HomeScreenScreenshotPreview() {
    CompositionLocalProvider(LocalUiMediaScope provides MockUiMediaScope()) {
      HomeScreenPreview()
    }
  }

  @OptIn(ExperimentalComposeUiApi::class)
  @PreviewTest
  @Preview(showBackground = true)
  @Composable
  fun DebugScreenScreenshotPreview() {
    val fakeViewModel = object : DebugViewModel(null, null, null, null, null) {
      override fun resetDatabase() {}
    }
    CompositionLocalProvider(LocalUiMediaScope provides MockUiMediaScope()) {
      DebugScreen(onBack = {}, viewModel = fakeViewModel)
    }
  }
}
