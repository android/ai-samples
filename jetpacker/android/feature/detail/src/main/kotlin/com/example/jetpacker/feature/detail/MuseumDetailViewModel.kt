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
import com.example.jetpacker.data.itinerary.EventDao
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for loading and exposing the state of a specific museum visit event.
 */
@HiltViewModel
class MuseumDetailViewModel
@Inject
constructor(savedStateHandle: SavedStateHandle, private val eventDao: EventDao) : ViewModel() {

  private var eventId: String = savedStateHandle["eventId"] ?: ""

  private val _uiState = MutableStateFlow(MuseumDetailUiState())
  val uiState: StateFlow<MuseumDetailUiState> = _uiState.asStateFlow()

  init {
    loadDetail(eventId)
  }

  fun loadDetail(id: String) {
    if (id.isNotEmpty()) {
      eventId = id
    }
    viewModelScope.launch {
      if (eventId.isNotEmpty()) {
        combine(eventDao.getEventById(eventId), eventDao.getMuseumDetail(eventId)) { event, detail
            ->
            if (event != null) {
              val dateStr = SimpleDateFormat("EEEE, MMM dd", Locale.US).format(event.timestamp)
              val timeStr = SimpleDateFormat("h:mm a", Locale.US).format(event.timestamp)

              MuseumDetailUiState(
                eventId = event.id,
                title = event.title,
                timestamp = event.timestamp,
                date = dateStr,
                time = timeStr,
                location = event.location,
                description = detail?.description ?: event.description ?: "",
                address = detail?.address ?: event.location,
                openingHours = detail?.openingHours ?: "",
                admissionPrice = detail?.admissionPrice ?: "",
                ticketWebsite = detail?.ticketWebsite ?: "",
                rating = detail?.rating ?: "",
                phone = detail?.phone ?: "",
                imageRes = event.imageResList.firstOrNull() ?: 0,
                infoUrls = detail?.infoUrls
              )
            } else {
              MuseumDetailUiState()
            }
          }
          .collect { state -> _uiState.update { state } }
      }
    }
  }
}
