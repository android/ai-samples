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

package com.example.jetpacker.feature.trip

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.jetpacker.core.ui.components.JetPackerFab
import com.example.jetpacker.core.ui.components.JetPackerFabConfig
import com.example.jetpacker.data.itinerary.EventType
import com.example.jetpacker.feature.itinerary.ItineraryScreen

/**
 * Composable screen serving as the main container for viewing details of a specific trip.
 * Displays the itinerary and links to event details, maps, and editing options.
 */
@Composable
fun TripScreen(
  tripId: String,
  onBack: () -> Unit = {},
  onEditTripClick: () -> Unit = {},
  onEventClick: (String, EventType) -> Unit = { _, _ -> },
  onNavigateToDebug: () -> Unit = {},
) {
  var fabConfig by remember { mutableStateOf<JetPackerFabConfig?>(null) }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.primary,
    floatingActionButton = {
      AnimatedVisibility(
        visible = fabConfig != null,
        enter = slideInHorizontally { it / 2 } + fadeIn(),
        exit = slideOutHorizontally { it / 2 } + fadeOut(),
      ) {
        fabConfig?.let { config ->
          JetPackerFab(
            modifier = Modifier.padding(16.dp),
            onClick = config.onClick,
            icon = config.icon,
            contentDescription = config.contentDescription,
            containerColor = config.containerColor ?: MaterialTheme.colorScheme.tertiary,
            contentColor = config.contentColor ?: MaterialTheme.colorScheme.onTertiary,
          )
        }
      }
    },
  ) { innerPadding ->
    Box(modifier = Modifier.fillMaxSize()) {
      ItineraryScreen(
        tripId = tripId,
        contentPadding = innerPadding,
        onBack = onBack,
        onEditTripClick = onEditTripClick,
        onEventClick = onEventClick,
        onFabConfigChange = { fabConfig = it },
        onNavigateToDebug = onNavigateToDebug,
      )
    }
  }
}
