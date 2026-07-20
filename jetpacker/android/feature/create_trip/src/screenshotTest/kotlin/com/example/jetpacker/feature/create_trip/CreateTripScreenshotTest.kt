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

package com.example.jetpacker.feature.create_trip

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.SavedStateHandle
import com.android.tools.screenshot.PreviewTest
import com.example.jetpacker.core.ui.JetPackerTheme
import com.example.jetpacker.data.trips.DummyData
import com.example.jetpacker.data.trips.Trip
import com.example.jetpacker.data.trips.TripDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf

class CreateTripScreenshotTest {
  @PreviewTest
  @Preview(showBackground = true)
  @Composable
  fun CreateTripScreenPreview() {
    val fakeTripDao =
      object : TripDao {
        override fun getAllTrips() = flowOf(emptyList<Trip>())
        override fun getTripById(tripId: String) = flowOf(null)
        override suspend fun insertTrip(trip: Trip) {}
        override suspend fun insertTrips(trips: List<Trip>) {}
        override suspend fun deleteTrip(trip: Trip) {}
        override suspend fun deleteAllTrips() {}
      }

    val fakeViewModel =
      object : CreateTripViewModel(SavedStateHandle(), fakeTripDao) {
        private val dummyTrip = DummyData.trips.first()
        private val state =
          MutableStateFlow(
            CreateTripUiState(
              title = dummyTrip.title,
              location = dummyTrip.location,
              startDate = "May 15, 2026",
              endDate = "May 22, 2026",
              imageUri = null,
            )
          )
        override val uiState: StateFlow<CreateTripUiState> = state

        override fun onTitleChange(title: String) {}

        override fun onLocationChange(location: String) {}

        override fun onStartDateChange(date: String) {}

        override fun onEndDateChange(date: String) {}

        override fun onImageUriChange(uri: String?) {}

        override fun createTrip() {}
      }

    val uiState by fakeViewModel.uiState.collectAsState()
    JetPackerTheme {
      CreateTripPanelContent(uiState = uiState, viewModel = fakeViewModel, onCollapse = {})
    }
  }
}
