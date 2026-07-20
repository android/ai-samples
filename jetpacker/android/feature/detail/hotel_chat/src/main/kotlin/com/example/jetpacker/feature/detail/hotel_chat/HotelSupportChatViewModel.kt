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

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.ai.InferenceMode
import com.google.firebase.ai.OnDeviceConfig
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.content
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.nl.languageid.LanguageIdentification


class HotelSupportChatViewModel(val hotelName: String = "Hotel", val language: String = "English") :
  ViewModel() {

  private val _messages = MutableStateFlow<List<SupportChatMessage>>(emptyList())
  val messages: StateFlow<List<SupportChatMessage>> = _messages.asStateFlow()

  private val _currentUserId = MutableStateFlow("User")
  val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

  private val _translations = MutableStateFlow<Map<String, String>>(emptyMap())
  val translations: StateFlow<Map<String, String>> = _translations.asStateFlow()

  private val _selectedLanguage = MutableStateFlow("English")
  val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

  fun setSelectedLanguage(language: String) {
    _selectedLanguage.value = language
  }

  // ML Kit for Language Identification
  private val languageIdentifier = LanguageIdentification.getClient()

  // ML Kit / Gemini Nano for on-device translation
  @OptIn(PublicPreviewAPI::class)
  private val hybridTranslationModel =
    Firebase.ai(backend = GenerativeBackend.googleAI())
      .generativeModel(
        modelName = "gemini-3-flash-preview",
        onDeviceConfig = OnDeviceConfig(mode = InferenceMode.PREFER_ON_DEVICE),
      )

  // Cloud translator model (TranslateGemma fallback)
  @OptIn(PublicPreviewAPI::class)
  private val cloudTranslationModel =
    Firebase.ai(backend = GenerativeBackend.googleAI())
      .generativeModel(
        modelName = "gemini-3-flash-preview", // Represents cloud TranslateGemma
      )

  // Firebase AI for chat
  private val generativeModel =
    Firebase.ai(backend = GenerativeBackend.googleAI())
      .generativeModel(
        systemInstruction =
          content {
            text(
              "You are a helpful hotel receptionist at $hotelName only speaking $language. Answer politely in $language. The bar closes at 10pm and breakfast is from 7am to 10am. There's someone at the desk 24/7. You can retrieve your luggage from the storage room at the back of the lobby at any time."
            )
          },
        modelName = "gemini-3-flash-preview",
      )

  private val chat = generativeModel.startChat()

  fun translateMessage(message: SupportChatMessage) {
    if (message.id.isEmpty()) return
    viewModelScope.launch {
      try {
        _translations.update { current -> current + (message.id to "\n\nTranslating...") }
        val lang = _selectedLanguage.value

        // 1. Detect language using ML Kit Language Identification
        val sourceLang = try {
          Tasks.await(languageIdentifier.identifyLanguage(message.text))
        } catch (e: Exception) {
          "und"
        }

        // 2. Custom routing:
        // We route to TranslateGemma in the Cloud for specific languages (e.g. French 'fr'),
        // and run on-device Gemini Nano for others (e.g. English, Dutch, or unrecognized).
        val routeToCloud = sourceLang == "fr"

        val prompt =
          "Translate the following text to $lang. Just return the translated sentence: ${message.text}."

        val (translatedText, routePrefix) = if (routeToCloud) {
          val result = cloudTranslationModel.generateContent(prompt)
          result.text to "[Cloud / TranslateGemma]"
        } else {
          val result = hybridTranslationModel.generateContent(prompt)
          result.text to "[On-Device / Gemini Nano]"
        }

        if (translatedText != null) {
          _translations.update { current ->
            current + (message.id to "\n\n$routePrefix: $translatedText")
          }
        }
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        Log.e("HotelSupportChatViewModel", "Custom Hybrid Translation Error", e)
        _translations.update { current ->
          current + (message.id to "\n\nError: ${e.message}")
        }
      }
    }
  }

  fun setUserId(userId: String) {
    // Do nothing
  }

  fun sendMessage(text: String) {
    if (text.isBlank()) return
    val botMsgId = UUID.randomUUID().toString()
    _messages.update { current ->
      val userMessage =
        SupportChatMessage(
          id = UUID.randomUUID().toString(),
          text = text,
          senderId = "User",
          senderName = "You",
          timestamp = Timestamp.now(),
        )
      val botMessage =
        SupportChatMessage(
          id = botMsgId,
          text = "Typing...",
          senderId = "AI",
          senderName = "Hotel Support",
          timestamp = Timestamp.now(),
        )
      current + userMessage + botMessage
    }

    viewModelScope.launch {
      try {
        val response = chat.sendMessage(text)
        _messages.update { current ->
          current.map { msg ->
            if (msg.id == botMsgId) {
              msg.copy(text = response.text?.trim() ?: "No response")
            } else {
              msg
            }
          }
        }
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        Log.e("HotelSupportChatViewModel", "Chat Error", e)
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

  data class SupportChatMessage(
    val id: String = "",
    val text: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val timestamp: Timestamp? = null,
  )
}
