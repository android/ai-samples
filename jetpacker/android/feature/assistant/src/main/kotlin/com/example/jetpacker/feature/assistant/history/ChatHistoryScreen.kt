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

package com.example.jetpacker.feature.assistant.history

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.jetpacker.core.ui.SekuyaFontFamily
import com.example.jetpacker.core.ui.components.JetPackerFabConfig
import com.example.jetpacker.data.itinerary.AssistantConversation
import com.example.jetpacker.feature.assistant.model.ActionStepCategory
import com.example.jetpacker.feature.assistant.model.ConversationTurnUi
import com.example.jetpacker.feature.assistant.model.UserActionStep
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen displaying the reverse-chronological list of historical conversations with the Travel Assistant.
 *
 * The most recent conversation is shown at the top. Users can view past interactions, expand
 * conversation turns, or resume a chat session with the assistant.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatHistoryScreen(
  tripId: String,
  contentPadding: PaddingValues,
  onBack: () -> Unit = {},
  onFabConfigChange: (JetPackerFabConfig?) -> Unit = {},
  onSelectConversation: ((AssistantConversation) -> Unit)? = null,
  onStartNewChat: (() -> Unit)? = null,
  viewModel: ChatHistoryViewModel = hiltViewModel(),
  modifier: Modifier = Modifier,
) {
  val conversationsFlow = remember(tripId) { viewModel.getConversations(tripId) }
  val conversations by
    conversationsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

  var showClearAllDialog by remember { mutableStateOf(false) }

  LaunchedEffect(tripId) {
    if (tripId.isNotBlank()) {
      viewModel.seedDemoConversationsIfEmpty(tripId)
    }
  }

  val secondary = MaterialTheme.colorScheme.secondary
  val onSecondary = MaterialTheme.colorScheme.onSecondary

  LaunchedEffect(Unit) {
    onFabConfigChange(
      JetPackerFabConfig(
        icon = Icons.Rounded.AutoAwesome,
        onClick = { onStartNewChat?.invoke() },
        contentDescription = "New Conversation",
        containerColor = secondary,
        contentColor = onSecondary,
      )
    )
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = Color.Transparent,
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "Chat history",
            style =
              MaterialTheme.typography.titleLarge.copy(
                fontFamily = SekuyaFontFamily,
                fontSize = 17.sp,
              ),
            color = MaterialTheme.colorScheme.onSurface,
          )
        },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = "Back",
            )
          }
        },
        actions = {
          if (conversations.isNotEmpty()) {
            IconButton(onClick = { showClearAllDialog = true }) {
              Icon(
                imageVector = Icons.Rounded.DeleteOutline,
                contentDescription = "Clear all chat history",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        },
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
          ),
      )
    },
  ) { innerPadding ->
    val bottomPadding =
      maxOf(innerPadding.calculateBottomPadding(), contentPadding.calculateBottomPadding()) + 88.dp

    Box(
      modifier =
        Modifier.fillMaxSize()
          .padding(top = innerPadding.calculateTopPadding()),
    ) {
      if (conversations.isEmpty()) {
        EmptyChatHistoryView(
          onStartChat = { onStartNewChat?.invoke() },
          modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        )
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding =
            PaddingValues(
              start = 16.dp,
              end = 16.dp,
              top = 8.dp,
              bottom = bottomPadding,
            ),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          items(conversations, key = { it.id }) { conversation ->
            ChatHistoryCard(
              conversation = conversation,
              onSelect = { onSelectConversation?.invoke(conversation) },
              onDelete = { viewModel.deleteConversation(conversation.id) },
            )
          }
        }
      }
    }
  }

  if (showClearAllDialog) {
    AlertDialog(
      onDismissRequest = { showClearAllDialog = false },
      title = { Text("Clear chat history?") },
      text = {
        Text("This will permanently remove all past assistant conversations for this trip.")
      },
      confirmButton = {
        TextButton(
          onClick = {
            viewModel.clearAll(tripId)
            showClearAllDialog = false
          }
        ) {
          Text("Clear all", color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearAllDialog = false }) {
          Text("Cancel")
        }
      },
    )
  }
}

@Composable
private fun ChatHistoryCard(
  conversation: AssistantConversation,
  onSelect: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var isExpanded by remember { mutableStateOf(false) }
  val turns = remember(conversation.turnsJson) { conversation.toConversationTurns() }
  val formattedTime = remember(conversation.timestamp) { formatConversationTime(conversation.timestamp) }

  Card(
    modifier =
      modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(20.dp))
        .animateContentSize(
          animationSpec =
            spring(
              dampingRatio = Spring.DampingRatioLowBouncy,
              stiffness = Spring.StiffnessMediumLow,
            )
        ),
    shape = RoundedCornerShape(20.dp),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
      ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      // Header row: timestamp & badges & delete
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(28.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.Chat,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(15.dp),
              )
            }
          }
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = formattedTime,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
          ) {
            val countText = if (conversation.turnCount <= 1) "1 message" else "${conversation.turnCount} turns"
            Text(
              text = countText,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
          }

          Spacer(modifier = Modifier.width(4.dp))
          IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
            Icon(
              imageVector = Icons.Rounded.DeleteOutline,
              contentDescription = "Delete conversation",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp),
            )
          }
        }
      }

      // Title (first prompt)
      Text(
        text = if (conversation.title.isNotBlank()) "\"${conversation.title}\"" else "Assistant Conversation",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
      )

      // Latest message snippet (if collapsed)
      if (!isExpanded && conversation.lastMessage.isNotBlank()) {
        Text(
          text = conversation.lastMessage,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }

      // Expanded detailed transcript
      AnimatedVisibility(
        visible = isExpanded,
        enter = fadeIn(),
        exit = fadeOut(),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          turns.forEachIndexed { index, turn ->
            ConversationTurnDetail(turn = turn, turnNumber = index + 1)
          }
        }
      }

      // Action row: Expand/Collapse toggle & Continue Chat button
      Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        TextButton(
          onClick = { isExpanded = !isExpanded },
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        ) {
          Icon(
            imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(text = if (isExpanded) "Hide details" else "View conversation")
        }

        Button(
          onClick = onSelect,
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
          shape = RoundedCornerShape(12.dp),
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Continue chat",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }
  }
}

@Composable
private fun ConversationTurnDetail(
  turn: ConversationTurnUi,
  turnNumber: Int,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    // User prompt bubble
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.End,
    ) {
      Surface(
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth(0.9f),
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Text(
            text = "You",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = turn.userPrompt,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
          )
        }
      }
    }

    // Action steps (if any executed)
    if (turn.actionSteps.isNotEmpty()) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        turn.actionSteps.forEach { step ->
          ActionStepPill(step = step)
        }
      }
    }

    // Assistant response bubble
    if (!turn.assistantMessage.isNullOrBlank()) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
      ) {
        Surface(
          shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier.fillMaxWidth(0.95f),
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Trip Assistant",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
              )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = turn.assistantMessage,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ActionStepPill(step: UserActionStep) {
  val icon =
    when (step.category) {
      ActionStepCategory.EXPENSE -> Icons.Rounded.ReceiptLong
      ActionStepCategory.REMINDER -> Icons.Rounded.NotificationsActive
      ActionStepCategory.FLIGHT -> Icons.Rounded.Flight
      ActionStepCategory.DOCUMENT -> Icons.Rounded.DocumentScanner
      ActionStepCategory.ITINERARY -> Icons.Rounded.Event
      ActionStepCategory.TRIP -> Icons.Rounded.Search
    }

  Surface(
    shape = RoundedCornerShape(10.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.8f),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = step.title,
          style = MaterialTheme.typography.bodySmall,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Icon(
        imageVector = Icons.Rounded.CheckCircle,
        contentDescription = "Completed",
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(16.dp),
      )
    }
  }
}

@Composable
private fun EmptyChatHistoryView(
  onStartChat: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Surface(
      shape = CircleShape,
      color = MaterialTheme.colorScheme.primaryContainer,
      modifier = Modifier.size(80.dp),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.Chat,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.size(40.dp),
        )
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    Text(
      text = "No chat history yet",
      style =
        MaterialTheme.typography.titleLarge.copy(
          fontFamily = SekuyaFontFamily,
          fontSize = 20.sp,
        ),
      color = MaterialTheme.colorScheme.onSurface,
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = "Conversations you have with the Trip Assistant will be saved here for this trip, with your most recent chats at the top.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
      modifier = Modifier.padding(horizontal = 16.dp),
    )

    Spacer(modifier = Modifier.height(24.dp))

    Button(
      onClick = onStartChat,
      colors =
        ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
      shape = RoundedCornerShape(24.dp),
      contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    ) {
      Icon(
        imageVector = Icons.Rounded.AutoAwesome,
        contentDescription = null,
        modifier = Modifier.size(18.dp),
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = "Ask Trip Assistant",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}

private fun formatConversationTime(timestampMillis: Long): String {
  val now = System.currentTimeMillis()
  val diff = now - timestampMillis

  return when {
    diff < 60 * 1000L -> "Just now"
    DateUtils.isToday(timestampMillis) -> {
      val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
      "Today, ${timeFormat.format(Date(timestampMillis))}"
    }
    diff < 48 * 3600 * 1000L -> {
      val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
      "Yesterday, ${timeFormat.format(Date(timestampMillis))}"
    }
    else -> {
      val dateFormat = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
      dateFormat.format(Date(timestampMillis))
    }
  }
}
