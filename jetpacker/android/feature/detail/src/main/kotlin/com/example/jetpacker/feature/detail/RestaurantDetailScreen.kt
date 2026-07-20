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

package com.example.jetpacker.feature.detail

import android.util.Log
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.jetpacker.core.flags.FeatureFlags
import com.example.jetpacker.core.ui.SekuyaFontFamily

/**
 * Composable screen showing detailed restaurant reservation details, including address,
 * ratings, cuisines, reservation time, party size, and review triggers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestaurantDetailScreen(
  eventId: String?,
  onBack: () -> Unit,
  onOpenReviewScreen: (String, String) -> Unit = { _, _ -> },
  viewModel: RestaurantDetailViewModel = hiltViewModel(),
) {
  LaunchedEffect(eventId) { eventId?.let { viewModel.loadDetail(it) } }
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  RestaurantDetailScreen(
    onBack = onBack,
    onOpenReviewScreen = onOpenReviewScreen,
    uiState = uiState,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestaurantDetailScreen(
  onBack: () -> Unit,
  onOpenReviewScreen: (String, String) -> Unit = { _, _ -> },
  uiState: RestaurantDetailUiState,
) {
  val restaurantName = uiState.restaurantName ?: ""
  val cuisineType = uiState.cuisineType ?: ""
  val rating = uiState.rating ?: ""
  val reviewCount = uiState.reviewCount ?: ""
  val priceRange = uiState.priceRange ?: ""
  val date = uiState.date ?: ""
  val time = uiState.time ?: ""
  val guests = uiState.guests ?: ""
  val reservationName = uiState.reservationName ?: ""
  val address = uiState.address ?: ""
  val placeId = uiState.placeId ?: ""
  val phone = uiState.phone ?: ""
  val primaryGreen = MaterialTheme.colorScheme.tertiary
  val surfaceLightGreen = MaterialTheme.colorScheme.surface

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              restaurantName,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.onSurface,
              fontSize = 20.sp,
              fontFamily = SekuyaFontFamily,
            )
          }
        },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(
              Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = MaterialTheme.colorScheme.onSurface,
            )
          }
        },
        colors =
          TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
      )
    },
    containerColor = MaterialTheme.colorScheme.surface,
  ) { padding ->
    Column(
      modifier =
        Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
      // Top Header Card
      Card(
        shape = RoundedCornerShape(24.dp),
        colors =
          CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(24.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(
              modifier =
                Modifier.size(48.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .background(MaterialTheme.colorScheme.surfaceContainerLowest),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                Icons.Default.Restaurant,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(28.dp),
              )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
              Text(
                restaurantName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                cuisineType,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          Spacer(modifier = Modifier.height(24.dp))

          // Quick Stats Row
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                Icons.Default.Star,
                contentDescription = "Rating",
                tint = Color(0xFFFFB300),
                modifier = Modifier.size(20.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                rating,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                " ($reviewCount)",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
              )
            }
            Text(
              priceRange,
              color = MaterialTheme.colorScheme.tertiary,
              fontWeight = FontWeight.Medium,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Reservation Details
      Card(
        shape = RoundedCornerShape(24.dp),
        colors =
          CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(24.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              Icons.AutoMirrored.Filled.MenuBook,
              contentDescription = "Reservation Details",
              tint = MaterialTheme.colorScheme.tertiary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              "RESERVATION DETAILS",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.tertiary,
              letterSpacing = 1.sp,
              fontFamily = SekuyaFontFamily,
            )
          }
          Spacer(modifier = Modifier.height(16.dp))

          Row(modifier = Modifier.fillMaxWidth()) {
            ReservationItem(
              icon = Icons.Default.Schedule,
              label = "Date & Time",
              value = "$date\n$time",
              color = MaterialTheme.colorScheme.tertiary,
              modifier = Modifier.weight(1f),
            )
            ReservationItem(
              icon = Icons.Default.Person,
              label = "Guests",
              value = guests,
              color = MaterialTheme.colorScheme.tertiary,
              modifier = Modifier.weight(1f),
            )
          }
          Spacer(modifier = Modifier.height(16.dp))
          ReservationItem(
            icon = Icons.Default.EventSeat,
            label = "Reserved Under",
            value = reservationName,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Interactive Info Map/Location
      Card(
        shape = RoundedCornerShape(24.dp),
        colors =
          CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(24.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              Icons.Default.Place,
              contentDescription = "Location",
              tint = MaterialTheme.colorScheme.tertiary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              address,
              fontSize = 14.sp,
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.weight(1f),
            )
          }
          Spacer(modifier = Modifier.height(16.dp))
          Button(
            onClick = { /* Open maps */ },
            colors =
              ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
              ),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text("Open in Maps")
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Contact / Menu
      Card(
        shape = RoundedCornerShape(24.dp),
        colors =
          CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(24.dp).fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              Icons.Default.Phone,
              contentDescription = "Call",
              tint = MaterialTheme.colorScheme.tertiary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(phone, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
          }
          Button(
            onClick = { /* Call */ },
            colors =
              ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
              ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
          ) {
            Text("Call Now")
          }
        }
      }
    }
  }
}

@Composable
fun ReservationItem(
  icon: ImageVector,
  label: String,
  value: String,
  color: Color,
  modifier: Modifier = Modifier,
) {
  Row(modifier = modifier, verticalAlignment = Alignment.Top) {
    Box(
      modifier =
        Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.1f)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
    }
    Spacer(modifier = Modifier.width(12.dp))
    Column {
      Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Text(
        value,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface,
        lineHeight = 20.sp,
      )
    }
  }
}

@Preview(showBackground = true)
@Composable
fun RestaurantDetailScreenPreview() {
  RestaurantDetailScreen(
    onBack = {},
    uiState =
      RestaurantDetailUiState(
        restaurantName = "Le Parfait",
        cuisineType = "French Fine Dining",
        rating = "4.8",
        reviewCount = "850",
        priceRange = "$$$$",
        date = "May 12, 2026",
        time = "7:30 PM",
        guests = "2",
        reservationName = "Sarah J. Chen",
        address = "45 Rue de la Paix, Paris",
        phone = "+33 1 55 66 77 88",
      ),
  )
}
