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

package com.example.jetpacker.feature.booking_assistant

import android.util.Log
import androidx.a2ui.compose.runtime.A2uiMessageParser
import androidx.a2ui.compose.ui.A2uiMessageProcessor as createA2uiMessageProcessor
import androidx.a2ui.model.processor.A2uiMessageProcessor
import androidx.a2ui.model.processor.A2uiSurfaceModel
import androidx.a2ui.model.processor.processInput
import androidx.a2ui.model.protocol.A2uiClientEventMessage
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.a2ui.model.protocol.A2uiCreateSurfaceMessage
import androidx.a2ui.model.protocol.A2uiUpdateComponentsMessage
import androidx.a2ui.model.protocol.A2uiUpdateDataModelMessage
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jetpacker.data.itinerary.EventDao
import com.example.jetpacker.data.itinerary.EventType
import com.example.jetpacker.data.itinerary.FlightDetail
import com.example.jetpacker.data.itinerary.TimelineEvent
import com.example.jetpacker.data.trips.DummyData
import com.example.jetpacker.data.trips.Trip
import com.example.jetpacker.data.trips.TripDao
import com.example.jetpacker.feature.booking_assistant.domain.BookingAssistantUiState
import com.example.jetpacker.feature.booking_assistant.domain.BookingEventNormalizer
import com.example.jetpacker.feature.booking_assistant.domain.BookingIntentClassifier
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.BufferedReader
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

data class BookingChatMessage(
  val id: String,
  val isUser: Boolean,
  val text: String,
  val surfaceId: String? = null,
)

data class BookableJourneyItem(
  val id: String,
  val title: String,
  val subtitle: String,
  val ctaText: String,
  val prompt: String,
  val intent: String,
  val targetTitle: String,
)

// Configure with your deployed Cloud Run / API Gateway URL, or "http://10.0.2.2:8000" / "http://localhost:8000" for local testing.
private const val DEFAULT_BACKEND_URL = "https://YOUR_BACKEND_GATEWAY_URL/v2"

@HiltViewModel
class BookingAssistantViewModel
@Inject
constructor(
  private val savedStateHandle: SavedStateHandle,
  private val eventDao: EventDao,
  private val tripDao: TripDao,
) : ViewModel() {

  private val _uiState = MutableStateFlow(BookingAssistantUiState())
  val uiState: StateFlow<BookingAssistantUiState> = _uiState.asStateFlow()

  private val a2uiParser = A2uiMessageParser()
  private val messageProcessor: A2uiMessageProcessor =
    createA2uiMessageProcessor(catalogs = listOf(bookingAssistantCatalog()))

  private var tripEvents: List<TimelineEvent> = emptyList()
  private var currentTripId: String? = null
  private var activeStreamJob: Job? = null
  private var activeStreamCall: Call? = null
  private var streamIdleJob: Job? = null

  private val client = OkHttpClient.Builder().readTimeout(0, TimeUnit.MILLISECONDS).build()

  private var sessionId: String? = null
  private var streamToken: String? = null
  private var streamUrl: String? = null

  init {
    viewModelScope.launch { messageProcessor.collectMessages() }
    viewModelScope.launch {
      messageProcessor.activeSurfaces.collect { surfaces ->
        _uiState.update { it.copy(activeSurfaces = surfaces) }
      }
    }
    viewModelScope.launch {
      messageProcessor.outboundEvents.collect { message ->
        if (message is A2uiClientEventMessage) {
          handleClientEvent(message)
        }
      }
    }
  }

  fun initializeChat(tripId: String, forceReset: Boolean = false) {
    if (currentTripId == tripId && !forceReset) return
    currentTripId = tripId

    viewModelScope.launch {
      val loadedTrip = tripDao.getTripById(tripId).first()
      val rawEvents = eventDao.getEventsForTrip(tripId).first()
      tripEvents = BookingEventNormalizer.normalizeBookableEvents(rawEvents)

      val flightItems =
        tripEvents
          .filter { it.type == EventType.TRANSPORTATION }
          .map { event ->
            BookableJourneyItem(
              id = event.id,
              title = event.title,
              subtitle = "Flight booking pending",
              ctaText = "Select seat",
              prompt = "Book flight ${event.title}",
              intent = "flight",
              targetTitle = event.title,
            )
          }

      val activityItems =
        tripEvents
          .filter {
            it.type == EventType.ACTIVITY ||
              it.type == EventType.CULTURE ||
              it.type == EventType.FOOD_AND_DRINK ||
              it.type == EventType.ACCOMMODATION
          }
          .map { event ->
            val cta =
              when (event.type) {
                EventType.ACCOMMODATION -> "Select room"
                EventType.FOOD_AND_DRINK -> "Reserve table"
                else -> "Book tickets"
              }
            val intent =
              when (event.type) {
                EventType.ACCOMMODATION -> "hotel"
                EventType.FOOD_AND_DRINK -> "dining"
                else -> "activity"
              }
            BookableJourneyItem(
              id = event.id,
              title = event.title,
              subtitle = event.location.ifEmpty { "Reservation pending" },
              ctaText = cta,
              prompt = "Book ${event.title}",
              intent = intent,
              targetTitle = event.title,
            )
          }

      _uiState.update {
        it.copy(
          trip = loadedTrip,
          flightJourneyItems = flightItems,
          activityJourneyItems = activityItems,
          chatMessages =
            if (forceReset) emptyList()
            else it.chatMessages.ifEmpty {
              listOf(
                BookingChatMessage(
                  id = "welcome",
                  isUser = false,
                  text =
                    "Hi! I'm your Trip Concierge. Let's finish booking your flights, stays, and activities for ${loadedTrip?.title ?: "your trip"}.",
                )
              )
            },
        )
      }
    }
  }

  fun sendChatMessage(
    prompt: String,
    intentOverride: String? = null,
    targetTitleOverride: String? = null,
  ) {
    val cleanPrompt = prompt.trim()
    log("sendChatMessage: prompt='$cleanPrompt'")
    if (cleanPrompt.isEmpty() || _uiState.value.isStreaming) {
      log("sendChatMessage: early return (empty=${cleanPrompt.isEmpty()}, streaming=${_uiState.value.isStreaming})")
      return
    }

    val userMsg =
      BookingChatMessage(
        id = System.currentTimeMillis().toString(),
        isUser = true,
        text = cleanPrompt,
      )

    _uiState.update { it.copy(chatMessages = it.chatMessages + userMsg, isStreaming = true) }
    resetStreamIdleTimer()

    viewModelScope.launch(Dispatchers.IO) {
      val sId = sessionId ?: "session_${System.currentTimeMillis()}".also { sessionId = it }
      val directMatch =
        tripEvents.find {
          it.title.equals(cleanPrompt, ignoreCase = true) ||
            cleanPrompt.contains(it.title, ignoreCase = true)
        }
      val resolvedIntent =
        BookingIntentClassifier.resolveIntent(cleanPrompt, directMatch, intentOverride)
      val resolvedTarget =
        BookingIntentClassifier.resolveTargetTitle(
          cleanPrompt,
          resolvedIntent,
          tripEvents,
          directMatch,
          targetTitleOverride,
        )

      if (resolvedTarget.isNotEmpty()) {
        runCatching {
          val deleteJson =
            """{"version":"v0.9","deleteSurface":{"surfaceId":${JSONObject.quote(resolvedTarget)}}}"""
          messageProcessor.processInput(a2uiParser, deleteJson)
        }
      }

      try {
        connectToStream(
          sId = sId,
          command = "chat",
          prompt = cleanPrompt,
          intent = resolvedIntent,
          targetTitle = resolvedTarget,
        )
      } catch (e: Exception) {
        log("Error in sendChatMessage stream: ${e.message}")
        _uiState.update { it.copy(isStreaming = false) }
      }
    }
  }

  fun resetBookings() {
    val tripId = currentTripId ?: savedStateHandle["tripId"] ?: "2026-1"
    activeStreamJob?.cancel()
    activeStreamJob = null
    activeStreamCall?.cancel()
    activeStreamCall = null
    streamIdleJob?.cancel()
    streamIdleJob = null
    sessionId = null
    streamToken = null
    streamUrl = null
    _uiState.value.activeSurfaces.forEach { surface ->
      runCatching {
        val deleteJson =
          """{"version":"v0.9","deleteSurface":{"surfaceId":${JSONObject.quote(surface.id)}}}"""
        messageProcessor.processInput(a2uiParser, deleteJson)
      }
    }
    _uiState.update { it.copy(isStreaming = false, activeSurfaces = emptyList()) }
    initializeChat(tripId, forceReset = true)
  }

  private suspend fun connectToStream(
    sId: String,
    command: String = "start",
    prompt: String = "",
    intent: String = "",
    targetTitle: String = "",
  ) {
    val itineraryJson = JSONArray()
    for (event in tripEvents) {
      val activeTrip = _uiState.value.trip
      itineraryJson.put(
        BookingEventNormalizer.buildEventJson(
          event = event,
          eventDao = eventDao,
          tripTitle = activeTrip?.title,
          tripLocation = activeTrip?.location,
        )
      )
    }

    val fbToken = getFirebaseAuthToken()
    log("connectToStream: fbToken length=${fbToken.length}")
    val tokenUrl = "$DEFAULT_BACKEND_URL/stream?auth_only=true"
    val tokenRequest =
      Request.Builder()
        .url(tokenUrl)
        .apply {
          if (fbToken.isNotEmpty()) addHeader("Authorization", "Bearer $fbToken")
        }
        .get()
        .build()

    client.newCall(tokenRequest).execute().use { tokenResponse ->
      log("connectToStream: tokenResponse code=${tokenResponse.code}")
      if (!tokenResponse.isSuccessful) {
        log("Failed to get stream token: ${tokenResponse.code}")
        _uiState.update { it.copy(isStreaming = false) }
        return
      }
      val responseBody = tokenResponse.body?.string().orEmpty()
      val jsonResponse = JSONObject(responseBody)
      streamToken = jsonResponse.optString("token")
      streamUrl = jsonResponse.optString("url")
      log("connectToStream: streamToken=${streamToken?.take(10)}..., streamUrl=$streamUrl")
    }

    val payload = JSONObject()
    payload.put("app_name", "booking")
    payload.put("user_id", "user_id")
    payload.put("session_id", sId)
    payload.put("streaming", true)

    val messageJson = JSONObject()
    messageJson.put("command", command)
    if (prompt.isNotEmpty()) messageJson.put("prompt", prompt)
    if (intent.isNotEmpty()) messageJson.put("intent", intent)
    if (targetTitle.isNotEmpty()) messageJson.put("targetTitle", targetTitle)
    messageJson.put("itinerary", itineraryJson)

    val partsArr = JSONArray().apply { put(JSONObject().apply { put("text", messageJson.toString()) }) }
    payload.put("new_message", JSONObject().apply { put("parts", partsArr) })

    val st = streamToken ?: return
    val su = streamUrl ?: return

    val sseRequest =
      Request.Builder()
        .url(su)
        .addHeader("Accept", "text/event-stream")
        .addHeader("Authorization", "Bearer $st")
        .post(payload.toString().toRequestBody("application/json".toMediaType()))
        .build()

    activeStreamJob?.cancel()
    activeStreamCall?.cancel()
    activeStreamJob =
      viewModelScope.launch(Dispatchers.IO) {
        val call = client.newCall(sseRequest)
        activeStreamCall = call
        try {
          log("Opening SSE connection to $su")
          call.execute().use { response ->
            log("SSE connection established: code=${response.code}")
            if (!response.isSuccessful) {
              log("SSE error response: ${response.code}")
              _uiState.update { it.copy(isStreaming = false) }
              return@use
            }
            val reader = BufferedReader(response.body!!.charStream())
            var line: String?
            while (reader.readLine().also { line = it } != null) {
              if (line != null && line.startsWith("data: ")) {
                val data = line.removePrefix("data: ").trim()
                if (data.isNotEmpty() && data != "[DONE]") {
                  processSseChunk(data)
                }
              }
            }
          }
        } catch (e: Exception) {
          log("Stream reading error: ${e.message}")
        } finally {
          if (activeStreamCall === call) {
            activeStreamCall = null
          }
          log("SSE stream closed")
          _uiState.update { it.copy(isStreaming = false) }
        }
      }
  }

  private fun processSseChunk(chunk: String) {
    log("processSseChunk: received ${chunk.take(120)}...")
    resetStreamIdleTimer()
    runCatching {
      val json = JSONObject(chunk)
      val author = json.optString("author")
      val contentObj = json.optJSONObject("content")
      val partsArr = contentObj?.optJSONArray("parts")

      if (partsArr != null) {
        for (i in 0 until partsArr.length()) {
          val part = partsArr.getJSONObject(i)
          val text = part.optString("text", "").trim()
          if (text.isEmpty()) continue

          if (author == "__assistant__" || text.contains("\"chatMessage\"")) {
            runCatching {
              val obj = JSONObject(text)
              val chatMsg = obj.optString("chatMessage", "")
              val sId = obj.optString("surfaceId", "").ifEmpty { null }
              if (chatMsg.isNotEmpty()) {
                val msgId =
                  "assistant_${System.currentTimeMillis()}_${_uiState.value.chatMessages.size}"
                _uiState.update { state ->
                  state.copy(
                    chatMessages =
                      state.chatMessages +
                        BookingChatMessage(
                          id = msgId,
                          isUser = false,
                          text = chatMsg,
                          surfaceId = sId,
                        )
                  )
                }
              }
            }.onFailure {
              val msgId =
                "assistant_${System.currentTimeMillis()}_${_uiState.value.chatMessages.size}"
              _uiState.update { state ->
                state.copy(
                  chatMessages =
                    state.chatMessages +
                      BookingChatMessage(id = msgId, isUser = false, text = text)
                )
              }
            }
          }

          val directives = extractA2uiDirectives(text)
          for (directive in directives) {
            messageProcessor.processInput(a2uiParser, directive)
          }
        }
      } else {
        if (json.has("content")) {
          val text = json.optString("content")
          if (text.isNotBlank()) {
            val msgId = json.optString("id", System.currentTimeMillis().toString())
            _uiState.update { state ->
              val existing = state.chatMessages.find { it.id == msgId }
              val updatedMessages =
                if (existing != null) {
                  state.chatMessages.map { if (it.id == msgId) it.copy(text = it.text + text) else it }
                } else {
                  state.chatMessages + BookingChatMessage(id = msgId, isUser = false, text = text)
                }
              state.copy(chatMessages = updatedMessages)
            }
          }
        }
        messageProcessor.processInput(a2uiParser, chunk)
      }
    }
  }

  private fun handleClientEvent(event: A2uiClientEventMessage) {
    val context = event.context
    val agentId = (context["agentId"] as? String) ?: event.surfaceId
    val sId = sessionId ?: return

    _uiState.update { it.copy(isStreaming = true) }
    resetStreamIdleTimer(5000L)

    viewModelScope.launch(Dispatchers.IO) {
      try {
        val json = JSONObject()
        json.put("agent_id", agentId)
        event.context.forEach { (k, v) -> json.put(k, v) }
        val seatVal = event.context["selectedSeat"] ?: event.context["seat"]
        if (seatVal != null) {
          json.put("seat", seatVal)
        }
        event.clientDataModel?.toPayloadMap()?.let { dm ->
          json.put("clientDataModel", JSONObject(dm))
        }

        val token = getFirebaseAuthToken()
        val encodedSessionId = URLEncoder.encode(sId, "UTF-8")
        val encodedAgentId = URLEncoder.encode(agentId, "UTF-8")
        val request =
          Request.Builder()
            .url("$DEFAULT_BACKEND_URL/respond?session_id=$encodedSessionId&agent_id=$encodedAgentId")
            .apply {
              if (token.isNotEmpty()) addHeader("Authorization", "Bearer $token")
            }
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
          if (!response.isSuccessful) {
            log("Error sending respond POST: ${response.code}")
          }
        }
      } catch (e: Exception) {
        log("Error sending response: ${e.message}")
      }
    }
  }

  private fun resetStreamIdleTimer(timeoutMs: Long = 8000L) {
    streamIdleJob?.cancel()
    streamIdleJob =
      viewModelScope.launch {
        delay(timeoutMs)
        _uiState.update { it.copy(isStreaming = false) }
      }
  }

  override fun onCleared() {
    super.onCleared()
    streamIdleJob?.cancel()
    activeStreamJob?.cancel()
    activeStreamCall?.cancel()
  }

  private suspend fun getFirebaseAuthToken(): String {
    return runCatching {
        var user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
          log("FirebaseAuth: currentUser is null, performing anonymous sign-in...")
          FirebaseAuth.getInstance().signInAnonymously().await()
          user = FirebaseAuth.getInstance().currentUser
        }
        val token = user?.getIdToken(false)?.await()?.token.orEmpty()
        log("FirebaseAuth: retrieved token (length ${token.length})")
        token
      }
      .onFailure {
        log("FirebaseAuth: failed to get token: ${it.message}")
      }
      .getOrDefault("")
  }

  private fun log(message: String) {
    Log.i("BookingAssistantVM", message)
  }

  companion object {
    internal fun extractA2uiDirectives(text: String): List<String> {
      val trimmed = text.trim()
      if (!trimmed.contains("\"createSurface\"") &&
        !trimmed.contains("\"updateComponents\"") &&
        !trimmed.contains("\"updateDataModel\"") &&
        !trimmed.contains("\"deleteSurface\"")
      ) {
        return emptyList()
      }
      return if (trimmed.startsWith("[")) {
        runCatching {
          val array = JSONArray(trimmed)
          (0 until array.length()).map { array.getJSONObject(it).toString() }
        }.getOrDefault(emptyList())
      } else {
        listOf(trimmed)
      }
    }
  }
}
