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

import com.example.jetpacker.data.itinerary.AssistantConversation
import com.example.jetpacker.feature.assistant.model.ActionStepCategory
import com.example.jetpacker.feature.assistant.model.ActionStepStatus
import com.example.jetpacker.feature.assistant.model.ConversationTurnUi
import com.example.jetpacker.feature.assistant.model.UserActionStep
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

/**
 * Converts a list of [ConversationTurnUi] to a persistent JSON string.
 */
fun List<ConversationTurnUi>.toTurnsJson(): String {
  val jsonArr = JSONArray()
  for (turn in this) {
    val obj = JSONObject()
    obj.put("id", turn.id)
    obj.put("userPrompt", turn.userPrompt)
    turn.assistantMessage?.let { obj.put("assistantMessage", it) }

    val stepsArr = JSONArray()
    for (step in turn.actionSteps) {
      val stepObj = JSONObject()
      stepObj.put("id", step.id)
      stepObj.put("category", step.category.name)
      stepObj.put("status", step.status.name)
      stepObj.put("title", step.title)
      step.subtitle?.let { stepObj.put("subtitle", it) }
      stepsArr.put(stepObj)
    }
    obj.put("actionSteps", stepsArr)
    jsonArr.put(obj)
  }
  return jsonArr.toString()
}

/**
 * Parses [AssistantConversation.turnsJson] back into a list of [ConversationTurnUi].
 */
fun AssistantConversation.toConversationTurns(): List<ConversationTurnUi> {
  if (turnsJson.isBlank()) return emptyList()
  val turns = mutableListOf<ConversationTurnUi>()
  try {
    val jsonArr = JSONArray(turnsJson)
    for (i in 0 until jsonArr.length()) {
      val obj = jsonArr.getJSONObject(i)
      val id = obj.optString("id", UUID.randomUUID().toString())
      val userPrompt = obj.optString("userPrompt", "")
      val assistantMessage =
        if (obj.has("assistantMessage") && !obj.isNull("assistantMessage")) {
          obj.getString("assistantMessage")
        } else {
          null
        }

      val steps = mutableListOf<UserActionStep>()
      val stepsArr = obj.optJSONArray("actionSteps")
      if (stepsArr != null) {
        for (j in 0 until stepsArr.length()) {
          val sObj = stepsArr.getJSONObject(j)
          val catName = sObj.optString("category", "TRIP")
          val cat =
            try {
              ActionStepCategory.valueOf(catName)
            } catch (_: Exception) {
              ActionStepCategory.TRIP
            }
          val statusName = sObj.optString("status", "COMPLETED")
          val status =
            try {
              ActionStepStatus.valueOf(statusName)
            } catch (_: Exception) {
              ActionStepStatus.COMPLETED
            }
          steps.add(
            UserActionStep(
              id = sObj.optString("id", UUID.randomUUID().toString()),
              category = cat,
              status = status,
              title = sObj.optString("title", ""),
              subtitle =
                if (sObj.has("subtitle") && !sObj.isNull("subtitle")) {
                  sObj.getString("subtitle")
                } else {
                  null
                },
            )
          )
        }
      }

      turns.add(
        ConversationTurnUi(
          id = id,
          userPrompt = userPrompt,
          assistantMessage = assistantMessage,
          actionSteps = steps,
          isStreaming = false,
        )
      )
    }
  } catch (_: Exception) {
    // Return empty or fallback single turn if corrupted
    if (title.isNotBlank() || lastMessage.isNotBlank()) {
      return listOf(
        ConversationTurnUi(
          id = id,
          userPrompt = title,
          assistantMessage = lastMessage,
          isStreaming = false,
        )
      )
    }
  }
  return turns
}
