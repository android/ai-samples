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

package com.example.jetpacker.feature.create_trip

import android.annotation.SuppressLint
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalMediaQueryApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.derivedMediaQuery
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Composable screen for creating a new trip or editing an existing trip.
 * Manages form fields and coordinates lifecycle events with [CreateTripViewModel].
 */
@SuppressLint("ContextCastToActivity")
@OptIn(ExperimentalMediaQueryApi::class)
@Composable
fun CreateTripScreen(
  onBack: () -> Unit,
  onTripCreated: (String) -> Unit,
  tripIdToEdit: String? = null,
  viewModel: CreateTripViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  LaunchedEffect(tripIdToEdit) {
    if (tripIdToEdit != null) {
      viewModel.loadTripForEditing(tripIdToEdit)
    }
  }

  LaunchedEffect(uiState.isSuccess) {
    if (uiState.isSuccess) {
      onTripCreated(uiState.tripId)
    }
  }

  DisposableEffect(Unit) { onDispose { viewModel.resetState() } }

  BackHandler { onBack() }

  val tabletBreakpoint by derivedMediaQuery { windowWidth >= 1200.dp }
  val horizontalPadding = if (tabletBreakpoint) 128.dp else 0.dp

  Scaffold(
    modifier =
      Modifier.fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
    containerColor = MaterialTheme.colorScheme.surface,
  ) { innerPadding ->
    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
      CreateTripPanelContent(
        uiState = uiState,
        viewModel = viewModel,
        onCollapse = onBack,
        modifier = Modifier.padding(horizontal = horizontalPadding),
      )
    }
  }
}
