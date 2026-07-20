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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.dp
import com.example.jetpacker.core.ui.components.JetPackerExtendedFloatingActionButton
import com.example.jetpacker.data.trips.Trip
import com.example.jetpacker.feature.home.components.TripCard

@Composable
fun TabletLayout(
  currentTrip: Trip?,
  tripToDelete: Trip?,
  onTripClick: (String) -> Unit,
  onSwipeToDelete: (Trip) -> Unit,
  onCreateClick: () -> Unit,
  pastTrips: List<Trip>,
  upcomingTrips: List<Trip>,
) {
  var isUpcomingExpanded by remember { mutableStateOf(true) }
  var isCurrentExpanded by remember { mutableStateOf(true) }
  var isPastExpanded by remember { mutableStateOf(true) }

  LazyColumn(
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
    contentPadding = PaddingValues(bottom = 120.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    if (currentTrip != null) {
      stickyHeader {
        var stickiness by remember { mutableStateOf(0f) }
        val containerColor = MaterialTheme.colorScheme.surfaceContainer

        Row(
          modifier =
            Modifier.fillMaxWidth()
              .onGloballyPositioned { coordinates ->
                val y = coordinates.positionInParent().y
                stickiness = (1f - (y / 100f)).coerceIn(0f, 1f)
              }
              .padding(horizontal = 128.dp)
              .padding(top = 32.dp, bottom = 24.dp),
          horizontalArrangement = Arrangement.spacedBy(32.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          SectionHeader(
            modifier =
              Modifier.weight(1f)
                .background(containerColor.copy(alpha = stickiness), RoundedCornerShape(24.dp)),
            title = currentTrip.title,
            isExpandable = true,
            isExpanded = isCurrentExpanded,
            onToggle = { isCurrentExpanded = !isCurrentExpanded },
            drawBackground = false,
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
          )

          JetPackerExtendedFloatingActionButton(
            icon = Icons.Rounded.Add,
            label = "Create trip",
            onClick = onCreateClick,
          )
        }
      }

      item(key = "current_trip_content") {
        AnimatedVisibility(
          visible = isCurrentExpanded,
          enter = expandVertically() + fadeIn(),
          exit = shrinkVertically() + fadeOut(),
        ) {
          Box(modifier = Modifier.padding(horizontal = 128.dp).padding(bottom = 16.dp)) {
            TripCard(
              trip = currentTrip,
              useHorizontalLayout = true,
              isSwipedOff = tripToDelete == currentTrip,
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
        var stickiness by remember { mutableStateOf(0f) }
        val containerColor = MaterialTheme.colorScheme.surfaceContainer

        SectionHeader(
          title = "Upcoming",
          isExpandable = true,
          drawBackground = false,
          isExpanded = isUpcomingExpanded,
          onToggle = { isUpcomingExpanded = !isUpcomingExpanded },
          modifier =
            Modifier.onGloballyPositioned { coordinates ->
                val y = coordinates.positionInParent().y
                stickiness = (1f - (y / 100f)).coerceIn(0f, 1f)
              }
              .padding(horizontal = 128.dp, vertical = 16.dp)
              .background(containerColor.copy(alpha = stickiness), RoundedCornerShape(24.dp)),
          contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
        )
      }
      item(key = "upcoming_trips_content") {
        AnimatedVisibility(
          visible = isUpcomingExpanded,
          enter = expandVertically() + fadeIn(),
          exit = shrinkVertically() + fadeOut(),
        ) {
          LazyRow(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 128.dp),
          ) {
            itemsIndexed(upcomingTrips, key = { _, trip -> trip.id }) { _, trip ->
              TripCard(
                modifier = Modifier.width(504.dp),
                trip = trip,
                useHorizontalLayout = false,
                canSwipeToDelete = false,
                isSwipedOff = tripToDelete == trip,
                onClick = { onTripClick(trip.id) },
                onSwipeToDelete = { onSwipeToDelete(trip) },
              )
            }
          }
        }
      }

      if (pastTrips.isNotEmpty()) {
        stickyHeader {
          var stickiness by remember { mutableStateOf(0f) }
          val containerColor = MaterialTheme.colorScheme.surfaceContainer

          SectionHeader(
            title = "Past",
            isExpandable = true,
            drawBackground = false,
            isExpanded = isPastExpanded,
            onToggle = { isPastExpanded = !isPastExpanded },
            modifier =
              Modifier.onGloballyPositioned { coordinates ->
                  val y = coordinates.positionInParent().y
                  stickiness = (1f - (y / 100f)).coerceIn(0f, 1f)
                }
                .padding(horizontal = 128.dp, vertical = 16.dp)
                .background(containerColor.copy(alpha = stickiness), RoundedCornerShape(24.dp)),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
          )
        }
        item(key = "past_trips_content") {
          AnimatedVisibility(
            visible = isPastExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
          ) {
            LazyRow(
              modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
              horizontalArrangement = Arrangement.spacedBy(16.dp),
              contentPadding = PaddingValues(horizontal = 128.dp),
            ) {
              itemsIndexed(pastTrips, key = { _, trip -> trip.id }) { _, trip ->
                TripCard(
                  modifier = Modifier.width(504.dp),
                  trip = trip,
                  useHorizontalLayout = false,
                  canSwipeToDelete = false,
                  isSwipedOff = tripToDelete == trip,
                  onClick = { onTripClick(trip.id) },
                  onSwipeToDelete = { onSwipeToDelete(trip) },
                )
              }
            }
          }
        }
      }
    }
  }
}
