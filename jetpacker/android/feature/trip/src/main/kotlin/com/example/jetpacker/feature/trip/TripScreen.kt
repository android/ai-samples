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
import androidx.compose.animation.animateBounds
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.example.jetpacker.core.flags.FeatureFlags
import com.example.jetpacker.core.ui.R
import com.example.jetpacker.core.ui.components.JetPackerFab
import com.example.jetpacker.core.ui.components.JetPackerFabConfig
import com.example.jetpacker.core.ui.components.JetPackerToolbar
import com.example.jetpacker.core.ui.components.JetPackerToolbarAction
import com.example.jetpacker.data.itinerary.EventType
import com.example.jetpacker.feature.expenses.ManageExpensesScreen
import com.example.jetpacker.feature.itinerary.ItineraryScreen
import com.example.jetpacker.feature.voice_notes.VoiceNotesScreen

enum class TripTab {
  ITINERARY,
  EXPENSES,
  VOICE_NOTES,
}

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
  onVoiceNotesClick: () -> Unit = {},
  onNavigateToDebug: () -> Unit = {},
) {
  var selectedTab by remember { mutableStateOf(TripTab.ITINERARY) }
  var fabConfig by remember { mutableStateOf<JetPackerFabConfig?>(null) }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.primary,
    bottomBar = {
      val showExpenses = FeatureFlags.ENABLE_EXPENSE_MANAGEMENT
      val showVoiceNotes = FeatureFlags.ENABLE_VOICE_NOTES

      var visibleCount = 1
      if (showExpenses) visibleCount++
      if (showVoiceNotes) visibleCount++

      if (visibleCount > 1) {
        AnimatedVisibility(
          visible = true,
          enter = slideInVertically { it },
          exit = slideOutVertically { it },
        ) {
          JetPackerBottomBar(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            fabConfig = fabConfig,
          )
        }
      }
    },
  ) { innerPadding ->
    Box(modifier = Modifier.fillMaxSize()) {
      when (selectedTab) {
        TripTab.ITINERARY -> {
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

        TripTab.EXPENSES -> {
          ManageExpensesScreen(
            tripId = tripId,
            contentPadding = innerPadding,
            onBack = { selectedTab = TripTab.ITINERARY },
            onFabConfigChange = { fabConfig = it },
          )
        }

        TripTab.VOICE_NOTES -> {
          VoiceNotesScreen(
            tripId = tripId,
            contentPadding = innerPadding,
            onBack = { selectedTab = TripTab.ITINERARY },
            onFabConfigChange = { fabConfig = it },
          )
        }
      }
    }
  }
}

@Composable
fun JetPackerBottomBar(
  selectedTab: TripTab,
  onTabSelected: (TripTab) -> Unit,
  fabConfig: JetPackerFabConfig?,
  modifier: Modifier = Modifier,
) {
  val showExpenses = FeatureFlags.ENABLE_EXPENSE_MANAGEMENT
  val showVoiceNotes = FeatureFlags.ENABLE_VOICE_NOTES

  LookaheadScope {
    Row(
      modifier =
        modifier
          .fillMaxWidth()
          .windowInsetsPadding(WindowInsets.navigationBars)
          .padding(bottom = 16.dp)
          .padding(horizontal = 32.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
    ) {
      JetPackerToolbar(
        modifier = Modifier.widthIn(max = 272.dp).animateBounds(this@LookaheadScope)
      ) {
        JetPackerToolbarAction(
          icon = Icons.Rounded.Event,
          onClick = { onTabSelected(TripTab.ITINERARY) },
          selected = selectedTab == TripTab.ITINERARY,
          contentDescription = "Itinerary",
        )
        if (showExpenses) {
          JetPackerToolbarAction(
            icon = Icons.Rounded.Wallet,
            onClick = { onTabSelected(TripTab.EXPENSES) },
            selected = selectedTab == TripTab.EXPENSES,
            contentDescription = "Expenses",
          )
        }

        if (showVoiceNotes) {
          JetPackerToolbarAction(
            selected = selectedTab == TripTab.VOICE_NOTES,
            onClick = { onTabSelected(TripTab.VOICE_NOTES) },
            icon = ImageVector.vectorResource(R.drawable.speech_to_text),
            contentDescription = "Voice Notes",
          )
        }
      }

      Spacer(Modifier.width(4.dp))

      AnimatedVisibility(
        visible = fabConfig != null,
        enter = slideInHorizontally { it / 2 } + fadeIn(),
        exit = slideOutHorizontally { it / 2 } + fadeOut(),
      ) {
        JetPackerFab(
          modifier = Modifier.padding(4.dp),
          onClick = fabConfig?.onClick ?: {},
          icon = fabConfig?.icon ?: Icons.Rounded.Add,
          contentDescription = fabConfig?.contentDescription,
          containerColor = fabConfig?.containerColor ?: MaterialTheme.colorScheme.tertiary,
          contentColor = fabConfig?.contentColor ?: MaterialTheme.colorScheme.onTertiary,
        )
      }
    }
  }
}
