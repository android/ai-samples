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

package com.example.jetpacker.feature.assistant

import com.example.jetpacker.data.itinerary.AssistantConversation
import com.example.jetpacker.data.itinerary.AssistantConversationDao
import com.example.jetpacker.feature.assistant.history.ChatHistoryViewModel
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatHistoryViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var fakeDao: FakeAssistantConversationDao
  private lateinit var viewModel: ChatHistoryViewModel

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    fakeDao = FakeAssistantConversationDao()
    viewModel = ChatHistoryViewModel(fakeDao)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun getConversations_returnsConversationsForTrip() = runTest(testDispatcher) {
    val conv1 =
      AssistantConversation(
        id = "c1",
        tripId = "trip-1",
        title = "Check flight",
        timestamp = 1000L,
        lastMessage = "Flight details",
      )
    val conv2 =
      AssistantConversation(
        id = "c2",
        tripId = "trip-2",
        title = "Other trip",
        timestamp = 2000L,
        lastMessage = "Other details",
      )
    fakeDao.insertConversation(conv1)
    fakeDao.insertConversation(conv2)

    val trip1Convs = viewModel.getConversations("trip-1").first()
    assertThat(trip1Convs).containsExactly(conv1)
  }

  @Test
  fun deleteConversation_removesFromDao() = runTest(testDispatcher) {
    val conv =
      AssistantConversation(
        id = "c1",
        tripId = "trip-1",
        title = "To be deleted",
        timestamp = 1000L,
        lastMessage = "Delete me",
      )
    fakeDao.insertConversation(conv)

    viewModel.deleteConversation("c1")
    advanceUntilIdle()

    val remaining = viewModel.getConversations("trip-1").first()
    assertThat(remaining).isEmpty()
  }

  @Test
  fun clearAll_deletesAllConversationsForTrip() = runTest(testDispatcher) {
    fakeDao.insertConversation(
      AssistantConversation(
        id = "c1",
        tripId = "trip-1",
        title = "Conv 1",
        timestamp = 100L,
        lastMessage = "Msg 1",
      )
    )
    fakeDao.insertConversation(
      AssistantConversation(
        id = "c2",
        tripId = "trip-1",
        title = "Conv 2",
        timestamp = 200L,
        lastMessage = "Msg 2",
      )
    )
    fakeDao.insertConversation(
      AssistantConversation(
        id = "c3",
        tripId = "trip-2",
        title = "Conv 3",
        timestamp = 300L,
        lastMessage = "Msg 3",
      )
    )

    viewModel.clearAll("trip-1")
    advanceUntilIdle()

    val trip1Remaining = viewModel.getConversations("trip-1").first()
    assertThat(trip1Remaining).isEmpty()

    val trip2Remaining = viewModel.getConversations("trip-2").first()
    assertThat(trip2Remaining).hasSize(1)
  }

  @Test
  fun seedDemoConversationsIfEmpty_seedsWhenEmpty() = runTest(testDispatcher) {
    viewModel.seedDemoConversationsIfEmpty("2026-1")
    advanceUntilIdle()

    val seeded = viewModel.getConversations("2026-1").first()
    assertThat(seeded).isNotEmpty()
    assertThat(seeded.first().tripId).isEqualTo("2026-1")
  }
}

private class FakeAssistantConversationDao : AssistantConversationDao {
  private val conversations = MutableStateFlow<Map<String, AssistantConversation>>(emptyMap())

  override fun getConversationsForTrip(tripId: String): Flow<List<AssistantConversation>> {
    return conversations.map { map ->
      map.values.filter { it.tripId == tripId }.sortedByDescending { it.timestamp }
    }
  }

  override suspend fun getConversationById(id: String): AssistantConversation? {
    return conversations.value[id]
  }

  override suspend fun insertConversation(conversation: AssistantConversation) {
    conversations.value = conversations.value + (conversation.id to conversation)
  }

  override suspend fun insertConversations(conversations: List<AssistantConversation>) {
    this.conversations.value = this.conversations.value + conversations.associateBy { it.id }
  }

  override suspend fun deleteConversation(conversation: AssistantConversation) {
    conversations.value = conversations.value - conversation.id
  }

  override suspend fun deleteConversationById(id: String) {
    conversations.value = conversations.value - id
  }

  override suspend fun deleteAllForTrip(tripId: String) {
    conversations.value = conversations.value.filterValues { it.tripId != tripId }
  }
}
