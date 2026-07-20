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

package com.example.jetpacker.feature.detail.museum_assistant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

val PrimaryBlue = Color(0xFF0F172A)
val UserBubble = Color(0xFF0F172A)
val BotBubble = Color(0xFFF1F5F9)
val TextPrimary = Color(0xFF1E293B)
val TextSecondary = Color(0xFF64748B)

data class ChatMessage(val id: Int, val text: String, val isUser: Boolean, val sender: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatbotScreen(
  eventId: String?,
  viewModel: ChatViewModel = hiltViewModel(),
  onBack: (() -> Unit)? = null
) {
  val messages by viewModel.messages.collectAsStateWithLifecycle()
  ChatbotContent(
    messages = messages,
    onSendMessage = { viewModel.sendMessage(it) },
    onBack = onBack
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatbotContent(
  messages: List<ChatMessage>,
  onSendMessage: (String) -> Unit,
  onBack: (() -> Unit)? = null
) {
  var inputText by remember { mutableStateOf("") }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        navigationIcon = {
          if (onBack != null) {
            IconButton(onClick = onBack) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
          }
        },
        title = {
          Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Text(
              text = "Museum Assistant",
              fontWeight = FontWeight.Bold,
              style = MaterialTheme.typography.titleMedium,
            )
          }
        },
      )
    },
    bottomBar = {
      Column {
        InputBar(
          modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
          text = inputText,
          onTextChange = { inputText = it },
          onSend = {
            onSendMessage(inputText)
            inputText = ""
          },
        )
      }
    },
  ) { paddingValues ->
    LazyColumn(
      modifier =
        Modifier.fillMaxSize()
          .padding(paddingValues)
          .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item { Spacer(modifier = Modifier.height(8.dp)) }

      items(messages) { msg -> MessageRow(msg) }

      item { Spacer(modifier = Modifier.height(16.dp)) }
    }
  }
}

@Composable
fun MessageRow(message: ChatMessage) {
  if (message.isUser) {
    UserMessageRow(message)
  } else {
    BotMessageRow(message)
  }
}

@Composable
fun UserMessageRow(message: ChatMessage) {
  Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
      Text(text = message.sender, style = MaterialTheme.typography.labelSmall)
    }
    Spacer(modifier = Modifier.height(4.dp))
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.End) {
      Box(
        modifier =
          Modifier.weight(1f, fill = false)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp))
            .background(UserBubble)
            .padding(16.dp)
      ) {
        Text(text = message.text, color = Color.White, style = MaterialTheme.typography.bodyMedium)
      }
      Spacer(modifier = Modifier.width(8.dp))
      Icon(Icons.Filled.AccountCircle, contentDescription = "User", modifier = Modifier.size(32.dp))
    }
  }
}

@Composable
fun BotMessageRow(message: ChatMessage) {
  Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Start) {
      Icon(
        Icons.Filled.Face,
        contentDescription = "Bot",
        modifier =
          Modifier.size(32.dp)
            .clip(CircleShape)
            .padding(4.dp),
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(text = message.sender, style = MaterialTheme.typography.labelSmall)
    }
    Spacer(modifier = Modifier.height(4.dp))
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Start) {
      Spacer(modifier = Modifier.width(40.dp))
      Column {
        Box(
          modifier =
            Modifier.clip(
              RoundedCornerShape(topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
            )
            .background(BotBubble)
            .padding(16.dp)
        ) {
          Text(text = message.text, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
        }
      }
    }
  }
}

@Composable
fun InputBar(
  text: String,
  onTextChange: (String) -> Unit,
  onSend: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(modifier = Modifier.fillMaxWidth().then(modifier).navigationBarsPadding()) {
    Row(
      modifier =
        Modifier.padding(16.dp)
          .clip(RoundedCornerShape(32.dp))
          .background(Color(0xFFF1F5F9))
          .padding(horizontal = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      TextField(
        value = text,
        onValueChange = onTextChange,
        placeholder = { Text("Type a message...", color = TextSecondary) },
        modifier = Modifier.weight(1f),
        colors =
          TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = TextPrimary,
          ),
      )

      if (text.isNotBlank()) {
        IconButton(onClick = onSend) {
          Icon(Icons.Filled.Send, contentDescription = "Send", tint = PrimaryBlue)
        }
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
fun UserMessageRowPreview() {
  MaterialTheme {
    MessageRow(
      message =
        ChatMessage(
          id = 1,
          text = "Hello! Where is the Louvre located?",
          isUser = true,
          sender = "Sarah J. Chen",
        )
    )
  }
}

@Preview(showBackground = true)
@Composable
fun BotMessageRowPreview() {
  MaterialTheme {
    MessageRow(
      message =
        ChatMessage(
          id = 2,
          text = "The Louvre is located in the center of Paris, on the right bank of the Seine.",
          isUser = false,
          sender = "Museum Assistant",
        )
    )
  }
}
