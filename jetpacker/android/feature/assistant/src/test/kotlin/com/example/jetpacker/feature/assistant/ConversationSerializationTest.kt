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
import com.example.jetpacker.feature.assistant.history.toConversationTurns
import com.example.jetpacker.feature.assistant.history.toTurnsJson
import com.example.jetpacker.feature.assistant.model.ActionStepCategory
import com.example.jetpacker.feature.assistant.model.ActionStepStatus
import com.example.jetpacker.feature.assistant.model.ConversationTurnUi
import com.example.jetpacker.feature.assistant.model.UserActionStep
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ConversationSerializationTest {

  @Test
  fun roundTripSerialization_preservesAllFields() {
    val steps =
      listOf(
        UserActionStep(
          id = "step-1",
          category = ActionStepCategory.EXPENSE,
          status = ActionStepStatus.COMPLETED,
          title = "Added $45 Uber to Mountain View",
          subtitle = "Total expense: $45.00 USD",
        ),
        UserActionStep(
          id = "step-2",
          category = ActionStepCategory.ITINERARY,
          status = ActionStepStatus.COMPLETED,
          title = "Checked flight schedule",
        ),
      )

    val turns =
      listOf(
        ConversationTurnUi(
          id = "turn-1",
          userPrompt = "Add $45 for Uber",
          assistantMessage = "Added $45.00 USD for your Uber ride.",
          actionSteps = steps,
        ),
        ConversationTurnUi(
          id = "turn-2",
          userPrompt = "What flights do I have?",
          assistantMessage = "You have flight UA 428 departing tomorrow.",
          actionSteps = emptyList(),
        ),
      )

    val json = turns.toTurnsJson()
    assertThat(json).isNotEmpty()

    val conversation =
      AssistantConversation(
        id = "conv-1",
        tripId = "trip-1",
        title = "Uber expense & flight check",
        timestamp = System.currentTimeMillis(),
        lastMessage = "You have flight UA 428 departing tomorrow.",
        turnCount = 2,
        turnsJson = json,
      )

    val deserialized = conversation.toConversationTurns()
    assertThat(deserialized).hasSize(2)

    val firstTurn = deserialized[0]
    assertThat(firstTurn.id).isEqualTo("turn-1")
    assertThat(firstTurn.userPrompt).isEqualTo("Add $45 for Uber")
    assertThat(firstTurn.assistantMessage).isEqualTo("Added $45.00 USD for your Uber ride.")
    assertThat(firstTurn.actionSteps).hasSize(2)
    assertThat(firstTurn.actionSteps[0].id).isEqualTo("step-1")
    assertThat(firstTurn.actionSteps[0].category).isEqualTo(ActionStepCategory.EXPENSE)
    assertThat(firstTurn.actionSteps[0].status).isEqualTo(ActionStepStatus.COMPLETED)
    assertThat(firstTurn.actionSteps[0].title).isEqualTo("Added $45 Uber to Mountain View")
    assertThat(firstTurn.actionSteps[0].subtitle).isEqualTo("Total expense: $45.00 USD")

    val secondTurn = deserialized[1]
    assertThat(secondTurn.id).isEqualTo("turn-2")
    assertThat(secondTurn.userPrompt).isEqualTo("What flights do I have?")
    assertThat(secondTurn.assistantMessage).isEqualTo("You have flight UA 428 departing tomorrow.")
    assertThat(secondTurn.actionSteps).isEmpty()
  }

  @Test
  fun emptyList_serializesToEmptyJsonArray() {
    val json = emptyList<ConversationTurnUi>().toTurnsJson()
    assertThat(json).isEqualTo("[]")

    val conversation =
      AssistantConversation(
        id = "c1",
        tripId = "t1",
        title = "Title",
        timestamp = 1000L,
        lastMessage = "Msg",
        turnsJson = json,
      )
    assertThat(conversation.toConversationTurns()).isEmpty()
  }

  @Test
  fun blankOrInvalidJson_returnsEmptyList() {
    val blankConversation =
      AssistantConversation(
        id = "c1",
        tripId = "t1",
        title = "",
        timestamp = 1000L,
        lastMessage = "",
        turnsJson = "   ",
      )
    assertThat(blankConversation.toConversationTurns()).isEmpty()

    val corruptConversationWithoutFallback =
      AssistantConversation(
        id = "c2",
        tripId = "t1",
        title = "",
        timestamp = 1000L,
        lastMessage = "",
        turnsJson = "{ not a valid json [",
      )
    assertThat(corruptConversationWithoutFallback.toConversationTurns()).isEmpty()
  }

  @Test
  fun corruptJsonWithMetadata_fallsBackToSingleTurn() {
    val corruptWithMetadata =
      AssistantConversation(
        id = "c3",
        tripId = "t1",
        title = "Flight query",
        timestamp = 1000L,
        lastMessage = "Flight details",
        turnsJson = "{ corrupt json [",
      )
    val turns = corruptWithMetadata.toConversationTurns()
    assertThat(turns).hasSize(1)
    assertThat(turns[0].userPrompt).isEqualTo("Flight query")
    assertThat(turns[0].assistantMessage).isEqualTo("Flight details")
  }
}
