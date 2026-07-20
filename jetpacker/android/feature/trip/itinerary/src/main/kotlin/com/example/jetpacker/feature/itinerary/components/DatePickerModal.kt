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

package com.example.jetpacker.feature.itinerary.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import com.example.jetpacker.core.flags.FeatureFlags
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Modal dialog wrapping Material3 DatePicker.
 * Relies on [FeatureFlags.OVERRIDE_CURRENT_TIME_MILLIS] to enforce bounds.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerModal(onDateSelected: (Long?) -> Unit, onDismiss: () -> Unit) {
  val datePickerState =
    rememberDatePickerState(
      selectableDates =
        object : SelectableDates {
          override fun isSelectableDate(utcTimeMillis: Long): Boolean {
            val todayUtc =
              FeatureFlags.OVERRIDE_CURRENT_TIME_MILLIS?.let {
                Instant.ofEpochMilli(it)
                  .atZone(ZoneOffset.UTC)
                  .toLocalDate()
                  .atStartOfDay(ZoneOffset.UTC)
                  .toInstant()
                  .toEpochMilli()
              }
                ?: LocalDate.now(ZoneOffset.UTC)
                  .atStartOfDay(ZoneOffset.UTC)
                  .toInstant()
                  .toEpochMilli()
            return utcTimeMillis >= todayUtc
          }
        }
    )

  DatePickerDialog(
    onDismissRequest = onDismiss,
    confirmButton = {
      TextButton(
        onClick = {
          onDateSelected(datePickerState.selectedDateMillis)
          onDismiss()
        }
      ) {
        Text("OK")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  ) {
    DatePicker(state = datePickerState)
  }
}
