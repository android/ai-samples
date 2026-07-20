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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Luggage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.jetpacker.core.ui.JetPackerTheme
import com.example.jetpacker.core.ui.R as CoreUiR
import com.example.jetpacker.core.ui.SekuyaFontFamily

/**
 * Composable screen representing detailed boarding pass, flight times, gates, and seat assignment
 * for a specific flight event.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightDetailScreen(
  eventId: String? = null,
  onBack: () -> Unit,
  viewModel: FlightDetailViewModel = hiltViewModel(),
) {
  LaunchedEffect(eventId) { eventId?.let { viewModel.loadDetail(it) } }
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  FlightDetailScreen(onBack = onBack, uiState = uiState)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightDetailScreen(onBack: () -> Unit, uiState: FlightDetailUiState) {
  var showFullScreenQr by remember { mutableStateOf(false) }
  val flightNumber = uiState.flightNumber ?: ""
  val route = uiState.route ?: ""
  val departureCode = uiState.departureCode ?: ""
  val departureCity = uiState.departureCity ?: ""
  val departureTime = uiState.departureTime ?: ""
  val arrivalCode = uiState.arrivalCode ?: ""
  val arrivalCity = uiState.arrivalCity ?: ""
  val arrivalTime = uiState.arrivalTime ?: ""
  val date = uiState.date ?: ""
  val duration = uiState.duration ?: ""
  val departureTerminal = uiState.departureTerminal ?: ""
  val departureGate = uiState.departureGate ?: ""
  val arrivalTerminal = uiState.arrivalTerminal ?: ""
  val arrivalGate = uiState.arrivalGate ?: ""
  val boardingTime = uiState.boardingTime ?: ""
  val passenger = uiState.passenger ?: ""
  val seat = uiState.seat ?: ""
  val cabin = uiState.cabin ?: ""
  val bookingRef = uiState.bookingRef ?: ""
  val aircraft = uiState.aircraft ?: ""
  val baggageAllowance = uiState.baggageAllowance ?: ""
  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              flightNumber,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 20.sp,
              fontFamily = SekuyaFontFamily,
            )
          }
        },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            titleContentColor = MaterialTheme.colorScheme.tertiary,
          ),
      )
    },
    containerColor = MaterialTheme.colorScheme.surface,
  ) { padding ->
    val outlineVariantColor = MaterialTheme.colorScheme.outlineVariant
    Column(
      modifier =
        Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
      // Top Flight Header
      Card(
        shape = RoundedCornerShape(24.dp),
        colors =
          CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(24.dp)) {
          // Airline & Number
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(
              modifier =
                Modifier.size(48.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .background(MaterialTheme.colorScheme.surfaceContainerLowest),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                Icons.Default.Flight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(28.dp),
              )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
              Text(
                flightNumber,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                route,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          Spacer(modifier = Modifier.height(24.dp))

          // Flight Path
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(horizontalAlignment = Alignment.Start) {
              Text(
                departureCode,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                departureCity,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                departureTime,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.tertiary,
              )
            }

            // Dotted path with plane
            Box(
              modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
              contentAlignment = Alignment.Center,
            ) {
              Canvas(modifier = Modifier.fillMaxWidth().height(2.dp)) {
                drawLine(
                  color = outlineVariantColor,
                  start = Offset(0f, size.height / 2),
                  end = Offset(size.width, size.height / 2),
                  strokeWidth = 4f,
                  pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f),
                )
              }
              Icon(
                imageVector = Icons.Default.Flight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier =
                  Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 8.dp),
              )
            }

            Column(horizontalAlignment = Alignment.End) {
              Text(
                arrivalCode,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                arrivalCity,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                arrivalTime,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.tertiary,
              )
            }
          }

          Spacer(modifier = Modifier.height(24.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              date,
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              duration,
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Flight Information Details
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
              Icons.Default.Flight,
              contentDescription = "Flight Information",
              tint = MaterialTheme.colorScheme.tertiary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              "FLIGHT INFORMATION",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.tertiary,
              letterSpacing = 1.sp,
              fontFamily = SekuyaFontFamily,
            )
          }
          Spacer(modifier = Modifier.height(16.dp))

          Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                "DEPARTURE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                departureTerminal,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                "Gate",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                departureGate,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }

            Column(
              modifier = Modifier.weight(1f),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Text("", style = MaterialTheme.typography.labelSmall, color = Color.Transparent)
              Text("", style = MaterialTheme.typography.bodyMedium, color = Color.Transparent)
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                "Boarding",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                boardingTime,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.tertiary,
              )
            }

            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
              Text(
                "ARRIVAL",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                arrivalTerminal,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                "Gate",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                arrivalGate,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
          }

          Spacer(modifier = Modifier.height(16.dp))
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          Spacer(modifier = Modifier.height(16.dp))

          Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(2f)) {
              Text(
                "PASSENGER",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                passenger,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
            Column(modifier = Modifier.weight(1f)) {
              Text(
                "Seat",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Row(verticalAlignment = Alignment.Bottom) {
                Text(
                  seat,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  cabin,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))
          Text(
            "BOOKING REF: $bookingRef",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Aircraft & Amenities
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
              Icons.Rounded.Build,
              contentDescription = "Aircraft & Amenities",
              tint = MaterialTheme.colorScheme.tertiary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              "AIRCRAFT & AMENITIES",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.tertiary,
              letterSpacing = 1.sp,
              fontFamily = SekuyaFontFamily,
            )
          }
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            aircraft,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            "• In-flight Wi-Fi Available",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            "• Individual Power Outlets & USB Ports",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            "• Complimentary Snacks & Non-Alcoholic Beverages",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Baggage Information
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
              Icons.Rounded.Luggage,
              contentDescription = "Baggage Allowance",
              tint = MaterialTheme.colorScheme.tertiary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              "BAGGAGE ALLOWANCE",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.tertiary,
              letterSpacing = 1.sp,
              fontFamily = SekuyaFontFamily,
            )
          }
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            baggageAllowance,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            "Checked baggage cannot exceed 158cm in overall dimensions (length + width + height). Hand luggage must be capable of fitting into the overhead bin.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Boarding Pass QR
      Card(
        shape = RoundedCornerShape(24.dp),
        colors =
          CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Image(
            painter = painterResource(id = CoreUiR.drawable.qr),
            contentDescription = "QR Code",
            modifier = Modifier.size(120.dp).clickable { showFullScreenQr = true },
          )

          Spacer(modifier = Modifier.height(16.dp))
          Text(
            "BOARDING PASS",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = SekuyaFontFamily,
          )
          Text(
            "Scan at gate",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }

  if (showFullScreenQr) {
    Dialog(onDismissRequest = { showFullScreenQr = false }) {
      Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.size(320.dp).clickable { showFullScreenQr = false },
      ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
          Image(
            painter = painterResource(id = CoreUiR.drawable.qr),
            contentDescription = "QR Code",
            modifier = Modifier.size(260.dp),
          )
        }
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
fun FlightDetailScreenPreview() {
  JetPackerTheme {
    FlightDetailScreen(
      onBack = {},
      uiState =
        FlightDetailUiState(
          flightNumber = "AF 1234",
          route = "Paris (CDG) to New York (JFK)",
          departureCode = "CDG",
          departureCity = "Paris",
          departureTime = "10:30 AM",
          arrivalCode = "JFK",
          arrivalCity = "New York",
          arrivalTime = "1:15 PM",
          date = "May 15, 2026",
          duration = "7h 45m",
          departureTerminal = "Terminal 2E",
          departureGate = "M42",
          arrivalTerminal = "Terminal 4",
          arrivalGate = "B23",
          boardingTime = "09:45 AM",
          passenger = "Sarah J. Chen",
          seat = "12A",
          cabin = "Business",
          bookingRef = "VERNE123",
        ),
    )
  }
}
