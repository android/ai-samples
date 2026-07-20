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

package com.example.jetpacker.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalMediaQueryApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.derivedMediaQuery
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.jetpacker.data.trips.Trip
import com.example.jetpacker.feature.home.components.TripCard

@OptIn(ExperimentalMediaQueryApi::class)
@Composable
fun PhoneLayout(
  pastTrips: List<Trip>,
  paddingValues: PaddingValues,
  tripToDelete: Trip?,
  onTripClick: (String) -> Unit,
  onSwipeToDelete: (Trip) -> Unit,
  currentTrip: Trip?,
  upcomingTrips: List<Trip>,
  onCreateClick: () -> Unit = {},
) {
  var isUpcomingExpanded by remember { mutableStateOf(true) }
  var isCurrentExpanded by remember { mutableStateOf(true) }
  var isPastExpanded by remember { mutableStateOf(true) }
  val foldableBreakpoint by derivedMediaQuery { windowWidth >= 600.dp }

  rememberCoroutineScope()

  Box(modifier = Modifier.fillMaxSize()) {
    LazyVerticalGrid(
      columns = GridCells.Fixed(2),
      modifier =
        Modifier.fillMaxSize()
          .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
          .drawWithContent {
            drawContent()
            drawRect(
              brush =
                Brush.verticalGradient(
                  0f to Color.Black,
                  0.8f to Color.Black,
                  1f to Color.Transparent,
                ),
              blendMode = BlendMode.DstIn,
            )
          },
      contentPadding =
        PaddingValues(bottom = paddingValues.calculateBottomPadding() + 208.dp) +
          PaddingValues(horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      if (currentTrip != null) {
        stickyHeader {
          SectionHeader(
            title = currentTrip.title,
            isExpandable = true,
            isExpanded = isCurrentExpanded,
            onToggle = { isCurrentExpanded = !isCurrentExpanded },
          )
        }

        item(key = "current_trip_content", span = { GridItemSpan(2) }) {
          AnimatedVisibility(
            visible = isCurrentExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
          ) {
            Column(modifier = Modifier.padding(bottom = 16.dp)) {
              TripCard(
                trip = currentTrip,
                isSwipedOff = tripToDelete == currentTrip,
                useHorizontalLayout = foldableBreakpoint,
                showTitle = false,
                onClick = { onTripClick(currentTrip.id) },
                onSwipeToDelete = { onSwipeToDelete(currentTrip) },
              )
            }
          }
        }
      }

      if (upcomingTrips.isNotEmpty()) {
        stickyHeader {
          SectionHeader(
            title = "Upcoming",
            isExpandable = true,
            isExpanded = isUpcomingExpanded,
            onToggle = { isUpcomingExpanded = !isUpcomingExpanded },
          )
        }
        item(key = "upcoming_trips_content", span = { GridItemSpan(2) }) {
          AnimatedVisibility(
            visible = isUpcomingExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
          ) {
            Column(
              modifier = Modifier.padding(bottom = 16.dp),
              verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
              val columns = if (foldableBreakpoint) 2 else 1
              upcomingTrips.chunked(columns).forEach { rowTrips ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                  rowTrips.forEach { trip ->
                    Box(modifier = Modifier.weight(1f)) {
                      TripCard(
                        trip = trip,
                        isSwipedOff = tripToDelete == trip,
                        useHorizontalLayout = false,
                        onClick = { onTripClick(trip.id) },
                        onSwipeToDelete = { onSwipeToDelete(trip) },
                      )
                    }
                  }
                  if (rowTrips.size < columns) {
                    Spacer(modifier = Modifier.weight(1f))
                  }
                }
              }
            }
          }
        }
      }

      if (pastTrips.isNotEmpty()) {
        stickyHeader {
          SectionHeader(
            title = "Past",
            isExpandable = true,
            isExpanded = isPastExpanded,
            onToggle = { isPastExpanded = !isPastExpanded },
          )
        }
        item(key = "past_trips_content", span = { GridItemSpan(2) }) {
          AnimatedVisibility(
            visible = isPastExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
          ) {
            Column(
              modifier = Modifier.padding(bottom = 16.dp),
              verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
              val columns = if (foldableBreakpoint) 2 else 1
              pastTrips.chunked(columns).forEach { rowTrips ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                  rowTrips.forEach { trip ->
                    Box(modifier = Modifier.weight(1f)) {
                      TripCard(
                        trip = trip,
                        isSwipedOff = tripToDelete == trip,
                        useHorizontalLayout = false,
                        onClick = { onTripClick(trip.id) },
                        onSwipeToDelete = { onSwipeToDelete(trip) },
                      )
                    }
                  }
                  if (rowTrips.size < columns) {
                    Spacer(modifier = Modifier.weight(1f))
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
