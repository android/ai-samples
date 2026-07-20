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

package com.example.jetpacker.feature.detail.hotel_chat

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Surface
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import com.example.jetpacker.feature.detail.hotel_chat.HotelSupportChatViewModel.SupportChatMessage
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotelSupportChat(
  onBack: () -> Unit = {},
  hotelName: String = "Hotel",
  language: String = "English",
  viewModel: HotelSupportChatViewModel = viewModel { HotelSupportChatViewModel(hotelName, language) },
) {
  val messages by viewModel.messages.collectAsStateWithLifecycle()
  val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()
  val translations by viewModel.translations.collectAsStateWithLifecycle()
  val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()

  HotelSupportChatContent(
    hotelName = hotelName,
    messages = messages,
    currentUserId = currentUserId,
    translations = translations,
    selectedLanguage = selectedLanguage,
    onSendMessage = { viewModel.sendMessage(it) },
    onTranslateMessage = { viewModel.translateMessage(it) },
    onSelectLanguage = { viewModel.setSelectedLanguage(it) },
    onBack = onBack
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotelSupportChatContent(
  hotelName: String,
  messages: List<SupportChatMessage>,
  currentUserId: String,
  translations: Map<String, String>,
  selectedLanguage: String,
  onSendMessage: (String) -> Unit,
  onTranslateMessage: (SupportChatMessage) -> Unit,
  onSelectLanguage: (String) -> Unit,
  onBack: () -> Unit = {}
) {
  var inputText by remember { mutableStateOf("") }
  var showMenu by remember { mutableStateOf(false) }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        title = {
          Text(
            text = if (currentUserId == "UserA") "Customer support" else hotelName,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
          )
        },
        actions = {
          IconButton(onClick = { showMenu = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "More options")
          }
          DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            val languages =
              listOf(
                "English",
                "German",
                "Dutch",
                "Spanish",
                "French",
                "한국어",
                "中文",
                "日本語",
                "українська",
              )
            languages.forEach { lang ->
              DropdownMenuItem(
                text = {
                  Text(
                    text = lang,
                    fontWeight =
                      if (lang == selectedLanguage) FontWeight.Bold else FontWeight.Normal,
                  )
                },
                onClick = {
                  onSelectLanguage(lang)
                  showMenu = false
                },
              )
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(),
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
      modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item { Spacer(modifier = Modifier.height(8.dp)) }

      itemsIndexed(messages) { index, msg: SupportChatMessage ->
        val isLatest = index == messages.size - 1
        val translation = translations[msg.id]
        FirestoreMessageRow(
          msg,
          currentUserId,
          isLatest,
          translation,
          selectedLanguage,
          onTranslate = { onTranslateMessage(msg) },
        )
      }

      item { Spacer(modifier = Modifier.height(16.dp)) }
    }
  }
}

@Composable
fun FirestoreMessageRow(
  message: SupportChatMessage,
  currentUserId: String,
  isLatest: Boolean,
  translation: String?,
  selectedLanguage: String,
  onTranslate: () -> Unit,
) {
  val isCurrentUser = message.senderId == currentUserId
  Column(modifier = Modifier.fillMaxWidth()) {
    if (isCurrentUser) {
      UserMessageRowFixed(message)
    } else {
      BotMessageRowFixed(message, translation)
    }

    if (translation == null && isLatest && !isCurrentUser) {
      Text(
        text = "Translate to $selectedLanguage",
        style = MaterialTheme.typography.labelLarge,
        color = PrimaryBlue,
        modifier = Modifier.padding(start = 40.dp).clickable { onTranslate() }.padding(top = 4.dp),
      )
    }
  }
}

@Composable
fun UserMessageRowFixed(message: SupportChatMessage) {
  Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
    Text(
      text = message.senderName,
      style = MaterialTheme.typography.labelSmall,
      color = TextSecondary,
    )
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
      Icon(
        Icons.Filled.AccountCircle,
        contentDescription = "User",
        tint = TextSecondary,
        modifier = Modifier.size(32.dp),
      )
    }
  }
}

@Composable
fun BotMessageRowFixed(
  message: SupportChatMessage,
  translation: String? = null,
) {
  Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Start) {
      Icon(
        Icons.Filled.Face,
        contentDescription = "Other",
        tint = TextSecondary,
        modifier =
          Modifier.size(32.dp).clip(CircleShape).background(Color(0xFFE2E8F0)).padding(4.dp),
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = message.senderName,
        style = MaterialTheme.typography.labelSmall,
        color = TextSecondary,
      )
    }
    Spacer(modifier = Modifier.height(4.dp))
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Start) {
      Spacer(modifier = Modifier.width(40.dp))
      Box(
        modifier =
          Modifier.clip(RoundedCornerShape(topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
            .background(BotBubble)
            .padding(16.dp)
      ) {
        Column {
          Text(
            text = message.text,
            color = TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
          )
          if (translation != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = translation,
              color = PrimaryBlue,
              style = MaterialTheme.typography.labelLarge,
            )
          }
        }
      }
    }
  }
}

val PrimaryBlue = Color(0xFF0F172A)
val UserBubble = Color(0xFF0F172A)
val BotBubble = Color(0xFFF1F5F9)
val TextPrimary = Color(0xFF1E293B)
val TextSecondary = Color(0xFF64748B)

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

