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

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.example.jetpacker.feature.booking_assistant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.a2ui.A2uiSurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.jetpacker.core.ui.SekuyaFontFamily
import com.example.jetpacker.feature.booking_assistant.components.BookingChatComposer
import com.example.jetpacker.feature.booking_assistant.components.ChatMessageItem
import com.example.jetpacker.feature.booking_assistant.components.JourneyCategoryCard
import com.example.jetpacker.feature.booking_assistant.components.JourneySubItemRow

@Composable
fun BookingAssistantScreen(
  tripId: String,
  contentPadding: PaddingValues,
  onBack: () -> Unit = {},
  viewModel: BookingAssistantViewModel = hiltViewModel(),
  modifier: Modifier = Modifier,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  var inputText by remember { mutableStateOf("") }
  val listState = rememberLazyListState()

  LaunchedEffect(tripId) { viewModel.initializeChat(tripId) }

  // Auto-scroll to the latest message or newly streamed surface
  LaunchedEffect(uiState.chatMessages.size, uiState.activeSurfaces.size) {
    if (uiState.chatMessages.isNotEmpty()) {
      val totalItems = listState.layoutInfo.totalItemsCount
      if (totalItems > 0) {
        listState.animateScrollToItem(totalItems - 1)
      }
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
          title = {
            Column {
              Text(
                text = "Trip Concierge",
                style =
                  MaterialTheme.typography.titleLarge.copy(
                    fontFamily = SekuyaFontFamily,
                    fontSize = 17.sp,
                  ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                autoSize =
                  TextAutoSize.StepBased(
                    minFontSize = 12.sp,
                    maxFontSize = 17.sp,
                  ),
              )
              uiState.trip?.location?.let { location ->
                Text(
                  text = location,
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          },
          navigationIcon = {
            IconButton(onClick = onBack) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface,
              )
            }
          },
          actions = {
            IconButton(onClick = { viewModel.resetBookings() }) {
              Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = "Reset Chat",
                tint = MaterialTheme.colorScheme.onSurface,
              )
            }
          },
          colors =
            TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        )
        if (uiState.isStreaming) {
          LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth().height(3.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = Color.Transparent,
          )
        }
      }
    },
  ) { innerPadding ->
    val bottomBarPadding = contentPadding.calculateBottomPadding()
    val imeBottomPadding = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val effectiveBottomPadding = maxOf(bottomBarPadding, imeBottomPadding)

    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(top = innerPadding.calculateTopPadding(), bottom = effectiveBottomPadding)
    ) {
      val linkedSurfaceIds = remember(uiState.chatMessages) {
        uiState.chatMessages.mapNotNull { it.surfaceId }.toSet()
      }
      val unlinkedSurfaces = remember(uiState.activeSurfaces, linkedSurfaceIds) {
        uiState.activeSurfaces.filter { it.id !in linkedSurfaceIds }
      }

      LazyColumn(
        state = listState,
        modifier =
          Modifier.weight(1f)
            .fillMaxWidth()
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithContent {
              drawContent()
              drawRect(
                brush =
                  Brush.verticalGradient(
                    0f to Color.Black,
                    0.94f to Color.Black,
                    1f to Color.Transparent,
                  ),
                blendMode = BlendMode.DstIn,
              )
            }
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 52.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        items(items = uiState.chatMessages, key = { it.id }) { message ->
          val linkedSurface =
            remember(message.surfaceId, uiState.activeSurfaces) {
              message.surfaceId?.let { id -> uiState.activeSurfaces.find { it.id == id } }
            }
          ChatMessageItem(message = message, surfaceModel = linkedSurface)
        }

        items(items = unlinkedSurfaces, key = { "unlinked_surface_${it.id}" }) { surface ->
          A2uiSurface(surfaceModel = surface, modifier = Modifier.fillMaxWidth())
        }

        if (uiState.chatMessages.none { it.isUser } && uiState.activeSurfaces.isEmpty()) {
          item(key = "your_next_journey_overview") {
            YourNextJourneySection(
              flightItems = uiState.flightJourneyItems,
              activityItems = uiState.activityJourneyItems,
              onSelectItem = { item ->
                viewModel.sendChatMessage(
                  prompt = item.prompt,
                  intentOverride = item.intent,
                  targetTitleOverride = item.targetTitle,
                )
              },
              modifier = Modifier.padding(horizontal = 8.dp),
            )
          }
        }
      }

      BookingChatComposer(
        inputText = inputText,
        onInputTextChange = { inputText = it },
        onSendMessage = { prompt -> viewModel.sendChatMessage(prompt) },
        isStreaming = uiState.isStreaming,
      )
    }
  }
}

@Composable
private fun YourNextJourneySection(
  flightItems: List<BookableJourneyItem>,
  activityItems: List<BookableJourneyItem>,
  onSelectItem: (BookableJourneyItem) -> Unit,
  modifier: Modifier = Modifier,
) {
  var flightsExpanded by remember { mutableStateOf(false) }
  var activitiesExpanded by remember { mutableStateOf(false) }

  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = "Trip Concierge",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = "•",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Text(
        text = "FINALIZE YOUR TRIP",
        style =
          MaterialTheme.typography.titleLarge.copy(
            fontFamily = SekuyaFontFamily,
          ),
        color = MaterialTheme.colorScheme.onSurface,
      )
    }

    Text(
      text =
        "Complete your pending reservations below, or ask to customize your stay, seats, or dining.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(modifier = Modifier.height(2.dp))

    if (flightItems.isNotEmpty()) {
      JourneyCategoryCard(
        title = "Flights (${flightItems.size})",
        subtitle = "Action required",
        icon = Icons.Rounded.Flight,
        expanded = flightsExpanded,
        onToggle = { flightsExpanded = !flightsExpanded },
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          flightItems.forEach { item ->
            JourneySubItemRow(
              title = item.title,
              subtitle = item.subtitle,
              ctaText = item.ctaText,
              onClick = { onSelectItem(item) },
            )
          }
        }
      }
    }

    if (activityItems.isNotEmpty()) {
      JourneyCategoryCard(
        title = "Activities (${activityItems.size})",
        subtitle = "Action required",
        icon = Icons.Rounded.ConfirmationNumber,
        expanded = activitiesExpanded,
        onToggle = { activitiesExpanded = !activitiesExpanded },
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          activityItems.forEach { item ->
            JourneySubItemRow(
              title = item.title,
              subtitle = item.subtitle,
              ctaText = item.ctaText,
              onClick = { onSelectItem(item) },
            )
          }
        }
      }
    }
  }
}
