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

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Museum
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.jetpacker.core.flags.FeatureFlags
import com.example.jetpacker.core.ui.JetPackerTheme
import com.example.jetpacker.core.ui.SekuyaFontFamily

@OptIn(ExperimentalMaterial3Api::class)

/**
 * Composable screen showing detailed museum visit information, including exhibits,
 * address, operating hours, prices, ratings, and triggers for an on-device AI assistant.
 */
@Composable
fun MuseumDetailScreen(
  eventId: String? = null,
  onBack: () -> Unit = {},
  onOpenAssistant: (String) -> Unit = { _ -> },
  viewModel: MuseumDetailViewModel = hiltViewModel(),
) {
  LaunchedEffect(eventId) { eventId?.let { viewModel.loadDetail(it) } }
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              uiState.title ?: "",
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
              Icons.AutoMirrored.Rounded.ArrowBack,
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
  ) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .verticalScroll(rememberScrollState())
          .padding(16.dp)
    ) {
      // Top Image Card
      if ((uiState.imageRes ?: 0) != 0) {
        Card(
          shape = RoundedCornerShape(24.dp),
          colors =
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
          elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Image(
            painter = painterResource(id = uiState.imageRes ?: 0),
            contentDescription = uiState.title ?: "",
            modifier = Modifier.fillMaxWidth().height(200.dp),
            contentScale = ContentScale.Crop,
          )
        }
        Spacer(modifier = Modifier.height(8.dp))
      }

      // Main Info Card
      Card(
        shape = RoundedCornerShape(24.dp),
        colors =
          CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
                Icons.Default.Museum,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(28.dp),
              )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
              Text(
                uiState.title ?: "",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                "MUSEUM",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary,
                letterSpacing = 1.sp,
              )
            }
          }

          Spacer(modifier = Modifier.height(24.dp))

          // Date and Time Row
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              Icons.Rounded.CalendarToday,
              contentDescription = "Date",
              tint = MaterialTheme.colorScheme.tertiary,
              modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
              Text(
                uiState.date ?: "",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                uiState.time ?: "",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // About Card
      Card(
        shape = RoundedCornerShape(24.dp),
        colors =
          CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(24.dp)) {
          Text(
            "ABOUT THE MUSEUM",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.tertiary,
            letterSpacing = 1.sp,
            fontFamily = SekuyaFontFamily,
          )
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = uiState.description ?: "",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 24.sp,
          )

          Spacer(modifier = Modifier.height(24.dp))

          // Extra details
          if (!uiState.rating.isNullOrEmpty()) {
            InfoRow(
              icon = Icons.Rounded.Star,
              label = "Rating",
              value = uiState.rating ?: "",
              iconColor = Color(0xFFFFB300),
            )
          }
          if (!uiState.openingHours.isNullOrEmpty()) {
            InfoRow(
              icon = Icons.Rounded.Schedule,
              label = "Opening Hours",
              value = uiState.openingHours ?: "",
            )
          }
          if (!uiState.admissionPrice.isNullOrEmpty()) {
            InfoRow(
              icon = Icons.Rounded.Payments,
              label = "Admission",
              value = uiState.admissionPrice ?: "",
            )
          }
          if (!uiState.ticketWebsite.isNullOrEmpty()) {
            InfoRow(
              icon = Icons.Rounded.Language,
              label = "Tickets",
              value = uiState.ticketWebsite ?: "",
            )
          }
          if (!uiState.phone.isNullOrEmpty()) {
            InfoRow(icon = Icons.Rounded.Phone, label = "Phone", value = uiState.phone ?: "")
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Location Card
      Card(
        shape = RoundedCornerShape(24.dp),
        colors =
          CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(24.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              Icons.Rounded.LocationOn,
              contentDescription = "Location",
              tint = MaterialTheme.colorScheme.tertiary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              "LOCATION",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.tertiary,
              letterSpacing = 1.sp,
              fontFamily = SekuyaFontFamily,
            )
          }
          Spacer(modifier = Modifier.height(16.dp))
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFE0E0E0)),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              Icons.Rounded.Map,
              contentDescription = "Map Placeholder",
              tint = Color.Gray,
              modifier = Modifier.size(48.dp),
            )
          }
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            uiState.location ?: "",
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            uiState.address ?: "",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

    }
  }
}

@Composable
fun InfoRow(
  icon: ImageVector,
  label: String,
  value: String,
  iconColor: Color = MaterialTheme.colorScheme.tertiary,
) {
  Row(
    modifier = Modifier.padding(vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
    Spacer(modifier = Modifier.width(16.dp))
    Column {
      Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Text(
        value,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MuseumDetailScreenPreview() {
  JetPackerTheme { MuseumDetailScreen() }
}
