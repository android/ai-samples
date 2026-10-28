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

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jetpacker.data.itinerary.AssistantConversation
import com.example.jetpacker.data.itinerary.AssistantConversationDao
import com.example.jetpacker.feature.assistant.model.ActionStepCategory
import com.example.jetpacker.feature.assistant.model.ActionStepStatus
import com.example.jetpacker.feature.assistant.model.ConversationTurnUi
import com.example.jetpacker.feature.assistant.model.UserActionStep
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel
class ChatHistoryViewModel
@Inject
constructor(
  private val conversationDao: AssistantConversationDao,
) : ViewModel() {

  fun getConversations(tripId: String): Flow<List<AssistantConversation>> {
    return conversationDao.getConversationsForTrip(tripId)
  }

  fun deleteConversation(id: String) {
    viewModelScope.launch {
      try {
        conversationDao.deleteConversationById(id)
      } catch (e: Exception) {
        Log.e("ChatHistoryViewModel", "Failed to delete conversation $id", e)
      }
    }
  }

  fun clearAll(tripId: String) {
    viewModelScope.launch {
      try {
        conversationDao.deleteAllForTrip(tripId)
      } catch (e: Exception) {
        Log.e("ChatHistoryViewModel", "Failed to clear all conversations for trip $tripId", e)
      }
    }
  }

  /**
   * Pre-seeds initial sample conversations for demo trips if none exist yet.
   */
  fun seedDemoConversationsIfEmpty(tripId: String) {
    viewModelScope.launch {
      try {
        val existing = conversationDao.getConversationsForTrip(tripId).first()
        if (existing.isEmpty()) {
          val samples = createDemoConversationsForTrip(tripId)
          if (samples.isNotEmpty()) {
            conversationDao.insertConversations(samples)
          }
        }
      } catch (e: Exception) {
        Log.w("ChatHistoryViewModel", "Could not seed demo conversations", e)
      }
    }
  }

  private fun createDemoConversationsForTrip(tripId: String): List<AssistantConversation> {
    val now = System.currentTimeMillis()
    val hourAgo = now - 3600 * 1000L
    val dayAgo = now - 24 * 3600 * 1000L

    return when (tripId) {
      "2026-1" -> { // Android Dev Summit
        listOf(
          AssistantConversation(
            id = "demo-conv-ads-1",
            tripId = tripId,
            title = "What flights do I have scheduled for the summit?",
            timestamp = hourAgo,
            lastMessage = "You have United Airlines flight UA 452 arriving at San Francisco (SFO) on Oct 27 at 11:30 AM.",
            turnCount = 1,
            turnsJson =
              listOf(
                ConversationTurnUi(
                  id = UUID.randomUUID().toString(),
                  userPrompt = "What flights do I have scheduled for the summit?",
                  assistantMessage =
                    "You have United Airlines flight UA 452 arriving at San Francisco (SFO) on Oct 27 at 11:30 AM.",
                  actionSteps =
                    listOf(
                      UserActionStep(
                        category = ActionStepCategory.FLIGHT,
                        status = ActionStepStatus.COMPLETED,
                        title = "Checked flight itinerary",
                        subtitle = "United Airlines UA 452 • SFO",
                      )
                    ),
                )
              ).toTurnsJson(),
          ),
          AssistantConversation(
            id = "demo-conv-ads-2",
            tripId = tripId,
            title = "Please add 45 dollars to my expenses for the airport Uber to Mountain View.",
            timestamp = dayAgo,
            lastMessage = "Added $45.00 for airport Uber to Mountain View to your trip expenses under Transit.",
            turnCount = 1,
            turnsJson =
              listOf(
                ConversationTurnUi(
                  id = UUID.randomUUID().toString(),
                  userPrompt = "Please add 45 dollars to my expenses for the airport Uber to Mountain View.",
                  assistantMessage =
                    "Added $45.00 for airport Uber to Mountain View to your trip expenses under Transit.",
                  actionSteps =
                    listOf(
                      UserActionStep(
                        category = ActionStepCategory.EXPENSE,
                        status = ActionStepStatus.COMPLETED,
                        title = "Added $45.00 expense",
                        subtitle = "Airport Uber to Mountain View",
                      )
                    ),
                )
              ).toTurnsJson(),
          ),
        )
      }
      "2026-4" -> { // Tokyo Explorer
        listOf(
          AssistantConversation(
            id = "demo-conv-tokyo-1",
            tripId = tripId,
            title = "What's the address of our hotel in Shibuya?",
            timestamp = dayAgo,
            lastMessage = "You are staying at Cerulean Tower Tokyu Hotel in Shibuya, Tokyo. Check-in starts at 3:00 PM.",
            turnCount = 1,
            turnsJson =
              listOf(
                ConversationTurnUi(
                  id = UUID.randomUUID().toString(),
                  userPrompt = "What's the address of our hotel in Shibuya?",
                  assistantMessage =
                    "You are staying at Cerulean Tower Tokyu Hotel in Shibuya, Tokyo. Check-in starts at 3:00 PM.",
                  actionSteps =
                    listOf(
                      UserActionStep(
                        category = ActionStepCategory.ITINERARY,
                        status = ActionStepStatus.COMPLETED,
                        title = "Checked hotel details",
                        subtitle = "Cerulean Tower Tokyu Hotel",
                      )
                    ),
                )
              ).toTurnsJson(),
          )
        )
      }
      else -> emptyList()
    }
  }
}
