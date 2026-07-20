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

package com.example.jetpacker.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.example.jetpacker.data.itinerary.EventType
import com.example.jetpacker.feature.create_trip.CreateTripScreen
import com.example.jetpacker.feature.detail.FlightDetailScreen
import com.example.jetpacker.feature.detail.HotelDetailScreen
import com.example.jetpacker.feature.detail.MuseumDetailScreen
import com.example.jetpacker.feature.detail.RestaurantDetailScreen
import com.example.jetpacker.feature.detail.TourDetailScreen
import com.example.jetpacker.feature.home.DebugScreen
import com.example.jetpacker.feature.home.HomeScreen
import com.example.jetpacker.feature.itinerary.ItineraryScreen
import com.example.jetpacker.feature.trip.TripScreen
import kotlinx.serialization.Serializable

sealed interface Screen : NavKey {
  @Serializable data object CreateTrip : Screen
  @Serializable data object Debug : Screen
  @Serializable data class EditTrip(val tripId: String) : Screen
  @Serializable data class FlightDetail(val eventId: String) : Screen
  @Serializable data class HotelDetail(val eventId: String) : Screen
  @Serializable data class MuseumDetail(val eventId: String) : Screen
  @Serializable data object MyTrips : Screen
  @Serializable data class RestaurantDetail(val eventId: String) : Screen
  @Serializable data class Timeline(val tripId: String) : Screen
  @Serializable data class TourDetail(val eventId: String) : Screen
}

@Composable
fun JetPackerNavGraph(
  navigationState: NavigationState,
  navigator: Navigator,
) {
  val entryProvider = remember(navigator) {
    entryProvider<NavKey> {
      entry<Screen.CreateTrip> {
        CreateTripScreen(
          onBack = { navigator.goBack() },
          onTripCreated = { tripId ->
            navigator.goBack()
            navigator.navigate(Screen.Timeline(tripId))
          },
        )
      }
      entry<Screen.Debug> { DebugScreen(onBack = { navigator.goBack() }) }
      entry<Screen.EditTrip> { key ->
        CreateTripScreen(
          onBack = { navigator.goBack() },
          onTripCreated = { _ ->
            navigator.goBack()
          },
          tripIdToEdit = key.tripId,
        )
      }
      entry<Screen.FlightDetail> { key ->
        FlightDetailScreen(eventId = key.eventId, onBack = { navigator.goBack() })
      }
      entry<Screen.HotelDetail> { key ->
        HotelDetailScreen(
          eventId = key.eventId,
          onBack = { navigator.goBack() },
        )
      }
      entry<Screen.MuseumDetail> { key ->
        MuseumDetailScreen(
          eventId = key.eventId,
          onBack = { navigator.goBack() },
        )
      }
      entry<Screen.MyTrips> {
        HomeScreen(
          onTripClick = { tripId -> navigator.navigate(Screen.Timeline(tripId)) },
          onTripCreated = { tripId -> navigator.navigate(Screen.Timeline(tripId)) },
          onNavigateToDebug = { navigator.navigate(Screen.Debug) },
          onCreateTripClick = { navigator.navigate(Screen.CreateTrip) },
        )
      }
      entry<Screen.RestaurantDetail> { key ->
        RestaurantDetailScreen(
          eventId = key.eventId,
          onBack = { navigator.goBack() },
        )
      }
      entry<Screen.Timeline> { key ->
        val tripId = key.tripId
        TripScreen(
          tripId = tripId,
          onBack = { navigator.goBack() },
          onEditTripClick = { navigator.navigate(Screen.EditTrip(tripId)) },
          onEventClick = { eventId, eventType ->
            when (eventType) {
              EventType.TRANSPORTATION -> navigator.navigate(Screen.FlightDetail(eventId))
              EventType.FOOD_AND_DRINK -> navigator.navigate(Screen.RestaurantDetail(eventId))
              EventType.ACCOMMODATION -> navigator.navigate(Screen.HotelDetail(eventId))
              EventType.CULTURE -> navigator.navigate(Screen.MuseumDetail(eventId))
              else -> navigator.navigate(Screen.TourDetail(eventId))
            }
          },
          onNavigateToDebug = { navigator.navigate(Screen.Debug) },
        )
      }
      entry<Screen.TourDetail> { key ->
        TourDetailScreen(eventId = key.eventId, onBack = { navigator.goBack() })
      }
    }
  }

  NavDisplay(
    backStack = navigationState,
    onBack = { navigator.goBack() },
    entryProvider = entryProvider,
  )
}
