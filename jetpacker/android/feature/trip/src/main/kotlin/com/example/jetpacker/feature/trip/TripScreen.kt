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

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateBounds
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.jetpacker.core.flags.FeatureFlags
import com.example.jetpacker.core.ui.R
import com.example.jetpacker.core.ui.components.JetPackerFab
import com.example.jetpacker.core.ui.components.JetPackerFabConfig
import com.example.jetpacker.core.ui.components.JetPackerToolbar
import com.example.jetpacker.core.ui.components.JetPackerToolbarAction
import com.example.jetpacker.data.itinerary.EventType
import com.example.jetpacker.feature.assistant.TravelAssistantViewModel
import com.example.jetpacker.feature.assistant.history.ChatHistoryScreen
import com.example.jetpacker.feature.assistant.model.AssistantOverlayState
import com.example.jetpacker.feature.assistant.ui.AmbientTripAssistantSheet
import com.example.jetpacker.feature.booking_assistant.BookingAssistantScreen
import com.example.jetpacker.feature.expenses.ManageExpensesScreen
import com.example.jetpacker.feature.itinerary.ItineraryScreen
import com.example.jetpacker.feature.voice_notes.VoiceNotesScreen

enum class TripTab {
  ITINERARY,
  EXPENSES,
  VOICE_NOTES,
  BOOKING,
  CHAT_HISTORY,
}

/**
 * Composable screen serving as the main container for viewing details of a specific trip. Displays
 * the itinerary and links to event details, maps, and editing options.
 */
@Composable
fun TripScreen(
  tripId: String,
  onBack: () -> Unit = {},
  onEditTripClick: () -> Unit = {},
  onEventClick: (String, EventType) -> Unit = { _, _ -> },
  onVoiceNotesClick: () -> Unit = {},
  onNavigateToDebug: () -> Unit = {},
  assistantViewModel: TravelAssistantViewModel = hiltViewModel(),
) {
  var selectedTab by remember { mutableStateOf(TripTab.ITINERARY) }
  // One FAB config slot per tab so a tab can never render another tab's FAB, even for the frame
  // between switching tabs and the new screen publishing its config from a LaunchedEffect.
  val fabConfigs = remember { mutableStateMapOf<TripTab, JetPackerFabConfig?>() }

  LaunchedEffect(tripId) { assistantViewModel.setTripId(tripId) }

  val overlayState by assistantViewModel.overlayState.collectAsStateWithLifecycle()

  val audioPermissionLauncher =
    rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) {
      isGranted ->
      assistantViewModel.setVoicePermissionGranted(isGranted)
      if (isGranted) {
        assistantViewModel.startVoiceAssistant(onPermissionRequired = {})
      } else {
        assistantViewModel.dismissOverlay()
      }
    }

  val assistantFabConfig =
    JetPackerFabConfig(
      icon = Icons.Rounded.AutoAwesome,
      onClick = {
        assistantViewModel.startVoiceAssistant(
          onPermissionRequired = {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
          }
        )
      },
      contentDescription = "Trip Assistant",
      containerColor = MaterialTheme.colorScheme.secondary,
      contentColor = MaterialTheme.colorScheme.onSecondary,
    )

  val assistantEnabled = FeatureFlags.ENABLE_TRAVEL_ASSISTANT
  val assistantAvailableOnTab = assistantEnabled && selectedTab != TripTab.BOOKING
  val showAssistantOverlay =
    assistantAvailableOnTab && overlayState !is AssistantOverlayState.Collapsed
  // Tab's own action FAB takes precedence; if the tab has no specific creation FAB,
  // fall back to the assistant FAB when enabled.
  val tabFabConfig = fabConfigs[selectedTab]
  val bottomBarFabConfig =
    tabFabConfig ?: if (assistantAvailableOnTab) assistantFabConfig else null

  Scaffold(
    containerColor = MaterialTheme.colorScheme.primary,
    bottomBar = {
      TripBottomBarContainer(
        showAssistantOverlay = showAssistantOverlay,
        bottomBar = {
          JetPackerBottomBar(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            fabConfig = bottomBarFabConfig,
          )
        },
        assistantSheet = {
          AmbientTripAssistantSheet(
            viewModel = assistantViewModel,
            modifier = Modifier.padding(bottom = 8.dp),
          )
        },
      )
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
            onFabConfigChange = { fabConfigs[TripTab.ITINERARY] = it },
            onNavigateToDebug = onNavigateToDebug,
          )
        }

        TripTab.EXPENSES -> {
          ManageExpensesScreen(
            tripId = tripId,
            contentPadding = innerPadding,
            onBack = { selectedTab = TripTab.ITINERARY },
            onFabConfigChange = { fabConfigs[TripTab.EXPENSES] = it },
          )
        }

        TripTab.VOICE_NOTES -> {
          VoiceNotesScreen(
            tripId = tripId,
            contentPadding = innerPadding,
            onBack = { selectedTab = TripTab.ITINERARY },
            onFabConfigChange = { fabConfigs[TripTab.VOICE_NOTES] = it },
          )
        }
        TripTab.BOOKING -> {
          BookingAssistantScreen(
            tripId = tripId,
            contentPadding = innerPadding,
            onBack = { selectedTab = TripTab.ITINERARY },
          )
        }
        TripTab.CHAT_HISTORY -> {
          ChatHistoryScreen(
            tripId = tripId,
            contentPadding = innerPadding,
            onBack = { selectedTab = TripTab.ITINERARY },
            onFabConfigChange = { fabConfigs[TripTab.CHAT_HISTORY] = it },
            onSelectConversation = { conversation ->
              assistantViewModel.loadConversation(conversation)
            },
            onStartNewChat = {
              assistantViewModel.clearChat()
              assistantViewModel.openConversationalOverlay()
            },
          )
        }
      }
    }
  }
}

/**
 * Hosts the bottom navigation bar and the ambient assistant sheet in the same slot, overlapping
 * them so the Scaffold's bottom inset stays stable while one slides out and the other slides in.
 */
@Composable
private fun TripBottomBarContainer(
  showAssistantOverlay: Boolean,
  bottomBar: @Composable () -> Unit,
  assistantSheet: @Composable () -> Unit,
  modifier: Modifier = Modifier,
) {
  val enter =
    slideInVertically(tween(durationMillis = 240, easing = FastOutSlowInEasing)) { it } +
      fadeIn(tween(durationMillis = 180))
  val exit =
    slideOutVertically(tween(durationMillis = 220, easing = FastOutSlowInEasing)) { it } +
      fadeOut(tween(durationMillis = 120))

  Box(
    modifier =
      modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).imePadding(),
    contentAlignment = Alignment.BottomCenter,
  ) {
    AnimatedVisibility(visible = !showAssistantOverlay, enter = enter, exit = exit) { bottomBar() }
    AnimatedVisibility(visible = showAssistantOverlay, enter = enter, exit = exit) {
      assistantSheet()
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
  val showBooking = FeatureFlags.ENABLE_BOOKING_ASSISTANT
  val showChatHistory = FeatureFlags.ENABLE_TRAVEL_ASSISTANT
  val visibleTabCount =
    1 +
      (if (showExpenses) 1 else 0) +
      (if (showVoiceNotes) 1 else 0) +
      (if (showBooking) 1 else 0) +
      (if (showChatHistory) 1 else 0)
  val maxToolbarWidth =
    when {
      visibleTabCount <= 2 -> if (fabConfig != null) 240.dp else 300.dp
      visibleTabCount == 3 -> if (fabConfig != null) 280.dp else 340.dp
      visibleTabCount == 4 -> if (fabConfig != null) 340.dp else 400.dp
      visibleTabCount == 5 -> if (fabConfig != null) 390.dp else 450.dp
      else -> if (fabConfig != null) 440.dp else 500.dp
    }

  LookaheadScope {
    Row(
      modifier =
        modifier
          .fillMaxWidth()
          .padding(top = 12.dp, bottom = 16.dp)
          .padding(horizontal = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
    ) {
      JetPackerToolbar(
        modifier =
          Modifier.weight(1f, fill = false)
            .widthIn(max = maxToolbarWidth)
            .animateBounds(this@LookaheadScope)
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
        if (showBooking) {
          JetPackerToolbarAction(
            selected = selectedTab == TripTab.BOOKING,
            onClick = { onTabSelected(TripTab.BOOKING) },
            icon = Icons.Rounded.ConfirmationNumber,
            contentDescription = "Booking Assistant",
          )
        }
        if (showChatHistory) {
          JetPackerToolbarAction(
            selected = selectedTab == TripTab.CHAT_HISTORY,
            onClick = { onTabSelected(TripTab.CHAT_HISTORY) },
            icon = Icons.AutoMirrored.Rounded.Chat,
            contentDescription = "Chat history",
          )
        }
      }

      // While the FAB animates out (fabConfig == null) keep rendering the last config instead of a
      // generic fallback, so a transient null frame never flashes a different icon/colour.
      var lastFabConfig by remember { mutableStateOf(fabConfig) }
      if (fabConfig != null) lastFabConfig = fabConfig
      val displayFab = lastFabConfig

      AnimatedVisibility(
        visible = fabConfig != null,
        enter = slideInHorizontally { it / 2 } + fadeIn(),
        exit = slideOutHorizontally { it / 2 } + fadeOut(),
      ) {
        if (displayFab != null) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(8.dp))
            JetPackerFab(
              modifier = Modifier.padding(4.dp),
              onClick = displayFab.onClick,
              icon = displayFab.icon,
              contentDescription = displayFab.contentDescription,
              containerColor = displayFab.containerColor ?: MaterialTheme.colorScheme.tertiary,
              contentColor = displayFab.contentColor ?: MaterialTheme.colorScheme.onTertiary,
            )
          }
        }
      }
    }
  }
}
