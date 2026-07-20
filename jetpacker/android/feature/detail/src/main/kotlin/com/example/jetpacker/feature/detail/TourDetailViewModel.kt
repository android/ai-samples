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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jetpacker.data.itinerary.TourDetailDao
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for loading and exposing details of custom guided tour events.
 */
@HiltViewModel
open class TourDetailViewModel
@Inject
constructor(savedStateHandle: SavedStateHandle, private val tourDetailDao: TourDetailDao) :
  ViewModel() {
  private val _uiState = MutableStateFlow(TourDetailUiState())
  open val uiState: StateFlow<TourDetailUiState> = _uiState.asStateFlow()

  private var eventId: String = savedStateHandle["eventId"] ?: ""

  init {
    loadDetail(eventId)
  }

  fun loadDetail(id: String) {
    if (id.isNotEmpty()) {
      eventId = id
    }
    viewModelScope.launch {
      if (eventId.isNotEmpty()) {
        tourDetailDao.getTourDetailByEventId(eventId).collectLatest { detail ->
          if (detail != null) {
            _uiState.update {
              TourDetailUiState(
                title = detail.title,
                type = detail.type,
                imageRes = detail.imageRes,
                date = detail.date,
                time = detail.time,
                locationName = detail.locationName,
                locationAddress = detail.locationAddress,
                about = detail.about,
                meetingPoint = detail.meetingPoint,
                notes = detail.notes,
              )
            }
          }
        }
      }
    }
  }
}
