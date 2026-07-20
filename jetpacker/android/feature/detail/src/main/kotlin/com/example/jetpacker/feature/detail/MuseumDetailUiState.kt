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

data class MuseumDetailUiState(
  val eventId: String? = null,
  val title: String? = null,
  val timestamp: Long? = null,
  val date: String? = null,
  val time: String? = null,
  val location: String? = null,
  val description: String? = null,
  val address: String? = null,
  val openingHours: String? = null,
  val admissionPrice: String? = null,
  val ticketWebsite: String? = null,
  val rating: String? = null,
  val phone: String? = null,
  val imageRes: Int? = null,
  val infoUrls: List<String>? = null
)
