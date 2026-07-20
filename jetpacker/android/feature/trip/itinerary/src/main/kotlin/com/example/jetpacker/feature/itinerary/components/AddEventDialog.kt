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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.jetpacker.data.itinerary.EventType

/**
 * Dialog for adding a new event to the itinerary.
 * Supports dynamic fields based on selected [EventType].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventDialog(
  onDismiss: () -> Unit,
  onSave:
    (
      type: EventType,
      title: String,
      location: String,
      time: String,
      extraFields: Map<String, String>,
    ) -> Unit,
) {
  var type by remember { mutableStateOf(EventType.ACTIVITY) }
  var title by remember { mutableStateOf("") }
  var location by remember { mutableStateOf("") }
  var time by remember { mutableStateOf("") }

  // Extra fields based on type
  var airline by remember { mutableStateOf("") }
  var flightNum by remember { mutableStateOf("") }
  var hotelName by remember { mutableStateOf("") }
  var restaurantName by remember { mutableStateOf("") }

  var expanded by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Add Event") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
          OutlinedTextField(
            value = type.name,
            onValueChange = {},
            readOnly = true,
            label = { Text("Event Type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, enabled = true),
          )
          ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            EventType.values().forEach { eventType ->
              DropdownMenuItem(
                text = { Text(eventType.name) },
                onClick = {
                  type = eventType
                  expanded = false
                },
              )
            }
          }
        }

        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
        OutlinedTextField(
          value = location,
          onValueChange = { location = it },
          label = { Text("Location") },
        )
        OutlinedTextField(
          value = time,
          onValueChange = { time = it },
          label = { Text("Time (e.g. 10:00 AM)") },
        )

        // Dynamic fields
        when (type) {
          EventType.TRANSPORTATION -> {
            OutlinedTextField(
              value = airline,
              onValueChange = { airline = it },
              label = { Text("Airline") },
            )
            OutlinedTextField(
              value = flightNum,
              onValueChange = { flightNum = it },
              label = { Text("Flight Number") },
            )
          }
          EventType.ACCOMMODATION -> {
            OutlinedTextField(
              value = hotelName,
              onValueChange = { hotelName = it },
              label = { Text("Hotel Name") },
            )
          }
          EventType.FOOD_AND_DRINK -> {
            OutlinedTextField(
              value = restaurantName,
              onValueChange = { restaurantName = it },
              label = { Text("Restaurant Name") },
            )
          }
          else -> {}
        }
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          val extraFields = mutableMapOf<String, String>()
          when (type) {
            EventType.TRANSPORTATION -> {
              extraFields["airline"] = airline
              extraFields["flightNum"] = flightNum
            }
            EventType.ACCOMMODATION -> {
              extraFields["hotelName"] = hotelName
            }
            EventType.FOOD_AND_DRINK -> {
              extraFields["restaurantName"] = restaurantName
            }
            else -> {}
          }
          onSave(type, title, location, time, extraFields)
          onDismiss()
        }
      ) {
        Text("Save")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}
