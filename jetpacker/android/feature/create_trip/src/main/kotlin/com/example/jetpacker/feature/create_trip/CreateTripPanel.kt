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

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.jetpacker.core.flags.FeatureFlags
import com.example.jetpacker.core.ui.SekuyaFontFamily
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateTripPanelContent(
  uiState: CreateTripUiState,
  viewModel: CreateTripViewModel,
  onCollapse: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val photoPickerLauncher =
    rememberLauncherForActivityResult(contract = ActivityResultContracts.PickVisualMedia()) {
      uri: Uri? ->
      uri?.let {
        context.contentResolver.takePersistableUriPermission(
          it,
          android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
      }
      viewModel.onImageUriChange(uri?.toString())
    }

  var showStartDatePicker by remember { mutableStateOf(false) }
  var showEndDatePicker by remember { mutableStateOf(false) }
  var participantName by remember { mutableStateOf("") }

  val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault()) }

  if (showStartDatePicker) {
    DatePickerModal(
      onDateSelected = { selectedDate ->
        selectedDate?.let { millis ->
          val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
          viewModel.onStartDateChange(dateFormatter.format(date))
        }
      },
      onDismiss = { showStartDatePicker = false },
    )
  }

  if (showEndDatePicker) {
    DatePickerModal(
      onDateSelected = { selectedDate ->
        selectedDate?.let { millis ->
          val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
          viewModel.onEndDateChange(dateFormatter.format(date))
        }
      },
      onDismiss = { showEndDatePicker = false },
    )
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.surface,
    topBar = {
      Row(
        modifier =
          Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        IconButton(
          onClick = onCollapse,
          modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
        ) { Icon(Icons.Rounded.Close, contentDescription = "Back") }
        Text(
          if (uiState.isEditing) "EDIT TRIP" else "CREATE TRIP",
          style =
            MaterialTheme.typography.titleLarge.copy(
              fontSize = 22.sp,
              fontFamily = SekuyaFontFamily,
            ),
          color = MaterialTheme.colorScheme.secondary,
        )
      }
    },
    bottomBar = {
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 24.dp, vertical = 12.dp)
      ) {
        Button(
          onClick = { viewModel.createTrip() },
          modifier = Modifier.fillMaxWidth().height(56.dp),
          shape = CircleShape,
          enabled =
            uiState.title.isNotBlank() &&
              uiState.startDate.isNotBlank() &&
              uiState.endDate.isNotBlank() &&
              !uiState.isLoading &&
              !uiState.isGenerating,
        ) {
          if (uiState.isLoading) {
            CircularProgressIndicator(
              modifier = Modifier.size(24.dp),
              color = MaterialTheme.colorScheme.onPrimary,
            )
          } else {
            Text(if (uiState.isEditing) "Save Changes" else "Create Trip")
          }
        }
      }
    },
  ) { innerPadding ->
    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
      Column(
        modifier =
          Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Spacer(Modifier.height(8.dp))

        // Image Section
        Card(
          modifier = Modifier.fillMaxWidth().height(160.dp),
          shape = MaterialTheme.shapes.large,
          colors =
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
          enabled = !uiState.isGenerating,
          onClick = {
            photoPickerLauncher.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
          },
        ) {
          Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            if (uiState.imageUri != null) {
              AsyncImage(
                model = uiState.imageUri,
                contentDescription = "Selected Picture",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
              )
            } else {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                  Icons.Filled.ImageNotSupported,
                  contentDescription = null,
                  modifier = Modifier.size(32.dp),
                )
                Spacer(Modifier.height(16.dp))
                Text("Tap to pick a picture", style = MaterialTheme.typography.labelMedium)
              }
            }
          }
        }

        TextField(
          value = uiState.title,
          onValueChange = { viewModel.onTitleChange(it) },
          placeholder = { Text("Title") },
          maxLines = 1,
          enabled = !uiState.isGenerating,
          shape = CircleShape,
          colors =
            TextFieldDefaults.colors(
              focusedIndicatorColor = Color.Transparent,
              unfocusedIndicatorColor = Color.Transparent,
              errorIndicatorColor = Color.Transparent,
              disabledIndicatorColor = Color.Transparent,
              focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
              unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
          modifier = Modifier.fillMaxWidth(),
        )

        TextField(
          value = uiState.location,
          onValueChange = { viewModel.onLocationChange(it) },
          placeholder = { Text("Location") },
          maxLines = 1,
          enabled = !uiState.isGenerating,
          shape = CircleShape,
          colors =
            TextFieldDefaults.colors(
              focusedIndicatorColor = Color.Transparent,
              unfocusedIndicatorColor = Color.Transparent,
              errorIndicatorColor = Color.Transparent,
              disabledIndicatorColor = Color.Transparent,
              focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
              unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
          modifier = Modifier.fillMaxWidth(),
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Box(
            modifier =
              Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).clip(CircleShape).clickable(enabled = !uiState.isGenerating) {
                showStartDatePicker = true
              }
          ) {
            TextField(
              value = uiState.startDate,
              onValueChange = {},
              placeholder = { Text("Start Date") },
              maxLines = 1,
              enabled = false,
              shape = CircleShape,
              colors =
                TextFieldDefaults.colors(
                  disabledTextColor = MaterialTheme.colorScheme.onSurface,
                  disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                  disabledIndicatorColor = Color.Transparent,
                  disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
              modifier = Modifier.fillMaxWidth(),
            )
          }
          Box(
            modifier =
              Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).clip(CircleShape).clickable(enabled = !uiState.isGenerating) {
                showEndDatePicker = true
              }
          ) {
            TextField(
              value = uiState.endDate,
              onValueChange = {},
              placeholder = { Text("End Date") },
              maxLines = 1,
              enabled = false,
              shape = CircleShape,
              colors =
                TextFieldDefaults.colors(
                  disabledTextColor = MaterialTheme.colorScheme.onSurface,
                  disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                  disabledIndicatorColor = Color.Transparent,
                  disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
              modifier = Modifier.fillMaxWidth(),
            )
          }
        }

        // Participants Section
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            "Participants",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )

          uiState.participants.forEach { participant ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(participant, modifier = Modifier.weight(1f))
              IconButton(
                onClick = { viewModel.onRemoveParticipant(participant) },
                enabled = !uiState.isGenerating,
              ) {
                Icon(
                  Icons.Rounded.RemoveCircleOutline,
                  contentDescription = "Remove",
                  tint = MaterialTheme.colorScheme.error,
                )
              }
            }
          }

          TextField(
            value = participantName,
            onValueChange = { participantName = it },
            placeholder = { Text("New Participant") },
            maxLines = 1,
            enabled = !uiState.isGenerating,
            shape = CircleShape,
            colors =
              TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
              ),
            trailingIcon = {
              IconButton(
                onClick = {
                  if (participantName.isNotBlank()) {
                    viewModel.onAddParticipant(participantName)
                    participantName = ""
                  }
                },
                enabled = participantName.isNotBlank() && !uiState.isGenerating,
              ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
              }
            },
            modifier = Modifier.fillMaxWidth(),
          )
        }

        Spacer(Modifier.height(24.dp))
      }
      if (uiState.isGenerating) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
      }
    }
  }
}

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
