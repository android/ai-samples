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

package com.example.jetpacker.feature.booking_assistant

import androidx.a2ui.model.processor.A2uiSurfaceModel
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.jetpacker.core.ui.SekuyaFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingAssistantScreen(
  tripId: String,
  contentPadding: PaddingValues,
  onBack: () -> Unit = {},
  viewModel: BookingAssistantViewModel = hiltViewModel(),
  modifier: Modifier = Modifier,
) {
  val activeSurfaces by viewModel.activeSurfaces.collectAsStateWithLifecycle()
  val isStreaming by viewModel.isStreaming.collectAsStateWithLifecycle()
  val surfaceStatuses by viewModel.surfaceStatuses.collectAsStateWithLifecycle()

  val groupedSurfaces = remember(activeSurfaces) {
    activeSurfaces
      .groupBy { BookingCategory.fromCategoryName(viewModel.getCategory(it.id)) }
      .toSortedMap(compareBy { it.order })
  }

  var expandedCategories by remember {
    mutableStateOf(emptySet<BookingCategory>())
  }

  LaunchedEffect(tripId) {
    viewModel.startBooking(tripId)
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.primary,
    topBar = {
      Column(modifier = Modifier.background(MaterialTheme.colorScheme.primary)) {
        TopAppBar(
          title = {
            Text(
              text = "MANAGE BOOKINGS",
              style =
                MaterialTheme.typography.titleLarge.copy(
                  fontFamily = SekuyaFontFamily,
                  fontSize = 24.sp,
                  letterSpacing = 0.5.sp,
                ),
              color = Color(0xFF1E293B),
            )
          },
          navigationIcon = {
            IconButton(onClick = onBack) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = Color(0xFF1E293B),
              )
            }
          },
          actions = {
            IconButton(onClick = { viewModel.resetBookings() }) {
              Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = "Reset Bookings",
                tint = Color(0xFF1E293B).copy(alpha = 0.4f),
              )
            }
          },
          colors =
            TopAppBarDefaults.topAppBarColors(
              containerColor = MaterialTheme.colorScheme.primary,
            ),
        )
        if (isStreaming) {
          LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = Color.Transparent,
          )
        }
      }
    },
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
      contentPadding =
        PaddingValues(
          top = innerPadding.calculateTopPadding() + 8.dp,
          bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item {
        Column(
          modifier =
            Modifier
              .fillMaxWidth()
              .padding(top = 4.dp, bottom = 8.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text(
            text = "Current Itinerary •",
            style =
              MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
              ),
            color = Color(0xFF1E293B),
          )
          Text(
            text = "YOUR NEXT JOURNEY",
            style =
              MaterialTheme.typography.headlineMedium.copy(
                fontFamily = SekuyaFontFamily,
                fontSize = 24.sp,
                letterSpacing = 0.5.sp,
              ),
            color = Color(0xFF1E293B),
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text =
              "Review and finalize your travel segments. We've organized your selections for a seamless booking.",
            style =
              MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                lineHeight = 20.sp,
              ),
            color = Color(0xFF334155),
          )
        }
      }

      if (activeSurfaces.isEmpty()) {
        item {
          Box(
            modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
            contentAlignment = Alignment.Center,
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
              Text(
                text = "No active booking surfaces.\nPress 'Start Booking' to initialize.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Button(
                onClick = { viewModel.resetBookings() },
                shape = RoundedCornerShape(12.dp),
              ) {
                Text("Start Booking", fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      } else {
        groupedSurfaces.forEach { (category, surfaces) ->
          item(key = category.name) {
            CategoryCard(
              category = category,
              surfaces = surfaces,
              subtitle = viewModel.getCategorySubtitle(surfaces),
              isExpanded = expandedCategories.contains(category),
              onToggleExpand = {
                expandedCategories =
                  if (expandedCategories.contains(category)) {
                    expandedCategories - category
                  } else {
                    expandedCategories + category
                  }
              },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun CategoryCard(
  category: BookingCategory,
  surfaces: List<A2uiSurfaceModel>,
  subtitle: String,
  isExpanded: Boolean,
  onToggleExpand: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(28.dp),
    colors =
      CardDefaults.cardColors(
        containerColor = Color(0xFFFFFDF5),
      ),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .padding(20.dp),
    ) {
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .clickable(
              interactionSource = remember { MutableInteractionSource() },
              indication = null,
              onClick = onToggleExpand,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Box(
          modifier =
            Modifier
              .size(48.dp)
              .background(category.circleColor, CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = category.icon,
            contentDescription = category.displayName,
            tint = Color.White,
            modifier = Modifier.size(24.dp),
          )
        }

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "${category.displayName} (${surfaces.size})",
            style =
              MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
              ),
            color = Color(0xFF1E293B),
          )
          Text(
            text = subtitle,
            style =
              MaterialTheme.typography.bodySmall.copy(
                fontSize = 13.sp,
              ),
            color = Color(0xFF64748B),
          )
        }

        Box(
          modifier =
            Modifier
              .size(36.dp)
              .background(Color.White, CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector =
              if (isExpanded) Icons.Rounded.KeyboardArrowUp
              else Icons.Rounded.KeyboardArrowDown,
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            tint = Color(0xFF1E293B),
            modifier = Modifier.size(20.dp),
          )
        }
      }

      AnimatedVisibility(visible = isExpanded) {
        Column(
          modifier =
            Modifier
              .fillMaxWidth()
              .padding(top = 16.dp),
        ) {
          surfaces.forEachIndexed { index, surfaceModel ->
            if (index > 0) {
              HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = Color(0xFFE5E7EB),
                thickness = 1.dp,
              )
            }
            A2uiSurface(
              surfaceModel = surfaceModel,
              modifier = Modifier.fillMaxWidth(),
            )
          }
        }
      }
    }
  }
}

enum class BookingCategory(
  val displayName: String,
  val order: Int,
) {
  FLIGHT("Flights", 1),
  HOTEL("Hotels", 2),
  ACTIVITY("Activities", 3),
  DINING("Dining", 4),
  OTHER("Other", 5);

  val icon: ImageVector
    get() =
      when (this) {
        FLIGHT -> Icons.Rounded.Flight
        HOTEL -> Icons.Rounded.Hotel
        ACTIVITY -> Icons.Rounded.ConfirmationNumber
        DINING -> Icons.Rounded.Restaurant
        OTHER -> Icons.Rounded.ConfirmationNumber
      }

  val circleColor: Color
    get() =
      when (this) {
        FLIGHT -> Color(0xFFF05123)
        HOTEL -> Color(0xFF2563EB)
        ACTIVITY -> Color(0xFF059669)
        DINING -> Color(0xFFD97706)
        OTHER -> Color(0xFF64748B)
      }

  companion object {
    fun fromCategoryName(name: String): BookingCategory {
      return when (name.lowercase()) {
        "flight", "flights" -> FLIGHT
        "hotel", "hotels" -> HOTEL
        "activity", "activities" -> ACTIVITY
        "dining", "dining & restaurants" -> DINING
        else -> OTHER
      }
    }
  }
}
