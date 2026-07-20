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

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import androidx.lifecycle.SavedStateHandle
import com.example.jetpacker.core.ui.R
import com.example.jetpacker.core.ui.JetPackerTheme
import com.example.jetpacker.data.itinerary.TourDetail
import com.example.jetpacker.data.itinerary.TourDetailDao
import com.example.jetpacker.data.trips.DummyData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

class DetailScreenshotTest {
  @PreviewTest
  @Preview(showBackground = true)
  @Composable
  fun FlightDetailScreenshotPreview() {
    val f = DummyData.flightDetails.first()
    JetPackerTheme {
      FlightDetailScreen(
        onBack = {},
        uiState =
          FlightDetailUiState(
            flightNumber = "${f.airline} ${f.flightNum}",
            route = "${f.origin} to ${f.destination}",
            departureCode = f.origin,
            departureCity = "Origin",
            departureTime = "10:00 AM",
            arrivalCode = f.destination,
            arrivalCity = "Destination",
            arrivalTime = "06:00 PM",
            date = "May 15, 2026",
            duration = f.duration,
            departureTerminal = f.departureTerminal ?: "",
            departureGate = f.gate ?: "",
            arrivalTerminal = f.arrivalTerminal ?: "",
            arrivalGate = f.arrivalGate ?: "",
            boardingTime = "09:15 AM",
            passenger = "Sarah J. Chen",
            seat = f.seat ?: "",
            cabin = "Business",
            bookingRef = "VERNE123",
          ),
      )
    }
  }

  @PreviewTest
  @Preview(showBackground = true)
  @Composable
  fun HotelDetailScreenshotPreview() {
    val h = DummyData.hotelDetails.first()
    JetPackerTheme {
      HotelDetailScreen(
        onBack = {},
        uiState =
          HotelDetailUiState(
            hotelName = h.name,
            location = h.address,
            rating = h.rating ?: "4.9",
            ratingCount = h.ratingCount ?: "1.0k",
            pricePerNight = h.pricePerNight ?: "$300",
            checkInDate = "May 15",
            checkInTime = "3:00 PM",
            checkOutDate = "May 20",
            checkOutTime = "11:00 AM",
            roomType = "Deluxe Room",
            guests = h.guests ?: "2 Guests",
            address = h.address,
            phone = h.phone ?: "+1 555 0199",
            language = "",
          ),
      )
    }
  }

  @PreviewTest
  @Preview(showBackground = true)
  @Composable
  fun RestaurantDetailScreenshotPreview() {
    val d = DummyData.diningDetails.first()
    JetPackerTheme {
      RestaurantDetailScreen(
        onBack = {},
        uiState =
          RestaurantDetailUiState(
            restaurantName = d.restaurantName,
            cuisineType = "Fine Dining",
            rating = d.rating ?: "4.8",
            reviewCount = d.reviewCount ?: "500",
            priceRange = d.priceRange ?: "$$$",
            date = "May 16, 2026",
            time = "7:00 PM",
            guests = "${d.partySize}",
            reservationName = "Sarah J. Chen",
            address = d.address,
            phone = d.phone ?: "+1 555 0199",
          ),
      )
    }
  }

  @PreviewTest
  @Preview(showBackground = true)
  @Composable
  fun TourDetailScreenshotPreview() {
    val fakeTourDetailDao =
      object : TourDetailDao {
        override fun getTourDetailByEventId(eventId: String): Flow<TourDetail?> = emptyFlow()

        override fun getTourDetailById(id: String): Flow<TourDetail?> = emptyFlow()

        override suspend fun insertTourDetail(tourDetail: TourDetail) {}

        override suspend fun insertTourDetails(tourDetails: List<TourDetail>) {}

        override suspend fun deleteAllTourDetails() {}
      }

    val savedStateHandle = SavedStateHandle(mapOf("eventId" to "event4"))

    val fakeViewModel =
      object : TourDetailViewModel(savedStateHandle, fakeTourDetailDao) {
        private val dummyTour = DummyData.tourDetails.first()
        private val state =
          MutableStateFlow(
            TourDetailUiState(
              title = dummyTour.title,
              type = dummyTour.type,
              imageRes = dummyTour.imageRes ?: R.drawable.img_louvre,
              date = dummyTour.date,
              time = dummyTour.time,
              locationName = dummyTour.locationName,
              locationAddress = dummyTour.locationAddress,
              about = dummyTour.about ?: "",
              meetingPoint = dummyTour.meetingPoint ?: "",
              notes = dummyTour.notes,
            )
          )
        override val uiState: StateFlow<TourDetailUiState> = state
      }

    JetPackerTheme {
      TourDetailScreen(onBack = {}, viewModel = fakeViewModel)
    }
  }
}
