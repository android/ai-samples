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

package com.example.jetpacker.data.itinerary

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for managing historical conversations with the Travel Assistant.
 */
@Dao
interface AssistantConversationDao {
  /**
   * Retrieves all historical conversations for a specific trip, ordered with the most recent at the top.
   */
  @Query("SELECT * FROM assistant_conversations WHERE tripId = :tripId ORDER BY timestamp DESC")
  fun getConversationsForTrip(tripId: String): Flow<List<AssistantConversation>>

  @Query("SELECT * FROM assistant_conversations WHERE id = :id LIMIT 1")
  suspend fun getConversationById(id: String): AssistantConversation?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertConversation(conversation: AssistantConversation)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertConversations(conversations: List<AssistantConversation>)

  @Delete
  suspend fun deleteConversation(conversation: AssistantConversation)

  @Query("DELETE FROM assistant_conversations WHERE id = :id")
  suspend fun deleteConversationById(id: String)

  @Query("DELETE FROM assistant_conversations WHERE tripId = :tripId")
  suspend fun deleteAllForTrip(tripId: String)

  @Query("DELETE FROM assistant_conversations")
  suspend fun deleteAllConversations()
}
