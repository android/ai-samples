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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jetpacker.core.ui.SekuyaFontFamily

// A2UI imports
import androidx.a2ui.compose.runtime.observeA2uiComponentState
import androidx.a2ui.compose.runtime.A2uiComponentState
import androidx.a2ui.model.processor.A2uiSurfaceModel
import androidx.a2ui.engine.model.A2uiCoreSurfaceModel
import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.A2uiComponent

import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun BookingAssistantScreen(
    tripId: String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onBack: () -> Unit = {},
    viewModel: BookingAssistantViewModel = hiltViewModel()
) {
    val events by viewModel.events.collectAsState()
    val activeSurfaces by viewModel.activeSurfaces.collectAsState()

    LaunchedEffect(tripId) {
        viewModel.startBookingChat(tripId)
    }


    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.primary)
            .padding(contentPadding)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Custom Header Bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.manage_bookings_title),
                fontSize = 20.sp,
                fontFamily = SekuyaFontFamily,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }

        if (activeSurfaces.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp, bottom = 24.dp)
            ) {
                Text(
                    text = stringResource(R.string.current_itinerary_label),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
                Text(
                    text = stringResource(R.string.your_next_journey_title),
                    fontSize = 22.sp,
                    fontFamily = SekuyaFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.booking_assistant_description),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Connecting to booking server...",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                )
            }
        }

        // Render active booking requirement cards in a unified list
        if (activeSurfaces.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, bottom = 8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.current_itinerary_label),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                        Text(
                            text = stringResource(R.string.your_next_journey_title),
                            fontSize = 22.sp,
                            fontFamily = SekuyaFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.booking_assistant_description),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                        )
                    }
                }
                items(activeSurfaces) { surfaceModel ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            val coreSurface = surfaceModel as? A2uiCoreSurfaceModel
                            val id = coreSurface?.id ?: ""
                            
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = id,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            
                            BookingAssistantA2uiSurface(
                                surface = surfaceModel,
                                modifier = Modifier.fillMaxWidth().wrapContentHeight()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookingAssistantA2uiSurface(
    surface: A2uiSurfaceModel,
    modifier: Modifier = Modifier
) {
    val coreSurface = surface as? A2uiCoreSurfaceModel ?: return
    val state = observeA2uiComponentState(coreSurface)

    when (state) {
        is A2uiComponentState.Loading -> {
            Box(
                modifier = modifier.fillMaxWidth().height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        is A2uiComponentState.Error -> {
            Text(
                text = "Error: ${state.exception.message ?: "Unknown error"}",
                color = MaterialTheme.colorScheme.error,
                modifier = modifier
            )
        }
        is A2uiComponentState.Success -> {
            val componentModel = state.component
            val catalog = bookingAssistantCatalog()
            val component = catalog.getComponent(componentModel.type)

            if (component != null) {
                with(component) {
                    componentModel.scope.Content(
                        properties = componentModel.properties,
                        modifier = modifier
                    )
                }
            } else {
                Text(
                    text = "Unsupported component: ${componentModel.type}",
                    modifier = modifier
                )
            }
        }
    }
}
