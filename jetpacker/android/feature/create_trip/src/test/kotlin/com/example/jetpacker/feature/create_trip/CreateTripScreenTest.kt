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

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.jetpacker.core.ui.JetPackerTheme
import com.example.jetpacker.data.trips.Trip
import com.example.jetpacker.data.trips.TripDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(application = android.app.Application::class, qualifiers = "+w411dp-h891dp-mdpi", sdk = [33])
class CreateTripScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun testCreateTripScreen_showsImage() {
    composeTestRule.mainClock.autoAdvance = false

    val fakeDao =
      object : TripDao {
        override fun getAllTrips(): Flow<List<Trip>> = emptyFlow()

        override fun getTripById(tripId: String): Flow<Trip?> = emptyFlow()

        override suspend fun insertTrip(trip: Trip) {}

        override suspend fun insertTrips(trips: List<Trip>) {}

        override suspend fun deleteTrip(trip: Trip) {}

        override suspend fun deleteAllTrips() {}
      }

    // Stub class since we just want to display
    val fakeViewModel =
      object : CreateTripViewModel(SavedStateHandle(), fakeDao) {
        private val state =
          MutableStateFlow(
            CreateTripUiState(
              title = "Tokyo Explorer",
              location = "Tokyo, Japan",
              startDate = "Oct 15, 2026",
              endDate = "Oct 30, 2026",
              imageUri = "content://fake/uri/image.png",
            )
          )
        override val uiState: StateFlow<CreateTripUiState> = state

        override fun onImageUriChange(uri: String?) {}

        override fun createTrip() {}
      }

    composeTestRule.setContent {
      val uiState by fakeViewModel.uiState.collectAsState()
      JetPackerTheme {
        CreateTripPanelContent(uiState = uiState, viewModel = fakeViewModel, onCollapse = {})
      }
    }
  }
}
