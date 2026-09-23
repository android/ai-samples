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
import androidx.a2ui.compose.ui.A2uiMessageProcessor
import androidx.a2ui.model.processor.A2uiMessageProcessor
import androidx.a2ui.model.processor.A2uiSurfaceModel
import androidx.a2ui.model.protocol.A2uiClientEventMessage
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.a2ui.model.protocol.A2uiCreateSurfaceMessage
import androidx.a2ui.model.protocol.A2uiUpdateComponentsMessage
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jetpacker.data.itinerary.EventDao
import com.example.jetpacker.data.itinerary.EventType
import com.example.jetpacker.data.itinerary.TimelineEvent
import com.example.jetpacker.data.trips.DummyData
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.BufferedReader
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

data class SurfaceStatus(
  val surfaceId: String,
  val type: String,
  val status: String,
)

@HiltViewModel
class BookingAssistantViewModel @Inject constructor(
  private val savedStateHandle: SavedStateHandle,
  private val eventDao: EventDao,
) : ViewModel() {

  private val messageProcessor: A2uiMessageProcessor = A2uiMessageProcessor(
    catalogs = listOf(bookingAssistantCatalog())
  )

  val activeSurfaces: StateFlow<List<A2uiSurfaceModel>> = messageProcessor.activeSurfaces

  private var tripEvents: List<TimelineEvent> = emptyList()
  private var currentTripId: String? = null
  private var activeStreamJob: Job? = null
  private var streamIdleJob: Job? = null

  // Set to true when testing against local server (http://localhost:8000)
  val useLocalServer: Boolean = false

  private val client = OkHttpClient.Builder().readTimeout(0, TimeUnit.MILLISECONDS).build()

  private var sessionId: String?
    get() = savedStateHandle["session_id"]
    set(value) {
      savedStateHandle["session_id"] = value
    }

  private var streamToken: String?
    get() = savedStateHandle["stream_token"]
    set(value) {
      savedStateHandle["stream_token"] = value
    }

  private var streamUrl: String?
    get() = savedStateHandle["stream_url"]
    set(value) {
      savedStateHandle["stream_url"] = value
    }

  private val _isStreaming = MutableStateFlow(false)
  val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

  private val _surfaceStatuses = MutableStateFlow<Map<String, SurfaceStatus>>(emptyMap())
  val surfaceStatuses: StateFlow<Map<String, SurfaceStatus>> = _surfaceStatuses.asStateFlow()

  private val _logs = MutableStateFlow<List<String>>(emptyList())
  val logs: StateFlow<List<String>> = _logs.asStateFlow()

  init {
    viewModelScope.launch {
      messageProcessor.collectMessages()
    }
    viewModelScope.launch {
      messageProcessor.outboundEvents.collect { message ->
        if (message is A2uiClientEventMessage) {
          handleClientEvent(message)
        }
      }
    }
  }

  private fun resetStreamIdleTimer(delayMillis: Long = 2500L) {
    streamIdleJob?.cancel()
    streamIdleJob = viewModelScope.launch {
      kotlinx.coroutines.delay(delayMillis)
      _isStreaming.value = false
    }
  }

  fun startBooking(tripId: String, forceRestart: Boolean = false) {
    if (currentTripId == tripId && activeStreamJob?.isActive == true && !forceRestart) return
    currentTripId = tripId
    savedStateHandle["tripId"] = tripId

    activeStreamJob?.cancel()
    streamIdleJob?.cancel()
    activeStreamJob = viewModelScope.launch(Dispatchers.IO) {
      try {
        _isStreaming.value = true
        log("Loading trip events for trip: $tripId")

        val dbEvents = runCatching { eventDao.getEventsForTrip(tripId).first() }.getOrNull() ?: emptyList()
        tripEvents = if (dbEvents.isNotEmpty()) {
          dbEvents
        } else {
          DummyData.events.filter { it.tripId == tripId }.ifEmpty {
            DummyData.events.take(4)
          }
        }

        // Initialize A2UI surfaces for each itinerary event
        tripEvents.forEach { event ->
          val cat = when (event.type) {
            EventType.TRANSPORTATION -> "Flight"
            EventType.ACCOMMODATION -> "Hotel"
            EventType.ACTIVITY, EventType.CULTURE -> "Activity"
            EventType.FOOD_AND_DRINK -> "Dining"
            else -> "Booking"
          }
          upsertSurface(
            surfaceId = event.title,
            payload = A2uiComponentPayload(
              id = "root",
              type = "BookingStatus",
              properties = mapOf(
                "category" to cat,
                "title" to event.title,
                "description" to (event.description?.ifEmpty { "Waiting for coordinator..." } ?: "Waiting for coordinator..."),
                "status" to "Queued",
              ),
            ),
          )
        }

        val sId = if (forceRestart) {
          System.currentTimeMillis().toString().also { sessionId = it }
        } else {
          sessionId ?: System.currentTimeMillis().toString().also { sessionId = it }
        }

        log("Surfaces ready for trip $tripId, sId=$sId. Connecting to stream...")
        connectToStream(sId)
        log("connectToStream completed.")
      } catch (e: Exception) {
        log("Error starting booking: ${e.message}")
        Log.e("BookingAssistant", "Exception in startBooking", e)
        _isStreaming.value = false
      }
    }
  }

  fun resetBookings() {
    val tripId = currentTripId ?: savedStateHandle["tripId"] ?: "2026-1"
    sessionId = null
    streamToken = null
    streamUrl = null
    streamIdleJob?.cancel()
    _isStreaming.value = false
    _surfaceStatuses.value = emptyMap()
    startBooking(tripId, forceRestart = true)
  }

  private fun upsertSurface(surfaceId: String, payload: A2uiComponentPayload) {
    val exists = messageProcessor.activeSurfaces.value.any { it.id == surfaceId }
    if (!exists) {
      messageProcessor.processMessage(
        A2uiCreateSurfaceMessage(
          surfaceId = surfaceId,
          catalogId = BOOKING_ASSISTANT_CATALOG_ID,
        )
      )
    }
    messageProcessor.processMessage(
      A2uiUpdateComponentsMessage(
        surfaceId = surfaceId,
        components = listOf(payload),
      )
    )
    val status = (payload.properties["status"] as? String).orEmpty()
    _surfaceStatuses.value = _surfaceStatuses.value + (
      surfaceId to SurfaceStatus(
        surfaceId = surfaceId,
        type = payload.type,
        status = status,
      )
    )
  }

  fun getCategory(title: String): String {
    val event = tripEvents.find { it.title.equals(title, ignoreCase = true) || it.id == title }
    return when (event?.type) {
      EventType.TRANSPORTATION -> "Flight"
      EventType.ACCOMMODATION -> "Hotel"
      EventType.ACTIVITY, EventType.CULTURE -> "Activity"
      EventType.FOOD_AND_DRINK -> "Dining"
      else -> when {
        title.contains("Flight", ignoreCase = true) ||
          title.contains("AMS", ignoreCase = true) ||
          title.contains("SFO", ignoreCase = true) -> "Flight"
        title.contains("Hotel", ignoreCase = true) ||
          title.contains("Check-in", ignoreCase = true) ||
          title.contains("Checkout", ignoreCase = true) ||
          title.contains("Check out", ignoreCase = true) -> "Hotel"
        title.contains("Restaurant", ignoreCase = true) ||
          title.contains("Dining", ignoreCase = true) ||
          title.contains("Bistro", ignoreCase = true) ||
          title.contains("Cafe", ignoreCase = true) -> "Dining"
        else -> "Activity"
      }
    }
  }

  private suspend fun connectToStream(sId: String) {
    val itineraryJson = JSONArray()
    val accommodationEvents = tripEvents.filter { it.type == EventType.ACCOMMODATION }
    val checkins = accommodationEvents.filter {
      it.title.contains("Check-in", ignoreCase = true) || it.title.contains("Check in", ignoreCase = true)
    }
    val checkouts = accommodationEvents.filter {
      it.title.contains("Checkout", ignoreCase = true) ||
        it.title.contains("Check-out", ignoreCase = true) ||
        it.title.contains("Check out", ignoreCase = true)
    }
    val matchedCheckouts = mutableSetOf<TimelineEvent>()
    val processedCheckins = mutableSetOf<TimelineEvent>()

    for (checkin in checkins) {
      val matchingCheckout = checkouts.find {
        it.location.equals(checkin.location, ignoreCase = true) && it.timestamp > checkin.timestamp
      }
      if (matchingCheckout != null) {
        matchedCheckouts.add(matchingCheckout)
        processedCheckins.add(checkin)
        val eventJson = JSONObject()
        eventJson.put("type", "ACCOMMODATION")
        val cleanTitle = checkin.location.trim()
        eventJson.put("title", if (cleanTitle.isNotEmpty()) cleanTitle else checkin.title)
        itineraryJson.put(eventJson)
      }
    }

    for (event in tripEvents) {
      if (event.type == EventType.ACCOMMODATION) {
        if (checkins.contains(event)) {
          if (!processedCheckins.contains(event)) {
            val eventJson = JSONObject()
            eventJson.put("type", event.type.name)
            eventJson.put("title", event.title)
            itineraryJson.put(eventJson)
          }
        } else if (checkouts.contains(event)) {
          if (!matchedCheckouts.contains(event)) {
            val eventJson = JSONObject()
            eventJson.put("type", event.type.name)
            eventJson.put("title", event.title)
            itineraryJson.put(eventJson)
          }
        } else {
          val eventJson = JSONObject()
          eventJson.put("type", event.type.name)
          eventJson.put("title", event.title)
          itineraryJson.put(eventJson)
        }
      } else {
        val eventJson = JSONObject()
        eventJson.put("type", event.type.name)
        eventJson.put("title", event.title)
        itineraryJson.put(eventJson)
      }
    }

    log("Starting connectToStream with sId: $sId, useLocalServer: $useLocalServer")

    if (useLocalServer) {
      streamToken = "local_dev_token"
      streamUrl = "http://localhost:8000/run_sse"
    } else {
      val fbToken = getFirebaseAuthToken()
      if (fbToken.isEmpty()) {
        log("Failed to obtain Firebase Auth token")
        _isStreaming.value = false
        return
      }

      val gatewayUrl = "https://jetset-gateway-38wzy18y.uc.gateway.dev/stream?auth_only=true"
      val tokenRequest = Request.Builder()
        .url(gatewayUrl)
        .addHeader("Authorization", "Bearer $fbToken")
        .get()
        .build()

      client.newCall(tokenRequest).execute().use { tokenResponse ->
        if (!tokenResponse.isSuccessful) {
          log("Failed to get stream token from gateway: ${tokenResponse.code}")
          _isStreaming.value = false
          return
        }
        val responseBody = tokenResponse.body?.string().orEmpty()
        val jsonResponse = JSONObject(responseBody)
        streamToken = jsonResponse.optString("token")
        streamUrl = jsonResponse.optString("url")
      }
    }

    val payload = JSONObject()
    payload.put("app_name", "booking")
    payload.put("user_id", "user_id")
    payload.put("session_id", sId)
    payload.put("streaming", true)

    val messageJson = JSONObject()
    messageJson.put("command", "start")
    messageJson.put("itinerary", itineraryJson)

    val partObj = JSONObject()
    partObj.put("text", messageJson.toString())
    val partsArr = JSONArray()
    partsArr.put(partObj)

    val newMessageObj = JSONObject()
    newMessageObj.put("parts", partsArr)
    payload.put("new_message", newMessageObj)

    val st = streamToken
    val su = streamUrl
    if (st.isNullOrEmpty() || su.isNullOrEmpty()) {
      log("Missing stream credentials")
      _isStreaming.value = false
      return
    }

    log("Connecting to SSE stream at $su...")
    val streamRequest = Request.Builder()
      .url(su)
      .addHeader("Accept", "text/event-stream")
      .addHeader("Authorization", "Bearer $st")
      .post(payload.toString().toRequestBody("application/json".toMediaType()))
      .build()

    client.newCall(streamRequest).execute().use { streamResponse ->
      if (!streamResponse.isSuccessful) {
        log("Stream connection failed with code: ${streamResponse.code}")
        _isStreaming.value = false
        return
      }

      log("Stream connected successfully! (code: ${streamResponse.code})")
      resetStreamIdleTimer(3000L)

      val source = streamResponse.body?.source() ?: return
      val reader = BufferedReader(source.inputStream().reader())
      var line: String?

      try {
        while (reader.readLine().also { line = it } != null) {
          val currentLine = line ?: continue
          if (currentLine.startsWith("data: ")) {
            val jsonStr = currentLine.substring(6)
            _isStreaming.value = true
            resetStreamIdleTimer(2000L)
            parseAndApplyEvent(jsonStr)
          }
        }
      } catch (e: Exception) {
        log("Stream reading interrupted: ${e.message}")
      } finally {
        streamIdleJob?.cancel()
        _isStreaming.value = false
      }
    }
  }

  private fun parseAndApplyEvent(jsonStr: String) {
    try {
      val json = JSONObject(jsonStr)
      val author = json.optString("author")
      if (author.isEmpty()) return

      val category = getCategory(author)
      val actionsJson = json.optJSONObject("actions")
      val endOfAgent = actionsJson?.optBoolean("endOfAgent", false) ?: false
      val contentJson = json.optJSONObject("content")
      val partsArr = contentJson?.optJSONArray("parts")

      var textContent = ""
      var uiType: String? = null
      var uiOptions = emptyList<String>()
      var uiMessage: String? = null

      if (partsArr != null) {
        for (i in 0 until partsArr.length()) {
          val part = partsArr.getJSONObject(i)
          if (part.has("text")) {
            val txt = part.getString("text")
            if (txt.contains("\"ui\":")) {
              try {
                val uiJson = JSONObject(txt)
                uiType = uiJson.optString("ui")
                uiMessage = uiJson.optString("message")
                val opts = uiJson.optJSONArray("options")
                if (opts != null) {
                  uiOptions = List(opts.length()) { opts.getString(it) }
                }
              } catch (e: Exception) {
                textContent += txt
              }
            } else {
              textContent += txt
            }
          }
        }
      }

      when {
        uiType == "time_selection" -> {
          upsertSurface(
            surfaceId = author,
            payload = A2uiComponentPayload(
              id = "root",
              type = "InteractiveOptionPicker",
              properties = mapOf(
                "title" to author,
                "category" to category,
                "status" to "CONFIRMATION REQUIRED",
                "prompt" to (uiMessage ?: "Flight Departure"),
                "options" to uiOptions,
                "selectedOption" to uiOptions.firstOrNull(),
                "confirmBtnText" to "Confirm Selection",
                "action" to mapOf(
                  "event" to mapOf(
                    "name" to "confirm_option",
                    "context" to mapOf(
                      "agentId" to author,
                      "selectionType" to "time",
                    ),
                  )
                ),
              ),
            ),
          )
        }

        uiType == "seat_map" || uiType == "seat_selection" -> {
          upsertSurface(
            surfaceId = author,
            payload = A2uiComponentPayload(
              id = "root",
              type = "SeatSelectionPicker",
              properties = mapOf(
                "title" to author,
                "category" to category,
                "status" to "ACTION REQUIRED",
                "prompt" to (uiMessage ?: "Seat Selection"),
                "options" to uiOptions.ifEmpty { listOf("1A", "1B", "2A", "2B") },
                "selectedSeat" to uiOptions.firstOrNull(),
                "confirmBtnText" to "Confirm Selection",
                "action" to mapOf(
                  "event" to mapOf(
                    "name" to "confirm_seat",
                    "context" to mapOf(
                      "agentId" to author,
                      "selectionType" to "seat",
                    ),
                  )
                ),
              ),
            ),
          )
        }

        uiType == "ticket_selection" -> {
          upsertSurface(
            surfaceId = author,
            payload = A2uiComponentPayload(
              id = "root",
              type = "InteractiveOptionPicker",
              properties = mapOf(
                "title" to author,
                "category" to category,
                "status" to "CONFIRMATION REQUIRED",
                "prompt" to (uiMessage ?: "Select party size:"),
                "options" to uiOptions.ifEmpty { listOf("1", "2", "3", "4+") },
                "selectedOption" to uiOptions.firstOrNull(),
                "confirmBtnText" to "Confirm Selection",
                "action" to mapOf(
                  "event" to mapOf(
                    "name" to "confirm_option",
                    "context" to mapOf(
                      "agentId" to author,
                      "selectionType" to "tickets",
                    ),
                  )
                ),
              ),
            ),
          )
        }

        textContent.contains("Please confirm", ignoreCase = true) ||
          textContent.contains("confirm reservation", ignoreCase = true) ||
          textContent.contains("confirm tickets", ignoreCase = true) -> {
          upsertSurface(
            surfaceId = author,
            payload = A2uiComponentPayload(
              id = "root",
              type = "BookingConfirmation",
              properties = mapOf(
                "category" to category,
                "title" to author,
                "status" to "CONFIRMATION REQUIRED",
                "prompt" to textContent.trim(),
                "confirmBtnText" to "Confirm Reservation",
                "action" to mapOf(
                  "event" to mapOf(
                    "name" to "confirm_booking",
                    "context" to mapOf(
                      "agentId" to author,
                      "selectionType" to "confirm",
                    ),
                  )
                ),
              ),
            ),
          )
        }

        endOfAgent ||
          textContent.contains("confirmed", ignoreCase = true) ||
          textContent.contains("secured", ignoreCase = true) -> {
          upsertSurface(
            surfaceId = author,
            payload = A2uiComponentPayload(
              id = "root",
              type = "BookingStatus",
              properties = mapOf(
                "category" to category,
                "title" to author,
                "description" to textContent.ifEmpty { "Booking confirmed." },
                "status" to "Confirmed",
              ),
            ),
          )
        }

        textContent.isNotEmpty() -> {
          upsertSurface(
            surfaceId = author,
            payload = A2uiComponentPayload(
              id = "root",
              type = "BookingStatus",
              properties = mapOf(
                "category" to category,
                "title" to author,
                "description" to textContent,
                "status" to "Processing",
              ),
            ),
          )
        }
      }
    } catch (e: Exception) {
      log("Error parsing SSE event: ${e.message}")
    }
  }

  private fun handleClientEvent(event: A2uiClientEventMessage) {
    val context = event.context
    val agentId = (context["agentId"] as? String) ?: event.surfaceId
    val selectionType = (context["selectionType"] as? String) ?: (context["type"] as? String).orEmpty()
    val selectedOption = context["selectedOption"] as? String
    val selectedSeat = context["selectedSeat"] as? String
    val sId = sessionId ?: return

    log("User interaction for $agentId ($selectionType): option=$selectedOption, seat=$selectedSeat")

    _isStreaming.value = true
    resetStreamIdleTimer(5000L)

    val category = getCategory(agentId)
    upsertSurface(
      surfaceId = agentId,
      payload = A2uiComponentPayload(
        id = "root",
        type = "BookingStatus",
        properties = mapOf(
          "category" to category,
          "title" to agentId,
          "description" to when (selectionType) {
            "time" -> "Selected $selectedOption. Confirming with provider..."
            "seat" -> "Selected seat $selectedSeat. Confirming assignment..."
            "tickets" -> "Selected party size $selectedOption. Securing reservation..."
            else -> "Processing confirmation..."
          },
          "status" to "Processing",
        ),
      ),
    )

    viewModelScope.launch(Dispatchers.IO) {
      try {
        val json = JSONObject()
        when (selectionType) {
          "time" -> json.put("time", selectedOption)
          "seat" -> json.put("seat", selectedSeat ?: selectedOption)
          "tickets" -> {
            json.put("tickets", selectedOption)
            json.put("people", selectedOption)
          }
          "confirm", "confirm_booking" -> json.put("confirmed", true)
          else -> {
            if (selectedOption != null) json.put("time", selectedOption)
            else if (selectedSeat != null) json.put("seat", selectedSeat)
            else json.put("confirmed", true)
          }
        }

        val token = getFirebaseAuthToken()
        val baseUrl = if (useLocalServer) "http://localhost:8000" else "https://jetset-gateway-38wzy18y.uc.gateway.dev"
        val encodedSessionId = URLEncoder.encode(sId, "UTF-8")
        val encodedAgentId = URLEncoder.encode(agentId, "UTF-8")
        val request = Request.Builder()
          .url("$baseUrl/respond?session_id=$encodedSessionId&agent_id=$encodedAgentId")
          .addHeader("Authorization", "Bearer $token")
          .post(json.toString().toRequestBody("application/json".toMediaType()))
          .build()

        client.newCall(request).execute().use { response ->
          if (!response.isSuccessful) {
            log("Error sending response: ${response.code}")
          } else {
            log("Response successfully delivered to server for $agentId")
          }
        }
      } catch (e: Exception) {
        log("Error sending response to server: ${e.message}")
      }
    }
  }

  private suspend fun getFirebaseAuthToken(): String {
    log("getFirebaseAuthToken: getting FirebaseAuth instance")
    val auth = FirebaseAuth.getInstance()
    var user = auth.currentUser
    log("getFirebaseAuthToken: current user is ${user?.uid ?: "null"}")
    if (user == null) {
      log("getFirebaseAuthToken: signing in anonymously...")
      user = try {
        auth.signInAnonymously().await().user
      } catch (e: Exception) {
        log("Firebase signInAnonymously failed: ${e.message}")
        Log.e("BookingAssistant", "Firebase signInAnonymously failed", e)
        null
      }
      log("getFirebaseAuthToken: signed in as ${user?.uid ?: "null"}")
    }
    return try {
      val token = user?.getIdToken(false)?.await()?.token.orEmpty()
      log("getFirebaseAuthToken: got token length=${token.length}")
      token
    } catch (e: Exception) {
      log("Firebase getIdToken failed: ${e.message}")
      Log.e("BookingAssistant", "Firebase getIdToken failed", e)
      ""
    }
  }

  fun getCategorySubtitle(surfaces: List<A2uiSurfaceModel>): String {
    val statuses = surfaces.map { _surfaceStatuses.value[it.id] }
    val hasActionRequired = statuses.any { statusInfo ->
      statusInfo != null && (
        statusInfo.type in listOf("InteractiveOptionPicker", "SeatSelectionPicker", "BookingConfirmation") ||
        statusInfo.status.contains("REQUIRED", ignoreCase = true) ||
        statusInfo.status.contains("ACTION", ignoreCase = true)
      )
    }
    if (hasActionRequired) {
      return "Action required"
    }

    val allConfirmed = statuses.isNotEmpty() && statuses.all { statusInfo ->
      statusInfo != null && (
        statusInfo.status.equals("Confirmed", ignoreCase = true) ||
        statusInfo.status.equals("Complete", ignoreCase = true) ||
        statusInfo.status.equals("Completed", ignoreCase = true)
      )
    }
    if (allConfirmed) {
      return if (surfaces.size > 1) "All confirmed" else "Confirmed"
    }

    val anyProcessing = statuses.any { statusInfo ->
      statusInfo != null && statusInfo.status.equals("Processing", ignoreCase = true)
    }
    if (anyProcessing) {
      return "Processing..."
    }

    val anyConfirmed = statuses.any { statusInfo ->
      statusInfo != null && statusInfo.status.equals("Confirmed", ignoreCase = true)
    }
    if (anyConfirmed) {
      val confirmedCount = statuses.count { it?.status.equals("Confirmed", ignoreCase = true) }
      return "$confirmedCount of ${surfaces.size} confirmed"
    }

    return "Queued"
  }

  private fun log(message: String) {
    Log.e("BookingAssistant", message)
    _logs.value = (_logs.value + message).takeLast(100)
  }
}
