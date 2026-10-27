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

package com.example.jetpacker.feature.assistant.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.jetpacker.feature.assistant.TravelAssistantViewModel
import com.example.jetpacker.feature.assistant.model.ActionStepCategory
import com.example.jetpacker.feature.assistant.model.ActionStepStatus
import com.example.jetpacker.feature.assistant.model.AssistantOverlayState
import com.example.jetpacker.feature.assistant.model.ConversationTurnUi
import com.example.jetpacker.feature.assistant.model.HighlightedEntity
import com.example.jetpacker.feature.assistant.model.HighlightedEntity.ExpenseHighlight
import com.example.jetpacker.feature.assistant.model.HighlightedEntity.FlightDetailHighlight
import com.example.jetpacker.feature.assistant.model.HighlightedEntity.ItineraryEventHighlight
import com.example.jetpacker.feature.assistant.model.HighlightedEntity.MultiActionHighlight
import com.example.jetpacker.feature.assistant.model.HighlightedEntity.TravelDocumentHighlight
import com.example.jetpacker.feature.assistant.model.HighlightedEntity.VoiceNoteHighlight
import com.example.jetpacker.feature.assistant.model.ToolExecutionRecord
import com.example.jetpacker.feature.assistant.model.TravelAgentUiState.Error
import com.example.jetpacker.feature.assistant.model.TravelAgentUiState.ExecutingTool
import com.example.jetpacker.feature.assistant.model.TravelAgentUiState.Idle
import com.example.jetpacker.feature.assistant.model.TravelAgentUiState.Success
import com.example.jetpacker.feature.assistant.model.TravelAgentUiState.Thinking
import com.example.jetpacker.feature.assistant.model.UserActionStep
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.delay

/**
 * Modern Material 3 on-device Travel & Document Assistant card.
 *
 * Provides prompt suggestions, interactive chat input, real-time tool execution chips, and
 * structured highlight cards for passports, visas, and flight itineraries.
 */
@Composable
fun TravelAssistantCard(
  viewModel: TravelAssistantViewModel = hiltViewModel(),
  modifier: Modifier = Modifier,
  onManageDocumentsClick: () -> Unit = {},
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  var promptInput by remember { mutableStateOf("") }

  val voiceState by viewModel.voiceTranscript.collectAsStateWithLifecycle()

  val audioPermissionLauncher =
    rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) {
      isGranted ->
      viewModel.setVoicePermissionGranted(isGranted)
      if (isGranted) {
        viewModel.toggleVoiceInput(onPermissionRequired = {})
      }
    }

  LaunchedEffect(voiceState.partialTranscription, voiceState.transcription) {
    if (voiceState.isListening) {
      val liveText = voiceState.partialTranscription.ifBlank { voiceState.transcription }
      if (liveText.isNotBlank()) {
        promptInput = liveText
      }
    }
  }

  Card(
    modifier = modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
      // Header: Assistant title, Nano badge, Documents button, and Reset button
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = "Travel Assistant",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = "Trip Assistant",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            Surface(
              shape = MaterialTheme.shapes.extraSmall,
              color = MaterialTheme.colorScheme.primaryContainer,
            ) {
              Text(
                text = "On-Device Gemini Nano",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = { viewModel.clearChat() }) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = "Reset Assistant",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Dynamic State Content
      val conversationTurns by viewModel.conversationTurns.collectAsStateWithLifecycle()
      if (conversationTurns.size > 1) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          conversationTurns.dropLast(1).forEach { turn -> ConversationTurnItem(turn = turn) }
          HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        }
      }

      when (val state = uiState) {
        is Idle -> {
          Text(
            text = "Ask about flight schedules, expenses, or itinerary events.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        is Thinking -> {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 8.dp),
          ) {
            CircularProgressIndicator(
              modifier = Modifier.size(18.dp),
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = state.stepSummary,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
        is ExecutingTool -> {
          AgentEventStepper(tools = state.executedTools, activeToolName = state.toolName)
        }
        is Success -> {
          Column(modifier = Modifier.fillMaxWidth()) {
            if (state.toolExecutions.isNotEmpty()) {
              AgentEventStepper(tools = state.toolExecutions, activeToolName = null)
              Spacer(modifier = Modifier.height(10.dp))
            }

            HighlightedEntitySection(highlight = state.highlightedEntity)

            if (state.answerText.isNotBlank()) {
              Text(
                text = state.answerText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
          }
        }
        is Error -> {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 8.dp),
          ) {
            Icon(
              imageVector = Icons.Default.ErrorOutline,
              contentDescription = "Error",
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = state.message,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.error,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Input field, voice mic button, and send button
      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
          value = promptInput,
          onValueChange = { promptInput = it },
          placeholder = {
            Text(
              if (voiceState.isListening) "Listening on device..."
              else "Ask your travel assistant..."
            )
          },
          modifier = Modifier.weight(1f),
          singleLine = true,
          shape = MaterialTheme.shapes.medium,
        )

        Spacer(modifier = Modifier.width(4.dp))

        // On-device Voice Input Mic Button
        IconButton(
          onClick = {
            viewModel.toggleVoiceInput(
              onPermissionRequired = {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
              }
            )
          },
          enabled = uiState !is Thinking && uiState !is ExecutingTool,
        ) {
          Icon(
            imageVector = if (voiceState.isListening) Icons.Default.Mic else Icons.Default.MicNone,
            contentDescription =
              if (voiceState.isListening) "Stop voice recording" else "Record voice prompt",
            tint =
              if (voiceState.isListening) {
                MaterialTheme.colorScheme.primary
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              },
            modifier = Modifier.size(24.dp),
          )
        }

        Spacer(modifier = Modifier.width(4.dp))

        IconButton(
          onClick = {
            val textToSend = promptInput
            if (textToSend.isNotBlank()) {
              promptInput = ""
              viewModel.sendPrompt(textToSend)
            }
          },
          enabled = promptInput.isNotBlank() && uiState !is Thinking && uiState !is ExecutingTool,
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = "Send prompt",
            tint =
              if (promptInput.isNotBlank()) {
                MaterialTheme.colorScheme.primary
              } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
              },
          )
        }
      }

      if (voiceState.isListening) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          CircularProgressIndicator(
            modifier = Modifier.size(12.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = "🎙️ Listening on-device... Speak or tap mic to submit",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }
    }
  }
}

@Composable
private fun HighlightedEntitySection(highlight: HighlightedEntity?) {
  when (highlight) {
    is TravelDocumentHighlight -> {
      DocumentHighlightCard(highlight = highlight)
      Spacer(modifier = Modifier.height(8.dp))
    }
    is FlightDetailHighlight -> {
      FlightHighlightCard(highlight = highlight)
      Spacer(modifier = Modifier.height(8.dp))
    }
    is MultiActionHighlight -> {
      MultiActionHighlightCard(highlight = highlight)
      Spacer(modifier = Modifier.height(8.dp))
    }
    is ExpenseHighlight -> {
      ExpenseHighlightCard(highlight = highlight)
      Spacer(modifier = Modifier.height(8.dp))
    }
    is ItineraryEventHighlight -> {
      ItineraryEventHighlightCard(highlight = highlight)
      Spacer(modifier = Modifier.height(8.dp))
    }
    is VoiceNoteHighlight -> {
      VoiceNoteHighlightCard(highlight = highlight)
      Spacer(modifier = Modifier.height(8.dp))
    }
    null -> Unit
  }
}

/** Structured highlight card displaying travel document validity details. */
@Composable
private fun DocumentHighlightCard(
  highlight: TravelDocumentHighlight,
  modifier: Modifier = Modifier,
) {
  val (badgeColor, textColor, icon) =
    when (highlight.validityStatus) {
      "VALID" -> Triple(Color(0xFF01AF57), Color(0xFF061B0E), Icons.Default.CheckCircle)
      "EXPIRING_SOON" -> Triple(Color(0xFFE65100), Color.White, Icons.Default.Warning)
      "EXPIRED" -> Triple(MaterialTheme.colorScheme.error, Color.White, Icons.Default.ErrorOutline)
      else ->
        Triple(
          MaterialTheme.colorScheme.surfaceVariant,
          MaterialTheme.colorScheme.onSurfaceVariant,
          Icons.Default.Warning,
        )
    }

  Card(
    modifier = modifier.fillMaxWidth(),
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
    shape = MaterialTheme.shapes.medium,
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text =
            "${highlight.document.issuingCountry} ${highlight.document.documentType.replaceFirstChar { it.uppercase() }}",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
        )
        Surface(shape = MaterialTheme.shapes.small, color = badgeColor) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          ) {
            Icon(
              imageVector = icon,
              contentDescription = null,
              tint = textColor,
              modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = highlight.validityStatus.replace('_', ' '),
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = textColor,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      val formattedExpiry =
        if (highlight.document.expiryDate > 0L) {
          try {
            Instant.ofEpochMilli(highlight.document.expiryDate)
              .atZone(ZoneId.systemDefault())
              .toLocalDate()
              .toString()
          } catch (e: Exception) {
            highlight.document.expiryDate.toString()
          }
        } else {
          "Unknown"
        }

      Text(
        text = "Number: ${highlight.document.documentNumberMasked}  •  Expires: $formattedExpiry",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      if (highlight.validityExplanation.isNotBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = highlight.validityExplanation,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
    }
  }
}

/** Structured highlight card displaying flight itinerary details. */
@Composable
private fun FlightHighlightCard(highlight: FlightDetailHighlight, modifier: Modifier = Modifier) {
  Card(
    modifier = modifier.fillMaxWidth(),
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
    shape = MaterialTheme.shapes.medium,
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Flight,
            contentDescription = "Flight",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "${highlight.airline} ${highlight.flightNumber}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
          )
        }

        if (!highlight.status.isNullOrBlank()) {
          Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primaryContainer,
          ) {
            Text(
              text = highlight.status,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = "${highlight.origin} ➔ ${highlight.destination}",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
      )

      val detailsText =
        listOfNotNull(
            highlight.terminal?.takeIf { it.isNotBlank() }?.let { "Terminal $it" },
            highlight.gate?.takeIf { it.isNotBlank() }?.let { "Gate $it" },
          )
          .joinToString("  •  ")

      if (detailsText.isNotBlank()) {
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = detailsText,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

/** Structured highlight card displaying multi-action voice execution (e.g. Expense + Reminder). */
@Composable
private fun MultiActionHighlightCard(
  highlight: MultiActionHighlight,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
    shape = MaterialTheme.shapes.medium,
  ) {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Actions Completed",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
          )
        }
        if (!highlight.tripTitle.isNullOrBlank()) {
          Surface(
            shape = MaterialTheme.shapes.extraSmall,
            color = MaterialTheme.colorScheme.primaryContainer,
          ) {
            Text(
              text = highlight.tripTitle,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
          }
        }
      }

      if (highlight.expenseTitle != null) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.ReceiptLong,
              contentDescription = "Expense",
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = highlight.expenseTitle, style = MaterialTheme.typography.bodyMedium)
          }
          if (highlight.expenseAmount != null) {
            Text(
              text = "${highlight.expenseCurrency ?: "EUR"} ${highlight.expenseAmount}",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
      }

      if (highlight.eventTitle != null) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Event,
              contentDescription = "Reminder",
              tint = MaterialTheme.colorScheme.tertiary,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = highlight.eventTitle, style = MaterialTheme.typography.bodyMedium)
          }
          if (!highlight.eventLocation.isNullOrBlank()) {
            Text(
              text = highlight.eventLocation,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}

/** Structured highlight card displaying a recorded expense. */
@Composable
private fun ExpenseHighlightCard(highlight: ExpenseHighlight, modifier: Modifier = Modifier) {
  Card(
    modifier = modifier.fillMaxWidth(),
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
    shape = MaterialTheme.shapes.medium,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.ReceiptLong,
          contentDescription = "Expense",
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
          Text(
            text = highlight.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "Category: ${highlight.category}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      Text(
        text = "${highlight.currency} ${highlight.amount}",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
      )
    }
  }
}

/** Structured highlight card displaying an itinerary event or reminder. */
@Composable
private fun ItineraryEventHighlightCard(
  highlight: ItineraryEventHighlight,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
    shape = MaterialTheme.shapes.medium,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.Event,
          contentDescription = "Event",
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
          Text(
            text = highlight.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = highlight.location,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.primaryContainer,
      ) {
        Text(
          text = highlight.type,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
      }
    }
  }
}

/** Structured highlight card displaying a saved voice note / reminder. */
@Composable
private fun VoiceNoteHighlightCard(highlight: VoiceNoteHighlight, modifier: Modifier = Modifier) {
  Card(
    modifier = modifier.fillMaxWidth(),
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
    shape = MaterialTheme.shapes.medium,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Icon(
          imageVector = Icons.Default.NotificationsActive,
          contentDescription = "Voice Note",
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
          Text(
            text = highlight.transcription,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
          )
          if (!highlight.matchedEventTitle.isNullOrBlank()) {
            Text(
              text = "Matched to ${highlight.matchedEventTitle}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
      Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.primaryContainer,
      ) {
        Text(
          text = "Reminder",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
      }
    }
  }
}

/** Consumer-friendly action stepper without raw function names, JSON, or debug metrics. */
@Composable
private fun AgentEventStepper(
  tools: List<ToolExecutionRecord>,
  activeToolName: String? = null,
  modifier: Modifier = Modifier,
) {
  if (tools.isEmpty() && activeToolName == null) return

  Column(
    modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    tools.forEach { tool ->
      val isCurrent = (activeToolName == tool.name) && (tool.response == null)
      val (label, icon) = getHumanizedToolInfo(tool.name, isCompleted = !isCurrent)

      Surface(
        shape = RoundedCornerShape(10.dp),
        color =
          if (isCurrent) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
          } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
          },
        border = if (isCurrent) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
              imageVector = icon,
              contentDescription = null,
              tint =
                if (isCurrent) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = label,
              style = MaterialTheme.typography.bodySmall,
              fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }

          if (isCurrent) {
            CircularProgressIndicator(
              modifier = Modifier.size(14.dp),
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.primary,
            )
          } else {
            Icon(
              imageVector = Icons.Default.Check,
              contentDescription = "Completed",
              tint = Color(0xFF01AF57),
              modifier = Modifier.size(14.dp),
            )
          }
        }
      }
    }
  }
}

private fun getHumanizedToolInfo(
  toolName: String,
  isCompleted: Boolean,
): Pair<String, ImageVector> {
  val cleanName =
    toolName.removePrefix("BaseJetPackerAppFunctionService_").substringAfterLast('.').substringAfterLast('#')
  return when (cleanName) {
    "get_travel_document" ->
      (if (isCompleted) "Checked travel documents" else "Checking travel documents…") to
        Icons.Default.Search
    "read_document_content" ->
      (if (isCompleted) "Read document details" else "Reading document details…") to
        Icons.Default.DocumentScanner
    "check_document_validity" ->
      (if (isCompleted) "Verified document expiry" else "Verifying document expiry…") to
        Icons.Default.Verified
    "get_flight_itinerary" ->
      (if (isCompleted) "Checked flight schedule" else "Checking flight schedule…") to
        Icons.Default.Flight
    "add_expense",
    "addExpense" ->
      (if (isCompleted) "Added to your expenses" else "Adding to your expenses…") to
        Icons.Default.Payments
    "get_expenses" ->
      (if (isCompleted) "Checked trip expenses & budget" else "Checking trip expenses…") to
        Icons.Default.AttachMoney
    "addVoiceNote" ->
      (if (isCompleted) "Saved reminder to your trip" else "Saving reminder to your trip…") to
        Icons.Default.NotificationsActive
    "addItineraryEvent" ->
      (if (isCompleted) "Saved reminder to your trip" else "Saving reminder to your trip…") to
        Icons.Default.Event
    "getItinerary" ->
      (if (isCompleted) "Checked your trip schedule" else "Checking your trip schedule…") to
        Icons.Default.Event
    "getEventDetails" ->
      (if (isCompleted) "Checked activity details" else "Looking up activity details…") to
        Icons.Default.ConfirmationNumber
    "searchTrip" ->
      (if (isCompleted) "Found your trip" else "Looking up your trip…") to Icons.Default.Search
    else ->
      (if (isCompleted) "Updated your trip" else "Updating your trip…") to Icons.Default.AutoAwesome
  }
}

/**
 * Ambient, morphing Trip Assistant overlay anchored above the bottom bar on Expenses, Voice Notes,
 * Documents, and Itinerary screens.
 *
 * Supports:
 * - Demo 1: Single tool call -> Direct UI mutation with consumer-friendly action banner
 * - Demo 2: Sequential dependent tool calls -> Direct UI mutation with step-by-step progress rows
 * - Demo 3: Multi-turn conversation with session memory
 */
@Composable
fun AmbientTripAssistantSheet(
  viewModel: TravelAssistantViewModel,
  modifier: Modifier = Modifier,
  activeScreenHint: String = "Ask or say a command…",
) {
  val overlayState by viewModel.overlayState.collectAsStateWithLifecycle()
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val voiceState by viewModel.voiceTranscript.collectAsStateWithLifecycle()

  val audioPermissionLauncher =
    rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) {
      isGranted ->
      viewModel.setVoicePermissionGranted(isGranted)
      if (isGranted) {
        viewModel.toggleVoiceInput(onPermissionRequired = {})
      }
    }

  // The parent slides this sheet out when the overlay collapses. Keep rendering the last
  // non-Collapsed state during that exit so the content doesn't vanish mid-animation.
  var displayOverlayState by remember { mutableStateOf(overlayState) }
  if (overlayState !is AssistantOverlayState.Collapsed) {
    displayOverlayState = overlayState
  }

  // Auto-dismiss ActionCompleted after 4s (Single-Step Demo 1) or 5.5s (Multi-Step Demo 2)
  LaunchedEffect(overlayState) {
    val completedState = overlayState as? AssistantOverlayState.ActionCompleted
    if (completedState != null) {
      val holdMs = if (completedState.steps.size > 1) 5_500L else 4_000L
      delay(holdMs)
      viewModel.dismissOverlay()
    }
  }

  val sheetShape = RoundedCornerShape(24.dp)
  Surface(
    modifier =
      modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).dropShadow(
        shape = sheetShape
      ) {
        radius = 6.dp.toPx()
        offset = Offset(0f, 3.dp.toPx())
        spread = 1.dp.toPx()
        color = Color.Black.copy(alpha = 0.16f)
      },
    shape = sheetShape,
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    tonalElevation = 6.dp,
    shadowElevation = 0.dp,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .animateContentSize(
            animationSpec =
              spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
              )
          )
    ) {
      AnimatedContent(
        targetState = displayOverlayState,
        // Only crossfade when the *kind* of state changes; payload updates within the same state
        // (e.g. live transcript text) should update in place without a fade.
        contentKey = { it::class },
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "AmbientAssistantOverlayTransition",
      ) { state ->
        when (state) {
          is AssistantOverlayState.Collapsed -> {
            CollapsedAssistantBar(
              hint = activeScreenHint,
              onMicClick = {
                viewModel.startVoiceAssistant(
                  onPermissionRequired = {
                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                  }
                )
              },
              onExpandClick = {
                viewModel.startVoiceAssistant(
                  onPermissionRequired = {
                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                  }
                )
              },
            )
          }

          is AssistantOverlayState.Listening -> {
            val liveTranscript =
              voiceState.partialTranscription
                .ifBlank { voiceState.transcription }
                .ifBlank { state.liveTranscript }
            ListeningAssistantBar(
              partialTranscript = liveTranscript,
              onStopClick = { viewModel.toggleVoiceInput(onPermissionRequired = {}) },
              onCancelClick = { viewModel.cancelVoiceListening() },
            )
          }

          is AssistantOverlayState.PerformingActions -> {
            ActionProgressFeed(
              userPrompt = state.userPrompt,
              steps = state.steps,
              isCompleted = false,
              onDismiss = { viewModel.dismissOverlay() },
            )
          }

          is AssistantOverlayState.ActionCompleted -> {
            ActionProgressFeed(
              userPrompt = state.userPrompt,
              steps = state.steps,
              isCompleted = true,
              textResponse = state.textResponse,
              onDismiss = { viewModel.dismissOverlay() },
            )
          }

          is AssistantOverlayState.Conversational -> {
            val liveTranscript =
              voiceState.partialTranscription
                .ifBlank { voiceState.transcription }
                .ifBlank { state.liveTranscript }
            ConversationalAssistantSheet(
              turns = state.turns,
              activeSteps = state.activeSteps,
              isListening = state.isListening || voiceState.isListening,
              liveTranscript = liveTranscript,
              isBusy = uiState is Thinking || uiState is ExecutingTool,
              busySummary =
                when (val s = uiState) {
                  is Thinking -> s.stepSummary
                  is ExecutingTool -> getHumanizedToolInfo(s.toolName, isCompleted = false).first
                  else -> null
                },
              onMicClick = {
                viewModel.toggleVoiceInput(
                  onPermissionRequired = {
                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                  }
                )
              },
              onCancelListenClick = { viewModel.cancelVoiceListening() },
              onResetClick = { viewModel.clearChat() },
              onCollapse = { viewModel.dismissOverlay() },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun CollapsedAssistantBar(hint: String, onMicClick: () -> Unit, onExpandClick: () -> Unit) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clickable(onClick = onExpandClick)
        .padding(horizontal = 16.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
      Icon(
        imageVector = Icons.Default.AutoAwesome,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(20.dp),
      )
      Spacer(modifier = Modifier.width(10.dp))
      Text(
        text = "Trip Assistant",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
      )
    }

    FilledIconButton(
      onClick = onMicClick,
      shape = CircleShape,
      colors =
        IconButtonDefaults.filledIconButtonColors(
          containerColor = MaterialTheme.colorScheme.primary
        ),
    ) {
      Icon(
        imageVector = Icons.Default.Mic,
        contentDescription = "Speak to Trip Assistant",
        tint = MaterialTheme.colorScheme.onPrimary,
      )
    }
  }
}

@Composable
private fun ListeningAssistantBar(
  partialTranscript: String,
  onStopClick: () -> Unit,
  onCancelClick: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.size(40.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Default.GraphicEq,
            contentDescription = "Listening",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
          )
        }
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column {
        Text(
          text = "Listening…",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = partialTranscript.ifBlank { "Speak now…" },
          style = MaterialTheme.typography.bodyMedium,
          color =
            if (partialTranscript.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurface,
          fontStyle = if (partialTranscript.isBlank()) FontStyle.Italic else FontStyle.Normal,
        )
      }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
      FilledIconButton(
        onClick = onStopClick,
        shape = CircleShape,
        colors =
          IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.primary
          ),
        modifier = Modifier.size(36.dp),
      ) {
        Icon(
          imageVector = Icons.Default.Stop,
          contentDescription = "Done speaking",
          tint = MaterialTheme.colorScheme.onPrimary,
          modifier = Modifier.size(18.dp),
        )
      }
      Spacer(modifier = Modifier.width(4.dp))
      IconButton(onClick = onCancelClick, modifier = Modifier.size(32.dp)) {
        Icon(
          imageVector = Icons.Default.Close,
          contentDescription = "Cancel",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(18.dp),
        )
      }
    }
  }
}

/**
 * Consumer-friendly action banner for Demos 1 (Single-Step) and 2 (Multi-Step Sequential).
 *
 * Shows plain-English steps without any function names or JSON arguments.
 */
@Composable
private fun ActionProgressFeed(
  userPrompt: String,
  steps: List<UserActionStep>,
  isCompleted: Boolean,
  textResponse: String? = null,
  onDismiss: () -> Unit,
) {
  val isMultiStep = steps.size > 1
  val completedCount = steps.count { it.status == ActionStepStatus.COMPLETED }

  Column(
    modifier = Modifier.fillMaxWidth().padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Icon(
          imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.AutoAwesome,
          contentDescription = null,
          tint = if (isCompleted) Color(0xFF01AF57) else MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (userPrompt.isNotBlank()) "\"$userPrompt\"" else "Updating trip",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        if (isMultiStep) {
          Spacer(modifier = Modifier.width(8.dp))
          Surface(
            shape = RoundedCornerShape(12.dp),
            color =
              if (isCompleted) Color(0xFF01AF57).copy(alpha = 0.14f)
              else MaterialTheme.colorScheme.primaryContainer,
          ) {
            Text(
              text =
                if (isCompleted) "${steps.size} steps"
                else "${completedCount.coerceAtLeast(1)}/${steps.size}",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color =
                if (isCompleted) Color(0xFF01AF57)
                else MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
          }
        }
        if (isCompleted) {
          Spacer(modifier = Modifier.width(4.dp))
          IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Dismiss",
              modifier = Modifier.size(16.dp),
            )
          }
        }
      }
    }

    steps.forEachIndexed { index, step ->
      UserActionStepRow(step = step, stepNumber = if (isMultiStep) index + 1 else null)
    }

    if (isCompleted && !textResponse.isNullOrBlank()) {
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(
          text = textResponse,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        )
      }
    }
  }
}

@Composable
private fun UserActionStepRow(step: UserActionStep, stepNumber: Int? = null) {
  val icon =
    when (step.category) {
      ActionStepCategory.EXPENSE -> Icons.Default.ReceiptLong
      ActionStepCategory.REMINDER -> Icons.Default.NotificationsActive
      ActionStepCategory.FLIGHT -> Icons.Default.Flight
      ActionStepCategory.DOCUMENT -> Icons.Default.DocumentScanner
      ActionStepCategory.ITINERARY -> Icons.Default.Event
      ActionStepCategory.TRIP -> Icons.Default.Search
    }

  Surface(
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHighest,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        if (stepNumber != null) {
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(22.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text(
                text = stepNumber.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
            }
          }
          Spacer(modifier = Modifier.width(8.dp))
        }
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = step.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
          )
          if (!step.subtitle.isNullOrBlank()) {
            Text(
              text = step.subtitle,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      when (step.status) {
        ActionStepStatus.IN_PROGRESS -> {
          CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary,
          )
        }
        ActionStepStatus.COMPLETED -> {
          Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Done",
            tint = Color(0xFF01AF57),
            modifier = Modifier.size(20.dp),
          )
        }
        ActionStepStatus.FAILED -> {
          Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = "Failed",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp),
          )
        }
      }
    }
  }
}

/**
 * Multi-turn conversational sheet for Demo 3.
 *
 * Stays anchored as a half-screen overlay so the user never loses sight of the underlying trip,
 * with a pure voice follow-up trigger (no text input field).
 */
@Composable
private fun ConversationalAssistantSheet(
  turns: List<ConversationTurnUi>,
  activeSteps: List<UserActionStep>,
  isListening: Boolean,
  liveTranscript: String,
  isBusy: Boolean,
  busySummary: String?,
  onMicClick: () -> Unit,
  onCancelListenClick: () -> Unit,
  onResetClick: () -> Unit,
  onCollapse: () -> Unit,
) {
  val listState = rememberLazyListState()

  LaunchedEffect(turns.size, activeSteps.size, isBusy) {
    val totalItems = listState.layoutInfo.totalItemsCount
    if (totalItems > 0) {
      listState.animateScrollToItem(totalItems - 1)
    }
  }

  Column(
    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.AutoAwesome,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Trip Assistant",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
        if (turns.size > 1) {
          Spacer(modifier = Modifier.width(8.dp))
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
          ) {
            Text(
              text = "${turns.size} turns",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            )
          }
        }
      }
      Row(verticalAlignment = Alignment.CenterVertically) {
        if (turns.isNotEmpty()) {
          IconButton(onClick = onResetClick) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = "Reset conversation",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        IconButton(onClick = onCollapse) {
          Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Minimize")
        }
      }
    }

    val activeStatusText = activeSteps.lastOrNull()?.title ?: busySummary
    if (turns.isNotEmpty() || (isBusy && activeStatusText != null)) {
      LazyColumn(
        state = listState,
        modifier = Modifier.weight(1f, fill = false),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        items(turns, key = { it.id }) { turn -> ConversationTurnItem(turn = turn) }
        if (isBusy && activeStatusText != null) {
          item(key = "busy_indicator") {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp),
            ) {
              CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = activeStatusText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
              )
            }
          }
        }
      }
    }

    if (isListening) {
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
              imageVector = Icons.Default.GraphicEq,
              contentDescription = "Listening",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Listening…",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
              )
              Text(
                text = liveTranscript.ifBlank { "Speak your follow-up…" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontStyle = if (liveTranscript.isBlank()) FontStyle.Italic else FontStyle.Normal,
              )
            }
          }
          Row(verticalAlignment = Alignment.CenterVertically) {
            FilledIconButton(
              onClick = onMicClick,
              shape = CircleShape,
              colors =
                IconButtonDefaults.filledIconButtonColors(
                  containerColor = MaterialTheme.colorScheme.primary
                ),
              modifier = Modifier.size(34.dp),
            ) {
              Icon(
                imageVector = Icons.Default.Stop,
                contentDescription = "Done speaking",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(18.dp),
              )
            }
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(onClick = onCancelListenClick, modifier = Modifier.size(30.dp)) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Cancel",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
              )
            }
          }
        }
      }
    } else {
      Surface(
        shape = RoundedCornerShape(24.dp),
        color =
          if (isBusy) MaterialTheme.colorScheme.surfaceContainerHighest
          else MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth().clickable(enabled = !isBusy, onClick = onMicClick),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 16.dp),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "Speak follow-up",
            tint =
              if (isBusy) MaterialTheme.colorScheme.onSurfaceVariant
              else MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(20.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (isBusy) "Working…" else "Ask a follow-up",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color =
              if (isBusy) MaterialTheme.colorScheme.onSurfaceVariant
              else MaterialTheme.colorScheme.onPrimary,
          )
        }
      }
    }
  }
}

@Composable
private fun ConversationTurnItem(turn: ConversationTurnUi) {
  Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
      Surface(
        shape = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
      ) {
        Text(
          text = turn.userPrompt,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
      }
    }

    if (!turn.assistantMessage.isNullOrBlank()) {
      Surface(
        shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
      ) {
        Text(
          text = turn.assistantMessage,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        )
      }
    }
  }
}
