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

package com.example.jetpacker.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.jetpacker.core.flags.FeatureFlags
import java.time.Instant
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(onBack: () -> Unit = {}, viewModel: DebugViewModel = hiltViewModel()) {
  val context = LocalContext.current

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("AI Feature Debug Screen") },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
      )
    }
  ) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .padding(16.dp)
          .verticalScroll(rememberScrollState()),
    ) {
      Text(
        "Time Override Settings",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
      HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

      var overrideTimeState by remember { mutableStateOf(FeatureFlags.OVERRIDE_CURRENT_TIME_MILLIS) }
      var showDatePicker by remember { mutableStateOf(false) }

      Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("Mock System Date", style = MaterialTheme.typography.bodyLarge)
          val dateLabel = overrideTimeState?.let { Instant.ofEpochMilli(it).toString().take(10) } ?: "Real System Clock"
          Text(
            text = dateLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Button(onClick = { showDatePicker = true }) {
          Text("Set Date")
        }
        Spacer(Modifier.width(8.dp))
        Button(
          onClick = {
            FeatureFlags.putLongFlag(context, FeatureFlags.KEY_OVERRIDE_CURRENT_TIME_MILLIS, 0L)
            overrideTimeState = null
          }
        ) {
          Text("Clear")
        }
      }

      if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
          initialSelectedDateMillis = overrideTimeState ?: System.currentTimeMillis()
        )
        DatePickerDialog(
          onDismissRequest = { showDatePicker = false },
          confirmButton = {
            TextButton(
              onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                  FeatureFlags.putLongFlag(context, FeatureFlags.KEY_OVERRIDE_CURRENT_TIME_MILLIS, millis)
                  overrideTimeState = millis
                }
                showDatePicker = false
              }
            ) {
              Text("Confirm")
            }
          },
          dismissButton = {
            TextButton(onClick = { showDatePicker = false }) {
              Text("Cancel")
            }
          }
        ) {
          DatePicker(state = datePickerState)
        }
      }

      Spacer(modifier = Modifier.padding(vertical = 16.dp))

      Button(onClick = { viewModel.resetDatabase() }, modifier = Modifier.fillMaxWidth()) {
        Text("Reset Database to Mock Data")
      }

      Spacer(modifier = Modifier.padding(vertical = 8.dp))

      var showLicensesDialog by remember { mutableStateOf(false) }
      Button(onClick = { showLicensesDialog = true }, modifier = Modifier.fillMaxWidth()) {
        Text("Open Source Licenses")
      }

      if (showLicensesDialog) {
        LicensesDialog(onDismiss = { showLicensesDialog = false })
      }
    }
  }
}

@Composable
fun LicensesDialog(onDismiss: () -> Unit) {
  val context = LocalContext.current
  val licensesText = remember {
    try {
      context.assets.open("THIRD_PARTY_NOTICES").bufferedReader().use { it.readText() }
    } catch (e: Exception) {
      "Failed to load third-party notices:\n${e.localizedMessage}"
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Open Source Licenses") },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 400.dp)
            .verticalScroll(rememberScrollState())
        ) {
          Text(
            text = licensesText,
            style = MaterialTheme.typography.bodySmall.copy(
              fontFamily = FontFamily.Monospace
            ),
            modifier = Modifier.padding(4.dp)
          )
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Close")
      }
    }
  )
}

