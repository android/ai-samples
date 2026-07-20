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
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.KingBed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Pool
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
import com.example.jetpacker.core.ui.SekuyaFontFamily

/**
 * Composable screen showing detailed hotel reservation details, including address,
 * ratings, room types, check-in/out times, and support triggers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotelDetailScreen(
  eventId: String? = null,
  onBack: () -> Unit,
  onOpenHotelChat: (hotelName: String, language: String) -> Unit = { _, _ -> },
  viewModel: HotelDetailViewModel = hiltViewModel(),
) {
  LaunchedEffect(eventId) { eventId?.let { viewModel.loadDetail(it) } }
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  HotelDetailScreen(onBack = onBack, onOpenHotelChat = onOpenHotelChat, uiState = uiState)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotelDetailScreen(
  onBack: () -> Unit,
  onOpenHotelChat: (hotelName: String, language: String) -> Unit = { _, _ -> },
  uiState: HotelDetailUiState,
) {
  val hotelName = uiState.hotelName ?: ""
  val location = uiState.location ?: ""
  val rating = uiState.rating ?: ""
  val ratingCount = uiState.ratingCount ?: ""
  val pricePerNight = uiState.pricePerNight ?: ""
  val checkInDate = uiState.checkInDate ?: ""
  val checkInTime = uiState.checkInTime ?: ""
  val checkOutDate = uiState.checkOutDate ?: ""
  val checkOutTime = uiState.checkOutTime ?: ""
  val roomType = uiState.roomType ?: ""
  val guests = uiState.guests ?: ""
  val address = uiState.address ?: ""
  val phone = uiState.phone ?: ""
  val language = uiState.language ?: ""
  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              hotelName,
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
                Icons.Default.Hotel,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(28.dp),
              )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
              Text(
                hotelName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  Icons.Default.LocationOn,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = location,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  style = MaterialTheme.typography.bodyMedium,
                )
              }
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
                " ($ratingCount)",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
              )
            }
            Box(
              modifier =
                Modifier.clip(RoundedCornerShape(8.dp))
                  .background(MaterialTheme.colorScheme.tertiary)
                  .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Text(
                "Booked",
                color = MaterialTheme.colorScheme.onTertiary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))
          Text(
            pricePerNight,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
          )
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
              Icons.AutoMirrored.Filled.EventNote,
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
            HotelReservationItem(
              icon = Icons.AutoMirrored.Filled.EventNote,
              label = "Check-in",
              value = "$checkInDate\n($checkInTime)",
              color = MaterialTheme.colorScheme.tertiary,
              modifier = Modifier.weight(1f),
            )
            HotelReservationItem(
              icon = Icons.AutoMirrored.Filled.EventNote,
              label = "Check-out",
              value = "$checkOutDate\n($checkOutTime)",
              color = MaterialTheme.colorScheme.tertiary,
              modifier = Modifier.weight(1f),
            )
          }
          Spacer(modifier = Modifier.height(16.dp))
          Row(modifier = Modifier.fillMaxWidth()) {
            HotelReservationItem(
              icon = Icons.Default.KingBed,
              label = "Room Type",
              value = roomType,
              color = MaterialTheme.colorScheme.tertiary,
              modifier = Modifier.weight(2f),
            )
            HotelReservationItem(
              icon = Icons.Default.Person,
              label = "Guests",
              value = guests,
              color = MaterialTheme.colorScheme.tertiary,
              modifier = Modifier.weight(1f),
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Interactive Location
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
              Icons.Default.Map,
              contentDescription = "Location",
              tint = MaterialTheme.colorScheme.tertiary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              "LOCATION & CONTACT",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.tertiary,
              letterSpacing = 1.sp,
              fontFamily = SekuyaFontFamily,
            )
          }
          Spacer(modifier = Modifier.height(16.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier =
                Modifier.size(40.dp)
                  .clip(RoundedCornerShape(8.dp))
                  .background(MaterialTheme.colorScheme.surfaceContainerLowest),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                Icons.Default.LocationOn,
                contentDescription = "Location",
                tint = MaterialTheme.colorScheme.tertiary,
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              address,
              fontSize = 14.sp,
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.weight(1f),
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Contact / Manage
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
            onClick = { onOpenHotelChat(hotelName, language) },
            colors =
              ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
              ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
          ) {
            Text("Chat with staff")
          }
        }
      }
    }
  }
}

@Composable
fun HotelReservationItem(
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
fun HotelDetailScreenPreview() {
  HotelDetailScreen(
    onBack = {},
    uiState =
      HotelDetailUiState(
        hotelName = "JetPacker Resort",
        location = "Paris",
        rating = "4.9",
        ratingCount = "1.2k",
        pricePerNight = "$350/night",
        checkInDate = "May 10",
        checkInTime = "2:00 PM",
        checkOutDate = "May 15",
        checkOutTime = "11:00 AM",
        roomType = "Deluxe King Suite",
        guests = "2 Adults",
        address = "123 Champs-Élysées, Paris",
        phone = "+33 1 99 00 51 12",
        language = "",
      ),
  )
}
