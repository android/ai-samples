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

package com.example.jetpacker.feature.assistant.model

import com.example.jetpacker.data.itinerary.TravelDocument
import java.util.UUID

/** Record of a single tool execution during an agent turn. */
data class ToolExecutionRecord(
  val name: String,
  val args: Map<String, Any?> = emptyMap(),
  val response: Map<String, Any?>? = null,
  val id: String = UUID.randomUUID().toString(),
)

/**
 * Sealed hierarchy of structured travel entities surfaced directly to the user as rich UI cards
 * alongside the assistant's answer.
 */
sealed interface HighlightedEntity {
  /** Represents a travel document (e.g. passport or visa) with its checked validity status. */
  data class TravelDocumentHighlight(
    val document: TravelDocument,
    val validityStatus: String,
    val validityExplanation: String,
  ) : HighlightedEntity

  /** Represents flight details highlighted from the traveler's itinerary. */
  data class FlightDetailHighlight(
    val flightNumber: String,
    val airline: String,
    val origin: String,
    val destination: String,
    val gate: String? = null,
    val terminal: String? = null,
    val status: String? = null,
  ) : HighlightedEntity

  /** Represents an expense recorded by the assistant. */
  data class ExpenseHighlight(
    val title: String,
    val amount: Double,
    val currency: String,
    val category: String,
    val tripId: String,
  ) : HighlightedEntity

  /** Represents an itinerary event or reminder scheduled by the assistant. */
  data class ItineraryEventHighlight(
    val title: String,
    val type: String,
    val location: String,
    val tripId: String,
  ) : HighlightedEntity

  /** Represents a voice note or reminder linked to an itinerary event. */
  data class VoiceNoteHighlight(
    val title: String,
    val transcription: String,
    val matchedEventTitle: String? = null,
    val tripId: String,
  ) : HighlightedEntity

  /** Represents multiple actions executed together in a single voice command. */
  data class MultiActionHighlight(
    val expenseTitle: String? = null,
    val expenseAmount: Double? = null,
    val expenseCurrency: String? = null,
    val eventTitle: String? = null,
    val eventLocation: String? = null,
    val tripTitle: String? = null,
  ) : HighlightedEntity
}

/** High-level domain category used to pick friendly icons & colors — never exposed as code. */
enum class ActionStepCategory {
  EXPENSE,
  REMINDER,
  FLIGHT,
  DOCUMENT,
  ITINERARY,
  TRIP,
}

/** Execution lifecycle status of a single user-visible assistant step. */
enum class ActionStepStatus {
  IN_PROGRESS,
  COMPLETED,
  FAILED,
}

/**
 * Consumer-friendly representation of an assistant action. Never exposes raw function names, JSON
 * keys, or database IDs.
 */
data class UserActionStep(
  val id: String = UUID.randomUUID().toString(),
  val category: ActionStepCategory,
  val status: ActionStepStatus,
  val title: String,
  val subtitle: String? = null,
)

/** Represents a single user-to-assistant turn in the multi-turn conversational history (Demo 3). */
data class ConversationTurnUi(
  val id: String = UUID.randomUUID().toString(),
  val userPrompt: String,
  val actionSteps: List<UserActionStep> = emptyList(),
  val assistantMessage: String? = null,
  val highlightedEntity: HighlightedEntity? = null,
  val isStreaming: Boolean = false,
)

/** Unified state machine for the ambient assistant sheet across Demos 1, 2, and 3. */
sealed interface AssistantOverlayState {
  /** Idle pill docked above the bottom navigation bar. */
  data object Collapsed : AssistantOverlayState

  /** Active voice capture with live speech-to-text transcript. */
  data class Listening(val liveTranscript: String) : AssistantOverlayState

  /** Demos 1 & 2 (in-progress): Friendly action steps while underlying screen updates. */
  data class PerformingActions(val userPrompt: String, val steps: List<UserActionStep>) :
    AssistantOverlayState

  /** Demos 1 & 2 (complete): Compact confirmation pill (no chat bubble), auto-collapses. */
  data class ActionCompleted(
    val userPrompt: String = "",
    val steps: List<UserActionStep>,
    val summaryCard: HighlightedEntity? = null,
    val textResponse: String? = null,
  ) : AssistantOverlayState

  /** Demo 3: Multi-turn conversational pane with action steps, answer bubbles, and memory. */
  data class Conversational(
    val turns: List<ConversationTurnUi>,
    val activeSteps: List<UserActionStep> = emptyList(),
    val isListening: Boolean = false,
    val liveTranscript: String = "",
  ) : AssistantOverlayState
}

/** UI state representing the on-device Travel Assistant lifecycle. */
sealed interface TravelAgentUiState {
  /** Initial ready state with quick prompt chips. */
  data object Idle : TravelAgentUiState

  /** The assistant is analyzing the user prompt and planning tool invocations. */
  data class Thinking(val stepSummary: String = "Analyzing your travel plans...") :
    TravelAgentUiState

  /** A tool is currently executing on device. */
  data class ExecutingTool(
    val toolName: String,
    val args: Map<String, Any?> = emptyMap(),
    val executedTools: List<ToolExecutionRecord> = emptyList(),
  ) : TravelAgentUiState

  /** The assistant turn completed successfully. */
  data class Success(
    val answerText: String,
    val highlightedEntity: HighlightedEntity? = null,
    val toolExecutions: List<ToolExecutionRecord> = emptyList(),
    val latencyMs: Long? = null,
    val userActionSteps: List<UserActionStep> = emptyList(),
    val isDirectMutationOnly: Boolean = false,
  ) : TravelAgentUiState

  /** An error occurred during execution. */
  data class Error(val message: String) : TravelAgentUiState
}
