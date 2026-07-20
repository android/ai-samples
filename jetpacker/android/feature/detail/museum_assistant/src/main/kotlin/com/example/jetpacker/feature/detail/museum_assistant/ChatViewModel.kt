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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jetpacker.core.flags.FeatureFlags
import com.example.jetpacker.data.itinerary.EventDao
import com.example.jetpacker.data.itinerary.MuseumDetail
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

@HiltViewModel
class ChatViewModel
@Inject
constructor(savedStateHandle: SavedStateHandle, private val eventDao: EventDao) : ViewModel() {

  private val urlList = mutableListOf<String>()

  private var toolList = mutableListOf<Tool>()

  init {
    savedStateHandle.get<String>("eventId")?.let { loadDetail(it) }
    if (FeatureFlags.ENABLE_SEARCH_GROUNDING) {
      toolList.add(Tool.googleSearch())
    }
    if (FeatureFlags.ENABLE_URL_GROUNDING) {
      toolList.add(Tool.urlContext())
    }
  }

  private fun loadDetail(eventId: String) {
    viewModelScope.launch {
      eventDao.getMuseumDetail(eventId).collectLatest { museumDetail ->
        museumDetail?.let { m -> urlList.addAll(m.infoUrls) }
      }
    }
  }

  private val generativeModel =
    Firebase.ai(backend = GenerativeBackend.googleAI())
      .generativeModel(
        systemInstruction =
          content {
            text(
              "You are a helpful museum assistant, answering questions about a museum. Never use markdown, use plain text."
            )
          },
        modelName = "gemini-3.1-flash-lite",
        tools = toolList,
      )

  private val chat = generativeModel.startChat()

  private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
  val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

  fun sendMessage(text: String) {
    if (text.isBlank()) return

    val botMsgId = _messages.value.size + 2
    _messages.update { current ->
      val userMsg =
        ChatMessage(id = current.size + 1, text = text, isUser = true, sender = "You")
      val botMsg =
        ChatMessage(id = current.size + 2, text = "Thinking...", isUser = false, sender = "Museum Assistant")
      current + userMsg + botMsg
    }

    viewModelScope.launch {
      try {
        val prompt =
          "$text ${if (FeatureFlags.ENABLE_URL_GROUNDING) "\n\n If the following is message above about the rules and terms to visit Le Louvre, if needed answer this urls ${urlList.joinToString()}" else ""}"
        var response =
          chat.sendMessage(
            prompt
          )
        _messages.update { current ->
          current.map { msg ->
            if (msg.id == botMsgId) {
              msg.copy(text = response.text?.trim() ?: "")
            } else {
              msg
            }
          }
        }
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        _messages.update { current ->
          current.map { msg ->
            if (msg.id == botMsgId) {
              msg.copy(text = "Error: ${e.message}")
            } else {
              msg
            }
          }
        }
      }
    }
  }
}
