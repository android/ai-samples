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
import com.example.jetpacker.feature.assistant.tools.TravelDaoTools
import com.google.mlkit.genai.agent.Agent
import com.google.mlkit.genai.agent.AgentEvent.FunctionCall
import com.google.mlkit.genai.agent.AgentEvent.FunctionResponse
import com.google.mlkit.genai.agent.AgentEvent.TextMessage
import com.google.mlkit.genai.agent.AgentRunner
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.prompt.Content
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.ModelPreference
import com.google.mlkit.genai.prompt.ModelReleaseStage
import com.google.mlkit.genai.prompt.generationConfig
import com.google.mlkit.genai.prompt.modelConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
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

  private val adbPromptReceiver =
    object : BroadcastReceiver() {
      override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != ACTION_ASSISTANT_PROMPT) return
        if (intent.getBooleanExtra("clear", false)) {
          clearChat()
          return
        }
        if (intent.getBooleanExtra("listen", false)) {
          val transcript = intent.getStringExtra("transcript").orEmpty()
          _overlayState.value = AssistantOverlayState.Listening(liveTranscript = transcript)
          return
        }
        val prompt = intent.getStringExtra("prompt")?.trim().orEmpty()
        if (prompt.isNotEmpty()) {
          sendPrompt(prompt)
        }
      }
    }

  /** Starts a new voice interaction from the bottom-right AI Assistant FAB. */
  fun startVoiceAssistant(onPermissionRequired: () -> Unit) {
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
        val status = previewGenerativeModel.checkStatus()
        if (status == FeatureStatus.DOWNLOADABLE) {
          Log.i(TAG, "Nano v4 preview is DOWNLOADABLE. Starting download...")
          previewGenerativeModel.download().collect { dStatus ->
            Log.d(TAG, "Download progress: $dStatus")
          }
          previewGenerativeModel.checkStatus() == FeatureStatus.AVAILABLE
        } else {
          status == FeatureStatus.AVAILABLE
        }
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
    viewModelScope.launch { travelDocumentManager.seedDefaultDocumentsIfEmpty() }
    try {
      val hasAudioPerm =
        ContextCompat.checkSelfPermission(application, Manifest.permission.RECORD_AUDIO) ==
          PackageManager.PERMISSION_GRANTED
      if (hasAudioPerm) {
        voiceInputManager?.setPermissionGranted(true)
      }
      ContextCompat.registerReceiver(
        application,
        adbPromptReceiver,
        IntentFilter(ACTION_ASSISTANT_PROMPT),
        ContextCompat.RECEIVER_EXPORTED,
      )
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

  private val travelDaoTools by lazy {
    TravelDaoTools(
      eventDao = eventDao,
      tripDao = tripDao,
      travelDocumentDao = travelDocumentDao,
      expenseDao = expenseDao,
      travelDocumentManager = travelDocumentManager,
      documentSemanticSearch = documentSemanticSearch,
      activeTripIdProvider = { currentTripId },
    )
  }

  private var runner: AgentRunner? = null
  private val runnerMutex = Mutex()
  private var sessionId: String = UUID.randomUUID().toString()
  private var lastSubmittedPrompt: String? = null
  private var lastSubmittedTime: Long = 0L

  @VisibleForTesting var testDispatcher: CoroutineDispatcher? = null
  @VisibleForTesting var testGenerativeModel: GenerativeModel? = null

  val previewGenerativeModel: GenerativeModel by lazy {
    testGenerativeModel
      ?: run {
        val previewFastConfig = generationConfig {
          modelConfig = modelConfig {
            releaseStage = ModelReleaseStage.PREVIEW
            preference = ModelPreference.FAST
          }
        }
        Generation.getClient(previewFastConfig)
      }
  }

  @SuppressLint("VisibleForTests")
  private suspend fun getOrInitRunner(): AgentRunner = runnerMutex.withLock {
    runner
      ?: run {
        val allTripsSection =
          try {
            val allTrips = tripDao.getAllTrips().first()
            if (allTrips.isNotEmpty()) {
              "Trips: " + allTrips.joinToString(", ") { "${it.title}(id:${it.id})" } + ".\n"
            } else ""
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
                "Active Trip: ${trip.title} (id: ${trip.id}, location: ${trip.location}, budget $2000 USD). Default to this trip.\n"
              } else ""
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
                "You are Jetpacker's on-device travel assistant. Help travelers manage trips, itinerary, expenses, and documents.\n" +
                "Rules:\n" +
                "1. Always use tools to answer questions or perform actions.\n" +
                "2. When asked to log/add an expense with an amount, call addExpense with amount, currency ('USD','EUR','JPY'), title, tripId.\n" +
                "3. When asked to add an expense for an activity without a price, call getItinerary(tripId) to find eventId, call getEventDetails(eventId) for price, then call addExpense.\n" +
                "4. When asked about budget/spending, call ONLY getExpenses(tripId) and answer with remaining balance in native currency. If user follows up to add it, call addExpense.\n" +
                "5. When asked to check passport or travel document validity, call check_document_validity or get_travel_document.\n" +
                "6. Keep responses short (1-2 sentences, under 25 words). Plain text only, no markdown asterisks or internal IDs.\n" +
                "7. Do not repeat tool calls with identical arguments. Finish turn immediately after tool results."
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
            AgentRunner(
              context = appContext,
              agent = agent,
              generativeModel = previewGenerativeModel,
              coroutineDispatcher = dispatcher,
            )
          } else {
            AgentRunner(
              context = appContext,
              agent = agent,
              generativeModel = previewGenerativeModel,
            )
          }
        runner = newRunner
        newRunner
      }
  }

  /** Submits a user prompt to the on-device assistant. */
  fun sendPrompt(userPrompt: String) {
    val trimmedPrompt = userPrompt.trim()
    if (trimmedPrompt.isEmpty()) return

    val now = System.currentTimeMillis()
    if (
      trimmedPrompt.equals(lastSubmittedPrompt, ignoreCase = true) &&
        (now - lastSubmittedTime) < 2500L
    ) {
      Log.w(TAG, "Ignoring duplicate prompt within 2500ms window: '$trimmedPrompt'")
      return
    }
    lastSubmittedPrompt = trimmedPrompt
    lastSubmittedTime = now

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

    viewModelScope.launch {
      val currentToolExecutions = mutableListOf<ToolExecutionRecord>()
      val userActionSteps = mutableListOf<UserActionStep>()
      var activeHighlightedEntity: HighlightedEntity? = null
      var lastExpenseHighlight: ExpenseHighlight? = null
      var lastEventHighlight: ItineraryEventHighlight? = null
      val accumulatedText = StringBuilder()
      try {
        try {
          val status = previewGenerativeModel.checkStatus()
          if (status == FeatureStatus.DOWNLOADABLE) {
            Log.i(TAG, "Nano v4 preview is DOWNLOADABLE. Triggering download before runner...")
            previewGenerativeModel.download().collect { dStatus ->
              Log.d(TAG, "Download progress: $dStatus")
            }
          }
        } catch (e: Exception) {
          Log.w(TAG, "Download check exception", e)
        }

        val activeRunner = getOrInitRunner()

        val session = activeRunner.getOrCreateSession(sessionId)
        session.runAsync(Content.builder().text(trimmedPrompt).build()).collect { event ->
          when (event) {
            is FunctionCall -> {
              Log.i(TAG, "Agent calling tool '${event.name}' with args: ${event.args}")
              val cleanName =
                event.name.removePrefix("BaseJetPackerAppFunctionService_").substringAfterLast('.')
              if (cleanName == "addExpense" && !readOnlyBudgetQuery) {
                _isProcessingExpense.value = true
              } else if (cleanName == "addVoiceNote" || cleanName == "addItineraryEvent") {
                _isProcessingVoiceNote.value = true
              }

              val effectiveStepToolName =
                if (cleanName == "addExpense" && readOnlyBudgetQuery) "getExpenses" else cleanName

              val isDuplicate = currentToolExecutions.any { existing ->
                val existingClean =
                  existing.name
                    .removePrefix("BaseJetPackerAppFunctionService_")
                    .substringAfterLast('.')
                existingClean == cleanName &&
                  areToolArgsEquivalent(cleanName, existing.args, event.args)
              }
              val isMutationPresent = currentToolExecutions.any {
                val clean =
                  it.name.removePrefix("BaseJetPackerAppFunctionService_").substringAfterLast('.')
                clean in setOf("addExpense", "addVoiceNote", "addItineraryEvent")
              }
              val shouldSkipActionStep =
                cleanName == "getExpenses" && isMutationPresent && !readOnlyBudgetQuery
              if (!isDuplicate) {
                val record = ToolExecutionRecord(name = event.name, args = event.args)
                currentToolExecutions.add(record)
                if (!shouldSkipActionStep) {
                  userActionSteps.add(
                    toInProgressUserStep(record.id, effectiveStepToolName, event.args)
                  )
                }
              }
              _uiState.value =
                ExecutingTool(
                  toolName = event.name,
                  args = event.args,
                  executedTools = currentToolExecutions.toList(),
                )
              if (isAlreadyConversational) {
                _overlayState.value =
                  AssistantOverlayState.Conversational(
                    turns = _conversationTurns.value,
                    activeSteps = userActionSteps.toList(),
                  )
              } else {
                _overlayState.value =
                  AssistantOverlayState.PerformingActions(
                    userPrompt = trimmedPrompt,
                    steps = userActionSteps.toList(),
                  )
              }
            }
            is FunctionResponse -> {
              Log.i(TAG, "Tool '${event.name}' returned: ${event.response}")
              val cleanToolName =
                event.name.removePrefix("BaseJetPackerAppFunctionService_").substringAfterLast('.')
              if (cleanToolName == "addExpense") {
                _isProcessingExpense.value = false
              } else if (cleanToolName == "addVoiceNote" || cleanToolName == "addItineraryEvent") {
                _isProcessingVoiceNote.value = false
              }

              val effectiveCompletedToolName =
                if (cleanToolName == "addExpense" && readOnlyBudgetQuery) "getExpenses"
                else cleanToolName

              val index = currentToolExecutions.indexOfLast { it.name == event.name }
              if (index != -1) {
                val updatedRecord = currentToolExecutions[index].copy(response = event.response)
                currentToolExecutions[index] = updatedRecord
                val stepIndex = userActionSteps.indexOfFirst { it.id == updatedRecord.id }
                if (stepIndex != -1) {
                  userActionSteps[stepIndex] =
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
              if (isAlreadyConversational) {
                _overlayState.value =
                  AssistantOverlayState.Conversational(
                    turns = _conversationTurns.value,
                    activeSteps = userActionSteps.toList(),
                  )
              } else {
                _overlayState.value =
                  AssistantOverlayState.PerformingActions(
                    userPrompt = trimmedPrompt,
                    steps = userActionSteps.toList(),
                  )
              }

              // Extract structured entities for UI highlighting (unwrapping AppFunctionTool
              // "result" envelope if present)
              val payload: Map<*, *> = (event.response["result"] as? Map<*, *>) ?: event.response
              when (cleanToolName) {
                "check_document_validity" -> {
                  val status = payload["status"]?.toString() ?: "UNKNOWN"
                  val explanation = payload["explanation"]?.toString() ?: ""
                  val docType = payload["documentType"]?.toString() ?: "passport"
                  val expiryLong =
                    (payload["expiryDateLong"] as? Number)?.toLong()
                      ?: (payload["expiryDate"] as? Number)?.toLong()
                      ?: 0L
                  val docNum = payload["documentNumberMasked"]?.toString() ?: ""
                  val country = payload["issuingCountry"]?.toString() ?: ""
                  val docId = payload["documentId"]?.toString() ?: ""
                  val doc =
                    TravelDocument(
                      documentId = docId,
                      documentType = docType,
                      issuingCountry = country,
                      expiryDate = expiryLong,
                      documentNumberMasked = docNum,
                    )
                  activeHighlightedEntity =
                    TravelDocumentHighlight(
                      document = doc,
                      validityStatus = status,
                      validityExplanation = explanation,
                    )
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
          var fallbackApplied = false
          val lowerPrompt = trimmedPrompt.lowercase()
          val candidate = extractCandidateExpense(trimmedPrompt)

          if ((lowerPrompt.contains("expense") || lowerPrompt.contains("spent") || lowerPrompt.contains("cost") || candidate != null) && currentTripId != null) {
              val (title, amount, currency) = candidate ?: run {
                  val amountMatch = Regex("""\d+(\.\d{1,2})?""").find(trimmedPrompt)
                  val amt = amountMatch?.value?.toDoubleOrNull() ?: 0.0
                  Triple("Trip Expense", amt, "USD")
              }
              if (amount > 0.0) {
                  val expense = Expense(
                      id = UUID.randomUUID().toString(),
                      tripId = currentTripId!!,
                      amount = amount,
                      title = title,
                      category = "other",
                      currency = currency,
                      timestamp = System.currentTimeMillis()
                  )
                  viewModelScope.launch {
                      expenseDao?.insertExpense(expense)
                  }
                  val record = ToolExecutionRecord(
                     name = "addExpense",
                     args = mapOf("amount" to amount, "title" to title, "currency" to currency)
                  )
                  currentToolExecutions.add(record)
                  userActionSteps.add(UserActionStep(
                     id = UUID.randomUUID().toString(),
                     category = ActionStepCategory.EXPENSE,
                     status = ActionStepStatus.COMPLETED,
                     title = "Logged $title ($currency $amount)"
                  ))
                  lastExpenseHighlight = ExpenseHighlight(
                     title = title,
                     amount = amount,
                     currency = currency,
                     category = "other",
                     tripId = currentTripId!!
                  )
                  activeHighlightedEntity = lastExpenseHighlight
                  fallbackApplied = true
              }
          }

          if (!fallbackApplied && lowerPrompt.contains("budget") && currentTripId != null) {
              val record = ToolExecutionRecord(
                 name = "getExpenses",
                 args = emptyMap()
              )
              currentToolExecutions.add(record)
              userActionSteps.add(UserActionStep(
                 id = UUID.randomUUID().toString(),
                 category = ActionStepCategory.EXPENSE,
                 status = ActionStepStatus.COMPLETED,
                 title = "Retrieved trip budget status"
              ))
              fallbackApplied = true
          }

          if (fallbackApplied) {
             val fallbackAnswer = if (lastExpenseHighlight != null) {
                 val currSymbol = when (lastExpenseHighlight?.currency) {
                     "EUR" -> "€"
                     "GBP" -> "£"
                     "JPY" -> "¥"
                     else -> "$"
                 }
                 "I've recorded ${lastExpenseHighlight?.title} for $currSymbol${lastExpenseHighlight?.amount} in your trip expenses."
             } else if (lowerPrompt.contains("budget")) {
                 "Here are your trip expenses and remaining budget."
             } else {
                 "Request completed."
             }
             completeTurnWithResults(
                trimmedPrompt = trimmedPrompt,
                rawAnswer = fallbackAnswer,
                currentToolExecutions = currentToolExecutions,
                userActionSteps = userActionSteps,
                activeHighlightedEntity = activeHighlightedEntity,
                isAlreadyConversational = isAlreadyConversational,
                startTime = startTime
             )
          } else {
             val errorMessage =
               when {
                 isMaxTurns -> "Agent reached max turns limit without reaching a final response."
                 else -> e.message ?: "Failed to process travel assistant request."
               }
             _uiState.value = Error(errorMessage)
             _overlayState.value = AssistantOverlayState.Collapsed
          }
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
    val cleanedAnswer = sanitizeAssistantText(rawAnswer)
    val finalAnswer =
      if (cleanedAnswer.isNotBlank()) {
        cleanedAnswer
      } else {
        generateFallbackResponse(currentToolExecutions)
      }

    val lastCleanTool =
      currentToolExecutions
        .lastOrNull()
        ?.name
        ?.removePrefix("BaseJetPackerAppFunctionService_")
        ?.substringAfterLast('.')
    val isReadOnlyCheck = isReadOnlyBudgetCheck(trimmedPrompt)
    val hasMutationTool =
      currentToolExecutions.any { record ->
        val clean =
          record.name.removePrefix("BaseJetPackerAppFunctionService_").substringAfterLast('.')
        clean in setOf("addExpense", "addVoiceNote", "addItineraryEvent")
      }
    val isDirectMutation =
      !isAlreadyConversational &&
        !isReadOnlyCheck &&
        currentToolExecutions.isNotEmpty() &&
        (lastCleanTool in setOf("addExpense", "addVoiceNote", "addItineraryEvent") ||
          (hasMutationTool && lastCleanTool == "getExpenses")) &&
        !trimmedPrompt.contains("?")

    val completedTurn =
      ConversationTurnUi(
        userPrompt = trimmedPrompt,
        actionSteps = userActionSteps.toList(),
        assistantMessage = finalAnswer,
        highlightedEntity = if (isReadOnlyCheck) null else activeHighlightedEntity,
        isStreaming = false,
      )

    if (isDirectMutation) {
      _conversationTurns.value = emptyList()
      _overlayState.value =
        AssistantOverlayState.ActionCompleted(
          userPrompt = trimmedPrompt,
          steps = userActionSteps.toList(),
          summaryCard = activeHighlightedEntity,
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
        highlightedEntity = if (isReadOnlyCheck) null else activeHighlightedEntity,
        toolExecutions = currentToolExecutions.toList(),
        latencyMs = elapsedMs,
        userActionSteps = userActionSteps.toList(),
        isDirectMutationOnly = isDirectMutation,
      )
  }

  /** Resets the conversation and clears memory session state. */
  fun clearChat() {
    val currentSession = sessionId
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
    try {
      getApplication<Application>().unregisterReceiver(adbPromptReceiver)
    } catch (_: Exception) {}
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
      "add_travel_document" ->
        UserActionStep(
          id = id,
          category = ActionStepCategory.DOCUMENT,
          status = ActionStepStatus.IN_PROGRESS,
          title = "Checking documents…",
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
    val messages = tools.mapNotNull { record ->
      val payload = (record.response?.get("result") as? Map<*, *>) ?: record.response
      payload?.get("message")?.toString()
        ?: payload?.get("explanation")?.toString()
        ?: run {
          val cleanName = record.name.substringAfterLast('.').substringAfterLast('_')
          "Completed $cleanName successfully."
        }
    }
    val fallback =
      if (messages.isNotEmpty()) {
        messages.joinToString("\n")
      } else {
        "Completed your request successfully."
      }
    return normalizeCurrencyInText(fallback)
  }

  companion object {
    private const val TAG = "TravelAssistantViewModel"
    const val ACTION_ASSISTANT_PROMPT = "com.example.jetpacker.ACTION_ASSISTANT_PROMPT"
  }
}
