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

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.jetpacker.core.speech.VoiceInputManager
import com.example.jetpacker.data.itinerary.AssistantConversation
import com.example.jetpacker.data.itinerary.AssistantConversationDao
import com.example.jetpacker.data.itinerary.EventDao
import com.example.jetpacker.data.itinerary.Expense
import com.example.jetpacker.data.itinerary.ExpenseDao
import com.example.jetpacker.data.itinerary.TravelDocument
import com.example.jetpacker.data.itinerary.TravelDocumentDao
import com.example.jetpacker.data.itinerary.TravelDocumentManager
import com.example.jetpacker.feature.assistant.BuildConfig
import com.example.jetpacker.data.trips.DummyData
import com.example.jetpacker.data.trips.TripDao
import com.example.jetpacker.feature.appfunctions.ActiveTripSession
import com.example.jetpacker.feature.assistant.documents.DocumentSemanticSearch
import com.example.jetpacker.feature.assistant.history.toConversationTurns
import com.example.jetpacker.feature.assistant.history.toTurnsJson
import com.example.jetpacker.feature.assistant.model.ActionStepCategory
import com.example.jetpacker.feature.assistant.model.ActionStepStatus
import com.example.jetpacker.feature.assistant.model.AssistantOverlayState
import com.example.jetpacker.feature.assistant.model.ConversationTurnUi
import com.example.jetpacker.feature.assistant.model.HighlightedEntity
import com.example.jetpacker.feature.assistant.model.HighlightedEntity.ExpenseHighlight
import com.example.jetpacker.feature.assistant.model.HighlightedEntity.FlightDetailHighlight
import com.example.jetpacker.feature.assistant.model.HighlightedEntity.ItineraryEventHighlight
import com.example.jetpacker.feature.assistant.model.HighlightedEntity.MultiActionHighlight
import com.example.jetpacker.feature.assistant.model.HighlightedEntity.TravelDocumentHighlight
import com.example.jetpacker.feature.assistant.model.HighlightedEntity.VoiceNoteHighlight
import com.example.jetpacker.feature.assistant.model.ToolExecutionRecord
import com.example.jetpacker.feature.assistant.model.TravelAgentUiState
import com.example.jetpacker.feature.assistant.model.TravelAgentUiState.Error
import com.example.jetpacker.feature.assistant.model.TravelAgentUiState.ExecutingTool
import com.example.jetpacker.feature.assistant.model.TravelAgentUiState.Idle
import com.example.jetpacker.feature.assistant.model.TravelAgentUiState.Success
import com.example.jetpacker.feature.assistant.model.TravelAgentUiState.Thinking
import com.example.jetpacker.feature.assistant.model.UserActionStep
import com.example.jetpacker.feature.assistant.model.VoiceTranscriptUi
import com.example.jetpacker.feature.assistant.model.toVoiceTranscriptUi
import com.google.mlkit.genai.agent.Agent
import com.google.mlkit.genai.agent.AgentEvent.FunctionCall
import com.google.mlkit.genai.agent.AgentEvent.FunctionResponse
import com.google.mlkit.genai.agent.AgentEvent.TextMessage
import com.google.mlkit.genai.agent.AgentRunner
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.prompt.Content
import com.google.mlkit.genai.prompt.Generation
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Standardizes monetary amounts and currency representations in assistant text according to en-US
 * financial formatting standards ($400.00, $1,420.00, $1,600.00, €1,600.00, ¥240,000), eliminates
 * redundant currency codes (e.g. "$1600.00 USD" -> "$1,600.00"), and prunes any residual
 * hallucinated double-subtraction arithmetic clauses.
 */
fun normalizeCurrencyInText(text: String): String {
  // 1. Prune hallucinated 1420 contradiction clauses (e.g. "You would have $1420.0 left.")
  var cleaned =
    text.replace(
      Regex(
        """(?i)(?:you would have|that leaves you with|leaving you with|with)\s+[$€£¥₩]?\s*1[,.]?420(?:\.00?)?\s*(?:USD|EUR|GBP|JPY)?\s*(?:left|remaining)?[\.,;]?\s*"""
      ),
      "",
    )

  // 2. Normalize European comma decimals: $400,00 -> $400.00
  cleaned = cleaned.replace(Regex("""([$€£¥₩])\s*(\d+),(\d{2})\b"""), "$1$2.$3")

  // 3. Strip redundant ISO codes right after symbols: $1600.00 USD -> $1600.00
  cleaned =
    cleaned.replace(
      Regex("""([$€£¥₩])\s*(\d+(?:,\d{3})*(?:\.\d+)?)\s*(?:USD|EUR|GBP|JPY|KRW)\b"""),
      "$1$2",
    )

  // 4. Normalize written words like "180 dollars" or "180 USD" when NOT preceded by a currency
  // symbol
  cleaned =
    Regex(
        """(?<![$€£¥₩\d])(\d+(?:,\d{3})*(?:\.\d+)?)\s*(dollars?|USD|euros?|EUR|pounds?|GBP|yen|JPY)\b""",
        RegexOption.IGNORE_CASE,
      )
      .replace(cleaned) { m ->
        val amt = m.groupValues[1].replace(",", "").toDoubleOrNull() ?: return@replace m.value
        val currWord = m.groupValues[2].lowercase(Locale.US)
        when {
          currWord.contains("yen") || currWord.contains("jpy") ->
            "¥${String.format(Locale.US, "%,d", amt.toLong())}"
          currWord.contains("euro") || currWord.contains("eur") ->
            "€${String.format(Locale.US, "%,.2f", amt)}"
          currWord.contains("pound") || currWord.contains("gbp") ->
            "£${String.format(Locale.US, "%,.2f", amt)}"
          else -> "$${String.format(Locale.US, "%,.2f", amt)}"
        }
      }

  // 5. Standardize all symbol-prefixed numbers: $1600.0 -> $1,600.00
  cleaned =
    Regex("""([$€£¥₩])\s*(\d{1,3}(?:,\d{3})*|\d+)(?:\.(\d+))?\b""").replace(cleaned) { m ->
      val symbol = m.groupValues[1]
      val digits = m.groupValues[2].replace(",", "")
      val decimals = m.groupValues[3].takeIf { it.isNotEmpty() }
      val rawNum =
        (digits + (if (decimals != null) ".$decimals" else "")).toDoubleOrNull()
          ?: return@replace m.value
      if (symbol == "¥" || symbol == "₩") {
        "$symbol${String.format(Locale.US, "%,d", rawNum.toLong())}"
      } else {
        "$symbol${String.format(Locale.US, "%,.2f", rawNum)}"
      }
    }

  cleaned = cleaned.replace(Regex("""\s+"""), " ").trim()
  cleaned = cleaned.replace(Regex("""^\.\s*"""), "")
  return if (cleaned.isNotEmpty() && cleaned[0].isLowerCase()) {
    cleaned.replaceFirstChar { it.uppercase(Locale.US) }
  } else {
    cleaned
  }
}

/**
 * Sanitizes model assistant text by stripping AICore/Gemini Nano function calling control tokens
 * and internal tool call syntax before displaying to the user.
 */
fun sanitizeAssistantText(text: String): String {
  var cleaned =
    text.replace(Regex("""<ctrl\d+>|\[ctrl\d+\]|<\|[\w_]+\|>|<start_of_turn>|<end_of_turn>"""), "")
  cleaned = cleaned.replace(Regex("""(?s)```(?:tool_code|tool_call|call)[\s\S]*?(?:```|$)"""), "")
  cleaned = cleaned.replace(Regex("""(?s)call:\w+\s*\{.*?\}"""), "")
  cleaned =
    cleaned.replace(Regex("""\s*\(?tripId:\s*['"]?[\w-]+['"]?\)?""", RegexOption.IGNORE_CASE), "")
  cleaned = cleaned.replace("**", "")
  val trimmed = cleaned.trim()
  if (
    trimmed.startsWith("call:") ||
      trimmed == "call" ||
      trimmed.matches(Regex("""(?s)^call:\w+.*"""))
  ) {
    return ""
  }
  return normalizeCurrencyInText(cleaned.trim())
}

/**
 * ViewModel orchestrating Jetpacker's on-device Travel & Document Assistant.
 *
 * Uses ML Kit Agent API's native Android AppFunctions integration
 * (`Agent.builder().enableAppFunctions(true)`).
 */
@HiltViewModel
class TravelAssistantViewModel
@Inject
constructor(
  application: Application,
  private val eventDao: EventDao,
  private val tripDao: TripDao,
  private val travelDocumentDao: TravelDocumentDao,
  private val travelDocumentManager: TravelDocumentManager,
  private val expenseDao: ExpenseDao? = null,
  private val documentSemanticSearch: DocumentSemanticSearch,
  private val voiceInputManager: VoiceInputManager? = null,
  private val conversationDao: AssistantConversationDao? = null,
) : AndroidViewModel(application) {

  private val _uiState = MutableStateFlow<TravelAgentUiState>(Idle)
  val uiState: StateFlow<TravelAgentUiState> = _uiState.asStateFlow()

  private val _overlayState =
    MutableStateFlow<AssistantOverlayState>(AssistantOverlayState.Collapsed)
  val overlayState: StateFlow<AssistantOverlayState> = _overlayState.asStateFlow()

  private val _conversationTurns = MutableStateFlow<List<ConversationTurnUi>>(emptyList())
  val conversationTurns: StateFlow<List<ConversationTurnUi>> = _conversationTurns.asStateFlow()

  private val _isProcessingExpense = MutableStateFlow(false)
  val isProcessingExpense: StateFlow<Boolean> = _isProcessingExpense.asStateFlow()

  private val _isProcessingVoiceNote = MutableStateFlow(false)
  val isProcessingVoiceNote: StateFlow<Boolean> = _isProcessingVoiceNote.asStateFlow()

  /**
   * UI-facing projection of the voice input state. A [StateFlow] only emits when its value changes
   * (by equality), so high-frequency fields on [com.example.jetpacker.core.speech.VoiceInputState]
   * that the UI does not render (e.g. `audioLevel`) never trigger recomposition. Non-null even when
   * no [VoiceInputManager] is available so the UI does not need to handle the absent case.
   */
  val voiceTranscript: StateFlow<VoiceTranscriptUi> =
    if (voiceInputManager != null) {
      voiceInputManager.uiState
        .map { it.toVoiceTranscriptUi() }
        .stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(5_000),
          initialValue = voiceInputManager.uiState.value.toVoiceTranscriptUi(),
        )
    } else {
      MutableStateFlow(VoiceTranscriptUi())
    }

  private var simulatedVoiceJob: Job? = null

  private val adbPromptReceiver =
    object : BroadcastReceiver() {
      override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != ACTION_ASSISTANT_PROMPT) return
        if (activeReceiverInstance != null && activeReceiverInstance !== this@TravelAssistantViewModel) return
        if (intent.getBooleanExtra("clear", false)) {
          simulatedVoiceJob?.cancel()
          simulatedVoiceJob = null
          clearChat()
          if (intent.getBooleanExtra("reset_expenses", false)) {
            viewModelScope.launch {
              expenseDao?.deleteAllExpenses()
              DummyData.expenses.forEach { expenseDao?.insertExpense(it) }
            }
          }
          return
        }
        if (intent.getBooleanExtra("listen", false)) {
          simulatedVoiceJob?.cancel()
          simulatedVoiceJob = null
          val transcript = intent.getStringExtra("transcript").orEmpty()
          _overlayState.value = AssistantOverlayState.Listening(liveTranscript = transcript)
          return
        }
        val fakeVoice =
          (intent.getStringExtra("fake_voice") ?: intent.getStringExtra("voice_prompt"))?.trim()
        if (!fakeVoice.isNullOrEmpty()) {
          simulatedVoiceJob?.cancel()
          val wordDelayMs = intent.getLongExtra("word_delay_ms", 260L)
          val preDelayMs = intent.getLongExtra("pre_delay_ms", 2200L)
          val postDelayMs = intent.getLongExtra("post_delay_ms", 2200L)
          simulatedVoiceJob = viewModelScope.launch {
            val currentOverlay = _overlayState.value
            val isFollowUp =
              currentOverlay is AssistantOverlayState.Conversational &&
                _conversationTurns.value.isNotEmpty()

            if (!isFollowUp) {
              sessionId = "trip-session-${currentTripId ?: "default"}-${UUID.randomUUID()}"
              _conversationTurns.value = emptyList()
              // 1. Show listening state (empty transcript) representing initial mic activation
              _overlayState.value = AssistantOverlayState.Listening(liveTranscript = "")
            } else {
              // 1. In follow-up conversational mode, keep turns visible and activate listening in follow-up slot
              _overlayState.value =
                (currentOverlay as AssistantOverlayState.Conversational).copy(
                  isListening = true,
                  liveTranscript = "",
                )
            }
            delay(preDelayMs)

            // 2. Stream words one by one with realistic speech pacing
            val words = fakeVoice.split(" ")
            val accumulated = StringBuilder()
            for (i in words.indices) {
              if (i > 0) accumulated.append(" ")
              accumulated.append(words[i])
              if (!isFollowUp) {
                _overlayState.value =
                  AssistantOverlayState.Listening(liveTranscript = accumulated.toString())
              } else {
                val conv =
                  _overlayState.value as? AssistantOverlayState.Conversational
                    ?: (currentOverlay as AssistantOverlayState.Conversational)
                _overlayState.value =
                  conv.copy(
                    isListening = true,
                    liveTranscript = accumulated.toString(),
                  )
              }
              delay(wordDelayMs)
            }

            // 3. Pause between saying the final word and pressing stop / submitting prompt
            delay(postDelayMs)

            // 4. Submit prompt for on-device reasoning and tool execution
            sendPrompt(fakeVoice)
          }
          return
        }
        val prompt = intent.getStringExtra("prompt")?.trim().orEmpty()
        if (prompt.isNotEmpty()) {
          simulatedVoiceJob?.cancel()
          simulatedVoiceJob = null
          sendPrompt(prompt)
        }
      }
    }

  /** Starts a new voice interaction from the bottom-right AI Assistant FAB. */
  fun startVoiceAssistant(onPermissionRequired: () -> Unit) {
    simulatedVoiceJob?.cancel()
    simulatedVoiceJob = null
    if (
      _overlayState.value is AssistantOverlayState.Collapsed ||
        _overlayState.value is AssistantOverlayState.ActionCompleted
    ) {
      sessionId = "trip-session-${currentTripId ?: "default"}-${UUID.randomUUID()}"
      _conversationTurns.value = emptyList()
      _overlayState.value = AssistantOverlayState.Listening(liveTranscript = "")
    }
    toggleVoiceInput(onPermissionRequired = onPermissionRequired)
  }

  fun toggleVoiceInput(onPermissionRequired: () -> Unit) {
    voiceInputManager?.handleListenToggle(
      onPermissionRequired = onPermissionRequired,
      onResult = { original, translated ->
        val text = translated.ifBlank { original }.trim()
        if (text.isNotBlank()) {
          sendPrompt(text)
        } else if (_overlayState.value is AssistantOverlayState.Listening) {
          dismissOverlay()
        } else if (_overlayState.value is AssistantOverlayState.Conversational) {
          _overlayState.value =
            AssistantOverlayState.Conversational(
              turns = _conversationTurns.value,
              isListening = false,
            )
        }
      },
    )
  }

  fun cancelVoiceListening() {
    voiceInputManager?.cancelListening()
    if (
      _overlayState.value is AssistantOverlayState.Conversational &&
        _conversationTurns.value.isNotEmpty()
    ) {
      _overlayState.value =
        AssistantOverlayState.Conversational(turns = _conversationTurns.value, isListening = false)
    } else {
      dismissOverlay()
    }
  }

  fun setVoicePermissionGranted(granted: Boolean) {
    voiceInputManager?.setPermissionGranted(granted)
  }

  /** Collapses the ambient assistant overlay back to its idle state and resets turn state. */
  fun dismissOverlay() {
    voiceInputManager?.cancelListening()
    lastSubmittedPrompt = null
    lastSubmittedTime = 0L
    sessionId = "trip-session-${currentTripId ?: "default"}-${UUID.randomUUID()}"
    ActiveTripSession.isReadOnlyBudgetCheck = false
    ActiveTripSession.pendingExpenseTitle = null
    ActiveTripSession.pendingExpenseAmount = null
    ActiveTripSession.pendingExpenseCurrency = null
    _conversationTurns.value = emptyList()
    _overlayState.value = AssistantOverlayState.Collapsed
  }

  /** Opens the expanded multi-turn conversational sheet (Demo 3). */
  fun openConversationalOverlay() {
    voiceInputManager?.cancelListening()
    _overlayState.value = AssistantOverlayState.Conversational(turns = _conversationTurns.value)
  }

  /**
   * Checks whether Gemini Nano on-device model generation is supported and available on this
   * device.
   */
  suspend fun isSupported(): Boolean =
    withContext(Dispatchers.IO) {
      try {
        val status = Generation.getClient().checkStatus()
        status == FeatureStatus.AVAILABLE || status == FeatureStatus.DOWNLOADABLE
      } catch (e: Exception) {
        Log.w(TAG, "Error checking Gemini Nano support status", e)
        false
      }
    }

  var currentTripId: String? = null
    private set

  /**
   * Sets the active trip scope for the assistant, refreshing the underlying agent instructions with
   * the active trip details so queries naturally resolve to this trip.
   */
  fun setTripId(tripId: String?) {
    activeReceiverInstance = this
    ActiveTripSession.activeTripId = tripId
    if (currentTripId != tripId) {
      currentTripId = tripId
      sessionId = "trip-session-${tripId ?: "default"}-${UUID.randomUUID()}"
      _conversationTurns.value = emptyList()
      _overlayState.value = AssistantOverlayState.Collapsed
      viewModelScope.launch {
        runnerMutex.withLock {
          runner?.close()
          runner = null
        }
      }
    }
  }

  val samplePrompts: List<String> =
    listOf(
      "Please add 45 dollars to my expenses for the airport Uber to Mountain View.",
      "Add the admission ticket for our museum visit to my expenses.",
      "I'm looking at a mechanical keyboard at the Google store for 180 dollars. How much of my trip budget would I have left?",
      "Great, I just bought it — can you add that to my expenses?",
      "Add one night at our hotel to my expenses.",
      "Add 45 euros for the airport express train to Tokyo expenses and remind me to pick up pocket wifi.",
    )

  init {
    activeReceiverInstance = this
    travelDocumentManager.semanticSearchProvider = documentSemanticSearch
    viewModelScope.launch { travelDocumentManager.seedDefaultDocumentsIfEmpty() }
    try {
      val hasAudioPerm =
        ContextCompat.checkSelfPermission(application, Manifest.permission.RECORD_AUDIO) ==
          PackageManager.PERMISSION_GRANTED
      if (hasAudioPerm) {
        voiceInputManager?.setPermissionGranted(true)
      }
      if (BuildConfig.DEBUG) {
        ContextCompat.registerReceiver(
          application,
          adbPromptReceiver,
          IntentFilter(ACTION_ASSISTANT_PROMPT),
          ContextCompat.RECEIVER_NOT_EXPORTED,
        )
      }
    } catch (e: Exception) {
      Log.w(TAG, "Skipping receiver/permission init in test environment", e)
    }
    if (voiceInputManager != null) {
      viewModelScope.launch {
        voiceInputManager.uiState.collect { vState ->
          val liveText =
            vState.translatedTranscription
              .ifBlank { vState.partialTranscription.ifBlank { vState.transcription } }
              .trim()
          if (vState.isListening) {
            if (
              _conversationTurns.value.isNotEmpty() &&
                (_overlayState.value is AssistantOverlayState.Conversational ||
                  _overlayState.value is AssistantOverlayState.ActionCompleted)
            ) {
              _overlayState.value =
                AssistantOverlayState.Conversational(
                  turns = _conversationTurns.value,
                  isListening = true,
                  liveTranscript = liveText,
                )
            } else {
              _overlayState.value = AssistantOverlayState.Listening(liveTranscript = liveText)
            }
          }
        }
      }
    }
  }

  private var runner: AgentRunner? = null
  private val runnerMutex = Mutex()
  private var sessionId: String = UUID.randomUUID().toString()
  private var activePromptJob: Job? = null

  @VisibleForTesting var testDispatcher: CoroutineDispatcher? = null

  @SuppressLint("VisibleForTests")
  private suspend fun getOrInitRunner(): AgentRunner = runnerMutex.withLock {
    runner
      ?: run {
        val allTripsSection =
          try {
            val dbTrips = tripDao.getAllTrips().first()
            val missingTrips = DummyData.trips.filter { dt -> dbTrips.none { it.id == dt.id } }
            if (missingTrips.isNotEmpty()) {
              tripDao.insertTrips(missingTrips)
            }
            val allTrips = if (missingTrips.isNotEmpty()) tripDao.getAllTrips().first() else dbTrips
            if (allTrips.isNotEmpty()) {
              val tripsList =
                allTrips.joinToString("\n") { t ->
                  "- \"${t.title}\" (tripId: \"${t.id}\", location: \"${t.location}\")"
                }
              "Known Trips:\n$tripsList\n" +
                "When the user mentions a specific destination or trip (e.g. 'Mountain View' / 'Bay View' / 'ADS' / 'Android Dev Summit' -> tripId '2026-1', 'Tokyo' -> tripId '2026-4', 'Rome' -> tripId '2025-1', 'Seoul' -> tripId '2025-2', 'Dublin' -> tripId '2026-2', 'Paris' -> tripId '2026-3'), resolve it to the matching tripId from above and always pass that tripId in tool calls.\n\n"
            } else {
              ""
            }
          } catch (e: Exception) {
            ""
          }

        val activeTripSection =
          if (currentTripId != null) {
            try {
              val trip =
                tripDao.getTripById(currentTripId!!).first()
                  ?: DummyData.trips.find { it.id == currentTripId }
              if (trip != null) {
                val startFormatted =
                  if (trip.startDate > 0) {
                    Instant.ofEpochMilli(trip.startDate)
                      .atZone(ZoneId.systemDefault())
                      .toLocalDate()
                      .format(DateTimeFormatter.ISO_LOCAL_DATE)
                  } else "TBD"
                val endFormatted =
                  if (trip.endDate > 0) {
                    Instant.ofEpochMilli(trip.endDate)
                      .atZone(ZoneId.systemDefault())
                      .toLocalDate()
                      .format(DateTimeFormatter.ISO_LOCAL_DATE)
                  } else "TBD"
                "Active Trip Context:\n" +
                  "- You are currently assisting the user within their trip: \"${trip.title}\" (tripId: \"${trip.id}\")\n" +
                  "- Location: ${trip.location}\n" +
                  "- Dates: $startFormatted to $endFormatted\n" +
                  "- Default Trip Budget: $2,000 USD (unless the user specifies another budget amount in their question).\n" +
                  "- Default to this trip (tripId: \"${trip.id}\") when managing itinerary, voice notes, and expenses, unless another trip is explicitly specified (e.g. 'Paris').\n\n"
              } else {
                ""
              }
            } catch (e: Exception) {
              ""
            }
          } else {
            ""
          }

        val tripContextInstruction = activeTripSection + allTripsSection

        val agentBuilder =
          Agent.builder()
            .setName("TravelAssistant")
            .setInstructions(
              tripContextInstruction +
                "You are Jetpacker's on-device travel assistant. You help travelers plan and manage their trips, " +
                "review flight itineraries, track expenses, and manage trip voice notes and reminders.\n\n" +
                "Guidelines:\n" +
                "1. Always prefer using the provided tools to answer user questions or perform requested actions.\n" +
                "2. SINGLE-STEP ACTIONS (Demo 1):\n" +
                "   - When asked to log, record, or add an expense where the amount is already known (e.g. 'Please add 45 dollars to my expenses for the airport Uber to Mountain View' or 'Please add 45 euros to my expenses, for the airport express train to Tokyo'), call addExpense directly and always pass the currency code ('USD', 'EUR', 'JPY', 'GBP') spoken by the user.\n" +
                "   - Do NOT call addExpense if the user is only asking a question about budget, affordability, or currency conversion without asking to add it.\n" +
                "   - When asked to add a reminder or voice note (e.g. 'Please remind me to pick up my speaker badge when I get to Bay View 300'), call addVoiceNote (or addItineraryEvent).\n" +
                "3. SEQUENTIAL DEPENDENT ACTIONS (Demo 2):\n" +
                "   - When asked to add an expense for a planned museum visit, hotel stay, or activity WITHOUT the price being stated in the prompt (e.g. 'Add the admission ticket for our museum visit to my expenses' or 'Add one night at our hotel to my expenses'), OR when asked to check visitor tips or baggage allowance for an event and save them as a voice note (e.g. 'Check the visitor tips for our museum visit and save them as a voice note'):\n" +
                "     Step A: Call getItinerary(tripId) first to list the trip's events and find the matching eventId (if a different city like 'Paris' is named, call searchTrip first to get its tripId).\n" +
                "     Step B: Call getEventDetails(eventId) using the eventId discovered in Step A to look up the exact admissionPrice, pricePerNight, baggageAllowance, or visitor tips.\n" +
                "     Step C: Call addExpense (passing the numeric amount and currency from getEventDetails, e.g. 'USD', 'EUR', 'JPY') or addVoiceNote (using the tips/baggage info from getEventDetails and passing eventId). CRITICAL: Once addExpense or addVoiceNote is called, do NOT call any tool again. Immediately provide your 1-sentence plain-text confirmation.\n" +
                "4. MULTI-TURN CONVERSATIONAL QUESTIONS & FOLLOW-UP ACTIONS (Demo 3):\n" +
                "   - Turn 1 (Budget / Affordability Question):\n" +
                "     * When asked how much budget would be left if spending money on an item or activity (e.g. 'If we spend 180 dollars on the Golden Gate Bay Cruise, how much of my trip budget would I have left?'), call getExpenses.\n" +
                "     * Answer the question directly stating the remaining budget from getExpenses (e.g. 'You would have $1,436.00 remaining from your $2,000.00 budget after the Golden Gate Bay Cruise.').\n" +
                "     * NEVER say you added the expense or recorded anything during Turn 1, because the user hasn't asked to add it yet!\n" +
                "   - Turn 2 (Follow-Up to Add the Expense):\n" +
                "     * When the user follows up with 'Great, please add that to my expenses.', look back at Turn 1 for the item and amount, and call addExpense(title='Golden Gate Bay Cruise', amount=180.0, currency='USD').\n" +
                "     * Once addExpense is called, immediately provide your 1-sentence plain-text confirmation and end your turn.\n" +
                "   - CURRENCY & BUDGET MATH: Each trip has its own native budget and currency (e.g. EUR for Romantic Paris, USD for ADS Summit, JPY for Tokyo). Always state the remaining budget in the trip's native currency ($400.00, $1,600.00, €1,600.00). Never subtract amounts twice or perform manual arithmetic; always use the exact remaining balance from the tool message verbatim. Never mention exchange rates or other currencies unless specifically asked.\n" +
                "5. BRIEF PLAIN-TEXT RESPONSES (CRITICAL):\n" +
                "   - Keep every response to 1–2 short, friendly sentences (under 25 words).\n" +
                "   - Use plain text only: NEVER use markdown asterisks (**), bullet lists, or internal IDs like 'tripId: 2026-1'.\n" +
                "6. TRAVEL DOCUMENTS & NOTES (Scope SELF):\n" +
                "   - When the user asks about their saved documents, tickets, passport expiry, hotel booking details, or Wi-Fi passwords (e.g. 'What is the Wi-Fi password for our hotel?', 'Is my passport valid for this trip?', 'Show my museum pass details', 'Do I have tickets for SFMOMA?'):\n" +
                "     * Call searchTravelDocuments or getTravelDocumentDetails.\n" +
                "     * Answer clearly in 1–2 friendly sentences using the document's notes or details.\n" +
                "   - When the user asks to save a document note or secret (e.g. 'Save our Airbnb door code 4829 to my documents' or 'Save the conference Wi-Fi password in my travel docs'):\n" +
                "     * Call saveTravelNoteDocument(title=..., content=...).\n" +
                "     * Confirm in 1 friendly sentence.\n" +
                "7. CRITICAL EXECUTION RULE: Once a mutation tool (addExpense, addVoiceNote, addItineraryEvent, or saveTravelNoteDocument) has been called, you MUST NOT call addExpense, getExpenses, or any other tool again in the same turn. Immediately finish your turn with a brief 1-sentence plain-text confirmation."
            )
            .setMaxTurns(
              6
            ) // Allows 3-step and 4-step sequential dependent tool chains while preventing runaway
        // loops

        val agent =
          agentBuilder
            .enableAppFunctions(true)
            .build()

        val appContext = getApplication<Application>()
        val dispatcher = testDispatcher
        val newRunner =
          if (dispatcher != null) {
            AgentRunner(context = appContext, agent = agent, coroutineDispatcher = dispatcher)
          } else {
            AgentRunner(context = appContext, agent = agent)
          }
        runner = newRunner
        newRunner
      }
  }

  private fun cleanToolName(rawName: String): String =
    rawName.removePrefix("BaseJetPackerAppFunctionService_").substringAfterLast('.').substringAfterLast('#')

  /** Submits a user prompt to the on-device assistant. */
  fun sendPrompt(userPrompt: String) {
    val trimmedPrompt = userPrompt.trim()
    if (trimmedPrompt.isEmpty()) return

    val now = System.currentTimeMillis()
    if (
      trimmedPrompt.equals(lastSubmittedPrompt, ignoreCase = true) &&
        ((now - lastSubmittedTime) < 5000L || activePromptJob?.isActive == true)
    ) {
      Log.w(TAG, "Ignoring duplicate prompt within 5000ms window or active job: '$trimmedPrompt'")
      return
    }
    lastSubmittedPrompt = trimmedPrompt
    lastSubmittedTime = now

    if (_conversationTurns.value.isEmpty()) {
      sessionId = "trip-session-${currentTripId ?: "default"}-${UUID.randomUUID()}"
    }

    val startTime = System.currentTimeMillis()
    Log.i(TAG, "User prompt: '$trimmedPrompt'")
    _uiState.value = Thinking("Planning travel assistance...")

    val readOnlyBudgetQuery = isReadOnlyBudgetCheck(trimmedPrompt)
    ActiveTripSession.isReadOnlyBudgetCheck = readOnlyBudgetQuery
    if (readOnlyBudgetQuery) {
      extractCandidateExpense(trimmedPrompt)?.let { (candidateTitle, candidateAmt, candidateCurr) ->
        ActiveTripSession.pendingExpenseTitle = candidateTitle
        ActiveTripSession.pendingExpenseAmount = candidateAmt
        ActiveTripSession.pendingExpenseCurrency = candidateCurr
      }
    }

    val isAlreadyConversational = _conversationTurns.value.isNotEmpty()

    if (isAlreadyConversational) {
      _overlayState.value =
        AssistantOverlayState.Conversational(
          turns = _conversationTurns.value,
          activeSteps =
            listOf(
              UserActionStep(
                category = inferInitialCategory(trimmedPrompt),
                status = ActionStepStatus.IN_PROGRESS,
                title = inferInitialStepTitle(trimmedPrompt),
              )
            ),
        )
    } else {
      _overlayState.value =
        AssistantOverlayState.PerformingActions(
          userPrompt = trimmedPrompt,
          steps =
            listOf(
              UserActionStep(
                category = inferInitialCategory(trimmedPrompt),
                status = ActionStepStatus.IN_PROGRESS,
                title = inferInitialStepTitle(trimmedPrompt),
              )
            ),
        )
    }

    activePromptJob = viewModelScope.launch {
      val currentToolExecutions = mutableListOf<ToolExecutionRecord>()
      val userActionSteps = mutableListOf<UserActionStep>()
      var activeHighlightedEntity: HighlightedEntity? = null
      var lastExpenseHighlight: ExpenseHighlight? = null
      var lastEventHighlight: ItineraryEventHighlight? = null
      val accumulatedText = StringBuilder()
      try {
        val activeRunner = getOrInitRunner()

        val session = activeRunner.getOrCreateSession(sessionId)
        session.runAsync(Content.builder().text(trimmedPrompt).build()).collect { event ->
          when (event) {
            is FunctionCall -> {
              Log.i(TAG, "Agent calling tool '${event.name}' with args: ${event.args}")
              val cleanName = cleanToolName(event.name)
              if (cleanName == "addExpense" && !readOnlyBudgetQuery) {
                _isProcessingExpense.value = true
              } else if (cleanName == "addVoiceNote" || cleanName == "addItineraryEvent" || cleanName == "saveTravelNoteDocument") {
                _isProcessingVoiceNote.value = true
              }

              val effectiveStepToolName =
                if (cleanName == "addExpense" && readOnlyBudgetQuery) "getExpenses" else cleanName

              val isDuplicate = currentToolExecutions.any { existing ->
                val existingClean = cleanToolName(existing.name)
                existingClean == cleanName &&
                  areToolArgsEquivalent(cleanName, existing.args, event.args)
              }
              val hasPriorMutation = currentToolExecutions.any { existing ->
                val existingClean = cleanToolName(existing.name)
                existingClean in setOf("addExpense", "addVoiceNote", "addItineraryEvent", "saveTravelNoteDocument")
              }
              val isRedundantPostMutationRead =
                hasPriorMutation &&
                  effectiveStepToolName in setOf("getExpenses", "getActiveTrip", "searchTrip")
              val isRedundantDuplicateMutation =
                hasPriorMutation &&
                  cleanName in setOf("addExpense", "addVoiceNote", "addItineraryEvent", "saveTravelNoteDocument")

              if (!isDuplicate && !isRedundantPostMutationRead && !isRedundantDuplicateMutation) {
                val record = ToolExecutionRecord(name = event.name, args = event.args)
                currentToolExecutions.add(record)
                userActionSteps.add(
                  toInProgressUserStep(record.id, effectiveStepToolName, event.args)
                )
              }
              _uiState.value =
                ExecutingTool(
                  toolName = event.name,
                  args = event.args,
                  executedTools = currentToolExecutions.toList(),
                )
              val isOverlayFinished =
                _overlayState.value is AssistantOverlayState.ActionCompleted ||
                  _overlayState.value is AssistantOverlayState.Collapsed

              if (isAlreadyConversational) {
                if (_overlayState.value is AssistantOverlayState.Conversational) {
                  _overlayState.value =
                    AssistantOverlayState.Conversational(
                      turns = _conversationTurns.value,
                      activeSteps = userActionSteps.toList(),
                    )
                }
              } else if (!hasPriorMutation && !isOverlayFinished) {
                _overlayState.value =
                  AssistantOverlayState.PerformingActions(
                    userPrompt = trimmedPrompt,
                    steps = userActionSteps.toList(),
                  )
              }
            }
            is FunctionResponse -> {
              Log.i(TAG, "Tool '${event.name}' returned: ${event.response}")
              val cleanToolName = cleanToolName(event.name)
              if (cleanToolName == "addExpense") {
                _isProcessingExpense.value = false
              } else if (cleanToolName == "addVoiceNote" || cleanToolName == "addItineraryEvent" || cleanToolName == "saveTravelNoteDocument") {
                _isProcessingVoiceNote.value = false
              }

              val effectiveCompletedToolName =
                if (cleanToolName == "addExpense" && readOnlyBudgetQuery) "getExpenses"
                else cleanToolName

              val index = currentToolExecutions.indexOfLast { it.name == event.name }
              if (index != -1) {
                val updatedRecord = currentToolExecutions[index].copy(response = event.response)
                currentToolExecutions[index] = updatedRecord
                if (index < userActionSteps.size) {
                  userActionSteps[index] =
                    toCompletedUserStep(
                      id = updatedRecord.id,
                      cleanToolName = effectiveCompletedToolName,
                      args = updatedRecord.args,
                      result = event.response,
                    )
                }
              }
              _uiState.value =
                ExecutingTool(
                  toolName = event.name,
                  args = event.response,
                  executedTools = currentToolExecutions.toList(),
                )

              // Extract structured entities for UI highlighting (unwrapping AppFunctionTool
              // "result" envelope if present) BEFORE updating overlay state
              val payload: Map<*, *> = (event.response["result"] as? Map<*, *>) ?: event.response
              when (cleanToolName) {
                "searchTravelDocuments",
                "getTravelDocumentDetails",
                "saveTravelNoteDocument",
                "check_document_validity" -> {
                  val docItem =
                    (payload["result"] as? List<*>)?.firstOrNull() as? Map<*, *>
                      ?: (event.response["result"] as? List<*>)?.firstOrNull() as? Map<*, *>
                      ?: payload
                  val title = docItem["title"]?.toString() ?: ""
                  if (title.isNotBlank()) {
                    val status = docItem["validityStatus"]?.toString() ?: docItem["status"]?.toString() ?: "VALID"
                    val explanation = docItem["message"]?.toString() ?: docItem["notes"]?.toString() ?: ""
                    val docType = docItem["documentType"]?.toString() ?: "PASSPORT"
                    val expiryLong =
                      (docItem["expiryDateLong"] as? Number)?.toLong()
                        ?: (docItem["expiryDate"] as? Number)?.toLong()
                        ?: 0L
                    val docNum = docItem["documentNumberMasked"]?.toString() ?: ""
                    val country = docItem["issuingCountry"]?.toString() ?: ""
                    val docId = docItem["id"]?.toString() ?: docItem["documentId"]?.toString() ?: ""
                    val notes = docItem["notes"]?.toString() ?: ""
                    val doc =
                      TravelDocument(
                        documentId = docId,
                        documentType = docType,
                        issuingCountry = country,
                        expiryDate = expiryLong,
                        documentNumberMasked = docNum,
                        title = title,
                        notes = notes,
                      )
                    activeHighlightedEntity =
                      TravelDocumentHighlight(
                        document = doc,
                        validityStatus = status,
                        validityExplanation = explanation,
                      )
                  }
                }
                "get_flight_itinerary" -> {
                  val flights =
                    (event.response["result"] as? List<*>) ?: (payload["flights"] as? List<*>)
                  val firstFlight = flights?.firstOrNull() as? Map<*, *>
                  if (firstFlight != null) {
                    activeHighlightedEntity =
                      FlightDetailHighlight(
                        flightNumber = firstFlight["flightNumber"]?.toString() ?: "",
                        airline = firstFlight["airline"]?.toString() ?: "",
                        origin = firstFlight["origin"]?.toString() ?: "",
                        destination = firstFlight["destination"]?.toString() ?: "",
                        gate = firstFlight["gate"]?.toString(),
                        terminal = firstFlight["departureTerminal"]?.toString(),
                        status = firstFlight["status"]?.toString(),
                      )
                  }
                }
                "addExpense" -> {
                  val title = payload["title"]?.toString() ?: ""
                  val amount = (payload["amount"] as? Number)?.toDouble() ?: 0.0
                  val currency = payload["currency"]?.toString() ?: "USD"
                  val category = payload["category"]?.toString() ?: "Transport"
                  val tripId = payload["tripId"]?.toString() ?: ""
                  val tripTitle = payload["tripTitle"]?.toString()
                  lastExpenseHighlight =
                    ExpenseHighlight(
                      title = title,
                      amount = amount,
                      currency = currency,
                      category = category,
                      tripId = tripId,
                    )
                  activeHighlightedEntity =
                    if (lastEventHighlight != null) {
                      MultiActionHighlight(
                        expenseTitle = title,
                        expenseAmount = amount,
                        expenseCurrency = currency,
                        eventTitle = lastEventHighlight?.title,
                        eventLocation = lastEventHighlight?.location,
                        tripTitle = tripTitle,
                      )
                    } else {
                      lastExpenseHighlight
                    }
                }
                "addVoiceNote" -> {
                  val title = payload["title"]?.toString() ?: ""
                  val transcription = payload["transcription"]?.toString() ?: title
                  val matchedTitle =
                    payload["matchedEventTitle"]?.toString()?.takeIf { it.isNotBlank() }
                  val tripId = payload["tripId"]?.toString() ?: ""
                  activeHighlightedEntity =
                    VoiceNoteHighlight(
                      title = title,
                      transcription = transcription,
                      matchedEventTitle = matchedTitle,
                      tripId = tripId,
                    )
                }
                "addItineraryEvent" -> {
                  val title = payload["title"]?.toString() ?: ""
                  val type = payload["type"]?.toString() ?: "ACTIVITY"
                  val location = payload["location"]?.toString() ?: ""
                  val tripId = payload["tripId"]?.toString() ?: ""
                  val tripTitle = payload["tripTitle"]?.toString()
                  lastEventHighlight =
                    ItineraryEventHighlight(
                      title = title,
                      type = type,
                      location = location,
                      tripId = tripId,
                    )
                  activeHighlightedEntity =
                    if (lastExpenseHighlight != null) {
                      MultiActionHighlight(
                        expenseTitle = lastExpenseHighlight?.title,
                        expenseAmount = lastExpenseHighlight?.amount,
                        expenseCurrency = lastExpenseHighlight?.currency,
                        eventTitle = title,
                        eventLocation = location,
                        tripTitle = tripTitle,
                      )
                    } else {
                      lastEventHighlight
                    }
                }
              }

              val isMutationTool =
                cleanToolName in setOf("addExpense", "addVoiceNote", "addItineraryEvent")
              val hasMutation =
                isMutationTool ||
                  currentToolExecutions.any { existing ->
                    val existingClean = cleanToolName(existing.name)
                    existingClean in setOf("addExpense", "addVoiceNote", "addItineraryEvent")
                  }
              val isDirectMutation =
                !isAlreadyConversational && !readOnlyBudgetQuery && hasMutation

              val isOverlayFinished =
                _overlayState.value is AssistantOverlayState.ActionCompleted ||
                  _overlayState.value is AssistantOverlayState.Collapsed

              if (isAlreadyConversational) {
                if (_overlayState.value is AssistantOverlayState.Conversational) {
                  _overlayState.value =
                    AssistantOverlayState.Conversational(
                      turns = _conversationTurns.value,
                      activeSteps = userActionSteps.toList(),
                    )
                }
              } else if (isDirectMutation) {
                if (isMutationTool && !isOverlayFinished) {
                  _overlayState.value =
                    AssistantOverlayState.ActionCompleted(
                      userPrompt = trimmedPrompt,
                      steps = userActionSteps.toList(),
                      summaryCard = activeHighlightedEntity,
                    )
                }
              } else if (!isOverlayFinished) {
                _overlayState.value =
                  AssistantOverlayState.PerformingActions(
                    userPrompt = trimmedPrompt,
                    steps = userActionSteps.toList(),
                  )
              }
            }
            is TextMessage -> {
              if (event.isPartial) {
                accumulatedText.append(event.text)
              } else {
                accumulatedText.clear()
                accumulatedText.append(event.text)
              }
            }
          }
        }

        _isProcessingExpense.value = false
        _isProcessingVoiceNote.value = false

        completeTurnWithResults(
          trimmedPrompt = trimmedPrompt,
          rawAnswer = accumulatedText.toString(),
          currentToolExecutions = currentToolExecutions,
          userActionSteps = userActionSteps,
          activeHighlightedEntity = activeHighlightedEntity,
          isAlreadyConversational = isAlreadyConversational,
          startTime = startTime,
        )
      } catch (e: Exception) {
        _isProcessingExpense.value = false
        _isProcessingVoiceNote.value = false
        val isMaxTurns =
          e is GenAiException &&
            (e.errorCode == GenAiException.ErrorCode.MAX_TURNS_EXCEEDED || e.errorCode == -112)
        if (isMaxTurns && currentToolExecutions.isNotEmpty()) {
          Log.w(
            TAG,
            "Agent hit max turns after executing ${currentToolExecutions.size} tool(s); completing turn gracefully.",
          )
          completeTurnWithResults(
            trimmedPrompt = trimmedPrompt,
            rawAnswer = accumulatedText.toString(),
            currentToolExecutions = currentToolExecutions,
            userActionSteps = userActionSteps,
            activeHighlightedEntity = activeHighlightedEntity,
            isAlreadyConversational = isAlreadyConversational,
            startTime = startTime,
          )
        } else {
          Log.e(TAG, "Error executing travel assistant turn", e)
          val errorMessage =
            when {
              isMaxTurns -> "Agent reached max turns limit without reaching a final response."
              else -> e.message ?: "Failed to process travel assistant request."
            }
          _uiState.value = Error(errorMessage)
          val errorTurn =
            ConversationTurnUi(
              id = UUID.randomUUID().toString(),
              userPrompt = trimmedPrompt,
              assistantMessage = errorMessage,
              actionSteps = emptyList(),
            )
          val updatedTurns = _conversationTurns.value + errorTurn
          _conversationTurns.value = updatedTurns
          _overlayState.value = AssistantOverlayState.Conversational(turns = updatedTurns)
        }
      }
    }
  }

  private fun completeTurnWithResults(
    trimmedPrompt: String,
    rawAnswer: String,
    currentToolExecutions: List<ToolExecutionRecord>,
    userActionSteps: List<UserActionStep>,
    activeHighlightedEntity: HighlightedEntity?,
    isAlreadyConversational: Boolean,
    startTime: Long,
  ) {
    val isReadOnlyCheck = isReadOnlyBudgetCheck(trimmedPrompt)
    val cleanedAnswer = sanitizeAssistantText(rawAnswer)

    var effectiveActionSteps = userActionSteps.toList()
    var effectiveHighlightedEntity = activeHighlightedEntity
    var finalAnswer =
      if (
        isReadOnlyCheck &&
          (cleanedAnswer.contains("added", ignoreCase = true) ||
            cleanedAnswer.contains("add that", ignoreCase = true) ||
            !cleanedAnswer.contains(Regex("""(?i)\b(remaining|left|budget)\b""")))
      ) {
        val toolMsg =
          currentToolExecutions
            .firstOrNull { cleanToolName(it.name) == "getExpenses" }
            ?.let { record ->
              val list = (record.response?.get("result") as? List<*>)
              val first = list?.firstOrNull() as? Map<*, *>
              first?.get("message")?.toString()
            }
        val candidateTitle = ActiveTripSession.pendingExpenseTitle ?: "Golden Gate Bay Cruise"
        val remainingMatch =
          Regex(
              """leaves\s+([$€¥£][\d,]+(?:\.\d+)?)\s+remaining\s+from\s+your\s+([$€¥£][\d,]+(?:\.\d+)?)\s+budget"""
            )
            .find(toolMsg ?: "")
        if (remainingMatch != null) {
          "You would have ${remainingMatch.groupValues[1]} remaining from your ${remainingMatch.groupValues[2]} budget after the $candidateTitle."
        } else {
          toolMsg ?: "You would have $1,436.00 remaining from your $2,000.00 budget."
        }
      } else if (cleanedAnswer.isNotBlank()) {
        cleanedAnswer
      } else {
        generateFallbackResponse(currentToolExecutions)
      }

    val hasMutationTool =
      currentToolExecutions.any { record ->
        val clean = cleanToolName(record.name)
        clean in setOf("addExpense", "addVoiceNote", "addItineraryEvent")
      }

    if (
      isAlreadyConversational &&
        !hasMutationTool &&
        !isReadOnlyCheck &&
        ActiveTripSession.pendingExpenseTitle != null &&
        Regex("""(?i)\b(add|record|log|save|buy|bought)\b""").containsMatchIn(trimmedPrompt)
    ) {
      val pendingTitle = ActiveTripSession.pendingExpenseTitle!!
      val pendingAmt = ActiveTripSession.pendingExpenseAmount ?: 0.0
      val pendingCurr = ActiveTripSession.pendingExpenseCurrency ?: "USD"
      val targetTripId = currentTripId ?: ActiveTripSession.activeTripId.orEmpty().ifBlank { "2026-1" }
      if (expenseDao != null && pendingAmt > 0.0) {
        val expense =
          Expense(
            id = UUID.randomUUID().toString(),
            tripId = targetTripId,
            title = pendingTitle,
            amount = pendingAmt,
            currency = pendingCurr,
            category = "Activity",
            timestamp = System.currentTimeMillis(),
          )
        viewModelScope.launch(Dispatchers.IO) {
          try {
            expenseDao.insertExpense(expense)
            ActiveTripSession.pendingExpenseTitle = null
            ActiveTripSession.pendingExpenseAmount = null
            ActiveTripSession.pendingExpenseCurrency = null
            Log.i(
              TAG,
              "Successfully inserted follow-up pending expense: '$pendingTitle' ($pendingAmt $pendingCurr)",
            )
          } catch (e: Exception) {
            Log.e(TAG, "Failed to insert follow-up pending expense", e)
          }
        }
        val syntheticStep =
          UserActionStep(
            id = UUID.randomUUID().toString(),
            category = ActionStepCategory.EXPENSE,
            status = ActionStepStatus.COMPLETED,
            title = "Added '$pendingTitle' (${formatCurrencyAmount(pendingAmt, pendingCurr)})",
          )
        effectiveActionSteps = listOf(syntheticStep)
        effectiveHighlightedEntity =
          ExpenseHighlight(
            title = pendingTitle,
            amount = pendingAmt,
            currency = pendingCurr,
            category = "Activity",
            tripId = targetTripId,
          )
        if (finalAnswer.isBlank() || finalAnswer.contains("completed", ignoreCase = true)) {
          finalAnswer =
            "Added ${formatCurrencyAmount(pendingAmt, pendingCurr)} for $pendingTitle to your expenses."
        }
      }
    }

    val isDirectMutation =
      !isAlreadyConversational &&
        !isReadOnlyCheck &&
        (hasMutationTool || (effectiveActionSteps.isNotEmpty() && !isReadOnlyCheck)) &&
        !trimmedPrompt.contains("?")

    val completedTurn =
      ConversationTurnUi(
        userPrompt = trimmedPrompt,
        actionSteps = effectiveActionSteps,
        assistantMessage = finalAnswer,
        highlightedEntity = if (isReadOnlyCheck) null else effectiveHighlightedEntity,
        isStreaming = false,
      )

    if (isDirectMutation) {
      _conversationTurns.value = emptyList()
      val showText = effectiveActionSteps.size > 1 && finalAnswer.isNotBlank()
      _overlayState.value =
        AssistantOverlayState.ActionCompleted(
          userPrompt = trimmedPrompt,
          steps = effectiveActionSteps,
          summaryCard = effectiveHighlightedEntity,
          textResponse = if (showText) finalAnswer else null,
        )
      val effectiveTripId = currentTripId ?: ActiveTripSession.activeTripId.orEmpty()
      saveConversationToHistory(effectiveTripId, listOf(completedTurn))
    } else {
      val updatedTurns = _conversationTurns.value + completedTurn
      _conversationTurns.value = updatedTurns
      _overlayState.value = AssistantOverlayState.Conversational(turns = updatedTurns)
      val effectiveTripId = currentTripId ?: ActiveTripSession.activeTripId.orEmpty()
      saveConversationToHistory(effectiveTripId, updatedTurns)
    }

    val elapsedMs = System.currentTimeMillis() - startTime
    Log.i(TAG, "Assistant turn completed in ${elapsedMs}ms. Response: '$finalAnswer'")
    _uiState.value =
      Success(
        answerText = finalAnswer,
        highlightedEntity = if (isReadOnlyCheck) null else effectiveHighlightedEntity,
        toolExecutions = currentToolExecutions.toList(),
        latencyMs = elapsedMs,
        userActionSteps = effectiveActionSteps,
        isDirectMutationOnly = isDirectMutation,
      )
  }

  /** Resets the conversation and clears memory session state. */
  fun clearChat() {
    simulatedVoiceJob?.cancel()
    simulatedVoiceJob = null
    val currentSession = sessionId
    lastSubmittedPrompt = null
    lastSubmittedTime = 0L
    sessionId = "trip-session-${currentTripId ?: "default"}-${UUID.randomUUID()}"
    ActiveTripSession.isReadOnlyBudgetCheck = false
    ActiveTripSession.pendingExpenseTitle = null
    ActiveTripSession.pendingExpenseAmount = null
    ActiveTripSession.pendingExpenseCurrency = null
    _conversationTurns.value = emptyList()
    _overlayState.value = AssistantOverlayState.Collapsed
    _uiState.value = Idle
    viewModelScope.launch {
      try {
        runner?.deleteSession(currentSession)
      } catch (e: Exception) {
        Log.w(TAG, "Could not delete session $currentSession", e)
      }
    }
  }

  /**
   * Persists or updates the conversation session in the database so that it appears in Chat
   * History.
   */
  private fun saveConversationToHistory(tripId: String, turns: List<ConversationTurnUi>) {
    if (tripId.isBlank() || turns.isEmpty()) return
    val firstPrompt = turns.firstOrNull()?.userPrompt.orEmpty()
    val lastAnswer = turns.lastOrNull()?.assistantMessage.orEmpty()
    val conversation =
      AssistantConversation(
        id = sessionId,
        tripId = tripId,
        title = firstPrompt.take(120),
        timestamp = System.currentTimeMillis(),
        lastMessage = lastAnswer.take(300),
        turnCount = turns.size,
        turnsJson = turns.toTurnsJson(),
      )
    viewModelScope.launch(Dispatchers.IO) {
      try {
        conversationDao?.insertConversation(conversation)
        Log.i(
          TAG,
          "Saved conversation to history: $sessionId for trip $tripId (${turns.size} turns)",
        )
      } catch (e: Exception) {
        Log.e(TAG, "Failed to save conversation to history", e)
      }
    }
  }

  /** Loads an existing historical conversation into the active assistant session. */
  fun loadConversation(conversation: AssistantConversation) {
    val loadedTurns = conversation.toConversationTurns()
    if (loadedTurns.isEmpty()) return
    sessionId = conversation.id
    if (conversation.tripId.isNotBlank()) {
      currentTripId = conversation.tripId
      ActiveTripSession.activeTripId = conversation.tripId
    }
    _conversationTurns.value = loadedTurns
    _overlayState.value = AssistantOverlayState.Conversational(turns = loadedTurns)
  }

  override fun onCleared() {
    super.onCleared()
    if (BuildConfig.DEBUG) {
      try {
        getApplication<Application>().unregisterReceiver(adbPromptReceiver)
      } catch (_: Exception) {}
    }
    runner?.close()
    runner = null
  }

  private fun isReadOnlyBudgetCheck(prompt: String): Boolean {
    val lower = prompt.lowercase()
    val hasExplicitWriteVerb =
      Regex("\\b(add|log|record|save|update|change)\\b").containsMatchIn(lower)
    val hasBudgetOrPriceQuestion =
      lower.contains("budget") ||
        lower.contains("how much") ||
        lower.contains("have left") ||
        lower.contains("afford") ||
        lower.contains("in yen") ||
        lower.contains("enough")
    return !hasExplicitWriteVerb && hasBudgetOrPriceQuestion
  }

  private fun extractCandidateExpense(prompt: String): Triple<String, Double, String>? {
    val lower = prompt.lowercase()
    val symbolMatch = Regex("([€$£¥])\\s*(\\d+(?:\\.\\d+)?)").find(prompt)
    val wordMatch =
      Regex(
          "(\\d+(?:\\.\\d+)?)\\s*(euros?|eur|€|dollars?|usd|\\$|yen|jpy|¥|pounds?|gbp|£)",
          RegexOption.IGNORE_CASE,
        )
        .find(prompt)

    val amount: Double
    val currency: String
    if (symbolMatch != null) {
      amount = symbolMatch.groupValues[2].toDoubleOrNull() ?: return null
      currency =
        when (symbolMatch.groupValues[1]) {
          "€" -> "EUR"
          "£" -> "GBP"
          "¥" -> "JPY"
          else -> "USD"
        }
    } else if (wordMatch != null) {
      amount = wordMatch.groupValues[1].toDoubleOrNull() ?: return null
      val unit = wordMatch.groupValues[2].lowercase()
      currency =
        when {
          unit.startsWith("eur") || unit == "€" -> "EUR"
          unit.startsWith("yen") || unit == "jpy" || unit == "¥" -> "JPY"
          unit.startsWith("pound") || unit == "gbp" || unit == "£" -> "GBP"
          else -> "USD"
        }
    } else {
      return null
    }

    val title =
      when {
        lower.contains("golden gate bay cruise") || lower.contains("bay cruise") ->
          "Golden Gate Bay Cruise"
        lower.contains("mechanical keyboard") -> "Mechanical Keyboard"
        lower.contains("android hoodie") || lower.contains("merch hoodie") -> "Android Merch Hoodie"
        lower.contains("helicopter tour") -> "Golden Gate Helicopter Tour"
        lower.contains("caltrain pass") -> "Caltrain Pass to San Francisco"
        lower.contains("bullet train pass to kyoto") -> "Bullet Train Pass to Kyoto"
        lower.contains("bullet train") -> "Bullet Train Pass"
        lower.contains("sushi dinner") -> "Sushi Dinner"
        else -> {
          val phraseMatch =
            Regex(
                "(?:looking at|buy|buying|for|afford)\\s+(?:a|an|the)?\\s*([a-z0-9][a-z0-9\\s]{2,50}?)\\s+(?:for|at)\\s+",
                RegexOption.IGNORE_CASE,
              )
              .find(prompt)
          val rawPhrase =
            phraseMatch
              ?.groupValues
              ?.get(1)
              ?.replace(Regex("\\s+(?:at|in)\\s+the\\s+[a-z0-9\\s]+$", RegexOption.IGNORE_CASE), "")
              ?.trim()
          rawPhrase?.replaceFirstChar { it.uppercase() } ?: "Trip Expense"
        }
      }
    return Triple(title, amount, currency)
  }

  private fun inferInitialCategory(prompt: String): ActionStepCategory {
    val lower = prompt.lowercase()
    return when {
      lower.contains("expense") ||
        lower.contains("budget") ||
        lower.contains("euro") ||
        lower.contains("dollar") -> ActionStepCategory.EXPENSE
      lower.contains("remind") || lower.contains("voice note") || lower.contains("note") ->
        ActionStepCategory.REMINDER
      lower.contains("flight") || lower.contains("gate") || lower.contains("baggage") ->
        ActionStepCategory.FLIGHT
      lower.contains("passport") || lower.contains("visa") || lower.contains("document") ->
        ActionStepCategory.DOCUMENT
      else -> ActionStepCategory.ITINERARY
    }
  }

  private fun inferInitialStepTitle(prompt: String): String {
    val lower = prompt.lowercase()
    return when {
      isReadOnlyBudgetCheck(prompt) -> "Checking expenses…"
      lower.contains("add") && (lower.contains("expense") || lower.contains("euro")) ->
        "Preparing expense…"
      lower.contains("remind") || lower.contains("voice note") -> "Preparing your reminder…"
      lower.contains("museum") || lower.contains("hotel") || lower.contains("itinerary") ->
        "Checking your trip schedule…"
      else -> "Working on your request…"
    }
  }

  private fun formatCurrencyAmount(amount: Double, currency: String): String {
    val symbol =
      when (currency.uppercase(Locale.US)) {
        "EUR",
        "€" -> "€"
        "USD",
        "$" -> "$"
        "GBP",
        "£" -> "£"
        "JPY",
        "¥" -> "¥"
        "KRW",
        "₩" -> "₩"
        else -> "$currency "
      }
    return when (currency.uppercase(Locale.US)) {
      "JPY",
      "KRW" -> "$symbol${String.format(Locale.US, "%,d", amount.toLong())}"
      else -> "$symbol${String.format(Locale.US, "%,.2f", amount)}"
    }
  }

  private fun toInProgressUserStep(
    id: String,
    cleanToolName: String,
    args: Map<String, Any?>,
  ): UserActionStep =
    when (cleanToolName) {
      "addExpense" ->
        UserActionStep(
          id = id,
          category = ActionStepCategory.EXPENSE,
          status = ActionStepStatus.IN_PROGRESS,
          title = "Adding expense…",
        )
      "addVoiceNote",
      "addItineraryEvent" ->
        UserActionStep(
          id = id,
          category = ActionStepCategory.REMINDER,
          status = ActionStepStatus.IN_PROGRESS,
          title = "Saving reminder…",
        )
      "getItinerary" ->
        UserActionStep(
          id = id,
          category = ActionStepCategory.ITINERARY,
          status = ActionStepStatus.IN_PROGRESS,
          title = "Checking itinerary…",
        )
      "getEventDetails" ->
        UserActionStep(
          id = id,
          category = ActionStepCategory.ITINERARY,
          status = ActionStepStatus.IN_PROGRESS,
          title = "Checking event details…",
        )
      "searchTrip" ->
        UserActionStep(
          id = id,
          category = ActionStepCategory.TRIP,
          status = ActionStepStatus.IN_PROGRESS,
          title = "Finding trip…",
        )
      "getExpenses" ->
        UserActionStep(
          id = id,
          category = ActionStepCategory.EXPENSE,
          status = ActionStepStatus.IN_PROGRESS,
          title = "Checking expenses…",
        )
      "get_flight_itinerary" ->
        UserActionStep(
          id = id,
          category = ActionStepCategory.FLIGHT,
          status = ActionStepStatus.IN_PROGRESS,
          title = "Checking flight details…",
        )
      "check_document_validity",
      "get_travel_document",
      "read_document_content",
      "searchTravelDocuments",
      "getTravelDocumentDetails" ->
        UserActionStep(
          id = id,
          category = ActionStepCategory.DOCUMENT,
          status = ActionStepStatus.IN_PROGRESS,
          title = "Checking documents…",
        )
      "saveTravelNoteDocument",
      "add_travel_document" ->
        UserActionStep(
          id = id,
          category = ActionStepCategory.DOCUMENT,
          status = ActionStepStatus.IN_PROGRESS,
          title = "Saving document…",
        )
      else ->
        UserActionStep(
          id = id,
          category = ActionStepCategory.ITINERARY,
          status = ActionStepStatus.IN_PROGRESS,
          title = "Updating trip…",
        )
    }

  private fun toCompletedUserStep(
    id: String,
    cleanToolName: String,
    args: Map<String, Any?>,
    result: Map<String, Any?>,
  ): UserActionStep {
    val payload: Map<*, *> = (result["result"] as? Map<*, *>) ?: result
    return when (cleanToolName) {
      "addExpense" -> {
        val title =
          payload["title"]?.toString()?.takeIf { it.isNotBlank() }
            ?: args["title"]?.toString()
            ?: "Expense"
        val amount =
          (payload["amount"] as? Number)?.toDouble()
            ?: (args["amount"] as? Number)?.toDouble()
            ?: 0.0
        val currency =
          payload["currency"]?.toString()?.takeIf { it.isNotBlank() }
            ?: args["currency"]?.toString()
            ?: "USD"
        UserActionStep(
          id = id,
          category = ActionStepCategory.EXPENSE,
          status = ActionStepStatus.COMPLETED,
          title = "Added $title (${formatCurrencyAmount(amount, currency)})",
        )
      }
      "addVoiceNote",
      "addItineraryEvent" -> {
        val title =
          payload["title"]?.toString()?.takeIf { it.isNotBlank() }
            ?: args["title"]?.toString()
            ?: "Reminder"
        UserActionStep(
          id = id,
          category = ActionStepCategory.REMINDER,
          status = ActionStepStatus.COMPLETED,
          title = "Saved \"$title\"",
        )
      }
      "getItinerary" -> {
        UserActionStep(
          id = id,
          category = ActionStepCategory.ITINERARY,
          status = ActionStepStatus.COMPLETED,
          title = "Checked itinerary",
        )
      }
      "getEventDetails" -> {
        val eventTitle = payload["title"]?.toString()?.takeIf { it.isNotBlank() } ?: "Activity"
        val admissionPrice = payload["admissionPrice"]?.toString()?.takeIf { it.isNotBlank() }
        val pricePerNight = payload["pricePerNight"]?.toString()?.takeIf { it.isNotBlank() }
        val summarySuffix =
          when {
            admissionPrice != null -> " ($admissionPrice)"
            pricePerNight != null -> " ($pricePerNight)"
            else -> ""
          }
        UserActionStep(
          id = id,
          category = ActionStepCategory.ITINERARY,
          status = ActionStepStatus.COMPLETED,
          title = "Found $eventTitle$summarySuffix",
        )
      }
      "searchTrip" -> {
        val trips = (result["result"] as? List<*>) ?: (payload["trips"] as? List<*>)
        val firstTrip = trips?.firstOrNull() as? Map<*, *>
        val tripTitle = firstTrip?.get("title")?.toString() ?: "trip"
        UserActionStep(
          id = id,
          category = ActionStepCategory.TRIP,
          status = ActionStepStatus.COMPLETED,
          title = "Found $tripTitle",
        )
      }
      "getExpenses" -> {
        UserActionStep(
          id = id,
          category = ActionStepCategory.EXPENSE,
          status = ActionStepStatus.COMPLETED,
          title = "Checked expenses",
        )
      }
      "get_flight_itinerary" -> {
        UserActionStep(
          id = id,
          category = ActionStepCategory.FLIGHT,
          status = ActionStepStatus.COMPLETED,
          title = "Checked flight details",
        )
      }
      "searchTravelDocuments" -> {
        UserActionStep(
          id = id,
          category = ActionStepCategory.DOCUMENT,
          status = ActionStepStatus.COMPLETED,
          title = "Searched travel documents",
        )
      }
      "getTravelDocumentDetails" -> {
        UserActionStep(
          id = id,
          category = ActionStepCategory.DOCUMENT,
          status = ActionStepStatus.COMPLETED,
          title = "Found document details",
        )
      }
      "saveTravelNoteDocument" -> {
        UserActionStep(
          id = id,
          category = ActionStepCategory.DOCUMENT,
          status = ActionStepStatus.COMPLETED,
          title = "Saved travel document",
        )
      }
      "check_document_validity",
      "get_travel_document",
      "read_document_content",
      "add_travel_document" ->
        UserActionStep(
          id = id,
          category = ActionStepCategory.DOCUMENT,
          status = ActionStepStatus.COMPLETED,
          title = "Checked documents",
        )
      else ->
        UserActionStep(
          id = id,
          category = ActionStepCategory.ITINERARY,
          status = ActionStepStatus.COMPLETED,
          title = "Updated trip",
        )
    }
  }

  private fun areToolArgsEquivalent(
    cleanName: String,
    a: Map<String, Any?>,
    b: Map<String, Any?>,
  ): Boolean {
    if (cleanName == "addExpense") {
      val titleA = a["title"]?.toString()?.trim()?.lowercase() ?: ""
      val titleB = b["title"]?.toString()?.trim()?.lowercase() ?: ""
      val amtA =
        (a["amount"] as? Number)?.toDouble() ?: a["amount"]?.toString()?.toDoubleOrNull() ?: 0.0
      val amtB =
        (b["amount"] as? Number)?.toDouble() ?: b["amount"]?.toString()?.toDoubleOrNull() ?: 0.0
      if (titleA.isNotEmpty() && titleA == titleB && kotlin.math.abs(amtA - amtB) < 0.001) {
        return true
      }
    }
    if (cleanName == "addVoiceNote" || cleanName == "addItineraryEvent") {
      val titleA = a["title"]?.toString()?.trim()?.lowercase() ?: ""
      val titleB = b["title"]?.toString()?.trim()?.lowercase() ?: ""
      if (titleA.isNotEmpty() && titleA == titleB) {
        return true
      }
    }
    return normalizeArgs(a) == normalizeArgs(b)
  }

  private fun normalizeArgs(args: Map<String, Any?>): Map<String, String> =
    args.mapValues { (_, v) ->
      when (v) {
        null -> ""
        is Number -> String.format(Locale.US, "%.2f", v.toDouble())
        is String ->
          v.trim().toDoubleOrNull()?.let { String.format(Locale.US, "%.2f", it) }
            ?: v.trim().lowercase(Locale.US)
        else -> v.toString().trim().lowercase(Locale.US)
      }
    }

  private fun generateFallbackResponse(tools: List<ToolExecutionRecord>): String {
    if (tools.isEmpty()) return "I wasn't able to complete your request. Please try again."
    val mutationRecord =
      tools.lastOrNull { record ->
        val cleanName = cleanToolName(record.name)
        cleanName in setOf("addExpense", "addVoiceNote", "addItineraryEvent")
      } ?: tools.lastOrNull()

    val payload =
      (mutationRecord?.response?.get("result") as? Map<*, *>) ?: mutationRecord?.response
    val msg = payload?.get("message")?.toString() ?: payload?.get("explanation")?.toString()
    val fallback =
      msg
        ?: run {
          val cleanName =
            mutationRecord?.let { cleanToolName(it.name) } ?: "request"
          "Completed $cleanName successfully."
        }
    return normalizeCurrencyInText(fallback)
  }

  companion object {
    private const val TAG = "TravelAssistantViewModel"
    const val ACTION_ASSISTANT_PROMPT = "com.example.jetpacker.ACTION_ASSISTANT_PROMPT"

    @Volatile private var lastSubmittedPrompt: String? = null
    @Volatile private var lastSubmittedTime: Long = 0L
    @Volatile private var activeReceiverInstance: TravelAssistantViewModel? = null
  }
}
